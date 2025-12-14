-- Add detailed test result fields to support comprehensive JSON output
-- This migration adds fields for description, tags, priority, framework, environment details, owner, and assertions

-- Add new columns to test_results table
ALTER TABLE test_results 
ADD COLUMN description TEXT,
ADD COLUMN tags JSONB,
ADD COLUMN priority VARCHAR(50),
ADD COLUMN framework VARCHAR(100),
ADD COLUMN environment_details JSONB,
ADD COLUMN owner VARCHAR(255);

-- Add new columns to test_steps table
ALTER TABLE test_steps
ADD COLUMN step_number INTEGER,
ADD COLUMN action VARCHAR(255),
ADD COLUMN data JSONB;

-- Add description column to test_attachments table
ALTER TABLE test_attachments
ADD COLUMN description TEXT;

-- Create test_assertions table
CREATE TABLE test_assertions (
    id BIGSERIAL PRIMARY KEY,
    result_id BIGINT REFERENCES test_results(id) ON DELETE CASCADE,
    type VARCHAR(100),
    expect_value TEXT,
    actual_value TEXT,
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_test_results_tags ON test_results USING GIN(tags);
CREATE INDEX idx_test_results_priority ON test_results(priority);
CREATE INDEX idx_test_results_owner ON test_results(owner);
CREATE INDEX idx_test_steps_step_number ON test_steps(step_number);
CREATE INDEX idx_test_assertions_result_id ON test_assertions(result_id);
CREATE INDEX idx_test_assertions_status ON test_assertions(status);

-- Add comments for documentation
COMMENT ON COLUMN test_results.description IS 'Detailed description of what the test verifies';
COMMENT ON COLUMN test_results.tags IS 'Array of tags associated with the test (e.g., API, REGRESSION, PAYMENTS)';
COMMENT ON COLUMN test_results.priority IS 'Test priority level (e.g., P0_CRITICAL, P1_HIGH, P2_MEDIUM, P3_LOW)';
COMMENT ON COLUMN test_results.framework IS 'Testing framework used (e.g., JUnit, Playwright, TestNG)';
COMMENT ON COLUMN test_results.environment_details IS 'JSON object containing environment-specific details (env, version, region, browser, viewport, OS, headless)';
COMMENT ON COLUMN test_results.owner IS 'Team or individual responsible for the test';
COMMENT ON COLUMN test_steps.step_number IS 'Sequential step number within the test';
COMMENT ON COLUMN test_steps.action IS 'Action performed in this step (e.g., Navigate, Fill, Click, POST /api/v1/tables/links)';
COMMENT ON COLUMN test_steps.data IS 'JSON object containing step-specific data (requestPayload, responseCode, selector, value, target, etc.)';
COMMENT ON COLUMN test_attachments.description IS 'Description or metadata for the attachment';
COMMENT ON TABLE test_assertions IS 'Stores individual assertions made during test execution';
