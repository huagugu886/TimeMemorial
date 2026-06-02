package com.timememorial.app.ui.home

import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID

/**
 * WebView JS 桥接：图片存取 + IPC 文件通信
 *
 * 图片：JS 端通过 window.nativeBridge.saveImage(base64) 保存图片
 * IPC：JS 端通过 window.nativeBridge.writeIpcFile/readIpcFile 与 Termux 通信
 */
class WebBridge(private val context: Context, private val onSetBottomNavVisibility: ((Boolean) -> Unit)? = null) {

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

    // ========== IPC 文件读写（Termux 通信管道）==========

    /**
     * 将文本内容写入指定文件路径
     * 用于向 /sdcard/Download/.bash_ipc/cmd.json 写入命令
     */
    @JavascriptInterface
    fun writeIpcFile(path: String, content: String): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 读取指定文件的文本内容
     * 用于从 /sdcard/Download/.bash_ipc/result.json 读取结果
     */
    @JavascriptInterface
    fun readIpcFile(path: String): String {
        return try {
            val file = File(path)
            if (file.exists()) file.readText() else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 删除指定文件
     */
    @JavascriptInterface
    fun deleteIpcFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    /** 控制底部导航栏显示/隐藏（由 JS 在全屏预览时调用） */
    @JavascriptInterface
    fun setBottomNavVisibility(visible: Boolean) {
        onSetBottomNavVisibility?.invoke(visible)
    }

    // ── 纪念日数据同步到原生端（供提醒功能使用） ──
    @JavascriptInterface
    fun syncAnniversaries(jsonString: String) {
        try {
            android.util.Log.w("WebBridge", "syncAnniversaries CALLED, json length=${jsonString.length}")
            android.util.Log.w("WebBridge", "syncAnniversaries json=$jsonString")
            android.util.Log.w("WebBridge", "syncAnniversaries context.filesDir=${context.filesDir}")
            android.util.Log.w("WebBridge", "syncAnniversaries context.packageName=${context.packageName}")
            val jsonArray = org.json.JSONArray(jsonString)
            com.timememorial.app.reminder.ReminderSettings.saveAnniversaries(context, jsonArray)
            android.util.Log.w("WebBridge", "syncAnniversaries saveAnniversaries DONE")
            // 验证写入结果
            val verify = com.timememorial.app.reminder.ReminderSettings.getAnniversaries(context)
            android.util.Log.w("WebBridge", "syncAnniversaries verify read back count=${verify.length()}")
            // 同步后立即调度提醒
            com.timememorial.app.reminder.ReminderManager.scheduleDaily(context)
            android.util.Log.w("WebBridge", "syncAnniversaries scheduleDaily DONE")
        } catch (e: Exception) {
            android.util.Log.e("WebBridge", "syncAnniversaries FAILED", e)
        }
    }

    // ── 查询当前待提醒列表（供 WebView 展示） ──
    @JavascriptInterface
    fun getUpcomingReminders(): String {
        return try {
            val reminders = com.timememorial.app.reminder.ReminderSettings.getUpcomingReminders(context)
            val jsonArray = org.json.JSONArray()
            for (r in reminders) {
                val obj = org.json.JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("date", r.date)
                    put("daysUntil", r.daysUntil)
                    put("isToday", r.isToday)
                    put("category", r.category)
                }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    // ── 获取提醒设置 ──
    @JavascriptInterface
    fun getReminderSettings(): String {
        return try {
            val obj = org.json.JSONObject().apply {
                put("enabled", com.timememorial.app.reminder.ReminderSettings.isEnabled(context))
                put("daysBefore", com.timememorial.app.reminder.ReminderSettings.getDaysBefore(context))
                put("hour", com.timememorial.app.reminder.ReminderSettings.getHour(context))
                put("minute", com.timememorial.app.reminder.ReminderSettings.getMinute(context))
            }
            obj.toString()
        } catch (e: Exception) {
            "{}"
        }
    }

    // ── 保存提醒设置 ──
    @JavascriptInterface
    fun saveReminderSettings(jsonString: String) {
        try {
            val obj = org.json.JSONObject(jsonString)
            val settings = com.timememorial.app.reminder.ReminderSettings
            if (obj.has("enabled")) settings.setEnabled(context, obj.getBoolean("enabled"))
            if (obj.has("daysBefore")) settings.setDaysBefore(context, obj.getInt("daysBefore"))
            if (obj.has("hour") && obj.has("minute")) {
                settings.setTime(context, obj.getInt("hour"), obj.getInt("minute"))
            }
            // 重新调度
            if (settings.isEnabled(context)) {
                com.timememorial.app.reminder.ReminderManager.scheduleDaily(context)
            } else {
                com.timememorial.app.reminder.ReminderManager.cancel(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("WebBridge", "saveReminderSettings failed", e)
        }
    }
}
