-- ============================================================================
-- INDUSTRY SIMULATOR - Database Initialization Script
-- ============================================================================
-- This script initializes databases for all microservices
-- Each service has its own isolated database with independent schemas

-- ============================================================================
-- RAW MATERIAL SERVICE - raw_material_db
-- ============================================================================
\c raw_material_db;

CREATE TABLE IF NOT EXISTS raw_material (
    id SERIAL PRIMARY KEY,
    material_type VARCHAR(100) NOT NULL,
    batch_id VARCHAR(50) NOT NULL UNIQUE,
    quantity DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'kg',
    supplier VARCHAR(100),
    quality_score DECIMAL(3, 2) CHECK (quality_score >= 0 AND quality_score <= 1),
    production_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_raw_material_batch_id ON raw_material(batch_id);
CREATE INDEX IF NOT EXISTS idx_raw_material_type ON raw_material(material_type);
CREATE INDEX IF NOT EXISTS idx_raw_material_status ON raw_material(status);

-- ============================================================================
-- PROCESSING SERVICE - processing_db
-- ============================================================================
\c processing_db;

CREATE TABLE IF NOT EXISTS pipeline_step (
    id SERIAL PRIMARY KEY,
    step_name VARCHAR(100) NOT NULL,
    step_order INT NOT NULL,
    duration_ms BIGINT NOT NULL DEFAULT 1000,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS processing_batch (
    id SERIAL PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL UNIQUE,
    raw_material_batch_id VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    current_step_order INT DEFAULT 0,
    total_steps INT DEFAULT 0,
    start_time TIMESTAMP WITH TIME ZONE,
    end_time TIMESTAMP WITH TIME ZONE,
    processed_quantity DECIMAL(10, 2),
    waste_quantity DECIMAL(10, 2) DEFAULT 0,
    quality_score DECIMAL(3, 2),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_processing_batch_id ON processing_batch(batch_id);
CREATE INDEX IF NOT EXISTS idx_processing_raw_material_id ON processing_batch(raw_material_batch_id);
CREATE INDEX IF NOT EXISTS idx_processing_status ON processing_batch(status);

-- ============================================================================
-- COMPONENT SERVICE - component_db
-- ============================================================================
\c component_db;

CREATE TABLE IF NOT EXISTS component (
    id SERIAL PRIMARY KEY,
    component_name VARCHAR(100) NOT NULL,
    component_type VARCHAR(100) NOT NULL,
    version VARCHAR(50),
    processing_batch_id VARCHAR(50),
    quantity DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'unit',
    compatibility_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    is_compatible BOOLEAN DEFAULT false,
    quality_score DECIMAL(3, 2),
    batch_id VARCHAR(50) NOT NULL UNIQUE,
    produced_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bom (
    id SERIAL PRIMARY KEY,
    bom_name VARCHAR(100) NOT NULL,
    bom_version VARCHAR(50),
    product_type VARCHAR(100),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bom_requirement (
    id SERIAL PRIMARY KEY,
    bom_id INT NOT NULL REFERENCES bom(id) ON DELETE CASCADE,
    component_type VARCHAR(100) NOT NULL,
    required_quantity DECIMAL(10, 2) NOT NULL,
    compatibility_rule VARCHAR(255),
    is_optional BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_component_batch_id ON component(batch_id);
CREATE INDEX IF NOT EXISTS idx_component_type ON component(component_type);
CREATE INDEX IF NOT EXISTS idx_component_status ON component(status);
CREATE INDEX IF NOT EXISTS idx_component_compatibility ON component(compatibility_status);
CREATE INDEX IF NOT EXISTS idx_bom_active ON bom(is_active);

-- ============================================================================
-- ASSEMBLY SERVICE - assembly_db
-- ============================================================================
\c assembly_db;

CREATE TABLE IF NOT EXISTS market_order (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE,
    product_type VARCHAR(100) NOT NULL,
    bom_version VARCHAR(50),
    requested_quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority INT DEFAULT 1,
    customer_name VARCHAR(100),
    order_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    required_delivery_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory (
    id SERIAL PRIMARY KEY,
    product_type VARCHAR(100) NOT NULL,
    component_type VARCHAR(100),
    batch_id VARCHAR(50),
    quantity DECIMAL(10, 2) NOT NULL DEFAULT 0,
    unit VARCHAR(20) NOT NULL DEFAULT 'unit',
    warehouse_location VARCHAR(100),
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    UNIQUE(product_type, component_type, batch_id)
);

CREATE TABLE IF NOT EXISTS assembled_product (
    id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL UNIQUE,
    order_id VARCHAR(50) NOT NULL REFERENCES market_order(order_id),
    bom_id VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ASSEMBLY_IN_PROGRESS',
    assembly_start_time TIMESTAMP WITH TIME ZONE,
    assembly_end_time TIMESTAMP WITH TIME ZONE,
    quality_check_passed BOOLEAN,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_market_order_id ON market_order(order_id);
CREATE INDEX IF NOT EXISTS idx_market_order_status ON market_order(status);
CREATE INDEX IF NOT EXISTS idx_market_order_product_type ON market_order(product_type);
CREATE INDEX IF NOT EXISTS idx_inventory_product_type ON inventory(product_type);
CREATE INDEX IF NOT EXISTS idx_inventory_status ON inventory(status);
CREATE INDEX IF NOT EXISTS idx_assembled_product_id ON assembled_product(product_id);
CREATE INDEX IF NOT EXISTS idx_assembled_product_order_id ON assembled_product(order_id);
CREATE INDEX IF NOT EXISTS idx_assembled_product_status ON assembled_product(status);

-- ============================================================================
-- Grant permissions to industry_user on all databases
-- ============================================================================
GRANT ALL PRIVILEGES ON DATABASE raw_material_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE processing_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE component_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE assembly_db TO industry_user;

-- ============================================================================
-- Initialization complete
-- ============================================================================
