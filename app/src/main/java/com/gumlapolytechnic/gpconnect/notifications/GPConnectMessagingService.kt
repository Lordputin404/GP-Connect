package com.gumlapolytechnic.gpconnect.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gumlapolytechnic.gpconnect.MainActivity
import com.gumlapolytechnic.gpconnect.R

/**
 * FCM client service (push notifications). Receives Firebase Cloud Messaging
 * deliveries and shows them as Android notifications — both while the app is
 * in the foreground (`onMessageReceived` always fires for foreground
 * deliveries, which FCM would otherwise silently drop without a
 * `default_notification_channel_id` meta-data) and in the background (where
 * this service is invoked for data messages, and notification messages are
 * shown by the system tray).
 *
 * Minimal by design for this phase:
 *  - The FCM token is NOT stored anywhere (no Firestore write) — the
 *    `onNewToken` callback exists only for logging, as required plumbing.
 *  - Only notification messages (`title`/`body` extras) are rendered;
 *    data-only messages are logged and ignored.
 *  - Tapping a notification just opens the app via [MainActivity] —
 *    notice/calendar deep-links arrive with later phases.
 *  - No Cloud Functions; nothing is sent from the app.
 */
class GPConnectMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
        val body = message.notification?.body
        if (title == null && body == null) {
            Log.d(TAG, "onMessageReceived: data-only message (no notification payload) — ignored")
            return
        }
        showNotification(
            context = this,
            title = title ?: getString(R.string.app_name),
            body = body.orEmpty(),
        )
    }

    override fun onNewToken(token: String) {
        // Tokens are deliberately not persisted yet — this is the required
        // FCM plumbing so the client stays token-fresh; storage in Firestore
        // arrives with the notification-sending phase.
        Log.i(TAG, "FCM token rotated (not stored in this phase)")
    }

    companion object {
        private const val TAG = "GPFirebaseMessaging"
        const val CHANNEL_ID = "gp_connect_notifications"
        private const val NOTIFICATION_ID = 1001

        /**
         * Creates the GP Connect notification channel on API 26+. Safe to call
         * repeatedly — an existing channel is not recreated. Must be called
         * before the first notification is posted; the app calls it from
         * [com.gumlapolytechnic.gpconnect.GPConnectApplication.onCreate].
         */
        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        /**
         * Posts one notification to the GP Connect channel. POST_NOTIFICATIONS
         * is verified before posting on API 33+ (notify() without it would
         * throw SecurityException); on older versions the permission is
         * granted at install time. Foreground deliveries reach this via
         * [GPConnectMessagingService.onMessageReceived], background
         * notification messages are shown by the system tray.
         */
        fun showNotification(context: Context, title: String, body: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "showNotification skipped: POST_NOTIFICATIONS not granted")
                return
            }
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
