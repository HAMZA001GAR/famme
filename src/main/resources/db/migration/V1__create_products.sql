-- =========================================
-- V1__create_products.sql
-- Flyway migration: Create products, variants, images, and options tables
-- =========================================

-- PRODUCTS TABLE
CREATE TABLE products (
                          id SERIAL PRIMARY KEY,
                          external_id BIGINT UNIQUE NOT NULL,
                          title TEXT NOT NULL,
                          handle TEXT,
                          body_html TEXT,
                          vendor TEXT,
                          product_type TEXT,
                          published_at TIMESTAMP,
                          created_at TIMESTAMP,
                          updated_at TIMESTAMP,
                          tags TEXT[]
);

-- PRODUCT VARIANTS TABLE
CREATE TABLE product_variants (
                                  id SERIAL PRIMARY KEY,
                                  product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                  external_id BIGINT UNIQUE NOT NULL,
                                  title TEXT,
                                  option1 TEXT,
                                  option2 TEXT,
                                  option3 TEXT,
                                  sku TEXT,
                                  price NUMERIC(10,2),
                                  available BOOLEAN,
                                  created_at TIMESTAMP,
                                  updated_at TIMESTAMP
);

-- PRODUCT IMAGES TABLE
CREATE TABLE product_images (
                                id SERIAL PRIMARY KEY,
                                external_id BIGINT UNIQUE NOT NULL,
                                product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                src TEXT,
                                width INT,
                                height INT,
                                position INT,
                                created_at TIMESTAMP,
                                updated_at TIMESTAMP
);

-- PRODUCT OPTIONS TABLE
CREATE TABLE product_options (
                                 id SERIAL PRIMARY KEY,
                                 product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                                 name TEXT NOT NULL,
                                 position INT,
                                 values TEXT
);

-- Add unique constraint to prevent duplicate options for the same product
ALTER TABLE product_options ADD CONSTRAINT unique_product_option UNIQUE (product_id, name);