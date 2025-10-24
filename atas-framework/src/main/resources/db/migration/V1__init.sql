-- Initial schema for ATAS framework
CREATE TABLE IF NOT EXISTS test_executions (
    id SERIAL PRIMARY KEY,
    execution_id VARCHAR(255) UNIQUE NOT NULL,
    suite_name VARCHAR(255),
    status VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    environment VARCHAR(255),
    video_url VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS test_results (
    id SERIAL PRIMARY KEY,
    execution_id INTEGER REFERENCES test_executions(id) ON DELETE CASCADE,
    test_id VARCHAR(255),
    test_name VARCHAR(255),
    status VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_steps (
    id SERIAL PRIMARY KEY,
    result_id INTEGER REFERENCES test_results(id) ON DELETE CASCADE,
    description TEXT,
    status VARCHAR(20),
    start_time TIMESTAMP,
    end_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_attachments (
    id SERIAL PRIMARY KEY,
    result_id INTEGER REFERENCES test_results(id) ON DELETE CASCADE,
    step_id INTEGER REFERENCES test_steps(id) ON DELETE CASCADE,
    type VARCHAR(20),
    file_name VARCHAR(255),
    mime_type VARCHAR(100),
    url VARCHAR(1024),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_metrics (
    id SERIAL PRIMARY KEY,
    result_id INTEGER REFERENCES test_results(id) ON DELETE CASCADE,
    metric_key VARCHAR(255),
    metric_value VARCHAR(255)
);