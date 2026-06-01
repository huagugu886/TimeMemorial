package com.timememorial.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderManager {

    private const val REQUEST_CODE = 99001

    /** 根据用户设置的提醒时间，调度每日检查闹钟 */
    fun scheduleDaily(ctx: Context) {
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            ctx, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hour = ReminderSettings.getHour(ctx)
        val minute = ReminderSettings.getMinute(ctx)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 如果今天这个时间已过，设为明天
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(cal.timeInMillis, pending),
                pending
            )
        } catch (e: SecurityException) {
            // 降级为非精确闹钟
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending
            )
        }
    }

    /** 取消已调度的闹钟 */
    fun cancel(ctx: Context) {
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            ctx, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
