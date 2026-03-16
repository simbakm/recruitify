-- src/main/resources/schema.sql
CREATE TABLE IF NOT EXISTS candidate_profiles (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(50),
    address TEXT,
    objectives TEXT,
    experiences TEXT,
    educations TEXT,
    skills TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_candidate_profiles_email ON candidate_profiles(email);

-- create company query
-- Create the main company table
CREATE TABLE IF NOT EXISTS companies (
                           id SERIAL PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           logo_url VARCHAR(500),
                           industry VARCHAR(50) NOT NULL,
                           size VARCHAR(50) NOT NULL,
                           website VARCHAR(500),
                           location VARCHAR(255) NOT NULL,
                           description TEXT,
                           culture TEXT,
                           social_links TEXT,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create benefits table
CREATE TABLE IF NOT EXISTS company_benefits (
                                  id SERIAL PRIMARY KEY,
                                  company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
                                  benefit VARCHAR(255) NOT NULL,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_companies_name ON companies(name);
CREATE INDEX IF NOT EXISTS idx_companies_industry ON companies(industry);
CREATE INDEX IF NOT EXISTS idx_companies_size ON companies(size);
CREATE INDEX IF NOT EXISTS idx_company_benefits_company_id ON company_benefits(company_id);
