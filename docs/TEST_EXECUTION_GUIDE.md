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

Before using the API, you can run tests directly using the Makefile commands or Maven:

#### Using Makefile Commands

```bash
# Quick unit tests (fastest, H2-based, no external dependencies)
make test-unit

# Integration tests (PostgreSQL with Testcontainers)
make test-integration

# All test types in sequence
make test-by-type

# Traditional test categories
make test-ui    # UI tests only
make test-api   # API tests only
make test       # All tests
```

#### Using Maven with Tags

ATAS supports comprehensive test tagging for selective test execution. Use JUnit 5 tags to control which tests run:

```bash
# Run tests with specific tags
cd atas-tests
mvn test -Dgroups=smoke              # Run only smoke tests
mvn test -Dgroups=ui                 # Run only UI tests
mvn test -Dgroups=api                # Run only API tests
mvn test -Dgroups=auth               # Run only authentication tests

# Combine tags with OR logic (tests matching any tag)
mvn test -Dgroups="ui|smoke"         # Run UI tests OR smoke tests

# Combine tags with AND logic (tests matching all tags)
mvn test -Dgroups="ui&smoke"         # Run tests that are BOTH UI AND smoke

# Exclude specific tags
mvn test -DexcludedGroups=slow       # Run all tests except slow ones
mvn test -DexcludedGroups="slow|db"  # Exclude multiple tags

# Combine inclusion and exclusion
mvn test -Dgroups=ui -DexcludedGroups=slow    # Run UI tests excluding slow ones
mvn test -Dgroups=smoke -DexcludedGroups=p3   # Run smoke tests excluding P3 priority

# Priority-based execution
mvn test -Dgroups=p0                 # Run only P0 (critical) tests
mvn test -Dgroups="p0|p1"            # Run P0 or P1 priority tests

# Feature-based execution
mvn test -Dgroups=products           # Run product-related tests
mvn test -Dgroups=cart               # Run cart-related tests
mvn test -Dgroups="checkout|payment" # Run checkout OR payment tests

# Test suite type execution
mvn test -Dgroups=regression         # Run regression tests
mvn test -Dgroups="smoke&fast"       # Run tests that are both smoke AND fast
```

**Available Tags:**

*Test Type Tags:*
- `ui` - UI tests
- `api` - API tests
- `db` - Database tests
- `integration` - Integration tests

*Test Suite Tags:*
- `smoke` - Smoke tests
- `regression` - Regression tests
- `sanity` - Sanity tests

*Execution Tags:*
- `fast` - Fast-running tests
- `slow` - Slow-running tests
- `critical` - Critical tests

*Feature Area Tags:*
- `auth` - Authentication tests
- `products` - Product catalog tests
- `cart` - Shopping cart tests
- `checkout` - Checkout tests
- `payment` - Payment tests
- `navigation` - Navigation tests
- `contact` - Contact/support tests

*Priority Tags:*
- `p0` - Priority 0 (highest priority)
- `p1` - Priority 1
- `p2` - Priority 2
- `p3` - Priority 3 (lowest priority)

**Test Type Comparison:**
- **Unit Tests**: Fastest execution, H2 database, perfect for development feedback
- **Integration Tests**: Real PostgreSQL with Testcontainers, tests framework integration

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

### 1. Direct Maven/Makefile Commands (Recommended for Development)

Use Maven or Makefile commands for direct, fast test execution:

#### Maven Tag-Based Execution

```bash
cd atas-tests

# Common scenarios
mvn test -Dgroups=smoke              # Quick smoke test run
mvn test -Dgroups=regression         # Full regression suite
mvn test -Dgroups="ui&fast"          # Fast UI tests only
mvn test -Dgroups="api&smoke"        # API smoke tests
mvn test -Dgroups=p0                 # Critical tests only

# Development workflow
mvn test -Dgroups="smoke&fast"       # Fast smoke tests for quick feedback
mvn test -DexcludedGroups=slow       # All tests except slow ones
mvn test -Dgroups=ui -DexcludedGroups=p3  # UI tests excluding low priority

# Feature testing
mvn test -Dgroups=products           # Test product features
mvn test -Dgroups="cart|checkout"    # Test cart and checkout flows
```

#### Makefile Commands

```bash
# Development workflow
make test-unit         # Fast unit tests (H2-based)
make test-integration  # Integration tests (PostgreSQL)
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
- Tag-based selective execution
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

Execute all tests that have specific tags. This can be done via Maven (recommended) or API.

#### Maven Tag-Based Execution (Recommended)

Tag-based execution using Maven is the fastest and most flexible approach:

```bash
cd atas-tests

# Basic tag execution
mvn test -Dgroups=smoke              # Run smoke tests
mvn test -Dgroups=ui                 # Run UI tests
mvn test -Dgroups=api                # Run API tests

# Multiple tags (OR logic - matches any tag)
mvn test -Dgroups="ui|api"           # Run UI OR API tests
mvn test -Dgroups="smoke|regression" # Run smoke OR regression tests

# Multiple tags (AND logic - matches all tags)
mvn test -Dgroups="ui&smoke"         # Run tests that are BOTH UI AND smoke
mvn test -Dgroups="api&fast&p0"      # Run API tests that are fast AND P0 priority

# Excluding tags
mvn test -DexcludedGroups=slow       # Run all except slow tests
mvn test -DexcludedGroups="slow|db"  # Exclude slow OR db tests

# Combining inclusion and exclusion
mvn test -Dgroups=ui -DexcludedGroups=slow      # UI tests excluding slow ones
mvn test -Dgroups=smoke -DexcludedGroups=p3     # Smoke tests excluding P3 priority
mvn test -Dgroups=regression -DexcludedGroups="slow|p3"  # Regression excluding slow and P3

# Practical examples
mvn test -Dgroups="p0|p1"            # Run high priority tests
mvn test -Dgroups="products&fast"    # Fast product tests
mvn test -Dgroups="auth&smoke"       # Authentication smoke tests
```

**Tag Combinations Examples:**

```bash
# Smoke test suite (fast feedback)
mvn test -Dgroups="smoke&fast"

# Critical regression suite
mvn test -Dgroups="regression&p0&p1"

# Feature-specific smoke tests
mvn test -Dgroups="products&smoke"
mvn test -Dgroups="cart&checkout&smoke"

# Quick UI validation
mvn test -Dgroups="ui&fast&p0"

# Full test suite excluding slow tests
mvn test -DexcludedGroups=slow

# Production-ready test suite
mvn test -Dgroups="smoke&fast&p0&p1"
```

#### API Tag-Based Execution

Execute tests via REST API using tags:

**Use Cases:**
- Running smoke tests
- Running regression tests
- Running tests for a specific feature area
- Programmatic test execution

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
