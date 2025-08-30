package com.example.famme.controller

import com.example.famme.model.Product
import com.example.famme.service.ProductService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.time.LocalDateTime

@Controller
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    // REST API endpoints
    @GetMapping
    @ResponseBody
    fun getAllProducts(): ResponseEntity<List<Product>> {
        val products = productService.findAllProducts()
        return ResponseEntity.ok(products)
    }

    @GetMapping("/{externalId}")
    @ResponseBody
    fun getProductByExternalId(@PathVariable externalId: Long): ResponseEntity<Product> {
        val product = productService.findProductByExternalId(externalId)
        return if (product != null) {
            ResponseEntity.ok(product)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{externalId}")
    @ResponseBody
    fun deleteProductByExternalId(@PathVariable externalId: Long): ResponseEntity<String> {
        productService.deleteProductByExternalId(externalId)
        return ResponseEntity.ok("Product deleted")
    }

    @PostMapping("/sync")
    @ResponseBody
    fun syncProducts(): ResponseEntity<String> {
        productService.syncProductsFromExternalApi()
        return ResponseEntity.ok("Product sync started")
    }
}

@Controller
@RequestMapping("/products")
class ProductWebController(
    private val productService: ProductService
) {

    @GetMapping
    fun productsPage(model: Model): String {
        // initially empty, data loaded via HTMX
        return "products"
    }

    @GetMapping("/search-page")
    fun searchPage(model: Model): String {
        return "product-search"
    }

    @GetMapping("/load")
    fun loadProducts(model: Model): String {
        val products = productService.findAllProducts()
        model.addAttribute("products", products)
        return "fragments/product-table :: table"  // returns the table fragment
    }

    @GetMapping("/search")
    fun searchProducts(@RequestParam("query", defaultValue = "") query: String, model: Model): String {
        val products = if (query.isBlank()) {
            productService.findAllProducts()
        } else {
            productService.searchProducts(query)
        }
        model.addAttribute("products", products)
        model.addAttribute("searchQuery", query)
        return "fragments/product-table :: table"
    }

    @PostMapping("/add")
    fun addProduct(
        @RequestParam title: String,
        @RequestParam vendor: String?,
        @RequestParam productType: String?,
        @RequestParam handle: String?,
        @RequestParam bodyHtml: String?,
        @RequestParam tags: String?,
        model: Model
    ): String {
        return try {
            // Generate a unique external ID (you might want to use a different strategy)
            val externalId = System.currentTimeMillis()
            
            // Parse tags from comma-separated string
            val tagList = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            
            // Create new product
            val newProduct = Product(
                externalId = externalId,
                title = title,
                vendor = vendor,
                productType = productType,
                handle = handle,
                bodyHtml = bodyHtml,
                tags = tagList,
                createdAt = Timestamp.valueOf(LocalDateTime.now()),
                updatedAt = Timestamp.valueOf(LocalDateTime.now())
            )
            
            // Save the product
            productService.saveProduct(newProduct)
            
            // Return success message
            model.addAttribute("message", "Product '$title' added successfully!")
            model.addAttribute("messageType", "success")
            "fragments/form-status :: status"
            
        } catch (e: Exception) {
            // Return error message
            model.addAttribute("message", "Error adding product: ${e.message}")
            model.addAttribute("messageType", "error")
            "fragments/form-status :: status"
        }
    }

}
