package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "bookstore_session")

class SessionStore(context: Context) {

    private val dataStore = context.applicationContext.sessionDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _locale = MutableStateFlow(DEFAULT_LOCALE)
    val locale: StateFlow<String> = _locale.asStateFlow()

    private val _isHydrated = MutableStateFlow(false)
    val isHydrated: StateFlow<Boolean> = _isHydrated.asStateFlow()

    init {
        scope.launch {
            val prefs = dataStore.data.first()
            _token.value = prefs[KEY_TOKEN]?.takeIf { it.isNotBlank() }
            _locale.value = prefs[KEY_LOCALE]?.takeIf { it in SUPPORTED_LOCALES } ?: DEFAULT_LOCALE
            _isHydrated.value = true
        }
    }

    suspend fun awaitHydration() {
        if (!_isHydrated.value) {
            val prefs = dataStore.data.first()
            _token.value = prefs[KEY_TOKEN]?.takeIf { it.isNotBlank() }
            _locale.value = prefs[KEY_LOCALE]?.takeIf { it in SUPPORTED_LOCALES } ?: DEFAULT_LOCALE
            _isHydrated.value = true
        }
    }

    suspend fun saveToken(token: String) {
        _token.value = token
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
        }
    }

    suspend fun clearToken() {
        _token.value = null
        dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
        }
    }

    suspend fun setLocale(locale: String) {
        val valid = locale.lowercase().takeIf { it in SUPPORTED_LOCALES } ?: DEFAULT_LOCALE
        _locale.value = valid
        dataStore.edit { prefs ->
            prefs[KEY_LOCALE] = valid
        }
    }

    companion object {
        const val API_BASE_URL = "https://web-staging-9aaadc06.up.railway.app/api/v1/"
        const val DEFAULT_LOCALE = "en"
        val SUPPORTED_LOCALES = setOf("en", "fr", "ar")

        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_LOCALE = stringPreferencesKey("app_locale")
    }
}
