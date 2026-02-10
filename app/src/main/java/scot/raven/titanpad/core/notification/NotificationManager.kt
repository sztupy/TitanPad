package scot.raven.titanpad.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.StateFlow
import scot.raven.titanpad.R
import scot.raven.titanpad.TitanPad
import scot.raven.titanpad.core.control.ModeCoordinator
import scot.raven.titanpad.core.logs.Logger
import scot.raven.titanpad.settings.domain.ApplicationSettings
import scot.raven.titanpad.settings.ui.SettingsActivity

/**
 * Manages notifications for active cursor modes.
 */
class NotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "cursor_active_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val settingsFlow : StateFlow<ApplicationSettings>

    init {
        createNotificationChannel()
        settingsFlow = TitanPad.getInstance().getSettingsFlow()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Cursor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when a cursor mode is active"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(mode: ModeCoordinator.OverlayMode) {
        try {
            val (title, text, icon) = when (mode) {
                ModeCoordinator.OverlayMode.CURSOR -> Triple(
                    "TitanPad Active - ${settingsFlow.value.getActiveConfig().configName}",
                    "Tap to open settings",
                    R.drawable.ic_blur_on
                )
                ModeCoordinator.OverlayMode.AUTOHIDDEN -> Triple(
                    "TitanPad Autohidden - ${settingsFlow.value.getActiveConfig().configName}",
                    "Tap to open settings",
                    R.drawable.ic_blur_off
                )
                ModeCoordinator.OverlayMode.OFF -> {
                    hideNotification()
                    return
                }
            }

            val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setShowWhen(false)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
            Logger.d("Cursor notification shown for mode: $mode")

        } catch (e: Exception) {
            Logger.e("Error showing cursor notification", e)
        }
    }

    fun hideNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
            Logger.d("Cursor notification hidden")
        } catch (e: Exception) {
            Logger.e("Error hiding cursor notification", e)
        }
    }
}