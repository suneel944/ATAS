# ATAS Test Execution Guide

## Overview

This guide explains how to execute and monitor tests in the ATAS framework. ATAS is built on **Java 21, JUnit 5, and Playwright for Java**, providing a comprehensive test automation platform with REST APIs for test orchestration and monitoring.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Tech Stack](#tech-stack)
3. [Test Structure](#test-structure)
4. [Direct Test Execution](#direct-test-execution)
5. [API-Based Execution](#api-based-execution)
6. [Test Tag System](#test-tag-system)
7. [Advanced Configuration](#advanced-configuration)
8. [Monitoring and Results](#monitoring-and-results)
9. [Command Reference](#command-reference)
10. [Best Practices](#best-practices)
11. [Troubleshooting](#troubleshooting)
12. [CI/CD Integration](#cicd-integration)

---

## Quick Start

### 1. Start the ATAS Framework

```bash
make dev
```

This starts the ATAS framework and PostgreSQL database. The API will be available at `http://localhost:8080`.

### 2. Run Your First Test

**Quickest way (Maven):**
```bash
# Run all UI tests
mvn test -pl atas-tests -Dgroups=ui

# Run all API tests
mvn test -pl atas-tests -Dgroups=api

# Run a single test
mvn test -pl atas-tests -Dtest=LoginExistingUserTest
```

**Alternative (Makefile):**
```bash
make test-ui    # UI tests only
make test-api   # API tests only
make test       # All tests
```

---

## Tech Stack

ATAS uses the following technologies:

- **Java 21 (LTS)** - Programming language
- **JUnit 5 (Jupiter)** - Testing framework
- **Playwright for Java** - Browser and API automation
- **Maven 3.9+** - Build and dependency management
- **Spring Boot 3.5** - Framework service
- **Allure** - Test reporting
- **PostgreSQL** - Test execution data persistence

### Test Types

- **UI Tests** - Use Playwright for browser automation, extend `UiTestHooks`
- **API Tests** - Use Playwright APIRequestContext, extend `ApiTestHooks`
- **Unit Tests** - Framework component tests (fast, H2 database)
- **Integration Tests** - Framework integration tests (PostgreSQL with Testcontainers)

---

## Test Structure

### Test Organization

Tests are organized by product and feature:

```
atas-tests/src/test/java/com/atas/
├── products/
│   └── automationexercise/
│       ├── features/                   # Feature-based organization
│       │   ├── {feature-name}/
│       │   │   ├── api/               # API tests
│       │   │   └── ui/                # UI tests
│       └── pages/                      # Page Object Model
└── shared/
    ├── pages/
    │   └── BasePage.java              # Base page class
    ├── testing/
    │   ├── TestTags.java              # Standardized test tags
    │   ├── ApiTestHooks.java          # API test lifecycle hooks
    │   └── UiTestHooks.java           # UI test lifecycle hooks
    └── utility/
        ├── TestUtils.java             # Common test utilities
        ├── TestDataUtility.java       # Configuration loading (.env, properties)
        ├── BaseUrlResolver.java       # URL resolution for services/gateways
        └── FakerUtils.java           # Test data generation (JavaFaker wrapper)
    ├── api/
        ├── ApiRequestHelper.java     # API request utilities
        ├── FluentApiRequest.java     # Fluent API request builder
        └── FrameworkApiRequestContextHelper.java  # Framework API context
    ├── assertions/
        └── CommonAssertions.java     # Common assertion utilities
    ├── auth/
        ├── BrowserHelper.java        # Browser authentication helpers
        └── InternalApiTokenHelper.java  # Internal API token management
    └── email/
        └── GuerrillaMailHelper.java  # Email testing utilities
```

### UI Test Example

```java
package com.atas.products.automationexercise.features.user_auth_and_account.ui;

import com.atas.products.automationexercise.pages.LoginPage;
import com.atas.shared.testing.TestTags;
import com.atas.shared.testing.UiTestHooks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestTags.UI)
@Tag(TestTags.AUTH)
@Tag(TestTags.SMOKE)
public class LoginExistingUserTest extends UiTestHooks {

    @Test
    @DisplayName("Verify login page loads correctly")
    void testLoginPageLoads() {
        LoginPage loginPage = new LoginPage(page);  // 'page' provided by UiTestHooks
        loginPage.gotoPage();
        assertTrue(loginPage.isPageLoaded(), "Login page should load successfully");
    }
}
```

### API Test Example

```java
package com.atas.products.automationexercise.features.landing_and_navigation.api;

import com.atas.shared.testing.ApiTestHooks;
import com.atas.shared.testing.TestTags;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.API)
@Tag(TestTags.SMOKE)
public class SiteHealthApiTest extends ApiTestHooks {

    @Test
    @DisplayName("Verify home page returns 200 OK")
    void testHomePageHealth() {
        APIResponse response = request.get("/");  // 'request' provided by ApiTestHooks
        assertEquals(200, response.status(), "Home page should return 200 OK status");
    }
}
```

### Test Hooks

- **`UiTestHooks`** - Provides `Page`, `BrowserContext`, and `Browser` instances. Manages Playwright browser lifecycle.
- **`ApiTestHooks`** - Provides `APIRequestContext` for API testing. Manages Playwright API context lifecycle.

---

## Direct Test Execution

Direct execution is the recommended approach for development, CI/CD, and regular testing. It's faster, simpler, and doesn't require the API service to be running.

### Using Makefile Commands

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

# Run with framework service running
make test-with-service
```

**Test Type Comparison:**
- **Unit Tests**: Fastest execution, H2 database, perfect for development feedback
- **Integration Tests**: Real PostgreSQL with Testcontainers, tests framework integration

### Using Maven Commands

#### Running Individual Tests

```bash
# From project root - single test class
mvn test -pl atas-tests -Dtest=LoginExistingUserTest

# With full package name
mvn test -pl atas-tests -Dtest=com.atas.products.automationexercise.features.user_auth_and_account.ui.LoginExistingUserTest

# Run a specific test method
mvn test -pl atas-tests -Dtest=LoginExistingUserTest#testLoginPageLoads

# Run multiple test classes (comma-separated)
mvn test -pl atas-tests -Dtest=LoginExistingUserTest,HomeCarouselTest,ApiListPageLoadsTest

# Pattern matching
mvn test -pl atas-tests -Dtest="*ApiTest"  # All API test classes
mvn test -pl atas-tests -Dtest="com.atas.products.automationexercise.features.user_auth_and_account.*"  # All tests in package
```

#### Running Tests by Tags

**Basic tag filtering:**
```bash
mvn test -pl atas-tests -Dgroups=smoke              # Run only smoke tests
mvn test -pl atas-tests -Dgroups=ui                 # Run only UI tests
mvn test -pl atas-tests -Dgroups=api                # Run only API tests
mvn test -pl atas-tests -Dgroups=auth               # Run only authentication tests
mvn test -pl atas-tests -Dgroups=p0                 # Run only P0 (critical) tests
```

**OR logic (tests matching any tag):**
```bash
mvn test -pl atas-tests -Dgroups="ui|api"           # Run UI OR API tests
mvn test -pl atas-tests -Dgroups="smoke|regression" # Run smoke OR regression tests
mvn test -pl atas-tests -Dgroups="p0|p1"            # Run P0 or P1 priority tests
```

**AND logic (tests matching all tags):**
```bash
mvn test -pl atas-tests -Dgroups="ui&smoke"         # Run tests that are BOTH UI AND smoke
mvn test -pl atas-tests -Dgroups="api&fast&p0"      # Run API tests that are fast AND P0 priority
mvn test -pl atas-tests -Dgroups="products&smoke"   # Run product tests that are smoke
```

**Excluding tags:**
```bash
mvn test -pl atas-tests -DexcludedGroups=slow       # Run all except slow tests
mvn test -pl atas-tests -DexcludedGroups="slow|db"  # Exclude slow OR db tests
mvn test -pl atas-tests -Dgroups=ui -DexcludedGroups=slow      # UI tests excluding slow ones
mvn test -pl atas-tests -Dgroups=smoke -DexcludedGroups=p3     # Smoke tests excluding P3 priority
```

#### Common Test Execution Scenarios

```bash
# Quick smoke test run for fast feedback
mvn test -pl atas-tests -Dgroups="smoke&fast"

# UI regression suite (excluding slow tests)
mvn test -pl atas-tests -Dgroups=ui -DexcludedGroups=slow

# Critical API tests (P0 priority)
mvn test -pl atas-tests -Dgroups="api&p0"

# Authentication tests only
mvn test -pl atas-tests -Dgroups=auth

# Production-ready test suite
mvn test -pl atas-tests -Dgroups="smoke&fast&p0&p1"
```

#### Environment Configuration

```bash
# Run tests with specific Spring profile
mvn test -pl atas-tests -Denv.SPRING_PROFILES_ACTIVE=dev      # Default
mvn test -pl atas-tests -Denv.SPRING_PROFILES_ACTIVE=stage
mvn test -pl atas-tests -Denv.SPRING_PROFILES_ACTIVE=prod

# Database connection is auto-detected based on environment
# But you can override if needed:
mvn test -pl atas-tests -DDB_URL=jdbc:postgresql://localhost:5433/testdb -Dgroups=api
mvn test -pl atas-tests -DDB_USERNAME=testuser -DDB_PASSWORD=testpass -Dgroups=ui

# Control headless mode for UI tests
mvn test -pl atas-tests -DHEADLESS=true -Dgroups=ui

# Record test results with environment-aware database connection
SPRING_PROFILES_ACTIVE=dev ATAS_RECORD_LOCAL=true mvn test -pl atas-tests -Dgroups=smoke
# System automatically detects dev environment and connects to correct database
```

#### Test Output and Reporting

```bash
# Run tests with detailed output
mvn test -pl atas-tests -Dgroups=ui -X

# Show test output summary
mvn test -pl atas-tests -Dgroups=ui -Dsurefire.printSummary=true

# Generate Allure report after running tests
mvn allure:serve -pl atas-tests

# Save output to file with timestamp
mvn test -pl atas-tests -Dgroups=smoke > "test-output-$(date +%Y%m%d-%H%M%S).log" 2>&1

# Compile tests without running
mvn test-compile -pl atas-tests
```

**Advantages of Direct Execution:**
- Fastest execution
- No API overhead
- Direct Maven integration
- Tag-based selective execution
- Perfect for development and CI/CD

#### Test Execution Recording to Database

When running tests directly via Maven (`mvn test`), test results are automatically recorded to the database in the following scenarios:

**Automatic Recording (Always Enabled):**
- When tests are executed via the API (`ATAS_EXECUTION_ID` is automatically set)
- Test execution and results are automatically persisted for monitoring dashboard visibility

**Opt-in Local Recording:**
By default, local Maven runs do NOT record to the database. To enable recording for local runs:

```bash
# Enable recording for local test run
ATAS_RECORD_LOCAL=true mvn test -pl atas-tests -Dgroups="smoke&fast"

# Or using system property
mvn test -pl atas-tests -Dgroups="smoke&fast" -Datas.record.local=true
```

**Why Opt-in for Local Runs?**
- Prevents database pollution from debug runs
- Keeps monitoring dashboard focused on intentional/official runs
- Reduces database growth from frequent local testing
- Maintains clean separation between development and production data

**Recording Behavior:**
- ✅ **API-triggered runs**: Always recorded (via `ATAS_EXECUTION_ID`)
- ⚙️ **Local runs with opt-in**: Recorded when `ATAS_RECORD_LOCAL=true` is set
- ❌ **Regular local runs**: Not recorded (silent, fast execution)

#### Database Connection for Test Recording

When recording test results (`ATAS_RECORD_LOCAL=true`), the system automatically detects and connects to the correct database based on your environment:

**Automatic Detection:**
- Detects `SPRING_PROFILES_ACTIVE` (from `make dev`, `make dev-stage`, `make dev-prod`)
- Checks running Docker containers (`atas-db` for dev/stage, `atas-db-prod` for prod)
- Verifies port availability (5433 for local Docker Compose, 5432 for standard PostgreSQL)

**Examples:**

```bash
# Development environment
make dev
ATAS_RECORD_LOCAL=true mvn test
# → Connects to: localhost:5433/atasdb (dev database)

# Staging environment  
make dev-stage
SPRING_PROFILES_ACTIVE=stage ATAS_RECORD_LOCAL=true mvn test
# → Connects to: localhost:5433/atasdb (staging data via Spring profile)

# Production environment
make dev-prod
SPRING_PROFILES_ACTIVE=prod ATAS_RECORD_LOCAL=true mvn test
# → Warning: Production DB not accessible from host (port not exposed)
# → Falls back to dev DB if available, or throws error with solutions

# Override with explicit DB_URL
DB_URL="jdbc:postgresql://localhost:5433/atasdb" ATAS_RECORD_LOCAL=true mvn test
# → Uses specified database connection
```

**Connection Priority:**
1. `DB_URL` environment variable (if set)
2. `spring.datasource.url` system property (if set)
3. Environment-aware detection (checks profile + Docker containers + ports)
4. Default fallback with detailed logging

**Viewing Recorded Results:**
Once tests are recorded, you can view them in:
- **Monitoring Dashboard**: http://localhost:8080/monitoring
- **Database Management Dashboard**: http://localhost:8080/database
- **API endpoints**: `/api/v1/test-execution/*`

For more details on database connection detection, see [Environment Configuration Guide](ENVIRONMENT_CONFIGURATION.md#-database-connection-detection).

---

## API-Based Execution

API-based execution is useful for programmatic control, integration with external systems, and real-time monitoring.

### Discover Available Tests

Before executing tests via API, discover what tests are available:

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

### Execute Tests via API

#### 1. Individual Test Execution

Execute a specific test method from a test class.

**Use Cases:**
- Debugging a specific test
- Running a single test after making changes
- Testing a specific functionality

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/individual?testClass=LoginExistingUserTest&testMethod=testLoginPageLoads&environment=dev&browserType=chromium&timeoutMinutes=45" \
  | jq .
```

#### 2. Tag-Based Execution

Execute all tests that have specific tags.

**Use Cases:**
- Running smoke tests
- Running regression tests
- Running tests for a specific feature area
- Programmatic test execution

**Examples:**
```bash
# Run all smoke tests
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke&environment=dev&browserType=chromium" \
  | jq .

# Run all API tests with smoke tag
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke,api&environment=dev" \
  | jq .
```

#### 3. Pattern-Based Execution

Execute tests matching a specific pattern.

**Use Cases:**
- Running all tests in a specific package
- Running all tests with a specific naming convention
- Running tests for a specific module

**Examples:**
```bash
# Run all API tests
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/grep?pattern=*ApiTest&environment=dev" \
  | jq .

# Run all login-related tests
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/grep?pattern=Login*&environment=dev" \
  | jq .
```

#### 4. Test Suite Execution

Execute a predefined test suite.

**Use Cases:**
- Running a complete test suite for a feature
- Running integration test suites
- Running end-to-end test suites

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/suite?suiteName=AuthenticationApiTestSuite&environment=production&browserType=chromium" \
  | jq .
```

#### 5. Custom Execution Request

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

**Advantages of API Execution:**
- Programmatic control
- Real-time monitoring
- Integration with external systems
- Asynchronous execution

---

## Test Tag System

ATAS uses JUnit 5 tags for categorizing and filtering tests. Tags allow you to run specific subsets of tests based on test type, feature area, priority, or execution characteristics.

### Available Tags

#### Test Type Tags
- `ui` - UI/Playwright tests
- `api` - API/REST tests
- `db` - Database tests
- `integration` - Integration tests

#### Test Suite Tags
- `smoke` - Smoke tests (quick validation)
- `regression` - Regression tests (comprehensive)
- `sanity` - Sanity tests

#### Execution Tags
- `fast` - Fast-running tests
- `slow` - Slow-running tests
- `critical` - Critical tests

#### Feature Area Tags
- `auth` - Authentication tests
- `products` - Product catalog tests
- `cart` - Shopping cart tests
- `checkout` - Checkout process tests
- `payment` - Payment processing tests
- `navigation` - Navigation tests
- `contact` - Contact/Support tests

#### Priority Tags
- `p0` - Priority 0 (highest priority)
- `p1` - Priority 1
- `p2` - Priority 2
- `p3` - Priority 3 (lowest priority)

### Tag Combination Examples

```bash
# Smoke test suite (fast feedback)
mvn test -pl atas-tests -Dgroups="smoke&fast"

# Critical regression suite
mvn test -pl atas-tests -Dgroups="regression&p0&p1"

# Feature-specific smoke tests
mvn test -pl atas-tests -Dgroups="products&smoke"
mvn test -pl atas-tests -Dgroups="cart&checkout&smoke"

# Quick UI validation
mvn test -pl atas-tests -Dgroups="ui&fast&p0"

# Full test suite excluding slow tests
mvn test -pl atas-tests -DexcludedGroups=slow

# Production-ready test suite
mvn test -pl atas-tests -Dgroups="smoke&fast&p0&p1"
```

---

## Advanced Configuration

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

### Headless Mode

Control browser headless mode for UI tests:

```bash
# Run UI tests in headless mode
mvn test -pl atas-tests -DHEADLESS=true -Dgroups=ui

# Run UI tests with browser visible
mvn test -pl atas-tests -DHEADLESS=false -Dgroups=ui
```

---

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

### Allure Reports

Generate and view Allure reports:

```bash
# Generate Allure report
mvn allure:report -pl atas-tests

# Serve Allure report locally
mvn allure:serve -pl atas-tests
```

Reports are available at: `atas-tests/target/site/allure-maven-plugin/index.html`

---

## Command Reference

This section provides quick reference commands for common scenarios.

### Running Tests by Specific Test Files

#### UI Tests Examples
```bash
mvn test -pl atas-tests -Dtest=LoginExistingUserTest
mvn test -pl atas-tests -Dtest=HomeCarouselTest
mvn test -pl atas-tests -Dtest=ApiListPageLoadsTest
mvn test -pl atas-tests -Dtest=ViewAllProductsTest
mvn test -pl atas-tests -Dtest=AddProductToCartTest
```

#### API Tests Examples
```bash
mvn test -pl atas-tests -Dtest=SiteHealthApiTest
mvn test -pl atas-tests -Dtest=ListProductsApiTest
mvn test -pl atas-tests -Dtest=LoginUserApiTest
mvn test -pl atas-tests -Dtest=RegisterUserApiTest
mvn test -pl atas-tests -Dtest=GetCartContentsApiTest
```

### Check Available Tests

```bash
# List all test classes (compiles first)
mvn test-compile -pl atas-tests
find atas-tests/target/test-classes -name "*Test.class" | sed 's/.*\///' | sed 's/\.class$//'

# Run tests with verbose output
mvn test -pl atas-tests -Dgroups=ui -X

# Show test output summary
mvn test -pl atas-tests -Dgroups=ui -Dsurefire.printSummary=true
```

---

## Best Practices

### 1. Test Organization
- Use meaningful tags for test categorization
- Organize tests into logical test suites
- Use consistent naming conventions
- Follow the product/feature-based structure

### 2. Execution Strategy
- Start with individual tests for debugging
- Use tag-based execution for regular testing
- Use test suites for comprehensive testing
- Use pattern-based execution for module testing

### 3. Test Development
- Extend `UiTestHooks` for UI tests to get Playwright `Page` instance
- Extend `ApiTestHooks` for API tests to get Playwright `APIRequestContext`
- Use `@DisplayName` for readable test names
- Use AssertJ or JUnit assertions for clear failure messages

### 4. Monitoring
- Always save the `executionId` from the response
- Use polling for simple status checks
- Use SSE for real-time monitoring in applications
- Check final results for detailed information

### 5. Error Handling
- Check HTTP status codes
- Parse error responses for detailed information
- Implement retry logic for transient failures
- Log execution IDs for debugging

---

## Troubleshooting

### Common Issues

#### 1. Test Discovery Returns Empty Results

**Problem:** The discovery API returns empty lists instead of discovering real tests.

**Solution:** Mount the `atas-tests` directory in the Docker container and verify the test path configuration:

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
- Ensure test classes are compiled: `mvn test-compile -pl atas-tests`

#### 3. Execution Timeout

**Problem:** Test execution times out.

**Solution:**
- Increase the `timeoutMinutes` parameter
- Check if the test environment is accessible
- Verify that all dependencies are available
- Check for long-running tests and tag them as `slow`

#### 4. Monitoring Issues

**Problem:** Cannot get execution status or results.

**Solution:**
- Verify the `executionId` is correct
- Check if the execution is still running
- Ensure the ATAS framework is running
- Check framework logs: `docker logs atas-service`

#### 5. Playwright Browser Issues

**Problem:** UI tests fail with browser-related errors.

**Solution:**
- Ensure Playwright browsers are installed: Playwright downloads them automatically on first test run
- Check headless mode setting: Use `-DHEADLESS=true` for CI/CD environments
- Verify browser executable permissions
- Check system dependencies for Playwright

#### 6. Database Connection Issues (Test Recording)

**Problem:** Tests with `ATAS_RECORD_LOCAL=true` fail to connect to database or connect to wrong database.

**Symptoms:**
- Error: "Failed to connect to database"
- Test results not appearing in monitoring dashboard
- Connection to wrong environment database

**Solution:**
1. **Check environment detection:**
   ```bash
   # Check which profile is active
   echo $SPRING_PROFILES_ACTIVE
   
   # Check running Docker containers
   docker ps --format "{{.Names}}" | grep atas-db
   
   # Look for detection logs in test output:
   # "Environment detection - Spring profile: dev"
   # "Docker container status - atas-db (dev/stage): true"
   ```

2. **Verify correct environment is running:**
   ```bash
   # For development testing
   make dev  # Starts atas-db on port 5433
   
   # For staging testing  
   make dev-stage  # Starts atas-db on port 5433 with stage profile
   
   # For production (note: DB port not exposed)
   make dev-prod  # Starts atas-db-prod (not accessible from host)
   ```

3. **Override with explicit connection:**
   ```bash
   # If auto-detection fails, set DB_URL explicitly
   DB_URL="jdbc:postgresql://localhost:5433/atasdb" \
   ATAS_RECORD_LOCAL=true \
   mvn test -pl atas-tests -Dgroups=smoke
   ```

4. **Check port availability:**
   ```bash
   # Check if database port is accessible
   nc -zv localhost 5433  # Local Docker Compose
   nc -zv localhost 5432  # Standard PostgreSQL
   
   # Test connection manually
   psql -h localhost -p 5433 -U atas -d atasdb
   ```

5. **Multiple environments running:**
   ```bash
   # Stop unused environments
   make stop  # Stops current environment
   docker compose -f docker/docker-compose-prod.yml down  # Stop production
   
   # Start the correct environment
   make dev  # For development
   ```

**Common Scenarios:**
- **Production DB not accessible**: Production containers don't expose DB port. Use `make dev` for local testing.
- **Wrong database**: System auto-detects based on `SPRING_PROFILES_ACTIVE`. Set `DB_URL` to override.
- **Port conflict**: Check if another service is using port 5433 or 5432.

For detailed information, see [Database Connection Detection](ENVIRONMENT_CONFIGURATION.md#-database-connection-detection) in the Environment Configuration Guide.

### Debug Mode

Enable debug logging by setting the log level:

```bash
# Check logs
make logs

# Or directly
docker logs atas-service

# View Maven test output
mvn test -pl atas-tests -Dgroups=ui -X

# View database connection detection logs (when recording)
ATAS_RECORD_LOCAL=true mvn test -pl atas-tests -Dgroups=smoke 2>&1 | grep -E "(Environment detection|Docker container|Connecting to database|✅|⚠️)"
```

---

## CI/CD Integration

### Option 1: Direct Makefile Commands (Recommended)

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

### Option 2: API-Based Execution (Advanced)

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

### GitHub Actions Example

```yaml
name: CI/CD Pipeline

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: |
          make setup
          make test-unit
```

---

## Next Steps

1. **Explore the API Reference:** See [API_REFERENCE.md](API_REFERENCE.md) for detailed endpoint documentation
2. **Set up Real Tests:** Mount the test directory to enable test discovery
3. **Integrate with CI/CD:** Use the APIs in your continuous integration pipeline
4. **Monitor Performance:** Use the monitoring APIs to track test execution performance
5. **Customize Configuration:** Adjust timeout, recording, and other settings based on your needs

---

## Summary

ATAS provides a comprehensive test execution platform built on **Java 21, JUnit 5, and Playwright**. You can execute tests:

- **Directly** using Maven or Makefile commands (recommended for development)
- **Via REST APIs** for programmatic control and monitoring
- **By tags** for flexible test filtering
- **By patterns** for package/module-based execution

All tests use JUnit 5 annotations and Playwright for automation, with Allure for reporting and Spring Boot for service orchestration.
