package com.timememorial.app.data.model

data class Anniversary(
    val id: Long = 0,
    val title: String,
    val date: String,
    val category: String,
    val repeatYearly: Boolean = true,
    val reminderDays: Int = 3,
    val photoUri: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)