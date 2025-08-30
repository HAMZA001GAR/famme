package com.example.famme.service

import com.example.famme.model.Product

interface ProductService {
    fun syncProductsFromExternalApi()
    fun findAllProducts(): List<Product>
    fun findProductByExternalId(externalId: Long): Product?
    fun deleteProductByExternalId(externalId: Long)
    fun saveProduct(product: Product): Product
    fun updateProduct(externalId: Long, updatedProduct: Product): Product?
    fun searchProducts(query: String): List<Product>
}
