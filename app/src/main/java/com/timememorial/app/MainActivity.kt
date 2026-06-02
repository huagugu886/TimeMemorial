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
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.timememorial.app.databinding.ActivityMainBinding

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

        // 键盘弹出时，用 JS 把当前聚焦的输入框滚到可见区域
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
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
    private fun refreshBottomNavColors() {
        val bottomNav = binding.bottomNav
        // 重新加载 color state list，清除缓存
        val colorStateList = ContextCompat.getColorStateList(this, R.color.bottom_nav_color)
        bottomNav.itemIconTintList = colorStateList
        bottomNav.itemTextColor = colorStateList
        // 背景色跟随主题
        bottomNav.setBackgroundColor(
            MaterialColors.getColor(bottomNav, com.google.android.material.R.attr.colorSurface, 0)
        )
    }
}
