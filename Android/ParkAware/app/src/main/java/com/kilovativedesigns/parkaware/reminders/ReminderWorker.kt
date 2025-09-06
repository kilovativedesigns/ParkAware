package com.kilovativedesigns.parkaware.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kilovativedesigns.parkaware.R

class ReminderWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val title = ctx.getString(R.string.reminder_title)
        val text  = ctx.getString(R.string.reminder_body)

        ensureChannel(ctx)
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder) // add simple bell vector (below)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(ctx)) {
            notify(NOTIF_ID, notif)
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "parking_reminders"
        private const val NOTIF_ID = 42

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.reminder_channel_desc)
                }
                mgr.createNotificationChannel(ch)
            }
        }
    }
}