package com.timememorial.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // 开机或应用更新后，重新调度每日提醒闹钟
            if (ReminderSettings.isEnabled(context)) {
                ReminderManager.scheduleDaily(context)
            }
        }
    }
}
