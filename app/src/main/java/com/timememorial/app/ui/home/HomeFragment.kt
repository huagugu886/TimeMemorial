package com.timememorial.app.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.timememorial.app.databinding.FragmentHomeBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            clearCache(true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
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
            addJavascriptInterface(WebBridge(requireContext()), "nativeBridge")

            loadUrl("file:///android_asset/home_page.html?v=7")
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

    override fun onDestroyView() {
        fileUploadCallback?.onReceiveValue(null)
        fileUploadCallback = null
        _binding?.webView?.destroy()
        _binding = null
        super.onDestroyView()
    }
}
