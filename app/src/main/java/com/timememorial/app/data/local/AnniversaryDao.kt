package com.timememorial.app.data.local

import com.timememorial.app.data.model.Anniversary

class AnniversaryDao {
    private val items = mutableListOf<Anniversary>()

    fun getAll(): List<Anniversary> = items.sortedBy { it.date }

    fun getByCategory(category: String): List<Anniversary> =
        items.filter { it.category == category }.sortedBy { it.date }

    fun getById(id: Long): Anniversary? = items.find { it.id == id }

    fun insert(anniversary: Anniversary): Long {
        val id = if (anniversary.id == 0L) System.currentTimeMillis() else anniversary.id
        items.add(anniversary.copy(id = id))
        return id
    }

    fun update(anniversary: Anniversary) {
        val index = items.indexOfFirst { it.id == anniversary.id }
        if (index >= 0) items[index] = anniversary
    }

    fun delete(anniversary: Anniversary) {
        items.removeIf { it.id == anniversary.id }
    }

    fun deleteById(id: Long) {
        items.removeIf { it.id == id }
    }
}