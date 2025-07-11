package com.strling.notifyagarden

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "Settings")

class NotifyDataStore(private val context: Context) {
    val favorites: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[favoritesKey] ?: emptySet()
    }

    suspend fun setFavorites(favorites: Set<String>)
    {
        context.dataStore.edit { settings ->
            settings[favoritesKey] = favorites
        }
    }

    companion object
    {
        val favoritesKey = stringSetPreferencesKey("favorites")
    }
}