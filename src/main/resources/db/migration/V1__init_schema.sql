-- Schema inicial (compatível MySQL/H2 MODE=MySQL)

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE surveys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL UNIQUE,
    ativo BOOLEAN NOT NULL,
    data_validade DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL
);

CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    texto VARCHAR(500) NOT NULL,
    ordem INT NOT NULL,
    survey_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_questions_survey FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    CONSTRAINT uq_question_order UNIQUE (survey_id, ordem)
);

CREATE TABLE options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    texto VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL,
    question_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NULL,
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE response_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    question_id BIGINT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    device_type VARCHAR(50),
    operating_system VARCHAR(100),
    browser VARCHAR(100),
    traffic_source VARCHAR(100),
    country VARCHAR(80),
    state VARCHAR(80),
    city VARCHAR(80),
    status VARCHAR(20),
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_sessions_survey FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    CONSTRAINT fk_sessions_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE SET NULL
);

CREATE TABLE votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at DATETIME NOT NULL,
    response_session_id BIGINT NULL UNIQUE,
    CONSTRAINT fk_votes_survey FOREIGN KEY (survey_id) REFERENCES surveys(id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_option FOREIGN KEY (option_id) REFERENCES options(id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_session FOREIGN KEY (response_session_id) REFERENCES response_sessions(id) ON DELETE SET NULL
);

CREATE INDEX idx_votes_survey ON votes (survey_id);
CREATE INDEX idx_votes_question ON votes (question_id);
CREATE INDEX idx_votes_option ON votes (option_id);
CREATE INDEX idx_sessions_created ON response_sessions (created_at);
