package com.timememorial.app.data.local

import android.content.Context
import com.timememorial.app.data.model.Anniversary

/**
 * 纪念日 DAO，数据全部委托给 AnniversaryRepository 持久化。
 *
 * 需要在创建时传入 Context（通过 AppDatabase）。
 */
class AnniversaryDao(private val context: Context) {

    fun getAll(): List<Anniversary> =
        AnniversaryRepository.getAll(context).map { toModel(it) }

    fun getByCategory(category: String): List<Anniversary> =
        AnniversaryRepository.getByCategory(context, category).map { toModel(it) }

    fun getById(id: Long): Anniversary? =
        AnniversaryRepository.getById(context, id)?.let { toModel(it) }

    fun insert(anniversary: Anniversary): Long {
        val id = if (anniversary.id == 0L) System.currentTimeMillis() else anniversary.id
        AnniversaryRepository.insert(context, toMap(anniversary.copy(id = id)))
        return id
    }

    fun update(anniversary: Anniversary) {
        AnniversaryRepository.update(context, toMap(anniversary))
    }

    fun delete(anniversary: Anniversary) {
        AnniversaryRepository.deleteById(context, anniversary.id)
    }

    fun deleteById(id: Long) {
        AnniversaryRepository.deleteById(context, id)
    }

    // ========== 转换工具 ==========

    private fun toModel(map: Map<String, Any?>): Anniversary = Anniversary(
        id = map["id"] as? Long ?: 0L,
        title = map["title"] as? String ?: "",
        date = map["date"] as? String ?: "",
        category = map["category"] as? String ?: "other",
        repeatYearly = map["repeatYearly"] as? Boolean ?: true,
        reminderDays = (map["reminderDays"] as? Number)?.toInt() ?: 3,
        photoUri = map["photoUri"] as? String,
        note = map["note"] as? String,
        createdAt = map["createdAt"] as? Long ?: System.currentTimeMillis()
    )

    private fun toMap(a: Anniversary): Map<String, Any?> = mapOf(
        "id" to a.id,
        "title" to a.title,
        "date" to a.date,
        "category" to a.category,
        "repeatYearly" to a.repeatYearly,
        "reminderDays" to a.reminderDays,
        "photoUri" to a.photoUri,
        "note" to a.note,
        "createdAt" to a.createdAt
    )
}
