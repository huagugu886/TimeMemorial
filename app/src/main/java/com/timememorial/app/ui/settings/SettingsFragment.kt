package com.timememorial.app.ui.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.os.Environment
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import com.timememorial.app.reminder.ReminderSettings
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.timememorial.app.data.local.AnniversaryRepository
import com.timememorial.app.R

class SettingsFragment : Fragment() {

    private var webView: WebView? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleFileChooserResult(result)
    }

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
        fun exportData(): String {
            val ctx = webView?.context ?: return "{\"ok\":false}"
            try {
                val items = com.timememorial.app.data.local.AnniversaryRepository.getAll(ctx)
                val jsonArray = org.json.JSONArray().apply {
                    items.forEach { item ->
                        put(org.json.JSONObject(item))
                    }
                }
                val wrapper = JSONObject()
                wrapper.put("version", 1)
                wrapper.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))
                wrapper.put("count", jsonArray.length())
                wrapper.put("data", jsonArray)
                val json = wrapper.toString(2)
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "anniversaries_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                val file = File(dir, fileName)
                file.writeText(json)
                return "{\"ok\":true,\"path\":\"${file.absolutePath}\"}"
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "Export failed", e)
                return "{\"ok\":false,\"error\":\"${e.message}\"}"
            }
        }

        @JavascriptInterface
        fun saveReminderSettings(jsonString: String) {
            try {
                val json = org.json.JSONObject(jsonString)
                val prefs = requireContext().getSharedPreferences("reminder_settings", android.content.Context.MODE_PRIVATE)
                val timeParts = json.optString("reminderTime", "09:00").split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                prefs.edit()
                    .putBoolean("enabled", true)
                    .putInt("days_before", json.optInt("reminderDays", 3))
                    .putInt("hour", hour)
                    .putInt("minute", minute)
                    .putString("default_repeat", json.optString("repeatType", "yearly"))
                    .apply()
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "saveReminderSettings failed", e)
            }
        }

        @JavascriptInterface
        fun restoreData(jsonString: String): String {
            return try {
                val json = org.json.JSONObject(jsonString)
                val data = json.getJSONArray("data")
                // 全删：先清除所有旧数据，避免 Room DB 与 localStorage 不同步导致去重失败
                val oldItems = AnniversaryRepository.getAll(requireContext())
                for (item in oldItems) {
                    val id = (item["id"] as? Number)?.toLong() ?: continue
                    AnniversaryRepository.deleteById(requireContext(), id)
                }
                // 全插：将备份数据完整写入
                var count = 0
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                        val map = mutableMapOf<String, Any?>()
                        map["title"] = obj.optString("title", "")
                        map["date"] = obj.optString("date", "")
                        map["category"] = obj.optString("category", "")
                        map["repeatYearly"] = obj.optBoolean("repeatYearly", true)
                        map["reminderDays"] = obj.optInt("reminderDays", 3)
                        map["photoUri"] = obj.optString("photoUri", "")
                        map["note"] = obj.optString("note", "")
                        AnniversaryRepository.insert(requireContext(), map)
                        count++
                }
                // 设置标志，通知首页 WebView 同步恢复数据
                val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("pending_restore_sync", true).apply()

                // 导航回首页（在主线程执行）
                activity?.runOnUiThread {
                    try {
                        androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.nav_home)
                    } catch (e: Exception) {
                        android.util.Log.e("SettingsFragment", "navigate to home failed", e)
                    }
                }

                org.json.JSONObject().put("ok", true).put("count", count).toString()
            } catch (e: Exception) {
                android.util.Log.e("SettingsFragment", "restoreData failed", e)
                org.json.JSONObject().put("ok", false).put("error", e.message ?: "unknown").toString()
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
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = filePathCallback

                    val intent = fileChooserParams?.createIntent() ?: return false
                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (e: Exception) {
                        fileChooserCallback = null
                        return false
                    }
                    return true
                }
            }
            loadUrl("file:///android_asset/settings_page.html?v=2")
        }

        return view
    }

    private fun handleFileChooserResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = when {
                data?.clipData != null -> {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
            fileChooserCallback?.onReceiveValue(uris)
        } else {
            fileChooserCallback?.onReceiveValue(null)
        }
        fileChooserCallback = null
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
