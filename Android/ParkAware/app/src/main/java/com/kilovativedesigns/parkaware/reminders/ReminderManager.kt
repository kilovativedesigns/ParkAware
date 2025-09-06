package com.kilovativedesigns.parkaware.reminders

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.math.max

object ReminderManager {

    /**
     * Schedule using current prefs (fires 5 min early; min delay 60s).
     */
    suspend fun scheduleFromPrefs(context: Context): Long {
        val s = ReminderPrefs.get(context)
        val totalSec = s.minutes * 60L
        val fireInSec = max(totalSec - 300L, 60L)
        schedule(context, fireInSec)
        return fireInSec
    }

    /**
     * Explicit seconds.
     */
    fun schedule(context: Context, delaySeconds: Long) {
        ReminderWorker.ensureChannel(context)
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(work)
    }

    fun clearAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}