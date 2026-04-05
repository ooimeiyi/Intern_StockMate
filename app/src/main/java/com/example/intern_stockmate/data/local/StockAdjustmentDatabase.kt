package com.example.intern_stockmate.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.intern_stockmate.model.StockAdjustmentDetail
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap

@Database(entities = [StockAdjustmentEntity::class], version = 1, exportSchema = false)
@TypeConverters(StockAdjustmentConverters::class)
abstract class StockAdjustmentDatabase : RoomDatabase() {
    abstract fun stockAdjustmentDao(): StockAdjustmentDao

    companion object {
        private val instances = ConcurrentHashMap<String, StockAdjustmentDatabase>()

        fun getInstance(context: Context, companyId: String): StockAdjustmentDatabase {
            val key = companyId.ifBlank { "unselected" }
            return instances.getOrPut(key) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StockAdjustmentDatabase::class.java,
                    "stock_adjustment_db_${sanitize(key)}"
                ).build()
            }
        }
        private fun sanitize(value: String): String =
            value.lowercase().replace(Regex("[^a-z0-9_]"), "_")
    }
}

class StockAdjustmentConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromDetails(details: List<StockAdjustmentDetail>): String {
        return gson.toJson(details)
    }

    @TypeConverter
    fun toDetails(raw: String): List<StockAdjustmentDetail> {
        if (raw.isBlank()) return emptyList()
        val type = object : TypeToken<List<StockAdjustmentDetail>>() {}.type
        return gson.fromJson(raw, type)
    }
}