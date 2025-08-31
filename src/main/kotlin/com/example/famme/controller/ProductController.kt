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

    @PostMapping("/sync")
    @ResponseBody
    fun syncProducts(): ResponseEntity<String> {
        productService.syncProductsFromExternalApi()
        return ResponseEntity.ok("Product sync started")
    }

    @DeleteMapping("/{externalId}")
    @ResponseBody
    fun deleteProduct(@PathVariable externalId: Long): ResponseEntity<String> {
        return try {
            productService.deleteProductByExternalId(externalId)
            ResponseEntity.ok("Product deleted successfully")
        } catch (e: IllegalArgumentException) {
//            ResponseEntity.notFound().body("Product not found: ${e.message}")
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error deleting product: ${e.message}")
        }
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
            
            // Parse tags from comma-separated string, removing any brackets
            val cleanTags = tags?.replace("[", "")?.replace("]", "") ?: ""
            val tagList = cleanTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
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

    @GetMapping("/update/{externalId}")
    fun updateProductPage(@PathVariable externalId: Long, model: Model): String {
        val product = productService.findProductByExternalId(externalId)
        return if (product != null) {
            model.addAttribute("product", product)
            // Format tags as comma-separated string for the form
            val tagsString = product.tags?.joinToString(", ") ?: ""
            model.addAttribute("tagsString", tagsString)
            "product-update"
        } else {
            model.addAttribute("error", "Product not found")
            "redirect:/products"
        }
    }

    @PostMapping("/update/{externalId}")
    fun updateProduct(
        @PathVariable externalId: Long,
        @RequestParam title: String,
        @RequestParam vendor: String?,
        @RequestParam productType: String?,
        @RequestParam handle: String?,
        @RequestParam bodyHtml: String?,
        @RequestParam tags: String?,
        model: Model
    ): String {
        return try {
            val existingProduct = productService.findProductByExternalId(externalId)
            if (existingProduct == null) {
                model.addAttribute("error", "Product not found")
                return "redirect:/products"
            }
            
            // Parse tags from comma-separated string, removing any brackets
            val cleanTags = tags?.replace("[", "")?.replace("]", "") ?: ""
            val tagList = cleanTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            // Create updated product
            val updatedProduct = existingProduct.copy(
                title = title,
                vendor = vendor,
                productType = productType,
                handle = handle,
                bodyHtml = bodyHtml,
                tags = tagList
            )
            
            // Update the product
            val result = productService.updateProduct(externalId, updatedProduct)
            
            if (result != null) {
                // Get the updated product to show in the form
                val updatedProductForForm = productService.findProductByExternalId(externalId)
                model.addAttribute("product", updatedProductForForm)
                // Format tags as comma-separated string for the form
                val tagsString = updatedProductForForm?.tags?.joinToString(", ") ?: ""
                model.addAttribute("tagsString", tagsString)
                model.addAttribute("message", "Product '$title' updated successfully!")
                model.addAttribute("messageType", "success")
                return "product-update"
            } else {
                model.addAttribute("product", existingProduct)
                // Format tags as comma-separated string for the form
                val tagsString = existingProduct.tags?.joinToString(", ") ?: ""
                model.addAttribute("tagsString", tagsString)
                model.addAttribute("message", "Failed to update product")
                model.addAttribute("messageType", "error")
                return "product-update"
            }
            
        } catch (e: Exception) {
            // Return error message
            val existingProduct = productService.findProductByExternalId(externalId)
            model.addAttribute("product", existingProduct)
            // Format tags as comma-separated string for the form
            val tagsString = existingProduct?.tags?.joinToString(", ") ?: ""
            model.addAttribute("tagsString", tagsString)
            model.addAttribute("message", "Error updating product: ${e.message}")
            model.addAttribute("messageType", "error")
            return "product-update"
        }
    }

    @DeleteMapping("delete/{externalId}")
    @ResponseBody
    fun deleteProductByExternalId(@PathVariable externalId: Long): ResponseEntity<String> {
        productService.deleteProductByExternalId(externalId)
        return ResponseEntity.ok("Product deleted")
    }

}
