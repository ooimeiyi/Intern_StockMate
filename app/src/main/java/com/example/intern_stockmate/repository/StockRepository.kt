package com.example.intern_stockmate.repository

import com.example.intern_stockmate.model.StockItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StockRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getStock(): List<StockItem> {
        val snapshot = firestore.collection(STOCK_COLLECTION)
            .document(STOCK_DOCUMENT)
            .get()
            .await()

        val rawData = snapshot.data.orEmpty()

        return rawData.values
            .mapNotNull { entry -> entry as? Map<*, *> }
            .mapNotNull(::mapToStockItem)
            .sortedBy { it.itemCode }
    }

    suspend fun getLocations(): List<String> {
        return getStock()
            .mapNotNull { it.location }
            .filter { it.isNotBlank() && it != "N/A" }
            .distinct()
            .sorted()
    }

    private fun mapToStockItem(data: Map<*, *>): StockItem? {
        val itemCode = data.string("ItemCode") ?: return null
        val description = data.string("Description") ?: return null
        val uom = data.string("UOM") ?: "UNIT"

        return StockItem(
            itemCode = itemCode,
            description = description,
            desc2 = data.string("Desc2"),
            isActive = data.string("IsActive"),
            itemGroup = data.string("ItemGroup"),
            uom = uom,
            rate = data.double("Rate") ?: 1.0,
            price = data.double("Price") ?: 0.0,
            price2 = data.double("Price2") ?: 0.0,
            price3 = data.double("Price3") ?: 0.0,
            price4 = data.double("Price4") ?: 0.0,
            price5 = data.double("Price5") ?: 0.0,
            price6 = data.double("Price6") ?: 0.0,
            shelf = data.string("Shelf"),
            barCode = data.string("BarCode"),
            balQty = data.int("BalQty"),
            location = data.string("Location") ?: "N/A",
            itemPhoto = data.string("ItemPhoto")
        )
    }

    private fun Map<*, *>.string(key: String): String? {
        return this[key]?.toString()?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun Map<*, *>.double(key: String): Double? {
        return when (val value = this[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun Map<*, *>.int(key: String): Int {
        return when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: value.toDoubleOrNull()?.toInt() ?: 0
            else -> 0
        }
    }

    companion object {
        private const val STOCK_COLLECTION = "StockList"
        private const val STOCK_DOCUMENT = "Current"
    }
}