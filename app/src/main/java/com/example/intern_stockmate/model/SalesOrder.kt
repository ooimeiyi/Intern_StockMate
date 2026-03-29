package com.example.intern_stockmate.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SalesOrderHeader(
    val debtor: String = "",
    val date: String = "",
    val soNo: String = "",
    val location: String = "",
    val items: List<SalesOrderDetail> = emptyList(),
    val status: String = "KIV"
) : Parcelable

@Parcelize
data class SalesOrderDetail(
    val itemCode: String = "",
    val qty: String = "",
    val uom: String = "",
    val unitPrice: Double = 0.0
) : Parcelable