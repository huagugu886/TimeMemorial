package com.timememorial.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.timememorial.app.databinding.ActivityMainBinding
import com.timememorial.app.reminder.ReminderManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 深色模式由 TimeMemorialApp.onCreate() 统一读取 SharedPreferences 并设置
        // 此处不再硬编码 AppCompatDelegate，避免覆盖用户选择

        // 沉浸式状态栏：让内容绘制到状态栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // 冷启动时恢复纪念日提醒闹钟
        val prefs = getSharedPreferences("reminder_settings", MODE_PRIVATE)
        if (prefs.getBoolean("enabled", false)) {
            ReminderManager.scheduleDaily(this)
        }

        // Android 13+ 请求通知权限（POST_NOTIFICATIONS 已在 Manifest 声明）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // 深色模式切换后强制刷新底栏颜色（BottomNavigationView 缓存 ColorStateList）
        refreshBottomNavColors()

        // 配置毛玻璃模糊效果（自动根据 API 版本选择算法）
        binding.blurView.setupWith(binding.blurTarget)
            .setBlurRadius(16f)
            .setBlurAutoUpdate(true)
        // 设置模糊叠加层颜色（亮/暗模式切换时同步更新）
        val nightMode2 = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDark2 = nightMode2 == android.content.res.Configuration.UI_MODE_NIGHT_YES
        binding.blurView.setOverlayColor(if (isDark2) 0x33000000 else 0x33FFFFFF)

        // 底栏：只吃导航栏底部 insets，阻止键盘(IME)把它顶上去
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            // 消费 IME insets，不让键盘影响底栏
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.NONE)
                .build()
        }

        // FragmentContainerView 默认不转发 insets 给子 fragment，
        // 手动 dispatch 让各 fragment 自行处理（home 用 viewport-fit=cover，
        // calendar/settings 在根 view 上加 top padding）
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            for (i in 0 until binding.navHostFragment.childCount) {
                ViewCompat.dispatchApplyWindowInsets(binding.navHostFragment.getChildAt(i), insets)
            }

            // 键盘弹出时，用 JS 把当前聚焦的输入框滚到可见区域
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            if (imeHeight > 0) {
                val fragment = navHostFragment.childFragmentManager
                    .primaryNavigationFragment
                if (fragment?.view is WebView) {
                    (fragment.view as WebView).evaluateJavascript(
                        """
                        var el = document.activeElement;
                        if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
                            el.scrollIntoView({block: 'center', behavior: 'smooth'});
                        }
                        """.trimIndent(), null
                    )
                }
            }
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        // 从设置页切深色模式回来时，确保底栏颜色同步
        refreshBottomNavColors()
    }

    /** 强制刷新底栏颜色，解决深色模式切换后不生效的问题 */
    fun refreshBottomNavColors() {
        val nightMode = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val night = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        // 完全透明：让 BlurView 的毛玻璃效果透出来
        binding.bottomNav.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        // 同步更新毛玻璃 overlay 颜色
        binding.blurView.setOverlayColor(if (night) 0x33000000 else 0x33FFFFFF)

        val themeName = getSharedPreferences("timememorial_prefs", MODE_PRIVATE)
            .getString("theme_color", "purple") ?: "purple"
        val night2 = night
        val primaryMap = mapOf(
          "purple" to if (night2) "#A78BFA" else "#8B5CF6",
          "blue"   to if (night2) "#60A5FA" else "#3B82F6",
          "pink"   to if (night2) "#F472B6" else "#EC4899",
          "rose"   to if (night2) "#FB7185" else "#F43F5E",
          "teal"   to if (night2) "#2DD4BF" else "#14B8A6",
          "red"    to if (night2) "#F87171" else "#EF4444"
        )
        val checked = android.graphics.Color.parseColor(primaryMap[themeName] ?: primaryMap["purple"]!!)
        val unchecked = if (night) android.graphics.Color.parseColor("#94A3B8")
                        else android.graphics.Color.parseColor("#6B7280")
        val states = arrayOf(
          intArrayOf(android.R.attr.state_checked),
          intArrayOf()
        )
        val colors = intArrayOf(checked, unchecked)
        binding.bottomNav.itemIconTintList = android.content.res.ColorStateList(states, colors)
        binding.bottomNav.itemTextColor = android.content.res.ColorStateList(states, colors)
    }
}
