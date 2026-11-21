package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "glyphchat_prefs")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SHOW_KEY_HINTS = booleanPreferencesKey("show_key_hints")
    }

    val darkModeFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val showKeyHintsFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SHOW_KEY_HINTS] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setShowKeyHints(show: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SHOW_KEY_HINTS] = show
        }
    }
}
