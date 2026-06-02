package com.timememorial.app.ui.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.timememorial.app.R

class SettingsFragment : Fragment() {

    private var webView: WebView? = null

    /** JS 桥接：让设置页切换暗色模式时同步到 SharedPreferences + AppCompatDelegate */
    inner class DarkModeBridge {
        @JavascriptInterface
        fun setDarkMode(enabled: Boolean) {
            requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("dark_mode", enabled).apply()
            // 必须在主线程调用，否则 Activity 重建可能不触发
            activity?.runOnUiThread {
                AppCompatDelegate.setDefaultNightMode(
                    if (enabled) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }

        @JavascriptInterface
        fun saveReminderSettings(jsonString: String) {
            try {
                val json = org.json.JSONObject(jsonString)
                val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt("reminder_days", json.optInt("reminderDays", 3))
                    .putString("reminder_time", json.optString("reminderTime", "09:00"))
                    .putString("repeat_type", json.optString("repeatType", "yearly"))
                    .apply()
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "saveReminderSettings failed", e)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        webView = view.findViewById(R.id.webView)

        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false

            addJavascriptInterface(DarkModeBridge(), "nativeBridge")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val sbHeight = getStatusBarHeight()
                    view?.evaluateJavascript(
                        "document.documentElement.style.setProperty('--sb-height', '${sbHeight}px');",
                        null
                    )
                    view?.evaluateJavascript(
                        """
                        var bn = document.querySelector('.bottom-nav');
                        if (bn) bn.style.display = 'none';
                        """.trimIndent(),
                        null
                    )
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl("file:///android_asset/settings_page.html?v=2")
        }

        return view
    }

    private fun getStatusBarHeight(): Int {
        val insets = ViewCompat.getRootWindowInsets(requireView()) ?: return 0
        return insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
    }

    override fun onDestroyView() {
        webView?.apply {
            stopLoading()
            destroy()
        }
        webView = null
        super.onDestroyView()
    }
}
