-- candidate table
CREATE TABLE candidates (
                            id SERIAL PRIMARY KEY,         -- use AUTO_INCREMENT for MySQL
                            full_name VARCHAR(255),
                            email VARCHAR(255),
                            phone VARCHAR(100),
                            total_experience_years REAL,
                            summary TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- skills table (many-to-many)
CREATE TABLE skills (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(150) UNIQUE
);

CREATE TABLE candidate_skills (
                                  candidate_id INTEGER REFERENCES candidates(id) ON DELETE CASCADE,
                                  skill_id INTEGER REFERENCES skills(id),
                                  proficiency INTEGER, -- optional: 1-10
                                  PRIMARY KEY (candidate_id, skill_id)
);

-- education table
CREATE TABLE education (
                           id SERIAL PRIMARY KEY,
                           candidate_id INTEGER REFERENCES candidates(id) ON DELETE CASCADE,
                           degree VARCHAR(255),
                           institution VARCHAR(255),
                           start_year INTEGER,
                           end_year INTEGER
);

-- experience table
CREATE TABLE experience (
                            id SERIAL PRIMARY KEY,
                            candidate_id INTEGER REFERENCES candidates(id) ON DELETE CASCADE,
                            title VARCHAR(255),
                            company VARCHAR(255),
                            start_year INTEGER,
                            end_year INTEGER,
                            description TEXT
);

CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_candidates_email ON candidates(email);
