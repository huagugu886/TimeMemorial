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

        // Android 13+ 请求通知权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        refreshBottomNavColors()

        // ====== 毛玻璃模糊效果 ======
        val decorView = window.decorView
        val windowBackground = decorView.background
        binding.blurView.setupWith(binding.blurTarget)
            .setFrameClearDrawable(windowBackground)
            .setBlurRadius(16f)
            .setBlurAutoUpdate(true)
        val nightMode = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        binding.blurView.setOverlayColor(if (isDark) 0x10000000 else 0x10FFFFFF)

        // 底栏 insets：吃掉导航栏底部 padding，防止键盘把它顶上去
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            // 消费 IME insets，不让键盘影响底栏
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.NONE)
                .build()
        }

        // FragmentContainerView 默认不转发 insets 给子 fragment
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
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
        refreshBottomNavColors()
    }

    /** 强制刷新底栏颜色，解决深色模式切换后不生效的问题 */
    fun refreshBottomNavColors() {
        val nightMode = resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val night = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        // 底栏完全透明，让 BlurView 的毛玻璃效果透出来
        binding.bottomNav.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        binding.blurView.setOverlayColor(if (night) 0x10000000 else 0x10FFFFFF)

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
