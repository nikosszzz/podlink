package dev.podlink.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import dev.podlink.core.AacpSpec
import dev.podlink.core.AttCccdHandle
import dev.podlink.core.AttHandle
import dev.podlink.core.TraceLog
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AttLink(
    private val adapter: BluetoothAdapter,
    private val device: BluetoothDevice,
    private val onNotify: (Byte, ByteArray) -> Unit,
    private val onAvailability: (Boolean) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(true)
    private val available = AtomicBoolean(false)
    private val responseQueues = ConcurrentHashMap<Byte, LinkedBlockingQueue<ByteArray>>()

    @Volatile private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun start() {
        scope.launch {
            delay(350)
            if (!running.get()) return@launch
            try {
                val s = L2capChannel.openSocket(
                    adapter = adapter,
                    device = device,
                    psm = AacpSpec.ATT_PSM,
                    uuid = ParcelUuid.fromString(AacpSpec.ATT_ZERO_UUID),
                ).getOrThrow()
                socket = s
                s.connect()
                available.set(true)
                onAvailability(true)
                TraceLog.record(TAG, "ATT PSM 31 connected")

                val reader = launch { readLoop(s) }
                launch {
                    delay(120)
                    enableNotify(AttCccdHandle.TRANSPARENCY)
                    enableNotify(AttCccdHandle.HEARING_AID)
                    readChar(AttHandle.TRANSPARENCY)?.let { onNotify(AttHandle.TRANSPARENCY.code, it) }
                    readChar(AttHandle.LOUD_SOUND_REDUCTION)?.let { onNotify(AttHandle.LOUD_SOUND_REDUCTION.code, it) }
                    readChar(AttHandle.HEARING_AID)?.let { onNotify(AttHandle.HEARING_AID.code, it) }
                }
                reader.join()
            } catch (t: Throwable) {
                TraceLog.record(TAG, "ATT unavailable: ${t.message}")
            } finally {
                available.set(false)
                onAvailability(false)
                closeSocket()
            }
        }
    }

    fun readChar(handle: AttHandle, timeoutMs: Long = 2000L): ByteArray? {
        val current = socket ?: return null
        if (!available.get()) return null
        return try {
            val out = current.outputStream
            synchronized(out) {
                out.write(byteArrayOf(OP_READ_REQ, handle.code, 0x00))
                out.flush()
            }
            val resp = waitResp(OP_READ_RSP, timeoutMs) ?: return null
            if (resp.size <= 1) return null
            resp.copyOfRange(1, resp.size)
        } catch (_: Throwable) {
            null
        }
    }

    fun enableNotify(handle: AttCccdHandle) {
        writeRaw(handle.code, byteArrayOf(0x01))
    }

    fun writeChar(handle: AttHandle, data: ByteArray, timeoutMs: Long = 2000L): Boolean =
        writeRaw(handle.code, data, timeoutMs)

    private fun writeRaw(handle: Byte, data: ByteArray, timeoutMs: Long = 2000L): Boolean {
        val current = socket ?: return false
        if (!available.get()) return false
        return try {
            val out = current.outputStream
            synchronized(out) {
                out.write(byteArrayOf(OP_WRITE_REQ, handle, 0x00) + data)
                out.flush()
            }
            waitResp(OP_WRITE_RSP, timeoutMs) != null
        } catch (_: Throwable) {
            false
        }
    }

    private fun readLoop(btSocket: BluetoothSocket) {
        val input = btSocket.inputStream
        val buf = ByteArray(512)
        while (running.get()) {
            val read = try {
                input.read(buf)
            } catch (_: IOException) {
                -1
            }
            if (read <= 0) break
            val data = buf.copyOf(read)
            val op = data[0]
            responseQueues.computeIfAbsent(op) { LinkedBlockingQueue() }.offer(data)
            if (op == OP_NOTIFY && data.size >= 3) {
                val handle = data[1]
                val value = if (data.size > 3) data.copyOfRange(3, data.size) else ByteArray(0)
                onNotify(handle, value)
            }
        }
    }

    private fun waitResp(opcode: Byte, timeoutMs: Long): ByteArray? = runCatching {
        responseQueues.computeIfAbsent(opcode) { LinkedBlockingQueue() }.poll(timeoutMs, TimeUnit.MILLISECONDS)
    }.getOrNull()

    fun close() {
        if (!running.getAndSet(false)) return
        available.set(false)
        closeSocket()
        scope.cancel()
    }

    private fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
    }

    private companion object {
        const val TAG = "AttLink"
        const val OP_READ_REQ: Byte = 0x0A
        const val OP_READ_RSP: Byte = 0x0B
        const val OP_WRITE_REQ: Byte = 0x12
        const val OP_WRITE_RSP: Byte = 0x13
        const val OP_NOTIFY: Byte = 0x1B
    }
}
