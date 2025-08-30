package com.example.famme.model

data class ProductOption(
    val id: Int? = null,
    val productId: Int,
    val name: String,
    val position: Int,
    val values: String? = null
)
