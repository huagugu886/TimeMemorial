package com.timememorial.app.reminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReminderSettings {

    private const val PREFS_NAME = "reminder_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_DAYS_BEFORE = "days_before"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_DEFAULT_REPEAT = "default_repeat"
    private const val KEY_ANNIVERSARIES = "anniversaries"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── 提醒设置 ──

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, true)

    fun setEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_ENABLED, v).apply()

    fun getDaysBefore(ctx: Context): Int = prefs(ctx).getInt(KEY_DAYS_BEFORE, 3)

    fun setDaysBefore(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_DAYS_BEFORE, v).apply()

    fun getHour(ctx: Context): Int = prefs(ctx).getInt(KEY_HOUR, 9)

    fun getMinute(ctx: Context): Int = prefs(ctx).getInt(KEY_MINUTE, 0)

    fun setTime(ctx: Context, hour: Int, minute: Int) =
        prefs(ctx).edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()

    fun getDefaultRepeat(ctx: Context): String = prefs(ctx).getString(KEY_DEFAULT_REPEAT, "yearly") ?: "yearly"

    fun setDefaultRepeat(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_DEFAULT_REPEAT, v).apply()

    // ── 纪念日数据同步 ──

    fun saveAnniversaries(ctx: Context, jsonArray: JSONArray) =
        prefs(ctx).edit().putString(KEY_ANNIVERSARIES, jsonArray.toString()).apply()

    fun getAnniversaries(ctx: Context): JSONArray {
        val raw = prefs(ctx).getString(KEY_ANNIVERSARIES, null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
    }

    /** 从 SharedPreferences 读取并筛选出需要提醒的纪念日 */
    fun getUpcomingReminders(ctx: Context): List<AnniversaryInfo> {
        val enabled = isEnabled(ctx)
        if (!enabled) return emptyList()

        val daysBefore = getDaysBefore(ctx)
        val arr = getAnniversaries(ctx)
        val today = java.time.LocalDate.now()
        val result = mutableListOf<AnniversaryInfo>()

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val dateStr = obj.optString("date", "") ?: continue
            if (dateStr.length < 10) continue

            try {
                val base = java.time.LocalDate.parse(dateStr.substring(0, 10))
                var next = base.withYear(today.year)
                if (next.isBefore(today)) next = next.withYear(today.year + 1)

                val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, next).toInt()

                if (daysUntil in 0..daysBefore) {
                    result.add(
                        AnniversaryInfo(
                            id = obj.optLong("id", 0),
                            title = obj.optString("title", "纪念日"),
                            date = dateStr,
                            daysUntil = daysUntil,
                            isToday = daysUntil == 0,
                            category = obj.optString("category", "other")
                        )
                    )
                }
            } catch (_: Exception) { /* 跳过无效日期 */ }
        }
        return result.sortedBy { it.daysUntil }
    }

    data class AnniversaryInfo(
        val id: Long,
        val title: String,
        val date: String,
        val daysUntil: Int,
        val isToday: Boolean,
        val category: String
    )
}
