package dev.podlink.system

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Process
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

class ShizukuBatteryService() : IShizukuBatteryBridge.Stub() {
    @Suppress("unused")
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    @Volatile
    private var exemptionsInitialized = false

    private fun ensureExemptions() {
        if (exemptionsInitialized) return
        runCatching {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/bluetooth/BluetoothDevice;",
                "Landroid/app/ActivityThread;",
                "Landroid/content/Context;",
                "Landroid/os/UserHandle;",
            )
        }
        exemptionsInitialized = true
    }

    private fun resolveSystemContext(): Context? {
        ensureExemptions()
        return runCatching {
            val threadClass = Class.forName("android.app.ActivityThread")
            val current = threadClass.getMethod("currentActivityThread").invoke(null)
                ?: threadClass.getMethod("systemMain").invoke(null)
            threadClass.getMethod("getSystemContext").invoke(current) as? Context
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    override fun pushBatteryState(
        macAddress: String,
        leftLevel: Int,
        leftCharging: Boolean,
        rightLevel: Int,
        rightCharging: Boolean,
        caseLevel: Int,
        caseCharging: Boolean,
        headsetLevel: Int,
        headsetCharging: Boolean,
        modelName: String,
        mainIconUri: String,
        leftIconUri: String,
        rightIconUri: String,
        caseIconUri: String,
        companionPackage: String,
    ): String {
        val ctx = resolveSystemContext() ?: return "systemContext unavailable"
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter
            ?: runCatching {
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()
            }.getOrNull()
            ?: return "BluetoothAdapter unavailable"

        val device = runCatching { adapter.getRemoteDevice(macAddress) }.getOrNull()
            ?: return "Invalid MAC $macAddress"

        val unified = computeUnifiedBattery(leftLevel, rightLevel, headsetLevel, caseLevel)
        val mainCharging = (leftLevel in 0..100 && leftCharging) ||
            (rightLevel in 0..100 && rightCharging) ||
            (headsetLevel in 0..100 && headsetCharging)

        var metaOk = 0
        var metaFail = 0
        fun putMeta(key: Int, value: String) {
            if (value.isBlank()) return
            if (setMetadata(device, key, value.toByteArray())) metaOk++ else metaFail++
        }

        putMeta(META_MODEL_NAME, modelName)
        putMeta(META_COMPANION_APP, companionPackage)
        putMeta(META_DEVICE_TYPE, "Untethered Headset")
        putMeta(META_IS_UNTETHERED, "true")
        putMeta(META_MAIN_ICON, mainIconUri)
        putMeta(META_LEFT_ICON, leftIconUri)
        putMeta(META_RIGHT_ICON, rightIconUri)
        putMeta(META_CASE_ICON, caseIconUri)
        putMeta(META_LEFT_BATTERY, if (leftLevel in 0..100) leftLevel.toString() else "-1")
        putMeta(META_RIGHT_BATTERY, if (rightLevel in 0..100) rightLevel.toString() else "-1")
        putMeta(META_CASE_BATTERY, if (caseLevel in 0..100) caseLevel.toString() else "-1")
        putMeta(META_LEFT_CHARGING, leftCharging.toString())
        putMeta(META_RIGHT_CHARGING, rightCharging.toString())
        putMeta(META_CASE_CHARGING, caseCharging.toString())
        putMeta(META_MAIN_BATTERY, unified.toString())
        putMeta(META_MAIN_CHARGING, mainCharging.toString())
        putMeta(META_MAIN_LOW_THRESH, "20")
        putMeta(META_LEFT_LOW_THRESH, "20")
        putMeta(META_RIGHT_LOW_THRESH, "20")
        putMeta(META_CASE_LOW_THRESH, "20")

        var broadcasts = 0
        if (unified in 0..100) {
            val hfpIntent = Intent(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).apply {
                putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD, "+IPHONEACCEV")
                putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, BluetoothHeadset.AT_CMD_TYPE_SET)
                putExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS, arrayOf<Any>(1, 1, unified))
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                putExtra(BluetoothDevice.EXTRA_NAME, modelName)
                addCategory("${BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY}.$APPLE_COMPANY_ID")
            }
            if (sendUserBroadcast(ctx, hfpIntent, android.Manifest.permission.BLUETOOTH_CONNECT)) broadcasts++

            val batIntent = Intent(ACTION_BATTERY_LEVEL_CHANGED).apply {
                putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                putExtra(EXTRA_BATTERY_LEVEL, unified)
            }
            if (sendUserBroadcast(ctx, batIntent, android.Manifest.permission.BLUETOOTH_CONNECT)) broadcasts++

            val asiIntent = Intent(ACTION_ASI_UPDATE).apply {
                setPackage(PKG_ASI)
                putExtra(ACTION_BATTERY_LEVEL_CHANGED, batIntent)
            }
            if (sendUserBroadcast(ctx, asiIntent, null)) broadcasts++
        }

        return "meta=$metaOk/${metaOk + metaFail} bcast=$broadcasts level=$unified%"
    }

    private fun setMetadata(device: BluetoothDevice, key: Int, value: ByteArray): Boolean = runCatching {
        (HiddenApiBypass.invoke(BluetoothDevice::class.java, device, "setMetadata", key, value) as? Boolean) == true
    }.getOrDefault(false)

    private fun sendUserBroadcast(ctx: Context, intent: Intent, permission: String?): Boolean {
        val asUser = runCatching {
            val handle = Process.myUserHandle()
            val method = if (permission != null) {
                Context::class.java.getMethod(
                    "sendBroadcastAsUser",
                    Intent::class.java,
                    android.os.UserHandle::class.java,
                    String::class.java,
                )
            } else {
                Context::class.java.getMethod(
                    "sendBroadcastAsUser",
                    Intent::class.java,
                    android.os.UserHandle::class.java,
                )
            }
            if (permission != null) method.invoke(ctx, intent, handle, permission) else method.invoke(ctx, intent, handle)
            true
        }.getOrDefault(false)
        if (asUser) return true
        return runCatching {
            if (permission != null) ctx.sendBroadcast(intent, permission) else ctx.sendBroadcast(intent)
            true
        }.getOrDefault(false)
    }

    private fun computeUnifiedBattery(left: Int, right: Int, headset: Int, case: Int): Int {
        val buds = buildList {
            if (left in 0..100) add(left)
            if (right in 0..100) add(right)
        }
        return when {
            buds.isNotEmpty() -> buds.min()
            headset in 0..100 -> headset
            case in 0..100 -> case
            else -> -1
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    private companion object {
        const val APPLE_COMPANY_ID = 0x004C
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        const val ACTION_ASI_UPDATE = "batterywidget.impl.action.update_bluetooth_data"
        const val PKG_ASI = "com.google.android.settings.intelligence"

        const val META_MODEL_NAME = 1
        const val META_COMPANION_APP = 4
        const val META_MAIN_ICON = 5
        const val META_IS_UNTETHERED = 6
        const val META_LEFT_ICON = 7
        const val META_RIGHT_ICON = 8
        const val META_CASE_ICON = 9
        const val META_LEFT_BATTERY = 10
        const val META_RIGHT_BATTERY = 11
        const val META_CASE_BATTERY = 12
        const val META_LEFT_CHARGING = 13
        const val META_RIGHT_CHARGING = 14
        const val META_CASE_CHARGING = 15
        const val META_DEVICE_TYPE = 17
        const val META_MAIN_BATTERY = 18
        const val META_MAIN_CHARGING = 19
        const val META_MAIN_LOW_THRESH = 20
        const val META_LEFT_LOW_THRESH = 21
        const val META_RIGHT_LOW_THRESH = 22
        const val META_CASE_LOW_THRESH = 23
    }
}
