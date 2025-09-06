package com.kilovativedesigns.parkaware.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DS_NAME = "parkaware_prefs"

// Keep this at top‑level so the delegate is created once per Context.
// Public on purpose in case other parts of the app want direct access.
// If you prefer to hide it, change `val` to `private val` and keep using the Prefs helpers.
val Context.dataStore by preferencesDataStore(name = DS_NAME)

object Prefs {
    // Keys
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    private val KEY_WARNING_SEEN   = booleanPreferencesKey("has_seen_warning")

    // Flows (cold) — emit `false` by default if unset
    fun onboardingDoneFlow(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { prefs -> prefs[KEY_ONBOARDING_DONE] ?: false }

    fun warningSeenFlow(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { prefs -> prefs[KEY_WARNING_SEEN] ?: false }

    // Setters
    suspend fun setOnboardingDone(ctx: Context, value: Boolean) {
        ctx.dataStore.edit { it[KEY_ONBOARDING_DONE] = value }
    }

    suspend fun setWarningSeen(ctx: Context, value: Boolean) {
        ctx.dataStore.edit { it[KEY_WARNING_SEEN] = value }
    }

    // Small convenience helpers (optional)
    suspend fun markOnboardingDone(ctx: Context) = setOnboardingDone(ctx, true)
    suspend fun markWarningSeen(ctx: Context) = setWarningSeen(ctx, true)
}