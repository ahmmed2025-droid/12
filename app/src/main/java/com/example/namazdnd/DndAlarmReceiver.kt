package com.example.namazdnd

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DndAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(PrayerScheduler.EXTRA_WAQT_KEY) ?: ""
        when (intent.action) {
            PrayerScheduler.ACTION_DND_ON -> handleDndOn(context, key)
            PrayerScheduler.ACTION_DND_OFF -> handleDndOff(
                context,
                intent.getLongExtra(PrayerScheduler.EXTRA_OFF_AT_MILLIS, 0L)
            )
        }
    }

    private fun handleDndOn(context: Context, key: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val durationMinutes = WaqtPrefs.getDurationMinutes(context)
        val offAt = System.currentTimeMillis() + durationMinutes * 60_000L

        if (notificationManager.isNotificationPolicyAccessGranted) {
            try {
                if (!WaqtPrefs.isActiveByApp(context)) {
                    WaqtPrefs.setPreviousFilter(context, notificationManager.currentInterruptionFilter)
                }
                WaqtPrefs.setActiveByApp(context, true)
                WaqtPrefs.setActiveUntilMillis(context, offAt)

                // Full DND: no notification sound/vibration interruption during namaz.
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            } catch (_: SecurityException) {
                // User may have revoked DND access after scheduling.
            }
        }

        if (key.isNotBlank()) {
            PrayerScheduler.scheduleDndOff(context, key, offAt)
            PrayerScheduler.scheduleDndOnForWaqt(context, key) // schedule next day
        }
    }

    private fun handleDndOff(context: Context, offAtFromAlarm: Long) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (!notificationManager.isNotificationPolicyAccessGranted) return

        // If a newer prayer/DND session extended the active time, ignore this older OFF alarm.
        val activeUntil = WaqtPrefs.getActiveUntilMillis(context)
        if (offAtFromAlarm > 0L && offAtFromAlarm < activeUntil - 5_000L) return

        if (WaqtPrefs.isActiveByApp(context)) {
            try {
                val previousFilter = WaqtPrefs.getPreviousFilter(
                    context,
                    NotificationManager.INTERRUPTION_FILTER_ALL
                )
                val targetFilter = if (previousFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                } else {
                    previousFilter
                }
                notificationManager.setInterruptionFilter(targetFilter)
            } catch (_: SecurityException) {
                // User may have revoked DND access after scheduling.
            } finally {
                WaqtPrefs.setActiveByApp(context, false)
                WaqtPrefs.setActiveUntilMillis(context, 0L)
            }
        }
    }
}
