package com.example.intern_stockmate.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StockItem(
    val itemCode: String = "",
    val description: String = "",
    val desc2: String? = null,
    val isActive: String? = null,
    val itemGroup: String? = null,
    val uom: String = "UNIT",
    val rate: Double? = null,
    val price: Double = 0.0,
    val price2: Double? = null,
    val price3: Double? = null,
    val price4: Double? = null,
    val price5: Double? = null,
    val price6: Double? = null,
    val shelf: String? = null,
    val barCode: String? = null,
    val balQty: Int = 0,
    val location: String = "",
    val itemPhoto: String? = null,
    val uomList: List<UomInfo> = emptyList(),
    val locationList: List<LocationInfo> = emptyList()
) : Parcelable

@Parcelize
data class UomInfo(
    val uom: String,
    val rate: Double,
    val price1: Double,
    val price2: Double,
    val price3: Double,
    val price4: Double,
    val price5: Double,
    val price6: Double,
    val barCode: String? = null
) : Parcelable

@Parcelize
data class LocationInfo(
    val location: String,
    val qty: Int,
    val diffQty: Int = 0
) : Parcelable