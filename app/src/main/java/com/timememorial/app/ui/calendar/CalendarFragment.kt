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
import com.timememorial.app.ui.handleWebViewBack

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
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
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

            // 页面加载完成后注入暗色模式主题
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
                            // 隐藏底部导航栏（原生已有底栏）
                            var bottomNav = document.querySelector('.bottom-nav');
                            if (bottomNav) bottomNav.style.display = 'none';
                            $darkJs
                        })();
                        """.trimIndent(), null
                    )
                }
            }

            // 系统返回手势：WebView 有历史记录时 goBack()，否则放行
            handleWebViewBack(this)

            // 返回键拦截：详情弹窗打开时先关弹窗
            requireActivity().onBackPressedDispatcher.addCallback(
                viewLifecycleOwner, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        binding.webView.evaluateJavascript(
                            "typeof isDetailOpen==='function'&&isDetailOpen()"
                        ) { result ->
                            if (result?.trim('"') == "true") {
                                binding.webView.evaluateJavascript("closeDetailForce()", null)
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

    override fun onDestroyView() {
        binding.webView.destroy()
        _binding = null
        super.onDestroyView()
    }
}
