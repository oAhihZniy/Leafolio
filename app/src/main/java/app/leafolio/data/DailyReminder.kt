package app.leafolio.data

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import app.leafolio.MainActivity
import app.leafolio.R
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

object DailyReminder {
    private const val ID = 2002
    private const val CHANNEL = "daily_reminders"
    fun nextDue(minutes: Int, now: ZonedDateTime): ZonedDateTime? {
        if (minutes !in 0..1439) return null
        val today = now.toLocalDate().atTime(minutes / 60, minutes % 60).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(minutes / 60, minutes % 60).atZone(now.zone)
    }
    fun allowed(context: Context): Boolean =
        (android.os.Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) &&
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled() &&
        context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    fun sync(context: Context, prefs: UserPreferences) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "写日记提醒", NotificationManager.IMPORTANCE_DEFAULT))
        val alarm = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, ID, Intent(context, DailyReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarm.cancel(pending)
        val due = nextDue(prefs.dailyReminderMinutes, ZonedDateTime.now())
        if (due == null) { manager.cancel(ID); return }
        alarm.setWindow(AlarmManager.RTC_WAKEUP, due.toInstant().toEpochMilli(), 15 * 60_000L, pending)
    }
    fun notify(context: Context) {
        if (!allowed(context)) return
        val open = PendingIntent.getActivity(context, ID, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(NotificationManager::class.java).notify(ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_backup_notification).setContentTitle("为今天留下一页")
            .setContentText("有空时，记录此刻的心情与生活。")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build())
    }
}
class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    val prefs = UserPreferencesStore(context).preferences.first()
                    if (prefs.dailyReminderMinutes >= 0) DailyReminder.notify(context)
                    DailyReminder.sync(context, prefs)
                }
            } finally { result.finish() }
        }
    }
}
