package com.self.dingAlarm

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.random.Random

class AlarmReceiver : BroadcastReceiver() {
    private val minDelaySeconds = 5 // 最小延迟秒数
    private val maxDelaySeconds = 10 // 最大延迟秒数
    private var handler: Handler? = null
    private var secondsLeft = 0

    private fun getDelayRange(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences("alarm_settings", Context.MODE_PRIVATE)
        val minDelay = prefs.getInt("min_delay", 5)
        val maxDelay = prefs.getInt("max_delay", 4*60)
        return Pair(minDelay, maxDelay)
    }


    override fun onReceive(context: Context, intent: Intent) {
        val (minDelay, maxDelay) = getDelayRange(context)
        secondsLeft = Random.nextInt(minDelay, maxDelay + 1)

        // 显示倒计时提示
        showCountdownToast(context)

        // 延迟后打开钉钉
        handler?.postDelayed({
            openDingding(context)
            handler?.postDelayed({
                backToApp(context)
                handler = null  // 清理 handler
            }, 120000)
        }, secondsLeft * 1000L)
    }

    private fun showCountdownToast(context: Context) {
        if (secondsLeft > 0) {
            Toast.makeText(context, "${secondsLeft}秒后打开钉钉...", Toast.LENGTH_SHORT).show()
            secondsLeft--
            handler?.postDelayed({ showCountdownToast(context) }, 1000)
        }
    }

    private fun backToApp(context: Context) {
        try {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            activityManager?.let { manager ->
                val taskInfoList = manager.getRunningTasks(100)
                for (taskInfo in taskInfoList) {
                    if (taskInfo.topActivity?.packageName == context.packageName) {
                        manager.moveTaskToFront(taskInfo.id, 0)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "返回应用失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDingding(context: Context) {
        val packageName = "com.alibaba.android.rimet" // 钉钉的包名
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
            }
        } catch (e: Exception) {
            Toast.makeText(context, "启动钉钉失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}