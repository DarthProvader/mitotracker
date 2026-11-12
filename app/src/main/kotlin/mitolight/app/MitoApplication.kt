package mitolight.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mitolight.panel.BluetoothPanel
import mitolight.panel.PanelConnection

/**
 * The connection to a panel belongs here rather than to a screen: an exposure lasts ten minutes and
 * nobody stares at one screen for ten minutes (ADR 0007). While a panel is held, a foreground
 * service keeps the process from being reclaimed halfway through.
 */
class MitoApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var connection: PanelConnection
        private set

    /** History, and the watcher that turns what the panel did into more of it. */
    val log = SessionLog()

    lateinit var presets: PresetPreferences
        private set

    lateinit var tracker: SessionTracker
        private set

    lateinit var locales: LocalePreferences
        private set

    override fun onCreate() {
        super.onCreate()

        // Applied before anything else touches a string resource, or the first frame is drawn in
        // whatever language the process happened to start in rather than the one that was chosen.
        locales = LocalePreferences(this)
        // Framework application of the choice happens from MainActivity.attachBaseContext; keeping
        // the preferences constructed here is enough for Settings to read the same instance.

        connection = PanelConnection(
            scope = scope,
            memory = PanelMemoryPreferences(this),
            reopen = { remembered -> BluetoothPanel.remembered(remembered, scope) },
        )

        presets = PresetPreferences(this)
        tracker = SessionTracker(scope = scope, connection = connection, log = log)

        scope.launch {
            connection.panel.collect { panel ->
                if (panel == null) PanelService.stop(this@MitoApplication) else PanelService.start(this@MitoApplication)
            }
        }
    }
}
