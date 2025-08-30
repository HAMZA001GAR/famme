package com.example.famme.service.impl

import com.example.famme.model.*
import com.example.famme.repository.ProductRepository
import com.example.famme.service.ProductService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val restTemplate: RestTemplate,
    private val jsonMapper: ObjectMapper
) : ProductService {

    private val logger = LoggerFactory.getLogger(ProductServiceImpl::class.java)
    @Value("\${famme.api.products-url}")
    private lateinit var productsUrl: String
    // private val productsUrl = "https://famme.no/products.json"

    // Helper function to safely parse timestamps
    private fun parseTimestamp(timestampStr: String?): Timestamp? {
        if (timestampStr.isNullOrBlank()) return null
        
        return try {
            // Try ISO format first (e.g., "2023-12-01T10:30:00Z")
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            val localDateTime = LocalDateTime.parse(timestampStr, formatter)
            Timestamp.valueOf(localDateTime)
        } catch (_: DateTimeParseException) {
            try {
                // Fallback to the original logic
                val cleaned = timestampStr.replace("T", " ").substringBefore("+")
                Timestamp.valueOf(cleaned)
            } catch (e2: Exception) {
                logger.warn("Failed to parse timestamp: $timestampStr", e2)
                null
            }
        }
    }

    @Scheduled(initialDelay = 0, fixedDelay = 24 * 60 * 60 * 1000) // after each start Or on every 24h
    override fun syncProductsFromExternalApi() {
        try {
            logger.info("Fetching products from JSON...")
            val response: String = restTemplate.getForObject(productsUrl, String::class.java) ?: run {
                logger.warn("Empty response from $productsUrl")
                return
            }

            val rootNode: JsonNode = jsonMapper.readTree(response)
            val productsNode = rootNode["products"] ?: run {
                logger.warn("No 'products' field in JSON response")
                return
            }

            productsNode.take(50).forEach { productNode ->
                try {
                    upsertProduct(productNode)
                } catch (ex: Exception) {
                    logger.error("Failed to upsert product id ${productNode["id"].asLong()}", ex)
                }
            }

            logger.info("Product sync completed successfully.")
        } catch (ex: Exception) {
            logger.error("Failed to fetch or parse products JSON", ex)
        }
    }

    override fun findAllProducts(): List<Product> {
        return productRepository.findAll()
    }

    override fun findProductByExternalId(externalId: Long): Product? {
        return productRepository.findByExternalId(externalId)
    }

    override fun deleteProductByExternalId(externalId: Long) {
        productRepository.deleteByExternalId(externalId)
    }

    override fun saveProduct(product: Product): Product {
        return productRepository.save(product)
    }

    override fun updateProduct(externalId: Long, updatedProduct: Product): Product? {
        val existingProduct = productRepository.findByExternalId(externalId)
        return if (existingProduct != null) {
            val productToUpdate = updatedProduct.copy(
                id = existingProduct.id,
                externalId = externalId,
                updatedAt = Timestamp.valueOf(LocalDateTime.now())
            )
            productRepository.update(productToUpdate)
        } else {
            null
        }
    }

    private fun upsertProduct(productNode: JsonNode) {
        val externalId = productNode["id"].asLong()
        val title = productNode["title"].asText()
        val handle = productNode["handle"]?.asText()
        val bodyHtml = productNode["body_html"]?.asText()
        val vendor = productNode["vendor"]?.asText()
        val productType = productNode["product_type"]?.asText()
        val publishedAt = parseTimestamp(productNode["published_at"]?.asText())
        val createdAt = parseTimestamp(productNode["created_at"]?.asText())
        val updatedAt = parseTimestamp(productNode["updated_at"]?.asText())
        val tags = productNode["tags"]?.map { it.asText() } ?: emptyList()

        try {
            // Check if product exists
            val existingProduct = productRepository.findByExternalId(externalId)
            
            val product = if (existingProduct != null) {
                // Update existing product
                val updatedProduct = existingProduct.copy(
                    title = title,
                    handle = handle,
                    bodyHtml = bodyHtml,
                    vendor = vendor,
                    productType = productType,
                    publishedAt = publishedAt,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    tags = tags
                )
                productRepository.update(updatedProduct)
            } else {
                // Create new product
                val newProduct = Product(
                    externalId = externalId,
                    title = title,
                    handle = handle,
                    bodyHtml = bodyHtml,
                    vendor = vendor,
                    productType = productType,
                    publishedAt = publishedAt,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    tags = tags
                )
                productRepository.save(newProduct)
            }

            // Upsert related data
            upsertVariants(productNode, product.id!!)
            upsertImages(productNode, product.id)
            upsertOptions(productNode, product.id)

        } catch (ex: Exception) {
            logger.error("Failed to upsert product $externalId", ex)
        }
    }

    private fun upsertVariants(productNode: JsonNode, productId: Int) {
        productNode["variants"]?.forEach { variantNode ->
            try {
                val variant = ProductVariant(
                    externalId = variantNode["id"].asLong(),
                    productId = productId,
                    title = variantNode["title"]?.asText(),
                    option1 = variantNode["option1"]?.asText(),
                    option2 = variantNode["option2"]?.asText(),
                    option3 = variantNode["option3"]?.asText(),
                    sku = variantNode["sku"]?.asText(),
                    price = variantNode["price"]?.asText()?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    available = variantNode["available"]?.asBoolean() ?: false,
                    createdAt = parseTimestamp(variantNode["created_at"]?.asText()),
                    updatedAt = parseTimestamp(variantNode["updated_at"]?.asText())
                )
                productRepository.saveVariant(variant)
            } catch (ex: Exception) {
                logger.error("Failed to upsert variant ${variantNode["id"].asLong()} for product $productId", ex)
            }
        }
    }

    private fun upsertImages(productNode: JsonNode, productId: Int) {
        productNode["images"]?.forEach { imageNode ->
            try {
                val image = ProductImage(
                    externalId = imageNode["id"].asLong(),
                    productId = productId,
                    src = imageNode["src"]?.asText(),
                    width = imageNode["width"]?.asInt(),
                    height = imageNode["height"]?.asInt(),
                    position = imageNode["position"]?.asInt(),
                    createdAt = parseTimestamp(imageNode["created_at"]?.asText()),
                    updatedAt = parseTimestamp(imageNode["updated_at"]?.asText())
                )
                productRepository.saveImage(image)
            } catch (ex: Exception) {
                logger.error("Failed to upsert image ${imageNode["id"].asLong()} for product $productId", ex)
            }
        }
    }

    private fun upsertOptions(productNode: JsonNode, productId: Int) {
        productNode["options"]?.forEach { optionNode ->
            try {
                val option = ProductOption(
                    productId = productId,
                    name = optionNode["name"].asText(),
                    position = optionNode["position"].asInt(),
                    values = optionNode["values"]?.joinToString(",") { it.asText() }
                )
                productRepository.saveOption(option)
            } catch (ex: Exception) {
                logger.error("Failed to upsert option ${optionNode["name"].asText()} for product $productId", ex)
            }
        }
    }
    override fun searchProducts(query: String): List<Product> {
        return productRepository.searchProductsByTitle(query)
    }
}