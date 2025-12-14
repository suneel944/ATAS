-- Performance optimization indexes for dashboard and reporting queries

-- Indexes for test_executions table
CREATE INDEX IF NOT EXISTS idx_test_executions_start_time ON test_executions(start_time DESC);
CREATE INDEX IF NOT EXISTS idx_test_executions_env_start ON test_executions(environment, start_time DESC);
CREATE INDEX IF NOT EXISTS idx_test_executions_execution_id ON test_executions(execution_id);

-- Indexes for test_results table
CREATE INDEX IF NOT EXISTS idx_test_results_status ON test_results(status);
CREATE INDEX IF NOT EXISTS idx_test_results_execution_id_status ON test_results(execution_id, status);
CREATE INDEX IF NOT EXISTS idx_test_results_start_time ON test_results(start_time DESC);

-- Composite index for common dashboard queries (execution_id + status + start_time)
CREATE INDEX IF NOT EXISTS idx_test_results_exec_status_time ON test_results(execution_id, status, start_time DESC);

