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

        // 给内容区设置底部 padding，防止被底栏完全遮挡
        binding.bottomNav.post {
            val navHeight = binding.bottomNav.height
            val density = resources.displayMetrics.density
            val bottomMarginPx = (8 * density).toInt()
            binding.navHostFragment.setPadding(0, 0, 0, navHeight + bottomMarginPx)
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
        val density = resources.displayMetrics.density

        // 系统导航栏颜色同步
        window.navigationBarColor = if (night) android.graphics.Color.BLACK
                                    else android.graphics.Color.WHITE

        // 胶囊底栏背景：HyperOS 风格
        val capsuleColor = if (night) android.graphics.Color.parseColor("#2A2A2A")
                           else android.graphics.Color.parseColor("#F0F0F0")
        val capsuleBg = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 28f * density
            setColor(capsuleColor)
        }
        binding.bottomNav.background = capsuleBg

        // 选中项高亮胶囊
        val selectedColor = if (night) android.graphics.Color.parseColor("#40FFFFFF")
                            else android.graphics.Color.parseColor("#D0D0D0")
        val selectedBg = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 22f * density
            setColor(selectedColor)
        }
        val transparentBg = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        val itemSelector = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), selectedBg)
            addState(intArrayOf(), transparentBg)
        }
        binding.bottomNav.itemBackground = itemSelector

        // 主题色：选中项图标和文字颜色
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
