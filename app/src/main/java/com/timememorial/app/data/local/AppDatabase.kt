package com.timememorial.app.data.local

import android.content.Context

class AppDatabase private constructor(context: Context) {

    private val dao = AnniversaryDao()

    fun anniversaryDao(): AnniversaryDao = dao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}