-- Increase column sizes for test_results table to accommodate longer test IDs and names
-- This prevents "value too long" errors that cause test results to not be saved

ALTER TABLE test_results 
ALTER COLUMN test_id TYPE VARCHAR(512),
ALTER COLUMN test_name TYPE VARCHAR(512);

-- Add comment explaining the change
COMMENT ON COLUMN test_results.test_id IS 'Unique test identifier (e.g. class.method or class#method[parameters]). Increased from VARCHAR(255) to VARCHAR(512) to support parameterized tests with long parameter values.';
COMMENT ON COLUMN test_results.test_name IS 'Human-friendly display name of the test. Increased from VARCHAR(255) to VARCHAR(512) to support longer test names.';
