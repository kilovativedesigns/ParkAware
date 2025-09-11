package com.kilovativedesigns.parkaware.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kilovativedesigns.parkaware.MainActivity
import com.kilovativedesigns.parkaware.R

class LocationTrackerService : Service() {

    private lateinit var fused: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)

        // Setup foreground notification so Android allows background tracking
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: request location updates and call FcmTopicManager.updateForLocation()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: stop location updates
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Helpers ---
    private fun buildNotification(): Notification {
        val channelId = "location_channel"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            mgr.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ParkAware")
            .setContentText("Tracking location for sightings")
            .setSmallIcon(R.drawable.ic_notification) // make sure ic_notification exists
            .setContentIntent(
                android.app.PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    companion object {
        private const val NOTIF_ID = 42
    }
}