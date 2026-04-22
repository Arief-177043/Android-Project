package com.growwx.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "growwx_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_UID = stringPreferencesKey("user_uid")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val CASH_BALANCE = doublePreferencesKey("cash_balance")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.USER_UID]?.isNotEmpty() == true }

    val userFlow: Flow<Triple<String, String, String>> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            Triple(
                prefs[Keys.USER_UID] ?: "",
                prefs[Keys.USER_NAME] ?: "",
                prefs[Keys.USER_EMAIL] ?: ""
            )
        }

    val cashBalance: Flow<Double> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.CASH_BALANCE] ?: 100_000.0 }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.DARK_MODE] ?: false }

    val hasOnboarded: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.ONBOARDED] ?: false }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.BIOMETRIC_ENABLED] ?: false }

    suspend fun saveUser(uid: String, name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_UID] = uid
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_EMAIL] = email
        }
    }

    suspend fun updateCashBalance(balance: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CASH_BALANCE] = balance
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DARK_MODE] = enabled }
    }

    suspend fun setOnboarded() {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDED] = true }
    }

    suspend fun setBiometric(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun logout() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_UID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
        }
    }
}
