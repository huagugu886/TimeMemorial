package com.timememorial.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.timememorial.app.reminder.ReminderReceiver
import com.timememorial.app.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private val colorMap = mapOf(
        "purple" to "#8B5CF6",
        "blue"   to "#3B82F6",
        "pink"   to "#EC4899",
        "rose"   to "#F43F5E",
        "teal"   to "#14B8A6",
        "red"    to "#EF4444"
    )
    private val colorMapDark = mapOf(
        "purple" to "#A78BFA",
        "blue"   to "#60A5FA",
        "pink"   to "#F472B6",
        "rose"   to "#FB7185",
        "teal"   to "#2DD4BF",
        "red"    to "#F87171"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("timememorial_prefs", MODE_PRIVATE)

        // ── 沉浸式 ──
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ── 窗口级毛玻璃（API 31+）──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setBackgroundBlurRadius(40)
            window.attributes = window.attributes.apply { dimAmount = 0f }
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        // ── 系统栏 Inset 处理（合并为一个监听器）──
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)
            // 底栏加导航栏高度 padding
            binding.bottomNav.setPadding(
                binding.bottomNav.paddingLeft,
                binding.bottomNav.paddingTop,
                binding.bottomNav.paddingRight,
                bars.bottom + (4 * resources.displayMetrics.density).toInt()
            )
            // 分发给 Fragment
            val vg = v as android.view.ViewGroup
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                if (child.id != R.id.bottom_nav) {
                    ViewCompat.dispatchApplyWindowInsets(child, insets)
                }
            }
            insets
        }

        // ── 导航 ──
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        binding.bottomNav.setupWithNavController(navHostFragment.navController)

        // ── 底栏外观 ──
        setupBottomNavAppearance()

        // ── 纪念日提醒 ──
        scheduleDailyReminder()

        // ── 通知权限 ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupBottomNavAppearance()
    }

    // ═══════════════════════════════════════════════
    //  MiUIX 胶囊底栏
    // ═══════════════════════════════════════════════

    
    private fun getThemeColor(context: android.content.Context): Int {
        val prefs = context.getSharedPreferences("timememorial_prefs", MODE_PRIVATE)
        val colorName = prefs.getString("theme_color", "purple") ?: "purple"
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val map = if (night) colorMapDark else colorMap
        val hex = map[colorName] ?: map["purple"]!!
        return Color.parseColor(hex)
    }

    private fun setupBottomNavAppearance() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val density = resources.displayMetrics.density
        val themeColor = getThemeColor(this)

        // 1. 背景 — MIUIX surfaceContainer 风格 squircle 胶囊
        val bgColor = if (night) Color.parseColor("#1A1A1A") else Color.parseColor("#F0F0F5")
        val capsule = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 28 * density
            setColor(bgColor)
        }
        binding.bottomNav.background = capsule

        // 2. 圆角裁剪
        binding.bottomNav.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val rect = RectF(0f, 0f, view.width.toFloat(), view.height.toFloat())
                val path = Path()
                path.addRoundRect(rect, 28 * density, 28 * density, Path.Direction.CW)
                outline.setPath(path, 0f)
            }
        }
        binding.bottomNav.clipToOutline = true

        // 3. 阴影 — MIUIX 风格仅 1dp
        binding.bottomNav.elevation = 1 * density

        // 4. 选中效果 — MIUIX 风格：透明度变化，无 indicator pill
        //    选中 100% 不透明，未选中 40% 不透明
        val unselectedAlpha = (255 * 0.4f).toInt()  // 102
        val selectedColor = themeColor
        val unselectedColor = androidx.core.graphics.ColorUtils.setAlphaComponent(themeColor, unselectedAlpha)

        val navTint = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(selectedColor, unselectedColor)
        )
        binding.bottomNav.itemIconTintList = navTint
        binding.bottomNav.itemTextColor = navTint

        // 5. 清除 Material 默认 item 背景
        binding.bottomNav.itemBackground = null
    }

    /**
     * 供 SettingsFragment 调用的公开方法
     */
    fun refreshBottomNavColors() {
        setupBottomNavAppearance()
    }

    // ═══════════════════════════════════════════════
    //  纪念日提醒
    // ═══════════════════════════════════════════════

    private fun scheduleDailyReminder() {
        val reminderPrefs = getSharedPreferences("reminder_settings", MODE_PRIVATE)
        if (!reminderPrefs.getBoolean("enabled", false)) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        reminderPrefs.edit().putLong("scheduled_time", calendar.timeInMillis).apply()
        // Removed empty BroadcastReceiver registration
    }

    // ═══════════════════════════════════════════════
    //  通知渠道
    // ═══════════════════════════════════════════════

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "anniversary_reminders",
                "纪念日提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "纪念日提醒通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
