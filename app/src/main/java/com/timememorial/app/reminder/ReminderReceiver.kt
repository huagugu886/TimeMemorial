package com.timememorial.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.timememorial.app.MainActivity

class ReminderReceiver : BroadcastReceiver {

    constructor() : super()

    companion object {
        private const val CHANNEL_ID = "anniversary_reminders"
        private const val CHANNEL_NAME = "纪念日提醒"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        // 确保通知渠道存在
        createNotificationChannel(context)

        // 获取需要提醒的纪念日列表
        val reminders = ReminderSettings.getUpcomingReminders(context)

        if (reminders.isEmpty()) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 点击通知打开主界面
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 为每个纪念日发送通知（最多 5 条，避免轰炸）
        for ((index, reminder) in reminders.take(5).withIndex()) {
            val title = if (reminder.isToday) {
                "🎉 今天是「${reminder.title}」！"
            } else {
                "📅 ${reminder.title} 还有 ${reminder.daysUntil} 天"
            }

            val text = if (reminder.isToday) {
                "今天就是 ${reminder.title} 啦，别忘了准备哦～"
            } else {
                "${reminder.title} 将在 ${reminder.daysUntil} 天后到来"
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            // 用不同 ID 发送，避免覆盖
            notificationManager.notify(10000 + index, notification)
        }

        // 调度明天的检查
        ReminderManager.scheduleDaily(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "纪念日到期提醒"
            }
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
