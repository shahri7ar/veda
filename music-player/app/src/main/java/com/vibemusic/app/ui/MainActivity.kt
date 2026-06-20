package com.vibemusic.app.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.vibemusic.app.playback.MusicService
import com.vibemusic.app.ui.screens.RootScreen
import com.vibemusic.app.ui.theme.VibeMusicTheme

class MainActivity : ComponentActivity() {

    private val vm: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Boot the playback service.
        val svc = Intent(this, MusicService::class.java).apply {
            component = ComponentName(this@MainActivity, MusicService::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }

        setContent {
            VibeMusicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootScreen(vm)
                }
            }
        }
    }
}
