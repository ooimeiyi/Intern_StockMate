package com.example.intern_stockmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.intern_stockmate.model.SalesOrderDetail
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap

@Database(entities = [SalesOrderEntity::class], version = 1, exportSchema = false)
@TypeConverters(SalesOrderConverters::class)
abstract class SalesOrderDatabase : RoomDatabase() {
    abstract fun salesOrderDao(): SalesOrderDao

    companion object {
        private val instances = ConcurrentHashMap<String, SalesOrderDatabase>()

        fun getInstance(context: Context, companyId: String): SalesOrderDatabase {
            val key = companyId.ifBlank { "unselected" }
            return instances.getOrPut(key) {
                Room.databaseBuilder(
                    context.applicationContext,
                    SalesOrderDatabase::class.java,
                    "sales_order_db_${sanitize(key)}"
                ).build()
            }
        }
        private fun sanitize(value: String): String =
            value.lowercase().replace(Regex("[^a-z0-9_]"), "_")
    }
}

class SalesOrderConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromDetails(details: List<SalesOrderDetail>): String {
        return gson.toJson(details)
    }

    @TypeConverter
    fun toDetails(raw: String): List<SalesOrderDetail> {
        if (raw.isBlank()) return emptyList()
        val type = object : TypeToken<List<SalesOrderDetail>>() {}.type
        return gson.fromJson(raw, type)
    }
}