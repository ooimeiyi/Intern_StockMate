package com.example.intern_stockmate.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StockAdjustmentHeader(
    val description: String = "",
    val date: String = "",
    val stockTakeNo: String = "",
    val location: String = "",
    val items: List<StockAdjustmentDetail> = emptyList(),
    val status: String = "KIV"
) : Parcelable

@Parcelize
data class StockAdjustmentDetail(
    val itemCode: String = "",
    val qty: String = "",
    val uom: String = ""
) : Parcelable