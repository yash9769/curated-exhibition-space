package com.gallery.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_prefs")

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACTIVE_PROFILE_KEY = longPreferencesKey("active_profile_id")

    val activeProfileId: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_PROFILE_KEY] ?: 1L // Default profile ID is 1
    }

    suspend fun setActiveProfile(profileId: Long) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_PROFILE_KEY] = profileId
        }
    }
}
