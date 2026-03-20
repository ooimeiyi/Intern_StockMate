package com.example.intern_stockmate.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments")
    suspend fun getAllHeaders(): List<StockAdjustmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(headers: List<StockAdjustmentEntity>)

    @Query("DELETE FROM stock_adjustments")
    suspend fun clearAll()
}