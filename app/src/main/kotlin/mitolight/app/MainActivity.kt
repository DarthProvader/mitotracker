package mitolight.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import mitolight.app.ui.BluetoothPermissions
import mitolight.app.ui.Shell
import mitolight.app.ui.theme.MitoTheme

/**
 * AppCompatActivity so per-app locales recreate this screen. The stored choice is applied
 * in [attachBaseContext] before Compose reads any string resource.
 */
class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val application = newBase.applicationContext as? MitoApplication
        application?.locales?.applyStored()
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // The connection, the history and the watcher that joins them all come from the application,
        // so a second helping of this screen finds the same panel already connected and the same
        // session still being recorded (ADR 0007).
        val application = application as MitoApplication

        setContent {
            MitoTheme {
                BluetoothPermissions {
                    Shell(
                        model = viewModel(
                            factory = viewModelFactory {
                                initializer { PanelViewModel(application.connection, application) }
                            },
                        ),
                        log = application.log,
                        presets = application.presets,
                        tracker = application.tracker,
                        locales = application.locales,
                    )
                }
            }
        }
    }
}
