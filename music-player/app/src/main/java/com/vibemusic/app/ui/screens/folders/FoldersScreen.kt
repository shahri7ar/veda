package com.vibemusic.app.ui.screens.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.ui.screens.ViMusicScaffold
import com.vibemusic.app.ui.screens.ViMusicSearchFab
import com.vibemusic.app.ui.screens.ViMusicTab
import com.vibemusic.app.ui.theme.*

/**
 * Folders screen — unified ViMusic layout.
 *
 * Left labels: Linear · Tree (the two folder-browse modes — Musicolet-style)
 * Big "Folders" title top-right
 * Floating search FAB
 */
@Composable
fun FoldersScreen(
    onSearch: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    var modeIndex by remember { mutableIntStateOf(0) }
    val folders = remember(modeIndex) {
        when (modeIndex) {
            0 -> FolderScanner.scanLinear()
            1 -> FolderScanner.scanHierarchical()
            else -> emptyList()
        }
    }

    val tabs = listOf(
        ViMusicTab("Linear", Icons.Rounded.List),
        ViMusicTab("Tree", Icons.Rounded.AccountTree),
    )

    ViMusicScaffold(
        title = "Folders",
        tabs = tabs,
        currentTab = modeIndex,
        onTabSelect = { modeIndex = it },
        onBack = onBack,
        floatingAction = { ViMusicSearchFab(onSearch) },
    ) {
        if (folders.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.FolderOpen, null,
                        tint = TextLow, modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No music folders found", color = TextLow)
                }
            }
        } else {
            when (modeIndex) {
                0 -> LinearFolderList(folders)
                1 -> HierarchicalFolderList(folders)
            }
        }
    }
}

@Composable
private fun LinearFolderList(folders: List<FolderScanner.MusicFolder>) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(folders) { folder ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { /* TODO open folder */ }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(InkRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Folder, null, tint = Primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        folder.name, color = TextHigh,
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1,
                    )
                    Text(
                        folder.path, color = TextLow,
                        fontSize = 11.sp, maxLines = 1,
                    )
                }
                Text(
                    "${folder.trackCount}", color = TextMid, fontSize = 13.sp,
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = TextLow)
            }
        }
    }
}

@Composable
private fun HierarchicalFolderList(folders: List<FolderScanner.MusicFolder>) {
    var expanded by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        fun render(items: List<FolderScanner.MusicFolder>, depth: Int) {
            items.forEach { folder ->
                item(key = folder.path) {
                    val isExpanded = folder.path in expanded
                    val canExpand = folder.subFolders.isNotEmpty()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                expanded = if (isExpanded) expanded - folder.path
                                else expanded + folder.path
                            }
                            .padding(
                                start = (depth * 18).dp,
                                top = 10.dp, bottom = 10.dp, end = 8.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            when {
                                canExpand && isExpanded -> Icons.Rounded.FolderOpen
                                canExpand -> Icons.Rounded.Folder
                                else -> Icons.Rounded.Folder
                            },
                            null,
                            tint = if (canExpand) Primary else Tertiary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                folder.name, color = TextHigh,
                                fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1,
                            )
                        }
                        Text(
                            "${folder.trackCount}", color = TextLow, fontSize = 12.sp,
                        )
                        if (canExpand) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isExpanded) Icons.Rounded.ExpandLess
                                else Icons.Rounded.ExpandMore,
                                null, tint = TextLow, modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                if (folder.path in expanded && folder.subFolders.isNotEmpty()) {
                    render(folder.subFolders, depth + 1)
                }
            }
        }
        render(folders, 0)
    }
}
