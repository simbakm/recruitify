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

-- Create vacancies table
CREATE TABLE IF NOT EXISTS vacancies (
                          id BIGSERIAL PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          category VARCHAR(255),
                          location VARCHAR(255),
                          employment_type VARCHAR(50),
                          salary_min INTEGER,
                          salary_max INTEGER,
                          salary_currency VARCHAR(10),
                          description TEXT,
                          requirements TEXT,
                          status VARCHAR(50),
                          posted_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          company_id INTEGER REFERENCES companies(id) ON DELETE SET NULL,
                          applicant_count INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_vacancies_company_id ON vacancies(company_id);
CREATE INDEX IF NOT EXISTS idx_vacancies_status ON vacancies(status);
CREATE INDEX IF NOT EXISTS idx_vacancies_category ON vacancies(category);

-- Create recruiters table
CREATE TABLE IF NOT EXISTS recruiters (
                           id BIGSERIAL PRIMARY KEY,
                           first_name VARCHAR(255) NOT NULL,
                           last_name VARCHAR(255) NOT NULL,
                           email VARCHAR(255) UNIQUE NOT NULL,
                           phone VARCHAR(50),
                           role VARCHAR(100) NOT NULL,
                           avatar_url VARCHAR(500),
                           company_id INTEGER REFERENCES companies(id) ON DELETE SET NULL,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           notification_preferences TEXT
);

CREATE INDEX IF NOT EXISTS idx_recruiters_company_id ON recruiters(company_id);
CREATE INDEX IF NOT EXISTS idx_recruiters_email ON recruiters(email);

-- Create interviews table
CREATE TABLE IF NOT EXISTS interviews (
                          id BIGSERIAL PRIMARY KEY,
                          application_id INTEGER,
                          candidate_id INTEGER,
                          candidate_name VARCHAR(255) NOT NULL,
                          candidate_avatar VARCHAR(500),
                          position VARCHAR(255) NOT NULL,
                          interview_date VARCHAR(50) NOT NULL,
                          interview_time VARCHAR(50) NOT NULL,
                          type VARCHAR(50) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          meeting_link VARCHAR(1000),
                          location VARCHAR(255),
                          notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_interviews_application_id ON interviews(application_id);
CREATE INDEX IF NOT EXISTS idx_interviews_candidate_id ON interviews(candidate_id);
CREATE INDEX IF NOT EXISTS idx_interviews_status ON interviews(status);

-- Create applications table
CREATE TABLE IF NOT EXISTS applications (
                            id BIGSERIAL PRIMARY KEY,
                            vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
                            candidate_id BIGINT,
                            candidate_name VARCHAR(255) NOT NULL,
                            candidate_avatar VARCHAR(500),
                            applied_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            status VARCHAR(50) NOT NULL,
                            resume_url VARCHAR(1000),
                            cover_letter TEXT,
                            position VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_applications_vacancy_id ON applications(vacancy_id);
CREATE INDEX IF NOT EXISTS idx_applications_candidate_id ON applications(candidate_id);
CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status);
