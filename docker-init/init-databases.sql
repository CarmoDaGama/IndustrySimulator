-- ============================================================================
-- INDUSTRY SIMULATOR - Single Database Server Initialization
-- ============================================================================
-- Requisito do enunciado: "apenas um servidor de Base de Dados global",
-- com uma base de dados isolada por microserviço dentro desse mesmo servidor.
-- As tabelas de cada base são criadas automaticamente pelo Hibernate
-- (spring.jpa.hibernate.ddl-auto=update) quando cada serviço arranca.
-- ============================================================================

CREATE DATABASE raw_material_db OWNER industry_user;
CREATE DATABASE processing_db OWNER industry_user;
CREATE DATABASE component_db OWNER industry_user;
CREATE DATABASE assembly_db OWNER industry_user;

GRANT ALL PRIVILEGES ON DATABASE raw_material_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE processing_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE component_db TO industry_user;
GRANT ALL PRIVILEGES ON DATABASE assembly_db TO industry_user;
