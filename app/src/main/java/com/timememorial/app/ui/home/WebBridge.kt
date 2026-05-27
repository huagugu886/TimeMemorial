package com.timememorial.app.ui.home

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * WebView JS 桥接：图片存取
 *
 * JS 端通过 window.nativeBridge.saveImage(base64) 保存图片，
 * 返回文件名（相对路径），加载时用 nativeBridge.getImagePath(name) 获取绝对路径。
 */
class WebBridge(private val context: Context) {

    /** 图片存放目录：app 内部存储/images/ */
    private val imageDir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    /**
     * 保存 base64 图片到本地文件
     * @param base64Data 完整的 data:image/xxx;base64,xxxx 或纯 base64
     * @return 保存成功返回文件名（如 "abc123.jpg"），失败返回空字符串
     */
    @JavascriptInterface
    fun saveImage(base64Data: String): String {
        return try {
            // 解析 data URL
            val (mimeType, rawBase64) = if (base64Data.startsWith("data:")) {
                val header = base64Data.substringBefore(",")
                val data = base64Data.substringAfter(",")
                val mime = header.substringAfter(":").substringBefore(";")
                mime to data
            } else {
                "image/jpeg" to base64Data
            }

            // 根据 MIME 确定扩展名
            val ext = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("gif") -> "gif"
                else -> "jpg"
            }

            // 解码并写入文件
            val bytes = Base64.decode(rawBase64, Base64.DEFAULT)
            val fileName = "${UUID.randomUUID()}.$ext"
            val file = File(imageDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }

            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 根据文件名获取图片绝对路径
     */
    @JavascriptInterface
    fun getImagePath(fileName: String): String {
        val file = File(imageDir, fileName)
        return if (file.exists()) file.absolutePath else ""
    }

    /**
     * 删除图片文件
     */
    @JavascriptInterface
    fun deleteImage(fileName: String): Boolean {
        return try {
            val file = File(imageDir, fileName)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清理孤儿图片（不在列表中的文件）
     * @param activeNames JSON 数组字符串，如 '["a.jpg","b.jpg"]'
     */
    @JavascriptInterface
    fun getImageBase64(fileName: String): String {
        val file = File(imageDir, fileName)
        if (!file.exists()) return ""
        val bytes = file.inputStream().use { it.readBytes() }
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val ext = file.extension.lowercase()
        val mime = when (ext) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }
        return "data:$mime;base64,$base64"
    }

    @JavascriptInterface
    fun cleanOrphanImages(activeNames: String): Int {
        return try {
            val active = activeNames
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .toSet()

            val files = imageDir.listFiles() ?: return 0
            var deleted = 0
            files.forEach { file ->
                if (file.name !in active) {
                    file.delete()
                    deleted++
                }
            }
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
