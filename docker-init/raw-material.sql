-- RAW MATERIAL SERVICE - raw_material_db
-- ============================================================================


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

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO industry_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO industry_user;
