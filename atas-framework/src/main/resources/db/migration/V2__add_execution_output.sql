-- Add output columns to test_executions table
ALTER TABLE test_executions 
ADD COLUMN IF NOT EXISTS stdout_output TEXT,
ADD COLUMN IF NOT EXISTS stderr_output TEXT,
ADD COLUMN IF NOT EXISTS output_complete BOOLEAN DEFAULT FALSE;

-- Add index for faster lookups
CREATE INDEX IF NOT EXISTS idx_test_executions_status 
ON test_executions(status);

CREATE INDEX IF NOT EXISTS idx_test_executions_environment 
ON test_executions(environment);

