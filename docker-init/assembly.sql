-- ASSEMBLY SERVICE - assembly_db
-- ============================================================================


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

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO industry_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO industry_user;
