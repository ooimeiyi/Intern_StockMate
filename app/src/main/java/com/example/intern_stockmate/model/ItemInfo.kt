package com.example.intern_stockmate.model

data class ItemInfo(
    val allItem: Int = 0,
    val active: Int = 0,
    val nonActive: Int = 0,
    val stockControl: Int = 0,
    val nonStockControl: Int = 0,
    val itemGroupCount: Int = 0,
    val itemTypeCount: Int = 0,
    val negativeQty: Int = 0,
    val lastUpdate: String = ""
)