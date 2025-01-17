package com.self.dingAlarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private lateinit var timeDisplay: TextView
    private lateinit var setTimeButton: Button
    private lateinit var alarmManager: AlarmManager

    companion object {
        private const val ALARM_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeDisplay = findViewById(R.id.timeDisplay)
        setTimeButton = findViewById(R.id.setTimeButton)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        setTimeButton.setOnClickListener {
            showTimePickerDialog()
        }

        updateTimeDisplay()
    }

    private fun scheduleAlarm() {
        val time = if (CommonUtil.isAM()) CommonUtil.amTime else CommonUtil.pmTime
        time?.let {
            if (!checkAlarmPermission()) {
                return
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, it.hour)
                set(Calendar.MINUTE, it.minute)
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                // 使用setAlarm函数来设置闹钟
                setAlarm(calendar.timeInMillis, pendingIntent)
                showSuccessToast(it)
            } catch (e: Exception) {
                Toast.makeText(this, "设置闹钟失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 处理不同版本的闹钟设置
    @SuppressLint("NewApi")
    private fun setAlarm(timeInMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 (API 23) 及以上使用 setExactAndAllowWhileIdle
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } else {
            // Android 6.0 以下使用 setExact
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    private fun showSuccessToast(time: AlarmTime) {
        Toast.makeText(
            this,
            "闹钟已设置：${time.hour}:${String.format("%02d", time.minute)}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkAlarmPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return false
            }
        }
        return true
    }

    private fun showTimePickerDialog() {
        val isAM = CommonUtil.isAM()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                if (isAM) {
                    CommonUtil.amTime = AlarmTime(hour, minute)
                } else {
                    CommonUtil.pmTime = AlarmTime(hour, minute)
                }
                updateTimeDisplay()
                scheduleAlarm()
            },
            12, 0, true
        ).show()
    }

    private fun updateTimeDisplay() {
        val displayText = StringBuilder("闹钟时间：\n")
        CommonUtil.amTime?.let {
            displayText.append("上午: ${it.hour}:${String.format("%02d", it.minute)}\n")
        }
        CommonUtil.pmTime?.let {
            displayText.append("下午: ${it.hour}:${String.format("%02d", it.minute)}")
        }
        timeDisplay.text = displayText.toString()
    }
}