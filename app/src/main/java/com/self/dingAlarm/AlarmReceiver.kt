package com.self.dingAlarm

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private var handler: Handler? = null
        private var secondsLeft = 0
        private const val RETURN_DELAY = 5 // 15秒后返回
        private var countDownToast: Toast? = null
    }

    private fun getDelayRange(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE)
        val minDelay = prefs.getInt("min_delay", 5)
        val maxDelay = prefs.getInt("max_delay", 4*60)
        return Pair(minDelay, maxDelay)
    }

    override fun onReceive(context: Context, intent: Intent) {

        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.d("AlarmTrigger", "闹钟触发时间：$currentTime")

        // 确保在主线程上创建 Handler
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }

        // 获取延迟时间并立即开始倒计时
        val (minDelay, maxDelay) = getDelayRange(context)
        secondsLeft = Random.nextInt(minDelay, maxDelay + 1)

        // 立即开始第一次倒计时显示
        handler?.post {
            showCountdownToast(context.applicationContext)
        }
    }

    private fun showCountdownToast(context: Context) {
        try {
            if (secondsLeft > 0) {
                countDownToast?.cancel()
                countDownToast = Toast.makeText(context, "${secondsLeft}秒后打开钉钉...", Toast.LENGTH_SHORT)
                countDownToast?.show()
                secondsLeft--
                handler?.postDelayed({
                    showCountdownToast(context)
                }, 1000)
            } else {
                // 倒计时结束，打开钉钉
                openDingding(context)
                // 开始返回倒计时
                var returnSeconds = RETURN_DELAY
                startReturnCountdown(context, returnSeconds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startReturnCountdown(context: Context, seconds: Int) {
        if (seconds > 0) {
            countDownToast?.cancel()
            countDownToast = Toast.makeText(context, "${seconds}秒后返回应用...", Toast.LENGTH_SHORT)
            countDownToast?.show()
            handler?.postDelayed({
                startReturnCountdown(context, seconds - 1)
            }, 1000)
        } else {
            // 返回应用
            backToApp(context)
            // 清理资源
            handler?.postDelayed({
                handler = null
                countDownToast = null
            }, 1000)
        }
    }

    private fun backToApp(context: Context) {
        try {
            // 方法1：使用启动器 Intent
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
            }

            // 方法2：使用 ActivityManager（作为备选方案）
            val activityManager = context.getSystemService(ActivityManager::class.java)
            activityManager?.let { manager ->
                val taskInfoList = manager.getRunningTasks(100)
                for (taskInfo in taskInfoList) {
                    if (taskInfo.topActivity?.packageName == context.packageName) {
                        manager.moveTaskToFront(taskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME)
                        break
                    }
                }
            }

            Toast.makeText(context, "已返回应用", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "返回应用失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDingding(context: Context) {
        val packageName = "com.alibaba.android.rimet"
        try {
            val packageManager = context.packageManager
            val pi = packageManager.getPackageInfo(packageName, 0)

            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            resolveIntent.setPackage(pi.packageName)

            val apps = packageManager.queryIntentActivities(resolveIntent, 0)
            val resolveInfo = apps.iterator().next()

            if (resolveInfo != null) {
                val className = resolveInfo.activityInfo.name
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                val cn = ComponentName(packageName, className)
                intent.component = cn
                context.startActivity(intent)
                Toast.makeText(context, "正在打开钉钉...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "启动钉钉失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}