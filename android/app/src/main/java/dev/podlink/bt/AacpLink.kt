package dev.podlink.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import dev.podlink.core.AacpCodec
import dev.podlink.core.AacpPacket
import dev.podlink.core.TraceLog
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class AacpLink(
    val deviceAddress: String,
    private val adapter: BluetoothAdapter,
    private val device: BluetoothDevice,
    private val onConnected: () -> Unit,
    private val onPacket: (AacpPacket) -> Unit,
    private val onDisconnected: (Throwable?, Boolean) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val outgoing = Channel<ByteArray>(capacity = 64)
    private val closed = AtomicBoolean(false)

    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var hasData = false
    @Volatile private var hasBattery = false
    @Volatile private var handshakeAck = CompletableDeferred<Unit>()
    @Volatile private var loggedHeadTracking = false
    private var runner: Job? = null

    fun start() {
        runner = scope.launch {
            var error: Throwable? = null
            var hiddenBlocked = false
            try {
                val btSocket = L2capChannel.openSocket(adapter, device).getOrElse { err ->
                    hiddenBlocked = (err as? L2capChannel.OpenException)?.hiddenApiBlocked == true
                    throw err
                }
                socket = btSocket
                val t0 = System.currentTimeMillis()
                connectWithTimeout(btSocket)
                TraceLog.record(TAG, "Connected AACP ($deviceAddress) in ${System.currentTimeMillis() - t0}ms")
                onConnected()

                val writer = launch { writeLoop(btSocket) }
                launchInitSequence()
                readLoop(btSocket)
                writer.cancel()
            } catch (t: Throwable) {
                if (!closed.get()) {
                    TraceLog.record(TAG, "AACP closed ($deviceAddress): ${t.javaClass.simpleName}: ${t.message}")
                    error = t
                }
            } finally {
                closeSocket()
                outgoing.close()
                onDisconnected(error, hiddenBlocked)
                scope.cancel()
            }
        }
    }

    fun send(packet: ByteArray): Boolean = !closed.get() && outgoing.trySend(packet).isSuccess

    fun requestNotifications() {
        if (closed.get()) return
        TraceLog.record(TAG, "TX RequestNotifications (sync)")
        outgoing.trySend(AacpCodec.setFeatureFlags())
        outgoing.trySend(AacpCodec.requestNotifications())
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        closeSocket()
        outgoing.close()
        runner?.cancel()
        scope.cancel()
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectWithTimeout(btSocket: BluetoothSocket) = withContext(Dispatchers.IO) {
        runCatching { if (adapter.isDiscovering) adapter.cancelDiscovery() }
        try {
            withTimeout(CONNECT_TIMEOUT_MS) { btSocket.connect() }
        } catch (t: Throwable) {
            runCatching { btSocket.close() }
            throw t
        }
    }

    private fun CoroutineScope.launchInitSequence() {
        launch {
            handshakeAck = CompletableDeferred()
            hasData = false
            hasBattery = false
            outgoing.trySend(AacpCodec.handshake())
            val acked = withTimeoutOrNull(500L) { handshakeAck.await() } != null
            if (!isActive || closed.get()) return@launch
            TraceLog.record(TAG, if (acked) "HandshakeAck -> Init" else "HandshakeAck timeout -> Init")
            outgoing.trySend(AacpCodec.setFeatureFlags())
            delay(40)
            outgoing.trySend(AacpCodec.requestNotifications())
            outgoing.trySend(AacpCodec.requestProximityKeys())
            outgoing.trySend(AacpCodec.eqUnlock())

            val delays = longArrayOf(400L, 800L, 1500L, 2500L)
            for (wait in delays) {
                delay(wait)
                if (!isActive || closed.get() || hasBattery) break
                TraceLog.record(TAG, "TX retry Notify (hasData=$hasData, hasBattery=$hasBattery)")
                if (!hasData) {
                    outgoing.trySend(AacpCodec.handshake())
                    delay(40)
                }
                outgoing.trySend(AacpCodec.setFeatureFlags())
                outgoing.trySend(AacpCodec.requestNotifications())
                if (!hasData) outgoing.trySend(AacpCodec.eqUnlock())
            }
        }
    }

    private suspend fun writeLoop(btSocket: BluetoothSocket) {
        val out = btSocket.outputStream
        for (pkt in outgoing) {
            try {
                out.write(pkt)
                out.flush()
            } catch (e: IOException) {
                if (!closed.get()) throw e
                break
            }
        }
    }

    private fun readLoop(btSocket: BluetoothSocket) {
        val input = btSocket.inputStream
        val buf = ByteArray(READ_BUFFER)
        while (!closed.get()) {
            val read = input.read(buf)
            if (read <= 0) break
            val parsed = AacpCodec.parse(buf.copyOf(read))
            when (parsed) {
                AacpPacket.HandshakeAck -> handshakeAck.complete(Unit)
                is AacpPacket.BatteryInfo -> {
                    if (parsed.metrics.any { it.isAvailable }) hasBattery = true
                    hasData = true
                }
                is AacpPacket.Unknown -> Unit
                else -> hasData = true
            }
            if (parsed is AacpPacket.HeadTrackingSample) {
                if (!loggedHeadTracking) {
                    loggedHeadTracking = true
                    TraceLog.record(TAG, "RX HeadTracking stream active")
                }
            } else {
                TraceLog.record(TAG, summarize(parsed))
            }
            onPacket(parsed)
        }
    }

    private fun summarize(p: AacpPacket): String = when (p) {
        AacpPacket.HandshakeAck -> "RX HandshakeAck"
        is AacpPacket.BatteryInfo -> "RX Battery(${p.metrics.joinToString { "${it.slot.name}:${it.level}%(${it.state.name})" }})"
        is AacpPacket.ControlCommand -> "RX Ctrl(${p.id?.name ?: "0x%02X".format(p.rawId)}=${p.value.joinToString("") { "%02X".format(it) }})"
        is AacpPacket.EarDetection -> "RX Wear(${p.wear.primary.name}/${p.wear.secondary.name})"
        is AacpPacket.ConversationAwareness -> "RX ConvAware(${p.status.toInt() and 0xFF})"
        is AacpPacket.Information -> "RX Info('${p.metadata.name}', model=${p.metadata.modelNumber}, fw=${p.metadata.version1})"
        is AacpPacket.ProximityKeys -> "RX ProxKeys(${p.keys.size})"
        is AacpPacket.StemEvent -> "RX Stem(${p.side.name}/${p.press.name})"
        is AacpPacket.AudioSourceUpdate -> "RX AudioSrc(${p.source.type.name})"
        is AacpPacket.ConnectedDevices -> "RX Peers(${p.peers.size})"
        is AacpPacket.SmartRouting -> "RX SmartRoute(ownFalse=${p.ownershipFalseRequested})"
        is AacpPacket.CustomEqUpdate -> "RX CustomEq(${p.curve.state}:${p.curve.low}/${p.curve.mid}/${p.curve.high})"
        is AacpPacket.HeadTrackingSample -> "RX HeadTrack"
        is AacpPacket.Accommodation -> "RX EqAccomm(${p.onMedia}/${p.onPhone})"
        is AacpPacket.EqRaw -> "RX Eq(${p.payload.size}B)"
        is AacpPacket.Unknown -> "RX Unknown(op=${p.opcode?.let { "0x%02X".format(it) } ?: "none"}, len=${p.raw.size})"
    }

    private fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
    }

    private companion object {
        const val TAG = "AacpLink"
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val READ_BUFFER = 4096
    }
}
