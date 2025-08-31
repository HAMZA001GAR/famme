package com.example.famme.repository.impl

import com.example.famme.model.Product
import com.example.famme.model.ProductVariant
import com.example.famme.model.ProductImage
import com.example.famme.model.ProductOption
import com.example.famme.repository.ProductRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository


@Repository
class ProductRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate
) : ProductRepository {

    override fun findByExternalId(externalId: Long): Product? {
        val sql = """
            SELECT id, external_id, title, handle, body_html, vendor, product_type, 
                   published_at, created_at, updated_at, tags
            FROM products 
            WHERE external_id = ?
        """.trimIndent()
        
        val products = jdbcTemplate.query(sql, { rs, _ ->
            Product(
                id = rs.getInt("id"),
                externalId = rs.getLong("external_id"),
                title = rs.getString("title"),
                handle = rs.getString("handle"),
                bodyHtml = rs.getString("body_html"),
                vendor = rs.getString("vendor"),
                productType = rs.getString("product_type"),
                publishedAt = rs.getTimestamp("published_at"),
                createdAt = rs.getTimestamp("created_at"),
                updatedAt = rs.getTimestamp("updated_at"),
                tags = if (rs.getArray("tags") != null) {
                    (rs.getArray("tags").array as Array<String>).toList()
                } else {
                    emptyList()
                }
            )
        }, externalId)
        
        return if (products.isNotEmpty()) products[0] else null
    }

    override fun save(product: Product): Product {
        val keyHolder = GeneratedKeyHolder()
        val sql = """
            INSERT INTO products (external_id, title, handle, body_html, vendor, product_type, 
                                published_at, created_at, updated_at, tags)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        
        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            ps.setLong(1, product.externalId)
            ps.setString(2, product.title)
            ps.setString(3, product.handle)
            ps.setString(4, product.bodyHtml)
            ps.setString(5, product.vendor)
            ps.setString(6, product.productType)
            ps.setTimestamp(7, product.publishedAt)
            ps.setTimestamp(8, product.createdAt)
            ps.setTimestamp(9, product.updatedAt)
            ps.setArray(10, connection.createArrayOf("text", product.tags.toTypedArray()))
            ps
        }, keyHolder)
        
        val generatedId = keyHolder.keys?.get("id") as Int
        return product.copy(id = generatedId)
    }

    override fun update(product: Product): Product {
        val sql = """
            UPDATE products 
            SET title = ?, handle = ?, body_html = ?, vendor = ?, product_type = ?, 
                published_at = ?, created_at = ?, updated_at = ?, tags = ?
            WHERE external_id = ?
        """.trimIndent()
        
        jdbcTemplate.update(sql,
            product.title, product.handle, product.bodyHtml, product.vendor, product.productType,
            product.publishedAt, product.createdAt, product.updatedAt, 
            product.tags.toTypedArray(), product.externalId
        )
        
        return product
    }

    override fun saveVariant(variant: ProductVariant): ProductVariant {
        val keyHolder = GeneratedKeyHolder()
        val sql = """
            INSERT INTO product_variants (external_id, product_id, title, option1, option2, option3, 
                                        sku, price, available, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (external_id) DO UPDATE
            SET title = EXCLUDED.title, option1 = EXCLUDED.option1, option2 = EXCLUDED.option2,
                option3 = EXCLUDED.option3, sku = EXCLUDED.sku, price = EXCLUDED.price,
                available = EXCLUDED.available, created_at = EXCLUDED.created_at, 
                updated_at = EXCLUDED.updated_at
        """.trimIndent()
        
        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            ps.setLong(1, variant.externalId)
            ps.setInt(2, variant.productId)
            ps.setString(3, variant.title)
            ps.setString(4, variant.option1)
            ps.setString(5, variant.option2)
            ps.setString(6, variant.option3)
            ps.setString(7, variant.sku)
            ps.setBigDecimal(8, variant.price)
            ps.setBoolean(9, variant.available)
            ps.setTimestamp(10, variant.createdAt)
            ps.setTimestamp(11, variant.updatedAt)
            ps
        }, keyHolder)
        
        val generatedId = keyHolder.keys?.get("id") as Int
        return variant.copy(id = generatedId)
    }

    override fun saveImage(image: ProductImage): ProductImage {
        val keyHolder = GeneratedKeyHolder()
        val sql = """
            INSERT INTO product_images (external_id, product_id, src, width, height, position, 
                                      created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (external_id) DO UPDATE
            SET src = EXCLUDED.src, width = EXCLUDED.width, height = EXCLUDED.height,
                position = EXCLUDED.position, created_at = EXCLUDED.created_at, 
                updated_at = EXCLUDED.updated_at
        """.trimIndent()
        
        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            ps.setLong(1, image.externalId)
            ps.setInt(2, image.productId)
            ps.setString(3, image.src)
            ps.setInt(4, image.width ?: 0)
            ps.setInt(5, image.height ?: 0)
            ps.setInt(6, image.position ?: 0)
            ps.setTimestamp(7, image.createdAt)
            ps.setTimestamp(8, image.updatedAt)
            ps
        }, keyHolder)
        
        val generatedId = keyHolder.keys?.get("id") as Int
        return image.copy(id = generatedId)
    }

    override fun saveOption(option: ProductOption): ProductOption {
        val keyHolder = GeneratedKeyHolder()
        val sql = """
            INSERT INTO product_options (product_id, name, position, values)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (product_id, name) DO UPDATE
            SET position = EXCLUDED.position, values = EXCLUDED.values
        """.trimIndent()
        
        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            ps.setInt(1, option.productId)
            ps.setString(2, option.name)
            ps.setInt(3, option.position)
            ps.setString(4, option.values)
            ps
        }, keyHolder)
        
        val generatedId = keyHolder.keys?.get("id") as Int
        return option.copy(id = generatedId)
    }

    override fun findAll(): List<Product> {
        val sql = """
            SELECT id, external_id, title, handle, body_html, vendor, product_type, 
                   published_at, created_at, updated_at, tags
            FROM products
        """.trimIndent()
        
        return jdbcTemplate.query(sql) { rs, _ ->
            Product(
                id = rs.getInt("id"),
                externalId = rs.getLong("external_id"),
                title = rs.getString("title"),
                handle = rs.getString("handle"),
                bodyHtml = rs.getString("body_html"),
                vendor = rs.getString("vendor"),
                productType = rs.getString("product_type"),
                publishedAt = rs.getTimestamp("published_at"),
                createdAt = rs.getTimestamp("created_at"),
                updatedAt = rs.getTimestamp("updated_at"),
                tags = if (rs.getArray("tags") != null) {
                    (rs.getArray("tags").array as Array<String>).toList()
                } else {
                    emptyList()
                }
            )
        }
    }

    override fun deleteByExternalId(externalId: Long) {
        val sql = "DELETE FROM products WHERE external_id = ?"
        jdbcTemplate.update(sql, externalId)
    }

    override fun searchProductsByTitle(query: String): List<Product> {
        val sql = """
            SELECT id, external_id, title, handle, body_html, vendor, product_type, 
                   published_at, created_at, updated_at, tags
            FROM products 
            WHERE LOWER(title) LIKE ? 
               OR LOWER(vendor) LIKE ? 
               OR LOWER(product_type) LIKE ?
               OR EXISTS (
                   SELECT 1 FROM unnest(tags) AS tag 
                   WHERE LOWER(tag) LIKE ?
               )
            ORDER BY 
                CASE 
                    WHEN LOWER(title) LIKE ? THEN 1
                    WHEN LOWER(vendor) LIKE ? THEN 2
                    WHEN LOWER(product_type) LIKE ? THEN 3
                    ELSE 4
                END,
                title ASC
        """.trimIndent()

        val searchPattern = "%${query.lowercase()}%"
        val exactTitleMatch = "${query.lowercase()}%"
        val exactVendorMatch = "${query.lowercase()}%"
        val exactTypeMatch = "${query.lowercase()}%"

        return jdbcTemplate.query(sql, arrayOf(
            searchPattern, searchPattern, searchPattern, searchPattern,
            exactTitleMatch, exactVendorMatch, exactTypeMatch
        )) { rs, _ ->
            Product(
                id = rs.getInt("id"),
                externalId = rs.getLong("external_id"),
                title = rs.getString("title"),
                handle = rs.getString("handle"),
                bodyHtml = rs.getString("body_html"),
                vendor = rs.getString("vendor"),
                productType = rs.getString("product_type"),
                publishedAt = rs.getTimestamp("published_at"),
                createdAt = rs.getTimestamp("created_at"),
                updatedAt = rs.getTimestamp("updated_at"),
                tags = if (rs.getArray("tags") != null) {
                    (rs.getArray("tags").array as Array<String>).toList()
                } else {
                    emptyList()
                }
            )
        }
    }

}
