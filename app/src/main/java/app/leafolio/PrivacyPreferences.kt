package app.leafolio

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.leafolio.data.UserPreferences

@Composable
fun PrivacyPreferences(prefs: UserPreferences, change: (Int?, Boolean?, Int?) -> Unit) {
    val context = LocalContext.current
    val activity = context as? PrivateActivity
    var allowed by remember { mutableStateOf(app.leafolio.data.DailyReminder.allowed(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        allowed = app.leafolio.data.DailyReminder.allowed(context)
        onPauseOrDispose { }
    }
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text("每日提醒", style = MaterialTheme.typography.titleSmall)
            Text(if (prefs.dailyReminderMinutes < 0) "关闭" else "%02d:%02d".format(prefs.dailyReminderMinutes / 60, prefs.dailyReminderMinutes % 60), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = prefs.dailyReminderMinutes >= 0, onCheckedChange = { checked ->
            if (!checked) change(null, null, -1)
            else TimePickerDialog(context, { _, h, m -> change(null, null, h * 60 + m) }, 21, 0, true).show()
        })
    }
    if (prefs.dailyReminderMinutes >= 0) {
        TextButton(onClick = { TimePickerDialog(context, { _, h, m -> change(null, null, h * 60 + m) }, prefs.dailyReminderMinutes / 60, prefs.dailyReminderMinutes % 60, true).show() }) { Text("修改提醒时间") }
        if (!allowed) {
            Text("系统通知未开启，提醒暂时无法送达。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("打开通知设置") }
        }
    }
    Text("隐私", modifier = Modifier.padding(top = 22.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    Text("应用锁", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(-1 to "关闭", 0 to "立即", 60 to "1 分钟").forEach { (value, label) ->
            FilterChip(selected = prefs.lockDelaySeconds == value, onClick = {
                if (prefs.lockDelaySeconds != value) activity?.authenticate { success -> if (success) change(value, null, null) }
            }, label = { Text(label) })
        }
    }
    Text("使用设备锁屏凭证或生物识别；只控制打开 App，不加密文件。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text("遮挡后台预览", style = MaterialTheme.typography.titleSmall)
            Text("隐藏最近任务中的内容，同时禁止截屏与录屏。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = prefs.hidePreview, onCheckedChange = { change(null, it, null) })
    }
}
