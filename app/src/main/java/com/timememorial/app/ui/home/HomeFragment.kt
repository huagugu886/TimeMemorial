package com.timememorial.app.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebViewClient
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.timememorial.app.databinding.FragmentHomeBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.timememorial.app.R

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 文件选择回调
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    // 拍照临时文件
    private var cameraPhotoUri: Uri? = null

    // 文件选择器
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleFileChooserResult(result.resultCode, result.data)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(false)

            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    // 取消上一次未完成的回调
                    fileUploadCallback?.onReceiveValue(null)
                    fileUploadCallback = filePathCallback

                    // 构建文件选择 Intent
                    val acceptTypes = fileChooserParams?.acceptTypes ?: arrayOf("image/*")
                    val isImageOnly = acceptTypes.any {
                        it.contains("image") || it.isEmpty()
                    }

                    val chooserIntents = mutableListOf<Intent>()

                    // 图库选择
                    val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = if (isImageOnly) "image/*" else "*/*"
                    }
                    chooserIntents.add(galleryIntent)

                    // 拍照（仅图片模式）
                    if (isImageOnly) {
                        val cameraIntent = createCameraIntent()
                        if (cameraIntent != null) {
                            chooserIntents.add(cameraIntent)
                        }
                    }

                    val chooser = Intent.createChooser(
                        chooserIntents.removeAt(0),
                        "选择图片"
                    ).apply {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, chooserIntents.toTypedArray())
                    }

                    try {
                        fileChooserLauncher.launch(chooser)
                    } catch (e: Exception) {
                        fileUploadCallback?.onReceiveValue(null)
                        fileUploadCallback = null
                    }

                    return true
                }
            }

            // 注册 Native 桥接
            addJavascriptInterface(WebBridge(requireContext()) { visible ->
                activity?.runOnUiThread {
                    activity?.findViewById<View>(R.id.bottom_nav)?.visibility =
                        if (visible) View.VISIBLE else View.GONE
                }
            }, "nativeBridge")

            // 注入状态栏高度 CSS 变量 + 暗色模式主题
            val isDarkMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val sbHeight = resources.getIdentifier(
                        "status_bar_height", "dimen", "android"
                    ).let { if (it > 0) resources.getDimensionPixelSize(it) else 48 }
                    val js = buildString {
                        append("document.documentElement.style.setProperty('--sb-height', '${sbHeight}px');")
                        if (isDarkMode) {
                            append("document.body.setAttribute('data-theme','dark');")
                        }
                    }
                    view?.evaluateJavascript(js, null)
                }
            }

            loadUrl("file:///android_asset/home_page.html?v=8")
        }
    }

    /**
     * 创建拍照 Intent，返回 null 表示不支持
     */
    private fun createCameraIntent(): Intent? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)

            cameraPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 处理文件选择结果
     */
    private fun handleFileChooserResult(resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || fileUploadCallback == null) {
            // 用户取消
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = null
            return
        }

        val result = when {
            // 从 Intent data 中获取 Uri（图库选择）
            data?.data != null -> arrayOf(data.data!!)
            // 拍照结果
            cameraPhotoUri != null -> arrayOf(cameraPhotoUri!!)
            else -> null
        }

        fileUploadCallback?.onReceiveValue(result)
        fileUploadCallback = null
    }

    override fun onResume() {
        super.onResume()
        // 检查是否有待同步的恢复数据（从 SettingsFragment restoreData 设置）
        val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("pending_restore_sync", false)) {
            prefs.edit().putBoolean("pending_restore_sync", false).apply()
            reloadFromRestore()
        }
    }

    fun reloadFromRestore() {
        val webView = _binding?.webView ?: return
        val context = requireContext()
        val allData = com.timememorial.app.data.AnniversaryRepository.getAll(context)
        val jsonArray = org.json.JSONArray()
        for (item in allData) {
            val obj = org.json.JSONObject()
            obj.put("id", item["id"] ?: "")
            obj.put("title", item["title"] ?: "")
            obj.put("date", item["date"] ?: "")
            obj.put("category", item["category"] ?: "")
            obj.put("repeatYearly", item["repeatYearly"] ?: true)
            obj.put("reminderDays", item["reminderDays"] ?: 3)
            obj.put("photoUri", item["photoUri"] ?: "")
            obj.put("note", item["note"] ?: "")
            jsonArray.put(obj)
        }
        val jsonStr = org.json.JSONObject().apply { put("data", jsonArray) }.toString()
        val escaped = android.util.Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        webView.evaluateJavascript(
            """
            (function() {
                var data = JSON.parse(atob('$escaped'));
                if (data.data && Array.isArray(data.data)) {
                    localStorage.setItem('anniversaries', JSON.stringify(data.data));
                    if (typeof loadAnniversaries === 'function') loadAnniversaries();
                    else if (typeof renderAll === 'function') renderAll();
                    else location.reload();
                }
            })();
            """.trimIndent(), null
        )
    }

    override fun onDestroyView() {
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = null
        _binding?.webView?.destroy()
        _binding = null
        super.onDestroyView()
    }
}
