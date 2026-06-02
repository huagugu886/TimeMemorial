package com.timememorial.app.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 纪念日数据的唯一原生存储层（单源）。
 *
 * 持久化：JSON 文件 (/data/data/.../files/anniversaries.json)
 * 同步：每次写入后自动同步到 SharedPreferences（供提醒功能读取）
 * 线程安全：synchronized
 */
object AnniversaryRepository {

    private const val FILE_NAME = "anniversaries.json"
    private const val PREFS_NAME = "reminder_settings"
    private const val KEY_ANNIVERSARIES = "anniversaries"

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    // ======================== 读取 ========================

    /** 读取所有纪念日，按日期排序 */
    @Synchronized
    fun getAll(context: Context): List<Map<String, Any?>> {
        return try {
            val file = getFile(context)
            if (!file.exists()) return emptyList()
            val arr = JSONArray(file.readText())
            parseArray(arr).sortedBy { it["date"] as? String ?: "" }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 按 ID 查找 */
    fun getById(context: Context, id: Long): Map<String, Any?>? {
        return getAll(context).find { it["id"] == id }
    }

    /** 按分类筛选 */
    fun getByCategory(context: Context, category: String): List<Map<String, Any?>> {
        return getAll(context).filter { it["category"] == category }
    }

    // ======================== 写入 ========================

    /** 全量写入（由 WebBridge syncAnniversaries 调用） */
    @Synchronized
    fun writeAll(context: Context, items: List<Map<String, Any?>>) {
        try {
            val arr = JSONArray()
            for (item in items) {
                arr.put(toJsonObject(item))
            }
            getFile(context).writeText(arr.toString(2))
            syncToPrefs(context, arr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 新增一条，返回生成的 ID */
    @Synchronized
    fun insert(context: Context, item: Map<String, Any?>): Long {
        val all = getAll(context).toMutableList()
        val id = if ((item["id"] as? Long ?: 0L) == 0L) System.currentTimeMillis()
                 else item["id"] as Long
        val newItem = item.toMutableMap().apply { put("id", id) }
        all.add(newItem)
        writeAll(context, all)
        return id
    }

    /** 更新一条 */
    @Synchronized
    fun update(context: Context, item: Map<String, Any?>) {
        val id = item["id"] as? Long ?: return
        val all = getAll(context).toMutableList()
        val index = all.indexOfFirst { it["id"] == id }
        if (index >= 0) {
            all[index] = item
            writeAll(context, all)
        }
    }

    /** 按 ID 删除 */
    @Synchronized
    fun deleteById(context: Context, id: Long) {
        val all = getAll(context).filter { it["id"] != id }
        writeAll(context, all)
    }

    // ======================== 提醒查询 ========================

    data class AnniversaryInfo(
        val id: Long,
        val title: String,
        val date: String,
        val daysUntil: Int,
        val isToday: Boolean,
        val category: String
    )

    /** 获取即将到来的纪念日（供提醒使用） */
    fun getUpcomingReminders(context: Context, daysBefore: Int = 3): List<AnniversaryInfo> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        return getAll(context).mapNotNull { item ->
            try {
                val dateStr = (item["date"] as? String) ?: return@mapNotNull null
                val date = LocalDate.parse(dateStr.take(10), formatter)
                var next = date.withYear(today.year)
                if (next.isBefore(today)) next = next.plusYears(1)
                val daysUntil = ChronoUnit.DAYS.between(today, next).toInt()
                if (daysUntil in 0..daysBefore) {
                    AnniversaryInfo(
                        id = item["id"] as? Long ?: 0L,
                        title = item["title"] as? String ?: "",
                        date = dateStr,
                        daysUntil = daysUntil,
                        isToday = daysUntil == 0,
                        category = item["category"] as? String ?: "other"
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.daysUntil }
    }

    // ======================== 内部工具 ========================

    /** JSON → Map */
    private fun parseArray(arr: JSONArray): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val map = mutableMapOf<String, Any?>()
            for (key in obj.keys()) {
                map[key] = when (obj.get(key)) {
                    is org.json.JSONObject -> null // 简化，目前无嵌套对象
                    else -> obj.opt(key)
                }
            }
            // 保证 id 是 Long 类型
            if (map.containsKey("id")) {
                map["id"] = obj.optLong("id", 0L)
            }
            result.add(map)
        }
        return result
    }

    /** Map → JSONObject */
    private fun toJsonObject(item: Map<String, Any?>): JSONObject {
        val obj = JSONObject()
        for ((key, value) in item) {
            when (value) {
                is Long -> obj.put(key, value)
                is Int -> obj.put(key, value)
                is Boolean -> obj.put(key, value)
                is Double -> obj.put(key, value)
                is String -> obj.put(key, value)
                null -> obj.put(key, JSONObject.NULL)
                else -> obj.put(key, value.toString())
            }
        }
        return obj
    }

    /** 同步到 SharedPreferences（保持与提醒功能兼容） */
    private fun syncToPrefs(context: Context, arr: JSONArray) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ANNIVERSARIES, arr.toString())
            .apply()
    }
}
