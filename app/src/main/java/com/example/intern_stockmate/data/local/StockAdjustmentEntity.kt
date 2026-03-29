package com.example.intern_stockmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.intern_stockmate.model.StockAdjustmentDetail
import com.example.intern_stockmate.model.StockAdjustmentHeader

@Entity(tableName = "stock_adjustments")
data class StockAdjustmentEntity(
    @PrimaryKey val stockTakeNo: String,
    val description: String,
    val date: String,
    val location: String,
    val status: String,
    val items: List<StockAdjustmentDetail>
)

fun StockAdjustmentEntity.toModel(): StockAdjustmentHeader {
    return StockAdjustmentHeader(
        description = description,
        date = date,
        stockTakeNo = stockTakeNo,
        location = location,
        status = status,
        items = items
    )
}

fun StockAdjustmentHeader.toEntity(): StockAdjustmentEntity {
    return StockAdjustmentEntity(
        stockTakeNo = stockTakeNo,
        description = description,
        date = date,
        location = location,
        status = status,
        items = items
    )
}