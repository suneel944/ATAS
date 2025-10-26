# ATAS Test Execution Guide

## Overview

This guide explains how to use the ATAS Test Execution APIs to trigger and monitor test executions. The ATAS framework supports multiple ways to execute tests and provides real-time monitoring capabilities.

## Quick Start

### 1. Start the ATAS Framework

```bash
make dev
```

This starts the ATAS framework and PostgreSQL database. The API will be available at `http://localhost:8080`.

### 1.5. Run Tests Directly (Recommended)

Before using the API, you can run tests directly using the Makefile commands:

```bash
# Quick unit tests (fastest, H2-based, no external dependencies)
make test-unit

# Integration tests (PostgreSQL with Testcontainers)
make test-integration

# Production tests (PostgreSQL-based, production-like environment)
make test-production

# All test types in sequence
make test-by-type

# Traditional test categories
make test-ui    # UI tests only
make test-api   # API tests only
make test       # All tests
```

**Test Type Comparison:**
- **Unit Tests**: Fastest execution, H2 database, perfect for development feedback
- **Integration Tests**: Real PostgreSQL with Testcontainers, tests framework integration
- **Production Tests**: Production-like environment, validates complete test execution flow

### 2. Discover Available Tests (API)

Before executing tests via API, you can discover what tests are available:

```bash
# Get all available tests, suites, and tags
curl -s "http://localhost:8080/api/v1/tests/discover" | jq .

# Get only test classes
curl -s "http://localhost:8080/api/v1/tests/discover/classes" | jq .

# Get only test suites
curl -s "http://localhost:8080/api/v1/tests/discover/suites" | jq .

# Get only tags
curl -s "http://localhost:8080/api/v1/tests/discover/tags" | jq .
```

### 3. Execute Tests (API)

Choose one of the API execution methods based on your needs:

#### Execute a Single Test
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/individual?testClass=LoginApiTest&testMethod=login_api_should_return_token" \
  | jq .
```

#### Execute Tests by Tags
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke,api" \
  | jq .
```

#### Execute Tests by Pattern
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/grep?pattern=*ApiTest" \
  | jq .
```

#### Execute a Test Suite
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/suite?suiteName=AuthenticationApiTestSuite" \
  | jq .
```

### 4. Monitor Execution

Use the `executionId` from the response to monitor the test execution:

```bash
# Get current status
curl -s "http://localhost:8080/api/v1/test-execution/status?executionId=<executionId>" | jq .

# Get live updates (Server-Sent Events)
curl -s "http://localhost:8080/api/v1/test-execution/live?executionId=<executionId>"

# Get final results
curl -s "http://localhost:8080/api/v1/test-execution/results/<executionId>" | jq .
```

## Execution Approaches

ATAS supports two main approaches for test execution:

### 1. Direct Makefile Commands (Recommended for Development)

Use Makefile commands for direct, fast test execution:

```bash
# Development workflow
make test-unit         # Fast unit tests (H2-based)
make test-integration  # Integration tests (PostgreSQL)
make test-production   # Production tests (PostgreSQL)
make test-by-type      # All test types in sequence

# Specific test categories
make test-ui           # UI tests only
make test-api          # API tests only
make test              # All tests
```

**Advantages:**
- Fastest execution
- No API overhead
- Direct Maven integration
- Perfect for development and CI/CD

### 2. REST API Execution (Advanced Use Cases)

Use REST APIs for programmatic test execution:

```bash
# Execute via API
curl -s -X POST "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke" | jq .

# Monitor execution
curl -s "http://localhost:8080/api/v1/test-execution/status?executionId=<executionId>" | jq .
```

**Advantages:**
- Programmatic control
- Real-time monitoring
- Integration with external systems
- Asynchronous execution

## Execution Types (API)

### 1. Individual Test Execution

Execute a specific test method from a test class.

**Use Cases:**
- Debugging a specific test
- Running a single test after making changes
- Testing a specific functionality

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/individual?testClass=LoginApiTest&testMethod=login_api_should_return_token&environment=staging&timeoutMinutes=45" \
  | jq .
```

### 2. Tag-Based Execution

Execute all tests that have specific tags.

**Use Cases:**
- Running smoke tests
- Running regression tests
- Running tests for a specific feature area

**Example:**
```bash
# Run all smoke tests
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke" \
  | jq .

# Run all API tests with smoke tag
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke,api" \
  | jq .
```

### 3. Pattern-Based Execution

Execute tests matching a specific pattern.

**Use Cases:**
- Running all tests in a specific package
- Running all tests with a specific naming convention
- Running tests for a specific module

**Example:**
```bash
# Run all API tests
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/grep?pattern=*ApiTest" \
  | jq .

# Run all login-related tests
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/grep?pattern=Login*" \
  | jq .
```

### 4. Test Suite Execution

Execute a predefined test suite.

**Use Cases:**
- Running a complete test suite for a feature
- Running integration test suites
- Running end-to-end test suites

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/suite?suiteName=AuthenticationApiTestSuite&environment=production" \
  | jq .
```

## Advanced Configuration

### Custom Execution Request

For advanced scenarios, use the custom execution endpoint with a JSON request body:

```bash
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "executionType": "TAGS",
    "tags": ["smoke", "regression"],
    "environment": "staging",
    "browserType": "chromium",
    "recordVideo": true,
    "captureScreenshots": true,
    "timeoutMinutes": 45
  }' \
  "http://localhost:8080/api/v1/tests/execute/custom" \
  | jq .
```

### Environment Configuration

You can specify different environments for test execution:

- `dev` - Development environment (default)
- `staging` - Staging environment
- `production` - Production environment
- `local` - Local environment

### Browser Configuration

For UI tests, you can specify the browser type:

- `chromium` - Chromium browser
- `firefox` - Firefox browser
- `webkit` - WebKit browser

### Recording and Screenshots

Control video recording and screenshot capture:

- `recordVideo`: true/false (default: true)
- `captureScreenshots`: true/false (default: true)

### Timeout Configuration

Set custom timeout for test execution:

- `timeoutMinutes`: Number of minutes (default: 30)

## Monitoring and Results

### Real-Time Monitoring

The ATAS framework provides multiple ways to monitor test execution:

#### 1. Polling for Status

```bash
# Check status every few seconds
while true; do
  curl -s "http://localhost:8080/api/v1/test-execution/status?executionId=<executionId>" | jq .
  sleep 5
done
```

#### 2. Server-Sent Events (SSE)

```bash
# Get real-time updates
curl -s "http://localhost:8080/api/v1/test-execution/live?executionId=<executionId>"
```

#### 3. Final Results

```bash
# Get complete results after execution
curl -s "http://localhost:8080/api/v1/test-execution/results/<executionId>" | jq .
```

### Execution Status

Test executions can have the following statuses:

- `RUNNING` - Test execution is in progress
- `COMPLETED` - Test execution completed successfully
- `FAILED` - Test execution failed
- `ERROR` - Test execution encountered an error
- `TIMEOUT` - Test execution timed out

## Best Practices

### 1. Test Organization

- Use meaningful tags for test categorization
- Organize tests into logical test suites
- Use consistent naming conventions

### 2. Execution Strategy

- Start with individual tests for debugging
- Use tag-based execution for regular testing
- Use test suites for comprehensive testing
- Use pattern-based execution for module testing

### 3. Monitoring

- Always save the `executionId` from the response
- Use polling for simple status checks
- Use SSE for real-time monitoring in applications
- Check final results for detailed information

### 4. Error Handling

- Check HTTP status codes
- Parse error responses for detailed information
- Implement retry logic for transient failures
- Log execution IDs for debugging

## Troubleshooting

### Common Issues

#### 1. Test Discovery Returns Mock Data

**Problem:** The discovery API returns mock data instead of real tests.

**Solution:** Mount the `atas-tests` directory in the Docker container:

```yaml
# In docker-compose-local-db.yml
volumes:
  - ../atas-tests:/app/atas-tests
```

#### 2. Tests Not Found

**Problem:** Tests are not found when executing.

**Solution:** 
- Check if the test class and method names are correct
- Verify that the tests exist in the test directory
- Use the discovery API to see available tests

#### 3. Execution Timeout

**Problem:** Test execution times out.

**Solution:**
- Increase the `timeoutMinutes` parameter
- Check if the test environment is accessible
- Verify that all dependencies are available

#### 4. Monitoring Issues

**Problem:** Cannot get execution status or results.

**Solution:**
- Verify the `executionId` is correct
- Check if the execution is still running
- Ensure the ATAS framework is running

### Debug Mode

Enable debug logging by setting the log level:

```bash
# Check logs
make logs

# Or directly
docker logs atas-service
```

## Integration Examples

### CI/CD Integration

#### Option 1: Direct Makefile Commands (Recommended)

```bash
#!/bin/bash
# Example CI/CD script using Makefile commands

# Start ATAS framework
make dev

# Wait for service to be ready
sleep 30

# Run tests based on CI stage
if [ "$CI_STAGE" = "unit" ]; then
  echo "Running unit tests..."
  make test-unit
elif [ "$CI_STAGE" = "integration" ]; then
  echo "Running integration tests..."
  make test-integration
elif [ "$CI_STAGE" = "production" ]; then
  echo "Running production tests..."
  make test-production
else
  echo "Running all test types..."
  make test-by-type
fi

# Generate reports
make report
```

#### Option 2: API-Based Execution (Advanced)

```bash
#!/bin/bash
# Example CI/CD script using API

# Start ATAS framework
make dev

# Wait for service to be ready
sleep 30

# Execute smoke tests
RESPONSE=$(curl -s -X POST "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke")
EXECUTION_ID=$(echo $RESPONSE | jq -r '.executionId')

# Monitor execution
while true; do
  STATUS=$(curl -s "http://localhost:8080/api/v1/test-execution/status?executionId=$EXECUTION_ID" | jq -r '.status')
  
  if [ "$STATUS" = "COMPLETED" ]; then
    echo "Tests completed successfully"
    break
  elif [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "ERROR" ]; then
    echo "Tests failed"
    exit 1
  fi
  
  sleep 10
done

# Get results
curl -s "http://localhost:8080/api/v1/test-execution/results/$EXECUTION_ID" | jq .
```

### Python Integration

```python
import requests
import time
import json

class ATASClient:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
    
    def discover_tests(self):
        response = requests.get(f"{self.base_url}/api/v1/tests/discover")
        return response.json()
    
    def execute_tests_by_tags(self, tags, environment="dev"):
        response = requests.post(
            f"{self.base_url}/api/v1/tests/execute/tags",
            params={"tags": tags, "environment": environment}
        )
        return response.json()
    
    def get_execution_status(self, execution_id):
        response = requests.get(
            f"{self.base_url}/api/v1/test-execution/status",
            params={"executionId": execution_id}
        )
        return response.json()
    
    def wait_for_completion(self, execution_id, timeout=1800):
        start_time = time.time()
        while time.time() - start_time < timeout:
            status = self.get_execution_status(execution_id)
            if status['status'] in ['COMPLETED', 'FAILED', 'ERROR']:
                return status
            time.sleep(10)
        raise TimeoutError("Test execution timed out")
    
    def get_results(self, execution_id):
        response = requests.get(f"{self.base_url}/api/v1/test-execution/results/{execution_id}")
        return response.json()

# Usage
client = ATASClient()
result = client.execute_tests_by_tags("smoke,api")
execution_id = result['executionId']
final_status = client.wait_for_completion(execution_id)
results = client.get_results(execution_id)
print(json.dumps(results, indent=2))
```

## Next Steps

1. **Explore the API Reference:** See [API_REFERENCE.md](API_REFERENCE.md) for detailed endpoint documentation
2. **Set up Real Tests:** Mount the test directory to use real tests instead of mock data
3. **Integrate with CI/CD:** Use the APIs in your continuous integration pipeline
4. **Monitor Performance:** Use the monitoring APIs to track test execution performance
5. **Customize Configuration:** Adjust timeout, recording, and other settings based on your needs
