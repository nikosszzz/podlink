package dev.podlink

import android.app.Application
import dev.podlink.bt.CompanionBridge
import dev.podlink.core.DeviceCoordinator
import dev.podlink.core.PrefsStore
import dev.podlink.system.BatteryNotifier
import dev.podlink.system.MediaBridge
import dev.podlink.system.ShizukuReporter

class PodLinkApplication : Application() {
    lateinit var prefs: PrefsStore
        private set
    lateinit var companion: CompanionBridge
        private set
    lateinit var media: MediaBridge
        private set
    lateinit var notifier: BatteryNotifier
        private set
    lateinit var shizuku: ShizukuReporter
        private set
    lateinit var coordinator: DeviceCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsStore(this)
        companion = CompanionBridge(this)
        media = MediaBridge(this)
        notifier = BatteryNotifier(this)
        shizuku = ShizukuReporter(this)
        coordinator = DeviceCoordinator(
            context = this,
            prefs = prefs,
            companion = companion,
            media = media,
            notifier = notifier,
            shizuku = shizuku,
        )
    }

    override fun onTerminate() {
        coordinator.release()
        super.onTerminate()
    }
}
