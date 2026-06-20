package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.ui.theme.*

/**
 * ViMusic-style unified scaffold:
 *
 *  ┌────────────────────────────────────────────────┐
 *  │ ┌───┐                          BIG TITLE      │
 *  │ │ < │                                          │
 *  │ └───┘  [optional action chip]                  │
 *  │                                                │
 *  │   QuickPicks                                   │
 *  │   Songs                                        │
 *  │   Albums       <main scrollable content>       │
 *  │   Artists                                      │
 *  │   Library                                      │
 *  │                                                │
 *  │                              ╭───────╮         │
 *  │                              │   🔍  │ ← FAB  │
 *  │                              ╰───────╯         │
 *  └────────────────────────────────────────────────┘
 *
 *  - Left labels: ROTATED (-90°) text indicators, part of the same page,
 *    not a separate panel. Currently selected one is highlighted.
 *  - Big title in top-right corner.
 *  - Content scrolls vertically in the main area.
 *  - Floating search button bottom-right.
 */

data class ViMusicTab(
    val label: String,
    val icon: ImageVector? = null,
)

@Composable
fun ViMusicScaffold(
    title: String,
    tabs: List<ViMusicTab>,
    currentTab: Int,
    onTabSelect: (Int) -> Unit,
    onBack: (() -> Unit)? = null,
    actionChip: (@Composable () -> Unit)? = null,
    topActions: (@Composable RowScope.() -> Unit)? = null,
    floatingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Ink)) {

        // ── Top bar with optional back button + BIG TITLE on the right ──
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.ChevronLeft, null,
                        tint = TextHigh, modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(56.dp))
            }

            // Optional pill chip below back button
            actionChip?.invoke()

            Spacer(Modifier.weight(1f))

            topActions?.invoke(this)

            // BIG title
            Text(
                text = title,
                color = TextHigh,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                maxLines = 1,
            )
        }

        Row(Modifier.fillMaxSize().padding(top = 88.dp)) {

            // ── LEFT label indicators (rotated -90°, part of same page) ──
            ViMusicLeftLabels(
                tabs = tabs,
                currentTab = currentTab,
                onTabSelect = onTabSelect,
            )

            // ── MAIN content ──
            Box(Modifier.weight(1f).fillMaxHeight()) {
                content()
            }
        }

        // ── Floating action button (bottom-right) ──
        floatingAction?.let {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 88.dp)
            ) { it() }
        }
    }
}

/**
 * Vertical rotated labels (Quick picks · Songs · Playlists · Artists · Albums)
 * shown on the left side of the page — part of the same surface.
 */
@Composable
private fun ViMusicLeftLabels(
    tabs: List<ViMusicTab>,
    currentTab: Int,
    onTabSelect: (Int) -> Unit,
) {
    Column(
        Modifier
            .width(56.dp)
            .fillMaxHeight()
            .padding(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == currentTab
            Box(
                Modifier
                    .clickable { onTabSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.rotate(-90f),
                ) {
                    if (tab.icon != null) {
                        Icon(
                            tab.icon, null,
                            tint = if (selected) TextHigh else TextLow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = tab.label,
                        color = if (selected) TextHigh else TextLow,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Round pill chip — used for "Shuffle", "Enqueue", etc. below the back button.
 */
@Composable
fun ViMusicPillChip(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = InkRaised,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            color = TextHigh,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

/**
 * Floating Search FAB — bottom-right round button with magnifying glass.
 */
@Composable
fun ViMusicSearchFab(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = InkRaised,
        shadowElevation = 4.dp,
        modifier = Modifier.size(60.dp).clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Search, null,
                tint = TextHigh, modifier = Modifier.size(26.dp)
            )
        }
    }
}
