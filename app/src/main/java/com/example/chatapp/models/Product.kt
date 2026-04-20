package com.example.chatapp.models

data class Product(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0
)
