package com.kilovativedesigns.parkaware.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kilovativedesigns.parkaware.MainActivity
import com.kilovativedesigns.parkaware.R

/**
 * Handles FCM token refresh and incoming push messages.
 *
 * Notes:
 * - Your Cloud Function uses android.notification.channelId = "reports".
 * - We defensively create that channel here too (in case the app wasn't opened yet).
 */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM new token: $token")
        // Save only when user is signed in (manager handles that check)
        FcmTokenManager.storeToken(token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        Log.d(TAG, "FCM message from=${msg.from}, data=${msg.data}, notif=${msg.notification}")

        // Ensure the channel exists (service can run before MainActivity)
        ensureReportsChannel()

        val title = msg.notification?.title
            ?: msg.data["title"]
            ?: getString(R.string.notif_default_title)

        val body = msg.notification?.body
            ?: msg.data["body"]
            ?: getString(R.string.notif_default_body)

        // Stable id so updated reports replace prior one in the tray.
        val collapseKey = msg.data["reportId"]
            ?: msg.messageId
            ?: System.currentTimeMillis().toString()

        showNotification(title, body, collapseKey.hashCode(), msg)
    }

    // --- helpers -------------------------------------------------------------

    private fun showNotification(
        title: String,
        body: String,
        notificationId: Int,
        msg: RemoteMessage
    ) {
        // Tapping the notification opens MainActivity (your nav graph decides where to go)
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            // You can pass reportId etc. if you want to deep-link later
            msg.data["reportId"]?.let { putExtra("reportId", it) }
            putExtra("fromPush", true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val flags = if (Build.VERSION.SDK_INT >= 23)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        val pending = PendingIntent.getActivity(this, 0, tapIntent, flags)

        val builder = NotificationCompat.Builder(this, CHANNEL_REPORTS)
            .setSmallIcon(R.drawable.ic_notification) // ensure this exists
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
    }

    private fun ensureReportsChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_REPORTS)
        if (existing != null) return

        val ch = NotificationChannel(
            CHANNEL_REPORTS,
            getString(R.string.channel_ranger_reports_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_ranger_reports_desc)
        }
        nm.createNotificationChannel(ch)
    }

    companion object {
        private const val TAG = "FcmService"
        /** Must match the channelId used by your Cloud Function ("reports"). */
        const val CHANNEL_REPORTS = "reports"
    }
}