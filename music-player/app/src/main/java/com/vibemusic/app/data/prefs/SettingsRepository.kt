package com.vibemusic.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {

    private val KEY_MAX_CACHE_MB = intPreferencesKey("max_cache_mb")
    private val KEY_CACHE_ON_PLAY = booleanPreferencesKey("cache_on_play")
    private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
    private val KEY_AUTO_SCAN_ON_START = booleanPreferencesKey("auto_scan_on_start")

    val maxCacheMb get() = context.dataStore.data.map { it[KEY_MAX_CACHE_MB] ?: 1024 }
    val cacheOnPlay get() = context.dataStore.data.map { it[KEY_CACHE_ON_PLAY] ?: true }
    val wifiOnly get() = context.dataStore.data.map { it[KEY_WIFI_ONLY] ?: false }
    val autoScanOnStart get() = context.dataStore.data.map { it[KEY_AUTO_SCAN_ON_START] ?: true }

    suspend fun setMaxCacheMb(v: Int)        = context.dataStore.edit { it[KEY_MAX_CACHE_MB] = v }
    suspend fun setCacheOnPlay(v: Boolean)   = context.dataStore.edit { it[KEY_CACHE_ON_PLAY] = v }
    suspend fun setWifiOnly(v: Boolean)      = context.dataStore.edit { it[KEY_WIFI_ONLY] = v }
    suspend fun setAutoScan(v: Boolean)      = context.dataStore.edit { it[KEY_AUTO_SCAN_ON_START] = v }

    /** Encrypted storage for SMB/FTP credentials. */
    fun secure(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context, "vibemusic_secure", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
