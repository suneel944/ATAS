-- H2-compatible schema for unit tests
-- This file creates all tables needed for unit tests with H2-compatible syntax

CREATE TABLE IF NOT EXISTS test_executions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(255) UNIQUE NOT NULL,
    suite_name VARCHAR(255),
    status VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    environment VARCHAR(255),
    video_url VARCHAR(1024),
    stdout_output TEXT,
    stderr_output TEXT,
    output_complete BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS test_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT,
    test_id VARCHAR(512),
    test_name VARCHAR(512),
    description TEXT,
    status VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    tags VARCHAR(1000),
    priority VARCHAR(50),
    framework VARCHAR(100),
    environment_details VARCHAR(1000),
    owner VARCHAR(255),
    FOREIGN KEY (execution_id) REFERENCES test_executions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS test_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT,
    step_number INTEGER,
    action VARCHAR(255),
    description TEXT,
    status VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    data VARCHAR(1000),
    FOREIGN KEY (result_id) REFERENCES test_results(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS test_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT,
    step_id BIGINT,
    type VARCHAR(20),
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    url VARCHAR(1024),
    description TEXT,
    created_at TIMESTAMP,
    FOREIGN KEY (result_id) REFERENCES test_results(id) ON DELETE CASCADE,
    FOREIGN KEY (step_id) REFERENCES test_steps(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS test_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT,
    metric_key VARCHAR(255),
    metric_value VARCHAR(255),
    FOREIGN KEY (result_id) REFERENCES test_results(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS test_assertions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT,
    type VARCHAR(100),
    expect_value TEXT,
    actual_value TEXT,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (result_id) REFERENCES test_results(id) ON DELETE CASCADE
);
