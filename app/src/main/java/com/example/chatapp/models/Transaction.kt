package com.example.chatapp.models

data class Transaction(
    val id: String = "",
    val type: String = "", // "STOCK" or "SALE"
    val itemName: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val buyerName: String? = null,
    val profit: Double? = null,
    val date: Long = 0L
)
