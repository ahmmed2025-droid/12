package com.example.namazdnd

import android.content.Context
import kotlin.math.max
import kotlin.math.min

data class Waqt(
    val key: String,
    val label: String,
    val defaultHour: Int,
    val defaultMinute: Int
)

object WaqtPrefs {
    const val PREFS_NAME = "namaz_dnd_prefs"
    const val PRE_DND_MINUTES = 5

    private const val KEY_DURATION = "duration_minutes"
    private const val KEY_ACTIVE_BY_APP = "active_by_app"
    private const val KEY_PREVIOUS_FILTER = "previous_filter"
    private const val KEY_ACTIVE_UNTIL = "active_until_millis"

    val waqts = listOf(
        Waqt("fajr", "Fajr", 5, 0),
        Waqt("dhuhr", "Dhuhr", 13, 0),
        Waqt("asr", "Asr", 16, 30),
        Waqt("maghrib", "Maghrib", 18, 0),
        Waqt("isha", "Isha", 20, 0)
    )

    fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWaqt(key: String): Waqt = waqts.first { it.key == key }

    fun getHour(context: Context, key: String): Int {
        val waqt = getWaqt(key)
        return prefs(context).getInt("${key}_hour", waqt.defaultHour)
    }

    fun getMinute(context: Context, key: String): Int {
        val waqt = getWaqt(key)
        return prefs(context).getInt("${key}_minute", waqt.defaultMinute)
    }

    fun setTime(context: Context, key: String, hour: Int, minute: Int) {
        prefs(context).edit()
            .putInt("${key}_hour", hour)
            .putInt("${key}_minute", minute)
            .apply()
    }

    fun isEnabled(context: Context, key: String): Boolean {
        return prefs(context).getBoolean("${key}_enabled", true)
    }

    fun setEnabled(context: Context, key: String, enabled: Boolean) {
        prefs(context).edit().putBoolean("${key}_enabled", enabled).apply()
    }

    fun getDurationMinutes(context: Context): Int {
        return prefs(context).getInt(KEY_DURATION, 15)
    }

    fun setDurationMinutes(context: Context, minutes: Int) {
        val safe = min(240, max(1, minutes))
        prefs(context).edit().putInt(KEY_DURATION, safe).apply()
    }

    fun setActiveByApp(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_ACTIVE_BY_APP, active).apply()
    }

    fun isActiveByApp(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ACTIVE_BY_APP, false)
    }

    fun setPreviousFilter(context: Context, filter: Int) {
        prefs(context).edit().putInt(KEY_PREVIOUS_FILTER, filter).apply()
    }

    fun getPreviousFilter(context: Context, fallback: Int): Int {
        return prefs(context).getInt(KEY_PREVIOUS_FILTER, fallback)
    }

    fun setActiveUntilMillis(context: Context, millis: Long) {
        prefs(context).edit().putLong(KEY_ACTIVE_UNTIL, millis).apply()
    }

    fun getActiveUntilMillis(context: Context): Long {
        return prefs(context).getLong(KEY_ACTIVE_UNTIL, 0L)
    }
}
