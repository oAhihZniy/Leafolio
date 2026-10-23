package app.leafolio.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import app.leafolio.MainActivity
import app.leafolio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

object BackupReminder {
    private const val DAY = 86_400_000L
    private const val CHANNEL = "backup_reminders"
    private const val ID = 2001

    fun nextDue(prefs: UserPreferences, now: Long): Long? {
        if (prefs.backupReminderDays !in setOf(7, 30)) return null
        val anchor = maxOf(prefs.lastBackupAt, prefs.reminderEnabledAt, prefs.lastBackupReminderAt)
        return (if (anchor > now || anchor <= 0) now else anchor) + prefs.backupReminderDays * DAY
    }

    fun notificationsAllowed(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled() &&
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun sync(context: Context, prefs: UserPreferences) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "备份提醒", NotificationManager.IMPORTANCE_DEFAULT))
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, ID, Intent(context, BackupReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(pending)
        val now = System.currentTimeMillis()
        val due = nextDue(prefs, now)
        if (due == null) { manager.cancel(ID); return }
        alarm.setInexactRepeating(AlarmManager.RTC, maxOf(due, now + 60_000), DAY, pending)
    }

    suspend fun notifyIfDue(context: Context, now: Long = System.currentTimeMillis()) {
        val store = UserPreferencesStore(context)
        val prefs = store.preferences.first()
        val due = nextDue(prefs, now) ?: return
        if (now < due || !notificationsAllowed(context)) return
        val open = PendingIntent.getActivity(context, ID, Intent(context, MainActivity::class.java).putExtra("open_backup_settings", true), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_backup_notification)
            .setContentTitle("为一页留一份备份")
            .setContentText("打开设置，手动创建完整备份，妥善保存你的记录。")
            .setContentIntent(open).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(ID, notification)
        store.markBackupReminder(now)
    }

    fun cancelNotification(context: Context) = context.getSystemService(NotificationManager::class.java).cancel(ID)
}

class BackupReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    if (intent.action == null) BackupReminder.notifyIfDue(context)
                    else {
                        val prefs = UserPreferencesStore(context).preferences.first()
                        BackupReminder.sync(context, prefs)
                        DailyReminder.sync(context, prefs)
                    }
                }
            } finally { pending.finish() }
        }
    }
}
