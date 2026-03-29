package com.example.intern_stockmate.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SalesOrderDao {
    @Query("SELECT * FROM sales_orders")
    suspend fun getAllHeaders(): List<SalesOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(headers: List<SalesOrderEntity>)

    @Query("DELETE FROM sales_orders")
    suspend fun clearAll()
}