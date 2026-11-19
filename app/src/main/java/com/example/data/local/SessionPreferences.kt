package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "agrisense_session")

class SessionPreferences(private val context: Context) {
  private val keyLoggedInUserId = longPreferencesKey("logged_in_user_id")
  private val keyLanguageCode = stringPreferencesKey("language_code")

  val loggedInUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
    prefs[keyLoggedInUserId]?.takeIf { it > 0L }
  }

  val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
    AppLanguage.fromCode(prefs[keyLanguageCode])
  }

  suspend fun setLoggedInUser(userId: Long?) {
    context.dataStore.edit { prefs ->
      if (userId == null || userId <= 0L) {
        prefs.remove(keyLoggedInUserId)
      } else {
        prefs[keyLoggedInUserId] = userId
      }
    }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { prefs ->
      prefs[keyLanguageCode] = language.code
    }
  }
}
