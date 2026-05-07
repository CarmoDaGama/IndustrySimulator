-- COMPONENT SERVICE - component_db
-- ============================================================================


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

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO industry_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO industry_user;
