package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserIdProvider(private val context: Context) {

    companion object {
        private val USER_ID_KEY = stringPreferencesKey("anonymous_user_id")
    }

    suspend fun getUserId(): String {
        val existing = context.dataStore.data.map { prefs ->
            prefs[USER_ID_KEY]
        }.first()
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = newId
        }
        return newId
    }
}
