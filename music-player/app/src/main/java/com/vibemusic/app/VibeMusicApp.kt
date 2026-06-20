package com.vibemusic.app

import android.app.Application
import com.vibemusic.app.data.AppDatabase
import com.vibemusic.app.data.cache.CacheManager
import com.vibemusic.app.data.prefs.SettingsRepository

class VibeMusicApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val cacheManager by lazy { CacheManager(this) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VibeMusicApp
            private set
    }
}
