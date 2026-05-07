-- PROCESSING SERVICE - processing_db
-- ============================================================================


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

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO industry_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO industry_user;
