package com.example.intern_stockmate.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.intern_stockmate.model.SalesOrderDetail
import com.example.intern_stockmate.model.SalesOrderHeader

@Entity(tableName = "sales_orders")
data class SalesOrderEntity(
    @PrimaryKey val soNo: String,
    val debtor: String,
    val date: String,
    val location: String,
    val status: String,
    val items: List<SalesOrderDetail>
)

fun SalesOrderEntity.toModel(): SalesOrderHeader {
    return SalesOrderHeader(
        debtor = debtor,
        date = date,
        soNo = soNo,
        location = location,
        status = status,
        items = items
    )
}

fun SalesOrderHeader.toEntity(): SalesOrderEntity {
    return SalesOrderEntity(
        soNo = soNo,
        debtor = debtor,
        date = date,
        location = location,
        status = status,
        items = items
    )
}