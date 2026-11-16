-keepclassmembers class * extends android.bluetooth.BluetoothSocket { *; }
-keep class android.bluetooth.BluetoothSocket { *; }
-keep class android.bluetooth.BluetoothDevice { *; }
-keepclassmembers class android.bluetooth.BluetoothDevice { *; }
-keepclassmembers class android.bluetooth.BluetoothA2dp { *; }
-keepclassmembers class android.bluetooth.BluetoothHeadset { *; }

-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn org.lsposed.hiddenapibypass.**

-keep class com.hamondev.shevery.** { *; }
-keep class dev.podlink.system.ShizukuBatteryService {
    public <init>(...);
}
-keep interface dev.podlink.system.IShizukuBatteryBridge { *; }
-keep class dev.podlink.system.IShizukuBatteryBridge$Stub { *; }
-keep class dev.podlink.system.IShizukuBatteryBridge$Stub$Proxy { *; }
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn com.hamondev.shevery.**
-dontwarn rikka.shizuku.**
-dontwarn moe.shizuku.**
