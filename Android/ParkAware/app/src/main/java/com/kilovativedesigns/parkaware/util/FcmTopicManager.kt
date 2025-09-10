package com.kilovativedesigns.parkaware.util

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.math.abs

/**
 * Subscribes the device to an FCM topic representing a GeoHash-5 tile.
 * Topic format: h5_<geohash5> (only [A-Za-z0-9-_.~%] allowed; our names are safe).
 *
 * NOTE: FCM keeps topic membership across token refresh; we still resubscribe on login for safety.
 */
object FcmTopicManager {
    private const val PREF = "push_prefs"
    private const val KEY_TOPIC = "notif_topic"

    /** Update the user’s topic based on a chosen lat/lon (e.g., “alert me around here”). */
    fun updateForLocation(ctx: Context, lat: Double, lon: Double) {
        val newTopic = topicFor(lat, lon)
        val oldTopic = currentTopic(ctx)

        if (newTopic == oldTopic) return

        // Unsubscribe old (if any), then subscribe new, then persist
        if (!oldTopic.isNullOrBlank()) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic)
        }
        FirebaseMessaging.getInstance().subscribeToTopic(newTopic)
        saveTopic(ctx, newTopic)
    }

    /** Re-subscribe to the saved topic (call after sign-in / app start). */
    fun refreshFromPrefs(ctx: Context) {
        currentTopic(ctx)?.takeIf { it.isNotBlank() }?.let {
            FirebaseMessaging.getInstance().subscribeToTopic(it)
        }
    }

    /** Clear all subscriptions we manage (called on sign-out). */
    fun clear(ctx: Context) {
        currentTopic(ctx)?.takeIf { it.isNotBlank() }?.let {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(it)
        }
        saveTopic(ctx, "")
    }

    // ---- helpers ----

    private fun currentTopic(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_TOPIC, null)

    private fun saveTopic(ctx: Context, topic: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_TOPIC, topic).apply()
    }

    private fun topicFor(lat: Double, lon: Double): String = "h5_${geoHash5(lat, lon)}"

    /**
     * Minimal GeoHash encoder (length=5).
     * Base32 alphabet per GeoHash spec: "0123456789bcdefghjkmnpqrstuvwxyz"
     */
    private fun geoHash5(lat: Double, lon: Double): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var latMin = -90.0;  var latMax = 90.0
        var lonMin = -180.0; var lonMax = 180.0

        val bits = intArrayOf(16, 8, 4, 2, 1)
        var even = true
        var bit = 0
        var ch = 0
        val sb = StringBuilder()

        while (sb.length < 5) {
            if (even) {
                val mid = (lonMin + lonMax) / 2
                if (lon >= mid) { ch = ch or bits[bit]; lonMin = mid } else lonMax = mid
            } else {
                val mid = (latMin + latMax) / 2
                if (lat >= mid) { ch = ch or bits[bit]; latMin = mid } else latMax = mid
            }
            even = !even
            if (bit < 4) {
                bit++
            } else {
                sb.append(base32[ch])
                bit = 0
                ch = 0
            }
        }
        return sb.toString()
    }
}