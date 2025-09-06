package com.kilovativedesigns.parkaware.reminders

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("reminders")

object ReminderPrefs {
    private val KEY_ENABLED = booleanPreferencesKey("reminder_enabled")
    private val KEY_MINUTES = intPreferencesKey("reminder_default_time")
    private val KEY_STYLE   = stringPreferencesKey("reminder_notification_style")

    // Defaults
    const val DEFAULT_ENABLED = false
    const val DEFAULT_MINUTES = 30
    const val DEFAULT_STYLE   = "banner" // banner|alert|silent (your mapping below)

    fun flow(context: Context) = context.dataStore.data.map { p ->
        ReminderSettings(
            enabled = p[KEY_ENABLED] ?: DEFAULT_ENABLED,
            minutes = p[KEY_MINUTES] ?: DEFAULT_MINUTES,
            style   = p[KEY_STYLE]   ?: DEFAULT_STYLE
        )
    }

    suspend fun get(context: Context) = flow(context).first()

    suspend fun setEnabled(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_ENABLED] = value }
    }

    suspend fun setMinutes(context: Context, minutes: Int) {
        context.dataStore.edit { it[KEY_MINUTES] = minutes }
    }

    suspend fun setStyle(context: Context, style: String) {
        context.dataStore.edit { it[KEY_STYLE] = style }
    }
}

data class ReminderSettings(
    val enabled: Boolean,
    val minutes: Int,
    val style: String
)