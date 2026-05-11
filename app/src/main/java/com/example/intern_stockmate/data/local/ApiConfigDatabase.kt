package com.example.intern_stockmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ApiConfigEntity::class], version = 1, exportSchema = false)
abstract class ApiConfigDatabase : RoomDatabase() {
    abstract fun apiConfigDao(): ApiConfigDao

    companion object {
        @Volatile
        private var INSTANCE: ApiConfigDatabase? = null

        fun getInstance(context: Context): ApiConfigDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApiConfigDatabase::class.java,
                    "api_config_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
