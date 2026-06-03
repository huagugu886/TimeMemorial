package com.timememorial.app.ui

import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

fun Fragment.getStatusBarHeightDp(): Float {
    // 优先用系统 dimen 资源，最准
    val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resId > 0) {
        return resources.getDimensionPixelSize(resId) / resources.displayMetrics.density
    }
    // fallback: WindowInsets
    val root = view?.rootView ?: return 28f
    val top = ViewCompat.getRootWindowInsets(root)
        ?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
    return top / resources.displayMetrics.density
}

fun Fragment.handleWebViewBack(webView: WebView) {
    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    )
}
