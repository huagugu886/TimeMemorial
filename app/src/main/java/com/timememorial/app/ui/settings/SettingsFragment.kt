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
                val imageDir = File(ctx.filesDir, "images")
                val items = AnniversaryRepository.getAll(ctx)
                val jsonArray = org.json.JSONArray().apply {
                    items.forEach { item ->
                        val obj = org.json.JSONObject(item)
                        val photoUri = item["photoUri"] as? String ?: ""
                        if (photoUri.isNotEmpty()) {
                            val imgFile = File(imageDir, photoUri)
                            if (imgFile.exists()) {
                                val bytes = imgFile.readBytes()
                                val mimeType = when {
                                    photoUri.endsWith(".png") -> "image/png"
                                    photoUri.endsWith(".webp") -> "image/webp"
                                    photoUri.endsWith(".gif") -> "image/gif"
                                    else -> "image/jpeg"
                                }
                                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                obj.put("imageBase64", "data:${mimeType};base64,${base64}")
                            }
                        }
                        put(obj)
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
                val oldItems = AnniversaryRepository.getAll(requireContext())
                for (item in oldItems) {
                    val id = (item["id"] as? Number)?.toLong() ?: continue
                    AnniversaryRepository.deleteById(requireContext(), id)
                }
                var count = 0
                val injectArray = org.json.JSONArray()
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val map = mutableMapOf<String, Any?>()
                    map["title"] = obj.optString("title", "")
                    map["date"] = obj.optString("date", "")
                    map["category"] = obj.optString("category", "")
                    map["repeatYearly"] = obj.optBoolean("repeatYearly", true)
                    map["reminderDays"] = obj.optInt("reminderDays", 3)
                    map["note"] = obj.optString("note", "")
                    map["favorite"] = obj.optBoolean("favorite", false)
                    val imageBase64 = obj.optString("imageBase64", "")
                    var photoUri = obj.optString("photoUri", "")
                    if (imageBase64.isNotEmpty()) {
                        photoUri = saveBase64Image(imageBase64) ?: photoUri
                    }
                    map["photoUri"] = photoUri
                    AnniversaryRepository.insert(requireContext(), map)
                    count++
                    val injectObj = org.json.JSONObject().apply {
                        put("id", obj.optLong("id", System.currentTimeMillis() + i))
                        put("title", obj.optString("title", ""))
                        put("date", obj.optString("date", ""))
                        put("desc", obj.optString("note", ""))
                        put("category", obj.optString("category", ""))
                        put("image", photoUri)
                        put("imagePosition", 50)
                        put("favorite", obj.optBoolean("favorite", false))
                        val repeatYearly = obj.optBoolean("repeatYearly", true)
                        put("repeatType", if (repeatYearly) "yearly" else "none")
                        put("remindDays", obj.optInt("reminderDays", 3))
                        put("dateType", "solar")
                    }
                    injectArray.put(injectObj)
                }
                val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("pending_restore_sync", true)
                    .putString("pending_restore_data", injectArray.toString())
                    .apply()

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

        @JavascriptInterface
        fun getCacheSize(): String {
            return try {
                val ctx = webView?.context ?: return "{\"ok\":false}"
                var total = 0L
                // WebView cache
                ctx.cacheDir?.let { dir ->
                    if (dir.exists()) {
                        dir.walkTopDown().filter { it.isFile }.forEach { total += it.length() }
                    }
                }
                // images directory
                File(ctx.filesDir, "images").let { dir ->
                    if (dir.exists()) {
                        dir.walkTopDown().filter { it.isFile }.forEach { total += it.length() }
                    }
                }
                JSONObject().apply {
                    put("ok", true)
                    put("bytes", total)
                    put("display", formatBytes(total))
                }.toString()
            } catch (e: Exception) {
                "{\"ok\":false,\"error\":\"${e.message}\"}"
            }
        }

        @JavascriptInterface
        fun clearCache(): String {
            return try {
                val ctx = webView?.context ?: return "{\"ok\":false}"
                var cleared = 0L
                // Clear cache directory
                ctx.cacheDir?.let { dir ->
                    if (dir.exists()) {
                        dir.walkTopDown().filter { it.isFile }.forEach { cleared += it.length(); it.delete() }
                    }
                }
                // WebView methods must be called on the main thread
                activity?.runOnUiThread {
                    webView?.clearCache(true)
                    webView?.clearHistory()
                    webView?.clearFormData()
                }
                JSONObject().apply {
                    put("ok", true)
                    put("display", "0 B")
                    put("cleared", cleared)
                }.toString()
            } catch (e: Exception) {
                "{\"ok\":false,\"error\":\"${e.message}\"}"
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)} KB"
            else -> "${String.format("%.1f", bytes / (1024.0 * 1024.0))} MB"
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
                    view?.evaluateJavascript(
                        "document.getElementById('versionInfo').textContent = 'v${com.timememorial.app.BuildConfig.VERSION_NAME}';",
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

    /** 将 data:image/xxx;base64,... 字符串保存到 images 目录，返回文件名 */
    private fun saveBase64Image(dataUrl: String): String? {
        return try {
            val ctx = webView?.context ?: return null
            val imageDir = File(ctx.filesDir, "images").apply { mkdirs() }
            val mimeType = dataUrl.substringAfter(":").substringBefore(";")
            val rawBase64 = dataUrl.substringAfter(",")
            val ext = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("gif") -> "gif"
                else -> "jpg"
            }
            val bytes = android.util.Base64.decode(rawBase64, android.util.Base64.DEFAULT)
            val fileName = "${java.util.UUID.randomUUID()}.$ext"
            File(imageDir, fileName).writeBytes(bytes)
            fileName
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "saveBase64Image failed", e)
            null
        }
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
