package com.self.dingAlarm
import java.util.Calendar
object CommonUtil {  // Kotlin 的单例对象
    var amTime: AlarmTime? = null  // 可空类型用 ? 表示
    var pmTime: AlarmTime? = null

    fun isTimeSet(): Boolean {
        return amTime != null && pmTime != null
    }

    fun isAM(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour < 12
    }
}