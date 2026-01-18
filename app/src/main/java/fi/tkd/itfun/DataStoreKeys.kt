package fi.tkd.itfun

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "app_prefs")

object PrefKeys {
    val PRIMARY_COLOR = intPreferencesKey("primary_color")
    val CONTENT_READY = booleanPreferencesKey("content_ready")

}