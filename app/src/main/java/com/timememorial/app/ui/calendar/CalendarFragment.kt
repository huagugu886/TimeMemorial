package com.timememorial.app.ui.calendar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.timememorial.app.R
import com.timememorial.app.databinding.FragmentCalendarBinding

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                loadWithOverviewMode = true
            }

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            // 页面加载完成后注入状态栏高度 + 暗色模式主题
            val isDarkMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val darkJs = if (isDarkMode) "document.body.setAttribute('data-theme','dark');" else ""
                    view?.evaluateJavascript(
                        """
                        (function() {
                            var sbHeight = ${getStatusBarHeight()};
                            document.documentElement.style.setProperty('--sb-height', sbHeight + 'px');
                            // 隐藏底部导航栏（原生已有底栏）
                            var bottomNav = document.querySelector('.bottom-nav');
                            if (bottomNav) bottomNav.style.display = 'none';
                            $darkJs
                        })();
                        """.trimIndent(), null
                    )
                }
            }

            // 返回键拦截：详情弹窗打开时先关弹窗
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        webView.evaluateJavascript(
                            "typeof isDetailOpen==='function'&&isDetailOpen()"
                        ) { result ->
                            if (result?.trim('"') == "true") {
                                webView.evaluateJavascript("closeDetailForce()", null)
                            } else {
                                isEnabled = false
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    }
                }
            )

            loadUrl("file:///android_asset/calendar_page.html?v=1")
        }
    }

    private fun getStatusBarHeight(): Int {
        val insets = ViewCompat.getRootWindowInsets(binding.root)
        return insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
    }

    override fun onDestroyView() {
        binding.webView.destroy()
        _binding = null
        super.onDestroyView()
    }
}
