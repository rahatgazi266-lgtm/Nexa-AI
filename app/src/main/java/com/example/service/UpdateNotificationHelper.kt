package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.example.MainActivity
import com.example.model.AppUpdateInfo

/** Creates a normal Android notification when a newer Nexa AI release is found. */
object UpdateNotificationHelper {
    private const val CHANNEL_ID = "nexa_app_updates"
    private const val CHANNEL_NAME = "Nexa AI updates"
    private const val NOTIFICATION_ID = 1401
    private const val PREFS_NAME = "nexa_update_notification_prefs"
    private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"

    fun showUpdateAvailable(context: Context, updateInfo: AppUpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_NOTIFIED_VERSION, null) == updateInfo.versionName) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications when a new Nexa AI version is available"
                }
            )
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Nexa AI update available")
            .setContentText("Version ${updateInfo.versionName} is ready to download")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Nexa AI version ${updateInfo.versionName} is available. Open Nexa AI to review and install the update."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, updateInfo.versionName).apply()
        } catch (_: SecurityException) {
            // The in-app update dialog remains functional if notification permission is unavailable.
        }
    }
}
