package com.vibemusic.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.vibemusic.app.R
import com.vibemusic.app.playback.PlayerHub
import com.vibemusic.app.ui.MainActivity

/**
 * Home-screen playback widget. Polls the [PlayerHub] for current track + state
 * and renders into music_widget.xml. Actions are sent via PendingIntent broadcasts.
 */
class MusicWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> render(context, mgr, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> PlayerHub.player()?.let { if (it.isPlaying) it.pause() else it.play() }
            ACTION_NEXT       -> PlayerHub.player()?.seekToNext()
            ACTION_PREV       -> PlayerHub.player()?.seekToPrevious()
            ACTION_REFRESH    -> { /* fallthrough to update below */ }
        }
        // refresh all widget instances
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, MusicWidgetReceiver::class.java))
        onUpdate(context, mgr, ids)
    }

    private fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
        val rv = RemoteViews(context.packageName, R.layout.music_widget)
        val snap = PlayerHub.state.value

        rv.setTextViewText(R.id.widget_title, snap.title.ifBlank { "Not Playing" })
        rv.setTextViewText(R.id.widget_artist, snap.artist)
        rv.setImageViewResource(
            R.id.widget_play_pause,
            if (snap.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )

        rv.setOnClickPendingIntent(R.id.widget_play_pause, broadcast(context, ACTION_PLAY_PAUSE))
        rv.setOnClickPendingIntent(R.id.widget_next, broadcast(context, ACTION_NEXT))
        rv.setOnClickPendingIntent(R.id.widget_prev, broadcast(context, ACTION_PREV))

        // tapping the artwork opens the app
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        rv.setOnClickPendingIntent(R.id.widget_album_art, openApp)

        mgr.updateAppWidget(widgetId, rv)
    }

    private fun broadcast(ctx: Context, action: String) =
        PendingIntent.getBroadcast(
            ctx, action.hashCode(),
            Intent(ctx, MusicWidgetReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    companion object {
        const val ACTION_PLAY_PAUSE = "com.vibemusic.app.widget.PLAY_PAUSE"
        const val ACTION_NEXT       = "com.vibemusic.app.widget.NEXT"
        const val ACTION_PREV       = "com.vibemusic.app.widget.PREV"
        const val ACTION_REFRESH    = "com.vibemusic.app.widget.REFRESH"

        fun requestUpdate(ctx: Context) {
            val intent = Intent(ctx, MusicWidgetReceiver::class.java)
                .setAction(ACTION_REFRESH)
            ctx.sendBroadcast(intent)
        }
    }
}
