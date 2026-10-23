package app.leafolio

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.leafolio.data.Diary
import app.leafolio.data.ExportScope
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ExportPage(entries: List<Diary>, onBack: () -> Unit, onExport: (ExportScope) -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf("all") }
    var start by rememberSaveable { mutableStateOf<String?>(null) }
    var end by rememberSaveable { mutableStateOf<String?>(null) }
    var selected by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val scope = ExportScope(mode, start?.let(LocalDate::parse), end?.let(LocalDate::parse), selected.toSet())
    val count = if (scope.valid) scope.select(entries).size else 0
    BackHandler(onBack = onBack)
    fun pick(value: String?, change: (String) -> Unit) {
        val date = value?.let(LocalDate::parse) ?: LocalDate.now()
        DatePickerDialog(context, { _, y, m, d -> change(LocalDate.of(y, m + 1, d).toString()) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 24.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ChevronLeft, "返回") }
            Text("导出日记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            item {
                Text("把记录带走", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp))
                Text("导出 Markdown、照片和 JSON 元数据，只包含正式日记。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 18.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "全部日记", "dates" to "日期范围", "selected" to "选择日记").forEach { (key, title) ->
                        FilterChip(selected = mode == key, onClick = { mode = key }, label = { Text(title) })
                    }
                }
                if (mode == "dates") {
                    Text("按记录日期筛选，包含起止当天，可只选一端。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { pick(start) { start = it } }) { Text(start ?: "开始日期") }
                        if (start != null) TextButton(onClick = { start = null }) { Text("清除开始日期") }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { pick(end) { end = it } }) { Text(end ?: "结束日期") }
                        if (end != null) TextButton(onClick = { end = null }) { Text("清除结束日期") }
                    }
                    if (!scope.valid) Text("开始日期不能晚于结束日期", color = MaterialTheme.colorScheme.error)
                }
                if (mode == "selected") Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("已选 $count 篇", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { selected = ArrayList(entries.map { it.id }) }) { Text("全选") }
                    TextButton(onClick = { selected = arrayListOf() }) { Text("清空选择") }
                }
                if (entries.isEmpty()) Text("还没有可导出的正式日记。", Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (mode == "selected") items(entries, key = { it.id }) { diary ->
                val checked = diary.id in selected
                Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) {
                    selected = ArrayList(if (checked) selected - diary.id else selected + diary.id)
                }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(diary.title.ifBlank { diary.content.ifBlank { "无标题日记" } }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                        Text(Instant.ofEpochMilli(diary.entryDate).atZone(ZoneId.systemDefault()).toLocalDate().toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("导出 $count 篇", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled = scope.valid && count > 0, onClick = { onExport(scope) }) { Text("选择保存位置") }
        }
    }
}
