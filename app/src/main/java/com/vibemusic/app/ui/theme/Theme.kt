package com.vibemusic.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── ViMusic-inspired palette ── (PUBLIC — exposed to screens)

/** Deepest background colour (almost-black ink). */
val Ink         = Color(0xFF0F0F14)
/** Surface — slightly lighter than Ink (used for cards, mini-player, etc.). */
val InkSurface  = Color(0xFF16161D)
/** Raised surface — used for icon backgrounds, chips, buttons. */
val InkRaised   = Color(0xFF1E1E27)
/** Hover / pressed colour. */
val InkHover    = Color(0xFF2A2A35)

/** Brand primary — Lilac. */
val Primary     = Color(0xFFB8A9E8)
val OnPrimary   = Color(0xFF1A1A1A)
/** Secondary accent — Amber (used for favorites star). */
val Secondary   = Color(0xFFF5A623)
/** Tertiary accent — Teal (used for cached / offline indicators). */
val Tertiary    = Color(0xFF4ECDC4)

/** Highest-emphasis text. */
val TextHigh    = Color(0xFFF5F5F7)
/** Medium-emphasis text. */
val TextMid     = Color(0xFFB0B0BB)
/** Low-emphasis text. */
val TextLow     = Color(0xFF6B6B78)

/** Hairline border / outline. */
val OutlineColor    = Color(0xFF2A2A35)
val OutlineVariant  = Color(0xFF22222C)

private val VibeDark = darkColorScheme(
    background = Ink,
    surface = InkSurface,
    surfaceVariant = InkRaised,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkHover,
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    tertiary = Tertiary,
    onBackground = TextHigh,
    onSurface = TextHigh,
    onSurfaceVariant = TextMid,
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
)

private val VibeLight = lightColorScheme(
    background = Color(0xFFFAFAF8),
    surface = Color.White,
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    tertiary = Tertiary,
)

@Composable
fun VibeMusicTheme(content: @Composable () -> Unit) {
    // ViMusic is famously dark — force dark unless system explicitly prefers light
    val dark = true || isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) VibeDark else VibeLight,
        content = content,
    )
}
