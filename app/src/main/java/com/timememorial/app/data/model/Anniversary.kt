package com.timememorial.app.data.model

data class Anniversary(
    val id: Int,
    val title: String,
    val desc: String,
    val date: String,
    val tag: String,
    val daysRemaining: Int,
    val totalDays: Int,
    val isExpired: Boolean = false
)
