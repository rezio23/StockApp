package com.example.chatapp.models

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    var quantity: Int = 0,
    val purchasePrice: Double = 0.0
)
