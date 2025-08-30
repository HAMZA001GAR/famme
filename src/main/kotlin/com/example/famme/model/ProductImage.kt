package com.example.famme.model

import java.sql.Timestamp

data class ProductImage(
    val id: Int? = null,
    val externalId: Long,
    val productId: Int,
    val src: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val position: Int? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
