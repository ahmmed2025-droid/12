package com.example.namazdnd

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var durationInput: EditText
    private val timeButtons = mutableMapOf<String, Button>()
    private val nextLabels = mutableMapOf<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshNextLabels()
    }

    private fun buildUi() {
        val scrollView = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setBackgroundColor(0xFFF7F7F7.toInt())
        }
        scrollView.addView(root)

        root.addView(TextView(this).apply {
            text = "Namaz DND"
            textSize = 28f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(8), 0, dp(6))
            setTextColor(0xFF004D40.toInt())
        })

        root.addView(TextView(this).apply {
            text = "প্রতি ওয়াক্তের ৫ মিনিট আগে মোবাইল DND mode-এ যাবে। আপনার দেওয়া সময় পরে DND auto off হবে।"
            textSize = 15f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(12))
            setTextColor(0xFF444444.toInt())
        })

        statusText = TextView(this).apply {
            textSize = 14f
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
            setTextColor(0xFF222222.toInt())
        }
        root.addView(statusText, matchWrap())

        root.addView(actionButton("DND Access Permission দিন") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        })

        root.addView(actionButton("Alarms & reminders Permission দিন") {
            openExactAlarmSettings()
        })

        root.addView(sectionTitle("ওয়াক্ত অনুযায়ী সময় দিন"))
        WaqtPrefs.waqts.forEach { waqt -> root.addView(waqtRow(waqt)) }

        root.addView(sectionTitle("DND কতক্ষণ থাকবে?"))
        val durationRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        durationInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(WaqtPrefs.getDurationMinutes(this@MainActivity).toString())
            hint = "15"
            textSize = 16f
            selectAllOnFocus = true
        }
        durationRow.addView(durationInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        durationRow.addView(TextView(this).apply {
            text = " মিনিট"
            textSize = 16f
            setPadding(dp(8), 0, dp(8), 0)
        })
        root.addView(durationRow)

        root.addView(actionButton("Save করে Schedule করুন") {
            saveDurationAndSchedule()
        })

        root.addView(actionButton("১ মিনিট Test DND চালু করুন") {
            testDndForOneMinute()
        })

        root.addView(Space(this), LinearLayout.LayoutParams(1, dp(8)))
        root.addView(TextView(this).apply {
            text = "Note: ফোন restart হলে app নিজে alarm আবার schedule করবে। Permission revoke করলে DND change হবে না।"
            textSize = 13f
            setTextColor(0xFF666666.toInt())
        })

        setContentView(scrollView)
        refreshStatus()
        refreshNextLabels()
    }

    private fun waqtRow(waqt: Waqt): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        card.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(6), 0, dp(10)) }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        topRow.addView(TextView(this).apply {
            text = waqt.label
            textSize = 18f
            setTextColor(0xFF111111.toInt())
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val timeButton = Button(this).apply {
            text = formatTime(WaqtPrefs.getHour(this@MainActivity, waqt.key), WaqtPrefs.getMinute(this@MainActivity, waqt.key))
            setOnClickListener { showTimePicker(waqt) }
        }
        timeButtons[waqt.key] = timeButton
        topRow.addView(timeButton)

        val checkBox = CheckBox(this).apply {
            isChecked = WaqtPrefs.isEnabled(this@MainActivity, waqt.key)
            text = "On"
            setOnCheckedChangeListener { _, checked ->
                WaqtPrefs.setEnabled(this@MainActivity, waqt.key, checked)
                if (checked) PrayerScheduler.scheduleDndOnForWaqt(this@MainActivity, waqt.key)
                else PrayerScheduler.cancelDndOnForWaqt(this@MainActivity, waqt.key)
                refreshStatus()
                refreshNextLabels()
            }
        }
        topRow.addView(checkBox)
        card.addView(topRow)

        val nextText = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            setPadding(0, dp(6), 0, 0)
        }
        nextLabels[waqt.key] = nextText
        card.addView(nextText)

        return card
    }

    private fun showTimePicker(waqt: Waqt) {
        val hour = WaqtPrefs.getHour(this, waqt.key)
        val minute = WaqtPrefs.getMinute(this, waqt.key)
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            WaqtPrefs.setTime(this, waqt.key, selectedHour, selectedMinute)
            timeButtons[waqt.key]?.text = formatTime(selectedHour, selectedMinute)
            if (WaqtPrefs.isEnabled(this, waqt.key)) {
                PrayerScheduler.scheduleDndOnForWaqt(this, waqt.key)
            }
            refreshStatus()
            refreshNextLabels()
        }, hour, minute, false).show()
    }

    private fun saveDurationAndSchedule() {
        val minutes = durationInput.text.toString().toIntOrNull()
        if (minutes == null || minutes < 1) {
            Toast.makeText(this, "সঠিক মিনিট দিন", Toast.LENGTH_SHORT).show()
            return
        }
        WaqtPrefs.setDurationMinutes(this, minutes)
        val count = PrayerScheduler.scheduleAll(this)
        Toast.makeText(this, "$count টি ওয়াক্ত schedule হয়েছে", Toast.LENGTH_SHORT).show()
        refreshStatus()
        refreshNextLabels()
    }

    private fun testDndForOneMinute() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "আগে DND Access Permission দিন", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        try {
            if (!WaqtPrefs.isActiveByApp(this)) {
                WaqtPrefs.setPreviousFilter(this, notificationManager.currentInterruptionFilter)
            }
            val offAt = System.currentTimeMillis() + 60_000L
            WaqtPrefs.setActiveByApp(this, true)
            WaqtPrefs.setActiveUntilMillis(this, offAt)
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            PrayerScheduler.scheduleDndOff(this, "test", offAt)
            Toast.makeText(this, "DND ১ মিনিটের জন্য চালু হয়েছে", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "DND access পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshStatus() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val dndOk = notificationManager.isNotificationPolicyAccessGranted
        val exactOk = canScheduleExactAlarms()
        val enabledCount = WaqtPrefs.waqts.count { WaqtPrefs.isEnabled(this, it.key) }
        statusText.text = buildString {
            append("DND access: ").append(if (dndOk) "OK" else "Permission needed")
            append("\nExact alarm: ").append(if (exactOk) "OK" else "Permission needed / fallback inexact")
            append("\nActive ওয়াক্ত: ").append(enabledCount).append("/5")
            append("\nDND before namaz: ").append(WaqtPrefs.PRE_DND_MINUTES).append(" মিনিট")
            append("\nDND duration: ").append(WaqtPrefs.getDurationMinutes(this@MainActivity)).append(" মিনিট")
        }
    }

    private fun refreshNextLabels() {
        val formatter = SimpleDateFormat("EEE, dd MMM hh:mm a", Locale.getDefault())
        WaqtPrefs.waqts.forEach { waqt ->
            val label = nextLabels[waqt.key] ?: return@forEach
            if (WaqtPrefs.isEnabled(this, waqt.key)) {
                val next = PrayerScheduler.nextDndOnMillis(this, waqt.key)
                label.text = "Next DND ON: ${formatter.format(Date(next))}"
            } else {
                label.text = "Disabled"
            }
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "এই Android version-এ আলাদা exact alarm permission লাগে না", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actionButton(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setAllCaps(false)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(10), 0, 0) }
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(0xFF004D40.toInt())
            setPadding(0, dp(22), 0, dp(6))
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val suffix = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, suffix)
    }

    private fun matchWrap(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
