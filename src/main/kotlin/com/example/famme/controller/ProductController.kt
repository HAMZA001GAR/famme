package com.example.famme.controller

import com.example.famme.model.Product
import com.example.famme.service.ProductService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

    @Operation(summary = "Get all products")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully retrieved products")
    )
    @GetMapping
    @ResponseBody
    fun getAllProducts(): ResponseEntity<List<Product>> {
        val products = productService.findAllProducts()
        return ResponseEntity.ok(products)
    }

    @Operation(summary = "Get a product by externalID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Product found"),
        ApiResponse(responseCode = "400", description = "Product not found")
    )
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

    @Operation(summary = "Sync products from external API")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sync started")
    )
    @PostMapping("/sync")
    @ResponseBody
    fun syncProducts(): ResponseEntity<String> {
        productService.syncProductsFromExternalApi()
        return ResponseEntity.ok("Product sync started")
    }

    @Operation(summary = "Delete product by externalId")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Product deleted successfully"),
        ApiResponse(responseCode = "404", description = "Product not found")
    )
    @DeleteMapping("/{externalId}")
    @ResponseBody
    fun deleteProduct(@PathVariable externalId: Long): ResponseEntity<String> {
        return try {
            productService.deleteProductByExternalId(externalId)
            ResponseEntity.ok("Product deleted successfully")
        } catch (e: IllegalArgumentException) {
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

    @Operation(summary = "Load Products Page")
    @GetMapping
    fun productsPage(model: Model): String {
        // initially empty, data loaded via HTMX
        return "products"
    }

    @Operation(summary = "Load Search Page")
    @GetMapping("/search-page")
    fun searchPage(model: Model): String {
        return "product-search"
    }

    @Operation(summary = "Load all products (HTMX fragment)")
    @GetMapping("/load")
    fun loadProducts(model: Model): String {
        val products = productService.findAllProducts()
        model.addAttribute("products", products)
        return "fragments/product-table :: table"  // returns the table fragment
    }

    @Operation(summary = "Search products")
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

    @Operation(summary = "Add new product")
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

    @Operation(summary = "Update product")
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

    @Operation(summary = "Update product")
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
