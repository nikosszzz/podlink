package dev.podlink.system

import android.media.AudioManager
import android.media.ToneGenerator
import dev.podlink.core.MotionSample
import dev.podlink.core.MotionUiState
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HeadMotionEngine(
    private val scope: CoroutineScope,
    private val startStream: () -> Boolean,
    private val stopStream: () -> Boolean,
) {
    private val _motion = MutableStateFlow(MotionUiState())
    val motion: StateFlow<MotionUiState> = _motion.asStateFlow()

    private val detecting = AtomicBoolean(false)

    private val calLock = Any()
    private val calSamples = mutableListOf<Triple<Int, Int, Int>>()
    private var calibrated = false
    private var o2Neutral = 0
    private var o3Neutral = 0

    private val hBuf = Collections.synchronizedList(ArrayList<Double>())
    private val vBuf = Collections.synchronizedList(ArrayList<Double>())
    private val hAvg = Collections.synchronizedList(ArrayList<Double>())
    private val vAvg = Collections.synchronizedList(ArrayList<Double>())
    private val hPeaks = CopyOnWriteArrayList<Triple<Int, Double, Long>>()
    private val hTroughs = CopyOnWriteArrayList<Triple<Int, Double, Long>>()
    private val vPeaks = CopyOnWriteArrayList<Triple<Int, Double, Long>>()
    private val vTroughs = CopyOnWriteArrayList<Triple<Int, Double, Long>>()
    private val peakIntervals = Collections.synchronizedList(ArrayList<Double>())
    private val speedIntervals = Collections.synchronizedList(ArrayList<Long>())

    private var prevH = 0.0
    private var prevV = 0.0
    private var hInc: Boolean? = null
    private var vInc: Boolean? = null
    private var lastPeakAt = 0L
    private var lastToneAt = 0L
    private var detectJob: Job? = null
    private var callback: ((Boolean) -> Unit)? = null

    init {
        resetSmoothing()
    }

    fun onSample(sample: MotionSample) {
        var pitch = 0f
        var yaw = 0f
        var ready = false
        synchronized(calLock) {
            if (!calibrated) {
                calSamples.add(Triple(sample.o1, sample.o2, sample.o3))
                if (calSamples.size >= CAL_COUNT) {
                    o2Neutral = calSamples.map { it.second + OFFSET }.average().roundToInt()
                    o3Neutral = calSamples.map { it.third + OFFSET }.average().roundToInt()
                    calibrated = true
                }
            } else {
                val o2n = (sample.o2 + OFFSET) - o2Neutral
                val o3n = (sample.o3 + OFFSET) - o3Neutral
                pitch = (o2n + o3n) / 2f / 32000f * 180f
                yaw = (o2n - o3n) / 2f / 32000f * 180f
                ready = true
            }
        }
        if (!ready) return
        _motion.value = MotionUiState(pitch, yaw, sample.verticalAccel, sample.horizontalAccel)
        if (detecting.get()) {
            ingestAcceleration(sample.horizontalAccel.toInt(), sample.verticalAccel.toInt())
        }
    }

    fun resetOrientation() {
        synchronized(calLock) {
            calSamples.clear()
            calibrated = false
        }
        _motion.value = MotionUiState()
    }

    fun startGestureTest(keepStreamingAfter: Boolean = true, onResult: (Boolean) -> Unit) {
        if (!detecting.compareAndSet(false, true)) return
        callback = onResult
        clearGestureBuffers()
        startStream()

        detectJob = scope.launch(Dispatchers.Default) {
            while (isActive && detecting.get()) {
                delay(50)
                val verdict = evaluateGesture() ?: continue
                playTone(if (verdict) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_NACK, 120)
                val cb = callback
                withContext(Dispatchers.Main) {
                    cb?.invoke(verdict)
                    stopGestureTest(keepStreaming = keepStreamingAfter)
                }
                break
            }
        }
    }

    fun stopGestureTest(keepStreaming: Boolean = false) {
        if (!detecting.compareAndSet(true, false)) return
        if (!keepStreaming) stopStream()
        detectJob?.cancel()
        detectJob = null
        callback = null
    }

    private fun ingestAcceleration(horizontal: Int, vertical: Int) {
        if (abs(horizontal) > MAX_VALID || abs(vertical) > MAX_VALID) return
        val now = System.currentTimeMillis()
        val dh = horizontal - prevH
        val dv = vertical - prevV
        val sigH = abs(dh) > FEEDBACK_THRESHOLD
        val sigV = abs(dv) > FEEDBACK_THRESHOLD
        if (now - lastToneAt > 180L) {
            if (sigH && (!sigV || abs(dh) > abs(dv))) {
                playTone(ToneGenerator.TONE_PROP_ACK, 40)
                lastToneAt = now
            } else if (sigV) {
                playTone(ToneGenerator.TONE_PROP_BEEP, 40)
                lastToneAt = now
            }
        }
        prevH = horizontal.toDouble()
        prevV = vertical.toDouble()

        val sh = smooth(horizontal.toDouble(), hAvg)
        val sv = smooth(vertical.toDouble(), vAvg)
        synchronized(hBuf) {
            hBuf.add(sh)
            if (hBuf.size > 100) hBuf.removeAt(0)
        }
        synchronized(vBuf) {
            vBuf.add(sv)
            if (vBuf.size > 100) vBuf.removeAt(0)
        }
        if (hBuf.size >= 4 && vBuf.size >= 4) {
            trackAxis(hBuf, hInc, variance(hBuf.takeLast(4)), hPeaks, hTroughs)?.let { hInc = it }
            trackAxis(vBuf, vInc, variance(vBuf.takeLast(4)), vPeaks, vTroughs)?.let { vInc = it }
        }
    }

    private fun smooth(value: Double, window: MutableList<Double>): Double = synchronized(window) {
        window.add(value)
        if (window.size > 3) window.removeAt(0)
        window.average()
    }

    private fun trackAxis(
        buf: List<Double>,
        inc: Boolean?,
        varVal: Double,
        peaks: MutableList<Triple<Int, Double, Long>>,
        troughs: MutableList<Triple<Int, Double, Long>>,
    ): Boolean? {
        if (buf.size < 2) return inc
        val cur = buf.last()
        val prev = buf[buf.size - 2]
        var up = inc ?: (cur > prev)
        val thresh = max(50.0, min(SENSITIVITY.toDouble(), varVal / 3.0))
        val now = System.currentTimeMillis()
        if (up && cur < prev - thresh) {
            if (abs(prev) > PEAK_THRESHOLD) recordExtreme(buf.size - 1, prev, now, peaks)
            up = false
        } else if (!up && cur > prev + thresh) {
            if (abs(prev) > PEAK_THRESHOLD) recordExtreme(buf.size - 1, prev, now, troughs)
            up = true
        }
        return up
    }

    private fun recordExtreme(idx: Int, value: Double, now: Long, list: MutableList<Triple<Int, Double, Long>>) {
        list.add(Triple(idx, value, now))
        if (lastPeakAt > 0L) {
            val dt = now - lastPeakAt
            synchronized(peakIntervals) {
                peakIntervals.add(dt / 1000.0)
                if (peakIntervals.size > 5) peakIntervals.removeAt(0)
            }
            synchronized(speedIntervals) {
                speedIntervals.add(dt)
                if (speedIntervals.size > 5) speedIntervals.removeAt(0)
            }
        }
        lastPeakAt = now
    }

    private fun variance(vals: List<Double>): Double {
        if (vals.size <= 1) return 0.0
        val m = vals.average()
        return vals.map { (it - m) * (it - m) }.average()
    }

    private fun rhythmScore(): Double {
        if (peakIntervals.size < 2) return 0.0
        val mean = peakIntervals.average()
        if (mean == 0.0) return 0.0
        val err = peakIntervals.map { (it / mean - 1.0).pow(2) }.average()
        return max(0.0, 1.0 - min(1.0, err / RHYTHM_LIMIT))
    }

    private fun confidence(extremes: List<Triple<Int, Double, Long>>, vertical: Boolean): Double {
        val req = requiredExtremes()
        if (extremes.size < req) return 0.0
        val recent = extremes.sortedBy { it.first }.takeLast(req)
        val amp = min(1.0, recent.map { abs(it.second) }.average() / 600.0)
        val signs = recent.map { if (it.second > 0) 1 else -1 }
        val alt = if ((1 until signs.size).all { signs[it] != signs[it - 1] }) 1.0 else 0.5
        val other = (if (vertical) hBuf else vBuf).takeLast(recent.size * 2).map { abs(it) }.average().takeIf { !it.isNaN() } ?: 0.0
        val iso = min(1.0, recent.map { abs(it.second) }.average() / (other + 0.1) * 1.2)
        return amp * 0.4 + rhythmScore() * 0.2 + alt * 0.2 + iso * 0.2
    }

    private fun requiredExtremes(): Int =
        if (speedIntervals.isNotEmpty() && speedIntervals.average() < FAST_MS) MAX_EXTREMES else MIN_EXTREMES

    private fun evaluateGesture(): Boolean? {
        val req = requiredExtremes()
        if (vPeaks.size + vTroughs.size >= req) {
            val all = (vPeaks + vTroughs).sortedBy { it.first }
            if (confidence(all, vertical = true) >= CONFIDENCE_MIN) return true
        }
        if (hPeaks.size + hTroughs.size >= req) {
            val all = (hPeaks + hTroughs).sortedBy { it.first }
            if (confidence(all, vertical = false) >= CONFIDENCE_MIN) return false
        }
        return null
    }

    private fun playTone(tone: Int, ms: Int) {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
            tg.startTone(tone, ms)
            scope.launch {
                try {
                    delay(ms + 80L)
                } finally {
                    runCatching { tg.release() }
                }
            }
        }
    }

    private fun clearGestureBuffers() {
        hBuf.clear()
        vBuf.clear()
        hPeaks.clear()
        hTroughs.clear()
        vPeaks.clear()
        vTroughs.clear()
        peakIntervals.clear()
        speedIntervals.clear()
        resetSmoothing()
        prevH = 0.0
        prevV = 0.0
        hInc = null
        vInc = null
        lastPeakAt = 0L
    }

    private fun resetSmoothing() {
        hAvg.clear()
        vAvg.clear()
        repeat(3) {
            hAvg.add(0.0)
            vAvg.add(0.0)
        }
    }

    private companion object {
        const val CAL_COUNT = 10
        const val OFFSET = 5500
        const val FEEDBACK_THRESHOLD = 600
        const val SENSITIVITY = 150
        const val FAST_MS = 300.0
        const val MIN_EXTREMES = 3
        const val MAX_EXTREMES = 4
        const val MAX_VALID = 6000
        const val PEAK_THRESHOLD = 400
        const val RHYTHM_LIMIT = 0.5
        const val CONFIDENCE_MIN = 0.7
    }
}
