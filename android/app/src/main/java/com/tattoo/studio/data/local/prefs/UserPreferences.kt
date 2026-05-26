package com.tattoo.studio.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val USER_ROLE_KEY = stringPreferencesKey("user_role")
        val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }

    val userRoleFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ROLE_KEY]
    }

    suspend fun saveUserInfo(role: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ROLE_KEY] = role
            prefs[USER_EMAIL_KEY] = email
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
