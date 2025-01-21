package com.self.dingAlarm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.EditText
import android.widget.Switch
import android.widget.Button
import android.app.TimePickerDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import java.util.Calendar
import androidx.core.content.ContextCompat
import android.os.Build

class MainActivity : AppCompatActivity() {
    private lateinit var morningTimeDisplay: TextView
    private lateinit var eveningTimeDisplay: TextView
    private lateinit var minDelayEdit: EditText
    private lateinit var maxDelayEdit: EditText
    private lateinit var alarmSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            checkPermissions()  // 添加权限检查
            initViews()
            loadSettings()
            setupListeners()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "启动错误：${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(android.Manifest.permission.SCHEDULE_EXACT_ALARM)) {
                requestPermissions(arrayOf(android.Manifest.permission.SCHEDULE_EXACT_ALARM), 1)
            }
        }
    }
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
    private fun initViews() {
        morningTimeDisplay = findViewById(R.id.morningTimeDisplay)
        eveningTimeDisplay = findViewById(R.id.eveningTimeDisplay)
        minDelayEdit = findViewById(R.id.minDelayEdit)
        maxDelayEdit = findViewById(R.id.maxDelayEdit)
        alarmSwitch = findViewById(R.id.alarmSwitch)
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("alarm_settings", MODE_PRIVATE)

        // 加载时间设置
        morningTimeDisplay.text = prefs.getString("morning_time", "8:30")
        eveningTimeDisplay.text = prefs.getString("evening_time", "18:30")

        // 加载延迟设置
        minDelayEdit.setText(prefs.getInt("min_delay", 5).toString())
        maxDelayEdit.setText(prefs.getInt("max_delay", 10).toString())

        // 加载开关状态
        alarmSwitch.isChecked = prefs.getBoolean("alarm_enabled", false)
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.setMorningTimeButton).setOnClickListener {
            showTimePickerDialog(true)
        }

        findViewById<Button>(R.id.setEveningTimeButton).setOnClickListener {
            showTimePickerDialog(false)
        }

        findViewById<Button>(R.id.saveSettingsButton).setOnClickListener {
            saveSettings()
        }

        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                scheduleAlarms()
            } else {
                cancelAlarms()
            }
            getSharedPreferences("alarm_settings", MODE_PRIVATE).edit()
                .putBoolean("alarm_enabled", isChecked)
                .apply()
        }
    }

    private fun showTimePickerDialog(isMorning: Boolean) {
        val currentTime = if (isMorning)
            morningTimeDisplay.text.toString() else
            eveningTimeDisplay.text.toString()

        val hour = currentTime.split(":")[0].toInt()
        val minute = currentTime.split(":")[1].toInt()

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val timeString = String.format("%d:%02d", selectedHour, selectedMinute)
            if (isMorning) {
                morningTimeDisplay.text = timeString
            } else {
                eveningTimeDisplay.text = timeString
            }
        }, hour, minute, true).show()
    }

    private fun saveSettings() {
        val minDelay = minDelayEdit.text.toString().toIntOrNull() ?: 5
        val maxDelay = maxDelayEdit.text.toString().toIntOrNull() ?: 240

        if (minDelay > maxDelay) {
            Toast.makeText(this, "最小延迟不能大于最大延迟", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("alarm_settings", MODE_PRIVATE)
        prefs.edit().apply {
            putString("morning_time", morningTimeDisplay.text.toString())
            putString("evening_time", eveningTimeDisplay.text.toString())
            putInt("min_delay", minDelay)
            putInt("max_delay", maxDelay)
            apply()
        }

        if (alarmSwitch.isChecked) {
            scheduleAlarms()
        }

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun scheduleAlarms() {
        val morningTime = morningTimeDisplay.text.toString()
        val eveningTime = eveningTimeDisplay.text.toString()

        // 设置上班闹钟
        setAlarm(morningTime, 1)
        // 设置下班闹钟
        setAlarm(eveningTime, 2)
    }

    private fun setAlarm(timeString: String, requestCode: Int) {
        val parts = timeString.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmIntent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)

        // 取消上班闹钟
        val morningPendingIntent = PendingIntent.getBroadcast(
            this, 1, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(morningPendingIntent)

        // 取消下班闹钟
        val eveningPendingIntent = PendingIntent.getBroadcast(
            this, 2, alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(eveningPendingIntent)
    }
}