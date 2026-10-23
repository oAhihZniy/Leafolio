package app.leafolio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.leafolio.data.Diary

@Composable
fun DraftsPage(entries: List<Diary>, onBack: () -> Unit, onOpen: (String) -> Unit, onTrash: (Diary) -> Unit) {
    var removing by remember { mutableStateOf<Diary?>(null) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ChevronLeft, "返回") }
            Text("草稿", style = MaterialTheme.typography.titleLarge)
        }
        if (entries.isEmpty()) Text("没有未完成的草稿", modifier = Modifier.padding(top = 32.dp))
        LazyColumn {
            items(entries, key = { it.id }) { entry ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onOpen(entry.id) }, modifier = Modifier.weight(1f)) {
                        Text(entry.title.ifBlank { entry.content.ifBlank { "照片草稿" } }, maxLines = 2)
                    }
                    IconButton(onClick = { removing = entry }) { Icon(Icons.Outlined.DeleteOutline, "删除草稿") }
                }
                HorizontalDivider()
            }
        }
    }
    removing?.let { entry -> AlertDialog(onDismissRequest = { removing = null }, title = { Text("删除这篇草稿？") },
        text = { Text("草稿将移入回收站，保留 30 天。") },
        confirmButton = { TextButton(onClick = { onTrash(entry); removing = null }) { Text("移到回收站") } },
        dismissButton = { TextButton(onClick = { removing = null }) { Text("取消") } }) }
}
