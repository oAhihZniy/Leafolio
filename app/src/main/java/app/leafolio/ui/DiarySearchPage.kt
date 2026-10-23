package app.leafolio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.leafolio.data.Diary
import app.leafolio.data.DiarySearchFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun DiarySearchPage(
    entries: List<Diary>,
    moods: List<String>,
    onBack: () -> Unit,
    entryContent: @Composable (Diary) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf<String?>(null) }
    var end by rememberSaveable { mutableStateOf<String?>(null) }
    var mood by rememberSaveable { mutableStateOf<String?>(null) }
    var tags by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val filter = DiarySearchFilter(query, start?.let(LocalDate::parse), end?.let(LocalDate::parse), mood, tags.toSet(), favoritesOnly)
    val results by produceState<List<Diary>>(emptyList(), entries, filter) {
        kotlinx.coroutines.delay(120)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { entries.filter { filter.matches(it) } }
    }
    val availableTags = remember(entries) { entries.flatMap { it.tags.split('|') }.filter(String::isNotBlank).distinct().sorted() }
    val filterCount = (if (start != null || end != null) 1 else 0) + (if (mood != null) 1 else 0) + tags.size + (if (favoritesOnly) 1 else 0)

    fun clearFilters() {
        start = null
        end = null
        mood = null
        tags = emptyList()
        favoritesOnly = false
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ChevronLeft, "返回") }
            Text("搜索日记", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { showFilters = true }) {
                Icon(Icons.Outlined.Tune, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (filterCount == 0) "筛选" else "筛选 $filterCount")
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer).padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            BasicTextField(
                query, { query = it }, Modifier.weight(1f).padding(start = 10.dp).padding(vertical = 15.dp),
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("搜索标题和正文", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                },
            )
            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                Icon(Icons.Outlined.Close, "清除关键词", modifier = Modifier.size(18.dp))
            } else Spacer(Modifier.width(10.dp))
        }
        if (filterCount > 0) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (start != null || end != null) item {
                    ActiveFilter("${start ?: "不限"} 至 ${end ?: "不限"}") { start = null; end = null }
                }
                if (mood != null) item { ActiveFilter(mood!!) { mood = null } }
                if (favoritesOnly) item { ActiveFilter("仅收藏") { favoritesOnly = false } }
                items(tags) { tag -> ActiveFilter(tag) { tags = tags - tag } }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${results.size} 篇日记", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (filterCount > 0) TextButton(onClick = { clearFilters() }) { Text("清空筛选") }
        }
        if (results.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                Text("没有找到日记", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
                Text("试试其他关键词，或调整筛选条件。", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(results, key = { it.id }) { entryContent(it) }
            }
        }
    }
    if (showFilters) SearchFilterSheet(
        entries = entries, moods = moods, availableTags = availableTags,
        applied = filter,
        onDismiss = { showFilters = false },
        onApply = {
            start = it.startDate?.toString()
            end = it.endDate?.toString()
            mood = it.mood
            tags = it.tags.toList()
            favoritesOnly = it.favoritesOnly
            showFilters = false
        },
    )
}

@Composable
private fun ActiveFilter(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true, onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Outlined.Close, "移除筛选：$label", modifier = Modifier.size(16.dp)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterSheet(
    entries: List<Diary>,
    moods: List<String>,
    availableTags: List<String>,
    applied: DiarySearchFilter,
    onDismiss: () -> Unit,
    onApply: (DiarySearchFilter) -> Unit,
) {
    var start by rememberSaveable { mutableStateOf(applied.startDate?.toString()) }
    var end by rememberSaveable { mutableStateOf(applied.endDate?.toString()) }
    var mood by rememberSaveable { mutableStateOf(applied.mood) }
    var tags by rememberSaveable { mutableStateOf(applied.tags.toList()) }
    var favoritesOnly by rememberSaveable { mutableStateOf(applied.favoritesOnly) }
    var pickingDate by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = DiarySearchFilter(applied.query, start?.let(LocalDate::parse), end?.let(LocalDate::parse), mood, tags.toSet(), favoritesOnly)
    val invalidRange = filter.startDate != null && filter.endDate != null && filter.startDate > filter.endDate
    val count = remember(entries, filter) { entries.count { filter.matches(it) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("筛选日记", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { start = null; end = null; mood = null; tags = emptyList(); favoritesOnly = false }) { Text("重置") }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterHeading("记录日期")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateFilterField("开始日期", start, Modifier.weight(1f), { pickingDate = "start" }, { start = null })
                DateFilterField("结束日期", end, Modifier.weight(1f), { pickingDate = "end" }, { end = null })
            }
            if (invalidRange) Text("结束日期不能早于开始日期", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            FilterHeading("心情")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mood == null, onClick = { mood = null }, label = { Text("不限") })
                moods.forEach { option -> FilterChip(selected = mood == option, onClick = { mood = if (mood == option) null else option }, label = { Text(option) }) }
            }
            FilterHeading("收藏")
            FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("仅看收藏日记") }, leadingIcon = if (favoritesOnly) ({ Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }) else null)
            FilterHeading("标签")
            Text("可多选，匹配其中任意一个标签。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            val visibleTags = (availableTags + tags).distinct().sorted()
            if (visibleTags.isEmpty()) Text("还没有标签", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                visibleTags.forEach { tag ->
                    FilterChip(selected = tag in tags, onClick = { tags = if (tag in tags) tags - tag else tags + tag }, label = { Text(tag) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
            Button(onClick = { onApply(filter) }, enabled = !invalidRange, modifier = Modifier.weight(1f)) {
                Text(if (invalidRange) "请调整日期" else "显示 $count 篇日记")
            }
        }
    }
    pickingDate?.let { target ->
        val initial = (if (target == "start") start else end)?.let(LocalDate::parse) ?: LocalDate.now()
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = null },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        if (target == "start") start = date else end = date
                    }
                    pickingDate = null
                }, enabled = picker.selectedDateMillis != null) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = null }) { Text("取消") } },
        ) { DatePicker(state = picker, title = { Text(if (target == "start") "开始日期" else "结束日期", Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    }
}

@Composable
private fun FilterHeading(title: String) {
    Text(title, modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun DateFilterField(label: String, value: String?, modifier: Modifier, onPick: () -> Unit, onClear: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onPick).padding(start = 12.dp, top = 10.dp, bottom = 6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value ?: "不限", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (value != null) IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Close, "清除$label", modifier = Modifier.size(16.dp)) }
            else Spacer(Modifier.height(40.dp).width(12.dp))
        }
    }
}
