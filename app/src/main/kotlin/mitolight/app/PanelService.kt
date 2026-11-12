package mitolight.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mitolight.panel.PanelState

/**
 * Keeps the app alive and visible while a panel is held.
 *
 * Without this, a ten minute exposure is at the mercy of whatever the system decides to reclaim, and
 * the light can be left running with nothing watching it. The notification is also the only honest
 * place to say how much time is left once the phone is in a pocket.
 */
class PanelService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notif_channel_description)
                setShowBadge(false)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val connection = (application as MitoApplication).connection

        if (intent?.action == ACTION_STOP_LIGHT) {
            connection.panel.value?.let { panel -> scope.launch { runCatching { panel.stop() } } }
            return START_NOT_STICKY
        }

        startInForeground(notification(name = null, text = getString(R.string.status_connecting), lit = false))

        scope.launch {
            combine(connection.panel, connection.state, connection.recovering, ::Triple)
                .collect { (panel, state, recovering) ->
                    val status = (state as? PanelState.Ready)?.status
                    val lit = status?.running == true

                    val text = when {
                        recovering -> getString(R.string.status_reconnecting)
                        state is PanelState.Connecting -> getString(R.string.status_connecting)
                        state is PanelState.Disconnected -> getString(R.string.status_not_connected)
                        state is PanelState.Failed -> state.reason
                        lit -> getString(R.string.notif_lit_remaining, clock(status.remainingSeconds))
                        status != null -> getString(R.string.notif_idle_set_to, status.countdownSeconds / 60)
                        else -> getString(R.string.status_idle)
                    }

                    getSystemService(NotificationManager::class.java)
                        .notify(ID, notification(name = panel?.name, text = text, lit = lit))
                }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(ID, notification)
        }
    }

    private fun notification(name: String?, text: String, lit: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = Notification.Builder(this, CHANNEL)
            .setContentTitle(name ?: getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (lit) {
            val stop = PendingIntent.getService(
                this,
                1,
                Intent(this, PanelService::class.java).setAction(ACTION_STOP_LIGHT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                Notification.Action.Builder(null, getString(R.string.action_stop), stop).build(),
            )
        }

        return builder.build()
    }

    private fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    companion object {
        private const val CHANNEL = "panel"
        private const val ID = 1
        private const val ACTION_STOP_LIGHT = "mitolight.app.STOP_LIGHT"

        fun start(context: Context) {
            val intent = Intent(context, PanelService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PanelService::class.java))
        }
    }
}
