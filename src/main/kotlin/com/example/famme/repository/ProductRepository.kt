package com.example.famme.repository

import com.example.famme.model.Product
import com.example.famme.model.ProductVariant
import com.example.famme.model.ProductImage
import com.example.famme.model.ProductOption

interface ProductRepository {
    fun findByExternalId(externalId: Long): Product?
    fun save(product: Product): Product
    fun update(product: Product): Product
    fun saveVariant(variant: ProductVariant): ProductVariant
    fun saveImage(image: ProductImage): ProductImage
    fun saveOption(option: ProductOption): ProductOption
    fun findAll(): List<Product>
    fun deleteByExternalId(externalId: Long)
    fun searchProductsByTitle(query: String): List<Product>
}
