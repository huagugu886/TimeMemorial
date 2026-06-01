package com.timememorial.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class TimeMemorialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 启动时读取深色模式设置，确保与用户上次选择一致
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val darkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}