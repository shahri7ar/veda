package com.vibemusic.app.ui.screens

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
import com.vibemusic.app.data.model.MountConfig
import com.vibemusic.app.data.model.SourceType
import com.vibemusic.app.ui.MusicViewModel
import com.vibemusic.app.ui.theme.Ink
import com.vibemusic.app.ui.theme.TextHigh
import kotlinx.coroutines.launch

@Composable
fun MountsScreen(
    vm: MusicViewModel,
    onPlay: () -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val mounts by vm.mounts.collectAsState()
    val busy by vm.busy.collectAsState()
    var editing by remember { mutableStateOf<MountConfig?>(null) }

    Box(Modifier.fillMaxSize().background(Ink).systemBarsPadding()) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                Icon(Icons.Rounded.Close, null, tint = TextHigh)
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = if (onBack != null) 56.dp else 16.dp,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionHeader("Network mounts")
            }
            if (mounts.isEmpty()) {
                item {
                    Text(
                        "Tap + to add an SMB share or FTP server. Files are streamed and cached on first play.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(mounts, key = { it.id }) { m ->
                MountCard(m,
                    busy = busy,
                    onEdit = { editing = m },
                    onScan = { vm.scan(m) },
                    onDelete = { vm.deleteMount(m.id) }
                )
            }
        }
        FloatingActionButton(
            onClick = { editing = MountConfig(
                id = "", name = "New mount", type = SourceType.SMB,
                host = "", port = 0, share = "", path = "/", username = "", password = "", domain = ""
            ) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Rounded.Add, null)
        }
    }

    editing?.let { cfg ->
        MountEditorDialog(
            initial = cfg,
            onDismiss = { editing = null },
            onSave = { vm.addOrUpdateMount(it); editing = null },
            vm = vm
        )
    }
}

@Composable
private fun MountCard(
    m: MountConfig,
    busy: Boolean,
    onEdit: () -> Unit,
    onScan: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val accent = if (m.type == SourceType.SMB)
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (m.type == SourceType.SMB) Icons.Rounded.Storage else Icons.Rounded.Cloud,
                    null, tint = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text("${m.type} · ${m.host}${if (m.share.isNotBlank()) "/${m.share}" else ""}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onScan, enabled = !busy) {
                if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Icon(Icons.Rounded.Sync, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MountEditorDialog(
    initial: MountConfig,
    vm: MusicViewModel,
    onDismiss: () -> Unit,
    onSave: (MountConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var type by remember { mutableStateOf(initial.type) }
    var host by remember { mutableStateOf(initial.host) }
    var port by remember { mutableStateOf(if (initial.port > 0) initial.port.toString() else "") }
    var share by remember { mutableStateOf(initial.share) }
    var path by remember { mutableStateOf(initial.path) }
    var user by remember { mutableStateOf(initial.username) }
    var pass by remember { mutableStateOf(initial.password) }
    var domain by remember { mutableStateOf(initial.domain) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(initial.copy(
                    name = name, type = type, host = host,
                    port = port.toIntOrNull() ?: 0,
                    share = share, path = path.ifBlank { "/" },
                    username = user, password = pass, domain = domain
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (initial.id.isBlank()) "Add mount" else "Edit mount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // type chooser
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == SourceType.SMB, onClick = { type = SourceType.SMB }, label = { Text("SMB") })
                    FilterChip(selected = type == SourceType.FTP, onClick = { type = SourceType.FTP }, label = { Text("FTP") })
                }
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(host, { host = it }, label = { Text("Host (e.g. 192.168.1.10)") }, singleLine = true)
                OutlinedTextField(port, { port = it.filter(Char::isDigit) }, label = { Text("Port (blank = default)") }, singleLine = true)
                if (type == SourceType.SMB) {
                    OutlinedTextField(share, { share = it }, label = { Text("Share name") }, singleLine = true)
                    OutlinedTextField(domain, { domain = it }, label = { Text("Domain / workgroup (optional)") }, singleLine = true)
                }
                OutlinedTextField(path, { path = it }, label = { Text("Root path") }, singleLine = true)
                OutlinedTextField(user, { user = it }, label = { Text("Username (blank = anonymous)") }, singleLine = true)
                OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        enabled = !testing,
                        onClick = {
                            testing = true
                            testResult = null
                            scope.launch {
                                val cfg = initial.copy(
                                    name = name, type = type, host = host,
                                    port = port.toIntOrNull() ?: 0,
                                    share = share, path = path, username = user,
                                    password = pass, domain = domain
                                )
                                val r = vm.test(cfg)
                                testResult = r.fold({ "Connection OK" }, { "Failed: ${it.message}" })
                                testing = false
                            }
                        }
                    ) {
                        if (testing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        else Text("Test connection")
                    }
                    testResult?.let {
                        Text(
                            it,
                            color = if (it.startsWith("Connection")) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    )
}
