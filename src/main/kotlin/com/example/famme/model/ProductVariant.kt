package com.example.famme.model

import java.math.BigDecimal
import java.sql.Timestamp

data class ProductVariant(
    val id: Int? = null,
    val externalId: Long,
    val productId: Int,
    val title: String? = null,
    val option1: String? = null,
    val option2: String? = null,
    val option3: String? = null,
    val sku: String? = null,
    val price: BigDecimal = BigDecimal.ZERO,
    val available: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
