package dev.podlink.system

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import dev.podlink.core.StemAction
import dev.podlink.core.TraceLog
import dev.podlink.core.WearSnapshot
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaBridge(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile private var pausedByWear = false
    @Volatile private var pausedBySpeech = false
    private var savedVolume: Int? = null
    private var fadeJob: Job? = null

    fun resetSession() {
        pausedByWear = false
        pausedBySpeech = false
        fadeJob?.cancel()
        savedVolume = null
    }

    fun onWearChanged(prev: WearSnapshot, next: WearSnapshot, autoPauseEnabled: Boolean) {
        if (!autoPauseEnabled) {
            pausedByWear = false
            return
        }
        val prevCount = prev.inEarCount
        val nextCount = next.inEarCount
        if (prevCount == nextCount) return

        val removed = (prevCount == 2 && nextCount < 2) || (prevCount == 1 && nextCount == 0)
        val inserted = (prevCount < 2 && nextCount == 2) || (prevCount == 0 && nextCount == 1)

        if (removed && audioManager?.isMusicActive == true) {
            dispatchKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
            pausedByWear = true
            TraceLog.record(TAG, "Auto-paused media ($prevCount -> $nextCount)")
        } else if (inserted && pausedByWear) {
            dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY)
            pausedByWear = false
            TraceLog.record(TAG, "Auto-resumed media ($prevCount -> $nextCount)")
        }
    }

    fun onSpeakingChanged(
        speaking: Boolean,
        duckVolume: Boolean,
        pauseMusic: Boolean,
        targetPercent: Int,
        relative: Boolean,
    ) {
        val manager = audioManager ?: return
        if (speaking) {
            if (duckVolume && savedVolume == null) {
                val cur = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                savedVolume = cur
                val base = if (relative) cur else manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val target = (base * (targetPercent.coerceIn(10, 85) / 100f)).roundToInt().coerceAtLeast(1)
                if (cur > target) fadeVolume(manager, cur, target)
            }
            if (pauseMusic && manager.isMusicActive) {
                dispatchKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
                pausedBySpeech = true
            }
        } else {
            savedVolume?.let { restore ->
                val cur = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                fadeVolume(manager, cur, restore)
                savedVolume = null
            }
            if (pauseMusic && pausedBySpeech) {
                dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY)
                pausedBySpeech = false
            }
        }
    }

    fun triggerAction(action: StemAction, onCycleNoiseControl: () -> Unit) {
        when (action) {
            StemAction.PLAY_PAUSE -> dispatchKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            StemAction.PREVIOUS_TRACK -> dispatchKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            StemAction.NEXT_TRACK -> dispatchKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            StemAction.CYCLE_NOISE_CONTROL -> onCycleNoiseControl()
            StemAction.DIGITAL_ASSISTANT -> runCatching {
                appContext.startActivity(
                    Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure { err ->
                TraceLog.record(TAG, "Voice command launch failed: ${err.message}")
            }
        }
    }

    private fun dispatchKey(keyCode: Int) {
        val manager = audioManager ?: return
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun fadeVolume(manager: AudioManager, from: Int, to: Int) {
        fadeJob?.cancel()
        if (from == to) return
        fadeJob = scope.launch {
            val step = if (to > from) 1 else -1
            var cur = from
            while (cur != to) {
                cur += step
                runCatching { manager.setStreamVolume(AudioManager.STREAM_MUSIC, cur, 0) }
                delay(FADE_STEP_MS)
            }
        }
    }

    private companion object {
        const val TAG = "MediaBridge"
        const val FADE_STEP_MS = 45L
    }
}
