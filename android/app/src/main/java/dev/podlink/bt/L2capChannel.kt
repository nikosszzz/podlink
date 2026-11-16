package dev.podlink.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.ParcelUuid
import android.util.Log
import dev.podlink.core.AacpSpec
import dev.podlink.core.TraceLog
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.concurrent.atomic.AtomicBoolean

object L2capChannel {
    private const val TAG = "L2capChannel"
    private const val TYPE_L2CAP = 3
    private val exempted = AtomicBoolean(false)
    private val loggedConstructors = AtomicBoolean(false)

    class OpenException(
        message: String,
        val hiddenApiBlocked: Boolean,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    fun ensureExempted(): Boolean {
        if (exempted.get()) return true
        val targetedOk = runCatching {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/bluetooth/BluetoothSocket;",
                "Landroid/bluetooth/BluetoothDevice;",
            )
        }.onFailure { err ->
            TraceLog.record(TAG, "Targeted HiddenApiBypass failed: ${err.message}")
        }.getOrDefault(false)

        val ok = if (targetedOk && canReflectConstructor()) {
            true
        } else {
            val broadOk = runCatching { HiddenApiBypass.addHiddenApiExemptions("L") }
                .onFailure { err -> TraceLog.record(TAG, "Global HiddenApiBypass failed: ${err.message}") }
                .getOrDefault(false)
            broadOk && canReflectConstructor()
        }
        if (ok) exempted.set(true)
        if (loggedConstructors.compareAndSet(false, true)) {
            TraceLog.record(TAG, "BluetoothSocket ctors=${describeConstructors()}")
        }
        return ok
    }

    fun isAccessible(): Boolean = exempted.get() || canReflectConstructor()

    fun openSocket(
        adapter: BluetoothAdapter,
        device: BluetoothDevice,
        psm: Int = AacpSpec.AACP_PSM,
        uuid: ParcelUuid = ParcelUuid.fromString(AacpSpec.SERVICE_UUID),
    ): Result<BluetoothSocket> {
        val ready = ensureExempted()
        val candidates = listOf(
            arrayOf<Any>(adapter, device, TYPE_L2CAP, true, true, psm, uuid),
            arrayOf<Any>(device, TYPE_L2CAP, true, true, psm, uuid),
            arrayOf<Any>(device, TYPE_L2CAP, 1, true, true, psm, uuid),
            arrayOf<Any>(TYPE_L2CAP, 1, true, true, device, psm, uuid),
            arrayOf<Any>(TYPE_L2CAP, true, true, device, psm, uuid),
        )

        val errors = mutableListOf<String>()
        var lastError: Throwable? = null
        for ((index, args) in candidates.withIndex()) {
            val result = reflectConstructor(args)
            result.getOrNull()?.let { socket ->
                TraceLog.record(TAG, "Opened L2CAP socket via spec #${index + 1} (psm=0x${psm.toString(16)})")
                return Result.success(socket)
            }
            result.exceptionOrNull()?.let { err ->
                lastError = err
                errors += "#${index + 1}:${err.javaClass.simpleName}"
            }
        }

        for (args in candidates) {
            val types = args.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray()
            val byCtor = runCatching {
                HiddenApiBypass.getDeclaredConstructor(BluetoothSocket::class.java, *types)
                    .apply { isAccessible = true }
                    .newInstance(*args) as BluetoothSocket
            }
            if (byCtor.isSuccess) return byCtor
            lastError = byCtor.exceptionOrNull()

            val byNew = runCatching { HiddenApiBypass.newInstance(BluetoothSocket::class.java, *args) as BluetoothSocket }
            if (byNew.isSuccess) return byNew
            lastError = byNew.exceptionOrNull() ?: lastError
        }

        val blocked = !ready && !canReflectConstructor()
        val msg = "Unable to open L2CAP socket (0x${psm.toString(16)}): ${errors.joinToString()} | ${lastError?.message}"
        Log.w(TAG, msg)
        TraceLog.record(TAG, msg)
        return Result.failure(OpenException(msg, hiddenApiBlocked = blocked, cause = lastError))
    }

    private fun reflectConstructor(args: Array<Any>): Result<BluetoothSocket> = runCatching {
        val paramTypes = args.map { it::class.javaPrimitiveType ?: it::class.java }.toTypedArray()
        BluetoothSocket::class.java.getDeclaredConstructor(*paramTypes)
            .apply { isAccessible = true }
            .newInstance(*args) as BluetoothSocket
    }

    private fun describeConstructors(): String = runCatching {
        BluetoothSocket::class.java.declaredConstructors.joinToString(" | ") { ctor ->
            ctor.parameterTypes.joinToString(",", "(", ")") { it.simpleName }
        }
    }.getOrDefault("unavailable")

    private fun canReflectConstructor(): Boolean =
        runCatching { BluetoothSocket::class.java.declaredConstructors.any { it.parameterCount >= 5 } }.getOrDefault(false)
}
