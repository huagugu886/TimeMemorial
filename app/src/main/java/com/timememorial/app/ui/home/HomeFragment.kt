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
import com.timememorial.app.ui.handleWebViewBack

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 文件选择回调
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    // 拍照临时文件
    private var cameraPhotoUri: Uri? = null

    // 文件选择器
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>
    
    // WebView 页面是否加载完成（防止 onPageFinished 前标志被清掉）
    @Volatile
    private var pageReady = false

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

                    // 标记页面就绪，再检查恢复同步
                    pageReady = true
                    syncRestoreDataIfNeeded(view)
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
        // 保留 onResume 检查：当页面已加载时，用户从设置页返回也能立即同步
        syncRestoreDataIfNeeded(_binding?.webView)
    }

    /**
     * 检查是否有待同步的恢复数据，有则注入 WebView。
     * 在 onPageFinished 和 onResume 中调用，确保 WebView 已加载。
     */
    private fun syncRestoreDataIfNeeded(webView: WebView?) {
        if (!isAdded || webView == null) return
        if (!pageReady) return  // 页面还没加载完，跳过（标志不清除，等 onPageFinished 再试）
        val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("pending_restore_sync", false)) return

        prefs.edit().putBoolean("pending_restore_sync", false).apply()
        android.widget.Toast.makeText(requireContext(), "数据恢复成功，正在刷新首页…", android.widget.Toast.LENGTH_SHORT).show()

        // 直接用 SettingsFragment.restoreData() 提前存好的首页格式 JSON
        // 不能从 AnniversaryRepository 读——页面初始化时 syncToNative() 已经
        // 把 localStorage（空的）同步回 native 覆盖了 anniversaries.json
        val pendingData = prefs.getString("pending_restore_data", null)
        if (pendingData.isNullOrEmpty()) {
            android.util.Log.w("HomeFragment", "pending_restore_data is empty, skip restore")
            return
        }

        val jsonStr = pendingData
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        // 先调 JS 端函数注入（更新内存+localStorage），再 reload 确保渲染
        val js = "window.__injectRestoreData && __injectRestoreData('$jsonStr'); setTimeout(function(){ window.location.reload(); }, 200);"
        webView.evaluateJavascript(js, null)
    }

    override fun onDestroyView() {
        pageReady = false
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = null
        _binding?.webView?.destroy()
        _binding = null
        super.onDestroyView()
    }
}
