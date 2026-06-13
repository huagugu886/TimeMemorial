package com.timememorial.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.timememorial.app.databinding.ActivityMainBinding
import com.timememorial.app.reminder.ReminderManager
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // 深色模式由 TimeMemorialApp.onCreate() 统一读取 SharedPreferences 并设置

        // 沉浸式状态栏：让内容绘制到状态栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ===== BlurView 毛玻璃初始化 =====
        val blurView = findViewById<BlurView>(R.id.blur_view)
        val windowBackground = window.decorView.background
        blurView.setupWith(binding.root, RenderScriptBlur(this))
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(16f)
            .setBlurAutoUpdate(true)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        // 冷启动时恢复纪念日提醒闹钟
        val prefs = getSharedPreferences("reminder_settings", MODE_PRIVATE)
        if (prefs.getBoolean("enabled", false)) {
            ReminderManager.scheduleDaily(this)
        }

        // Android 13+ 请求通知权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // 深色模式切换后强制刷新底栏颜色
        refreshBottomNavColors()

        // 底栏容器：只吃导航栏底部 insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavBackground) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.NONE)
                .build()
        }

        // FragmentContainerView 转发 insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            for (i in 0 until binding.navHostFragment.childCount) {
                ViewCompat.dispatchApplyWindowInsets(binding.navHostFragment.getChildAt(i), insets)
            }

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
        refreshBottomNavColors()
    }

    /** 强制刷新底栏颜色（MiUIX 风格） */
    fun refreshBottomNavColors() {
        val nightMode = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val night = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        // 胶囊底色（由 BlurView 提供毛玻璃背景，底栏本身透明）
        binding.bottomNav.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        // 读取用户主题色
        val themeName = getSharedPreferences("timememorial_prefs", MODE_PRIVATE)
            .getString("theme_color", "purple") ?: "purple"
        val primaryMap = mapOf(
            "purple" to if (night) "#A78BFA" else "#8B5CF6",
            "blue"   to if (night) "#60A5FA" else "#3B82F6",
            "pink"   to if (night) "#F472B6" else "#EC4899",
            "rose"   to if (night) "#FB7185" else "#F43F5E",
            "teal"   to if (night) "#2DD4BF" else "#14B8A6",
            "red"    to if (night) "#F87171" else "#EF4444"
        )
        val checked = android.graphics.Color.parseColor(primaryMap[themeName] ?: primaryMap["purple"]!!)
        val unchecked = if (night) android.graphics.Color.parseColor("#94A3B8")
                        else android.graphics.Color.parseColor("#888888")
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val colors = intArrayOf(checked, unchecked)
        binding.bottomNav.itemIconTintList = android.content.res.ColorStateList(states, colors)
        binding.bottomNav.itemTextColor = android.content.res.ColorStateList(states, colors)

        // 胶囊指示器背景色（选中态底色）
        val activeBg = if (night) android.graphics.Color.parseColor("#1E2A3A")
                       else android.graphics.Color.parseColor("#E8F0FE")
        val indicatorStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val indicatorColors = intArrayOf(activeBg, android.graphics.Color.TRANSPARENT)
        binding.bottomNav.background = createCapsuleBackground(activeBg)
    }

    /** 创建胶囊圆角背景 Drawable */
    private fun createCapsuleBackground(activeBgColor: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(android.graphics.Color.TRANSPARENT)
            cornerRadius = 28 * resources.displayMetrics.density
        }
    }
}
