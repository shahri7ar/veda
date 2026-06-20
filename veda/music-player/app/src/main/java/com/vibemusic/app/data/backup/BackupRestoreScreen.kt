package com.vibemusic.app.data.backup

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibemusic.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupRestoreScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backups by remember { mutableStateOf(BackupManager.listBackups(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Column(
        Modifier.fillMaxSize().background(Ink).systemBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Backup & Restore", color = TextHigh,
                fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Close") }
        }

        // Create backup button
        Button(
            onClick = {
                scope.launch {
                    try {
                        val file = BackupManager.createBackup(context)
                        backups = BackupManager.listBackups(context)
                        message = "Backup created: ${file.name}"
                    } catch (e: Exception) {
                        message = "Error: ${e.message}"
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Icon(Icons.Rounded.Backup, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create Backup")
        }

        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Tertiary, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp))
        }

        Spacer(Modifier.height(16.dp))

        if (backups.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Backup, null,
                        tint = TextLow, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No backups yet", color = TextLow)
                }
            }
        } else {
            Text("Existing backups", color = TextMid, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(backups) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = InkRaised),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Description, null,
                                tint = Primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.name, color = TextHigh, fontSize = 13.sp, maxLines = 1)
                                Text(dateFmt.format(Date(file.lastModified())),
                                    color = TextLow, fontSize = 11.sp)
                                Text(formatFileSize(file.length()),
                                    color = TextLow, fontSize = 11.sp)
                            }
                            FilledTonalButton(onClick = {
                                scope.launch {
                                    try {
                                        val count = BackupManager.restoreFromFile(context, file)
                                        message = "Restored $count items"
                                    } catch (e: Exception) {
                                        message = "Restore failed: ${e.message}"
                                    }
                                }
                            }) { Text("Restore", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
