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