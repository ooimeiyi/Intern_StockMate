package com.example.intern_stockmate.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StockItem(
    val itemCode: String,
    val description: String,
    val desc2: String? = null,
    val isActive: String? = null,
    val itemGroup: String? = null,
    val uom: String,
    val rate: Double? = null,
    val price: Double,
    val price2: Double? = null,
    val price3: Double? = null,
    val price4: Double? = null,
    val price5: Double? = null,
    val price6: Double? = null,
    val shelf: String? = null,
    val barCode: String? = null,
    val balQty: Int,
    val location: String,
    val itemPhoto: String? = null,
    // Grouping lists
    val uomList: List<UomInfo> = emptyList(),
    val locationList: List<LocationInfo> = emptyList()
) : Parcelable

// get the data based on the specific UOM
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


//get the quantity based on the specific location
@Parcelize
data class LocationInfo(
    val location: String,
    val qty: Int,
    val diffQty: Int = 0
) : Parcelable