-- Materialized view for dashboard metrics optimization
-- This pre-computes aggregated statistics for faster dashboard loading

CREATE MATERIALIZED VIEW IF NOT EXISTS dashboard_metrics AS
SELECT 
    COUNT(DISTINCT e.id) as total_executions,
    COUNT(r.id) as total_tests,
    COUNT(CASE WHEN r.status = 'PASSED' THEN 1 END) as passed_tests,
    COUNT(CASE WHEN r.status = 'FAILED' THEN 1 END) as failed_tests,
    COUNT(CASE WHEN r.status = 'ERROR' THEN 1 END) as error_tests,
    COUNT(CASE WHEN r.status = 'SKIPPED' THEN 1 END) as skipped_tests,
    COUNT(CASE WHEN r.status = 'RUNNING' THEN 1 END) as running_tests,
    COUNT(CASE WHEN e.status = 'RUNNING' THEN 1 END) as active_executions,
    MAX(e.start_time) as last_execution_time,
    AVG(EXTRACT(EPOCH FROM (e.end_time - e.start_time))) as avg_execution_duration_seconds
FROM test_executions e
LEFT JOIN test_results r ON r.execution_id = e.id
WHERE e.end_time IS NOT NULL OR e.status = 'RUNNING';

-- Create index on materialized view for faster queries
CREATE INDEX IF NOT EXISTS idx_dashboard_metrics_last_execution ON dashboard_metrics (last_execution_time);

-- Function to refresh the materialized view
CREATE OR REPLACE FUNCTION refresh_dashboard_metrics()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY dashboard_metrics;
END;
$$ LANGUAGE plpgsql;

-- Create a function to get dashboard metrics with date range
CREATE OR REPLACE FUNCTION get_dashboard_metrics_since(start_date TIMESTAMP)
RETURNS TABLE (
    total_executions BIGINT,
    total_tests BIGINT,
    passed_tests BIGINT,
    failed_tests BIGINT,
    error_tests BIGINT,
    skipped_tests BIGINT,
    running_tests BIGINT,
    active_executions BIGINT,
    last_execution_time TIMESTAMP,
    avg_execution_duration_seconds NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(DISTINCT e.id)::BIGINT as total_executions,
        COUNT(r.id)::BIGINT as total_tests,
        COUNT(CASE WHEN r.status = 'PASSED' THEN 1 END)::BIGINT as passed_tests,
        COUNT(CASE WHEN r.status = 'FAILED' THEN 1 END)::BIGINT as failed_tests,
        COUNT(CASE WHEN r.status = 'ERROR' THEN 1 END)::BIGINT as error_tests,
        COUNT(CASE WHEN r.status = 'SKIPPED' THEN 1 END)::BIGINT as skipped_tests,
        COUNT(CASE WHEN r.status = 'RUNNING' THEN 1 END)::BIGINT as running_tests,
        COUNT(CASE WHEN e.status = 'RUNNING' THEN 1 END)::BIGINT as active_executions,
        MAX(e.start_time) as last_execution_time,
        AVG(EXTRACT(EPOCH FROM (e.end_time - e.start_time))) as avg_execution_duration_seconds
    FROM test_executions e
    LEFT JOIN test_results r ON r.execution_id = e.id
    WHERE e.start_time >= start_date
        AND (e.end_time IS NOT NULL OR e.status = 'RUNNING');
END;
$$ LANGUAGE plpgsql;

