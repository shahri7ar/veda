package com.vibemusic.app.ui

import android.content.ComponentName
import android.content.Intent
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

        // Boot the playback service. Use a plain (non-foreground) start: MusicService
        // promotes itself to a foreground service only once playback actually begins
        // (Media3's MediaSessionService calls startForeground() from within its own
        // notification/player-state handling). Calling startForegroundService() here,
        // before any track is playing, risks the OS killing the app with
        // ForegroundServiceDidNotStartInTimeException if the user just browses the
        // library without pressing play within ~5 seconds of launch.
        val svc = Intent(this, MusicService::class.java).apply {
            component = ComponentName(this@MainActivity, MusicService::class.java)
        }
        startService(svc)

        setContent {
            VibeMusicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootScreen(vm)
                }
            }
        }
    }
}
