package com.example.namazdnd

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object PrayerScheduler {
    const val ACTION_DND_ON = "com.example.namazdnd.ACTION_DND_ON"
    const val ACTION_DND_OFF = "com.example.namazdnd.ACTION_DND_OFF"
    const val EXTRA_WAQT_KEY = "extra_waqt_key"
    const val EXTRA_OFF_AT_MILLIS = "extra_off_at_millis"

    private fun alarmManager(context: Context): AlarmManager {
        return context.getSystemService(AlarmManager::class.java)
    }

    fun scheduleAll(context: Context): Int {
        var count = 0
        WaqtPrefs.waqts.forEach { waqt ->
            if (WaqtPrefs.isEnabled(context, waqt.key)) {
                scheduleDndOnForWaqt(context, waqt.key)
                count++
            } else {
                cancelDndOnForWaqt(context, waqt.key)
            }
        }
        return count
    }

    fun scheduleDndOnForWaqt(context: Context, key: String) {
        if (!WaqtPrefs.isEnabled(context, key)) {
            cancelDndOnForWaqt(context, key)
            return
        }

        val triggerAt = nextDndOnMillis(context, key)
        val intent = Intent(context, DndAlarmReceiver::class.java).apply {
            action = ACTION_DND_ON
            putExtra(EXTRA_WAQT_KEY, key)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            onRequestCode(key),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(context, triggerAt, pendingIntent)
    }

    fun cancelDndOnForWaqt(context: Context, key: String) {
        val intent = Intent(context, DndAlarmReceiver::class.java).apply { action = ACTION_DND_ON }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            onRequestCode(key),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager(context).cancel(pendingIntent)
    }

    fun scheduleDndOff(context: Context, key: String, offAtMillis: Long) {
        val intent = Intent(context, DndAlarmReceiver::class.java).apply {
            action = ACTION_DND_OFF
            putExtra(EXTRA_WAQT_KEY, key)
            putExtra(EXTRA_OFF_AT_MILLIS, offAtMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            offRequestCode(key),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarm(context, offAtMillis, pendingIntent)
    }

    fun nextDndOnMillis(context: Context, key: String): Long {
        val prayerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, WaqtPrefs.getHour(context, key))
            set(Calendar.MINUTE, WaqtPrefs.getMinute(context, key))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -WaqtPrefs.PRE_DND_MINUTES)
        }

        val now = System.currentTimeMillis()
        if (prayerTime.timeInMillis <= now) {
            prayerTime.add(Calendar.DATE, 1)
        }
        return prayerTime.timeInMillis
    }

    private fun setAlarm(context: Context, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val manager = alarmManager(context)
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms() -> {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Fallback if user did not grant Alarms & reminders access. It may run a little late.
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                else -> {
                    manager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        } catch (_: SecurityException) {
            // Last-resort fallback for devices that reject exact alarms.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                manager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        }
    }

    private fun onRequestCode(key: String): Int {
        return when (key) {
            "fajr" -> 1001
            "dhuhr" -> 1002
            "asr" -> 1003
            "maghrib" -> 1004
            "isha" -> 1005
            else -> 1099
        }
    }

    private fun offRequestCode(key: String): Int {
        return when (key) {
            "fajr" -> 2001
            "dhuhr" -> 2002
            "asr" -> 2003
            "maghrib" -> 2004
            "isha" -> 2005
            else -> 2099
        }
    }
}
