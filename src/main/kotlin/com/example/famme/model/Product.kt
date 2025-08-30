package com.example.famme.model

import java.sql.Timestamp

data class Product(
    val id: Int? = null,
    val externalId: Long,
    val title: String,
    val handle: String? = null,
    val bodyHtml: String? = null,
    val vendor: String? = null,
    val productType: String? = null,
    val publishedAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val tags: List<String> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val images: List<ProductImage> = emptyList(),
    val options: List<ProductOption> = emptyList()
)
