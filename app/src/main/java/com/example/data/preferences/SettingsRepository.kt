package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AutomationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sachin_clicker_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MAX_PICKUP_KM = doublePreferencesKey("max_pickup_km")
        val MAX_DROP_KM = doublePreferencesKey("max_drop_km")
        val PRICE_FILTER_ENABLED = booleanPreferencesKey("price_filter_enabled")
        val MIN_PRICE = doublePreferencesKey("min_price")
        val AUTO_ACCEPT_ENABLED = booleanPreferencesKey("auto_accept_enabled")
        val FLOATING_CONTROLS_ENABLED = booleanPreferencesKey("floating_controls_enabled")
        val SOUND_FEEDBACK = booleanPreferencesKey("sound_feedback")
        val VIBRATION_FEEDBACK = booleanPreferencesKey("vibration_feedback")
        val COOLDOWN_SECONDS = intPreferencesKey("cooldown_seconds")
    }

    val settingsFlow: Flow<AutomationSettings> = context.dataStore.data.map { preferences ->
        AutomationSettings(
            maxPickupKm = preferences[Keys.MAX_PICKUP_KM] ?: 3.0,
            maxDropKm = preferences[Keys.MAX_DROP_KM] ?: 5.0,
            priceFilterEnabled = preferences[Keys.PRICE_FILTER_ENABLED] ?: false,
            minPrice = preferences[Keys.MIN_PRICE] ?: 50.0,
            autoAcceptEnabled = preferences[Keys.AUTO_ACCEPT_ENABLED] ?: true,
            floatingControlsEnabled = preferences[Keys.FLOATING_CONTROLS_ENABLED] ?: true,
            soundFeedbackEnabled = preferences[Keys.SOUND_FEEDBACK] ?: true,
            vibrationFeedbackEnabled = preferences[Keys.VIBRATION_FEEDBACK] ?: true,
            cooldownSeconds = preferences[Keys.COOLDOWN_SECONDS] ?: 3
        )
    }

    suspend fun updateMaxPickupKm(km: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_PICKUP_KM] = km
        }
    }

    suspend fun updateMaxDropKm(km: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_DROP_KM] = km
        }
    }

    suspend fun updatePriceFilter(enabled: Boolean, minPrice: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRICE_FILTER_ENABLED] = enabled
            prefs[Keys.MIN_PRICE] = minPrice
        }
    }

    suspend fun updateAutoAccept(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_ACCEPT_ENABLED] = enabled
        }
    }

    suspend fun updateFloatingControls(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FLOATING_CONTROLS_ENABLED] = enabled
        }
    }

    suspend fun updateFeedbackSettings(sound: Boolean, vibration: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_FEEDBACK] = sound
            prefs[Keys.VIBRATION_FEEDBACK] = vibration
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_PICKUP_KM] = 3.0
            prefs[Keys.MAX_DROP_KM] = 5.0
            prefs[Keys.PRICE_FILTER_ENABLED] = false
            prefs[Keys.MIN_PRICE] = 50.0
            prefs[Keys.AUTO_ACCEPT_ENABLED] = true
            prefs[Keys.FLOATING_CONTROLS_ENABLED] = true
            prefs[Keys.SOUND_FEEDBACK] = true
            prefs[Keys.VIBRATION_FEEDBACK] = true
            prefs[Keys.COOLDOWN_SECONDS] = 3
        }
    }
}
