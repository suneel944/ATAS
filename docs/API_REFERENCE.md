# ATAS Test Execution API Reference

## Overview

The ATAS framework provides comprehensive REST APIs for test discovery and execution. You can trigger tests using multiple filtering options and monitor their execution in real-time.

## Base URL

```
http://localhost:8080/api/v1
```

## Test Discovery APIs

### Discover All Tests, Suites, and Tags

**Endpoint:** `GET /tests/discover`

**Description:** Returns comprehensive information about all available tests, test suites, and tags.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/tests/discover" | jq .
```

**Response:**
```json
{
  "testClasses": [
    {
      "className": "LoginUiTest",
      "packageName": "com.atas.features.authentication.ui",
      "fullName": "com.atas.features.authentication.ui.LoginUiTest",
      "testMethods": [
        "login_should_succeed",
        "login_should_fail_with_invalid_credentials"
      ],
      "tags": ["smoke", "ui", "authentication"],
      "type": "UI"
    }
  ],
  "testSuites": [
    {
      "suiteName": "AuthenticationUiTestSuite",
      "className": "com.atas.suites.authentication.ui.AuthenticationUiTestSuite",
      "description": "Test suite for authentication UI tests",
      "includedTestClasses": ["LoginUiTest", "LoginValidationUiTest"],
      "type": "UI"
    }
  ],
  "availableTags": ["ui", "regression", "smoke", "api", "monitoring", "validation", "authentication"],
  "totalTests": 8
}
```

### Get All Test Classes

**Endpoint:** `GET /tests/discover/classes`

**Description:** Returns a list of all test classes with their details.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/tests/discover/classes" | jq .
```

### Get All Test Suites

**Endpoint:** `GET /tests/discover/suites`

**Description:** Returns a list of all test suites with their details.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/tests/discover/suites" | jq .
```

### Get All Tags

**Endpoint:** `GET /tests/discover/tags`

**Description:** Returns a list of all available test tags.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/tests/discover/tags" | jq .
```

### Get Test Classes by Type

**Endpoint:** `GET /tests/discover/classes/{type}`

**Description:** Returns test classes filtered by type (UI or API).

**Parameters:**
- `type`: Either "ui" or "api"

**Examples:**
```bash
curl -s "http://localhost:8080/api/v1/tests/discover/classes/ui" | jq .
curl -s "http://localhost:8080/api/v1/tests/discover/classes/api" | jq .
```

### Get Test Suites by Type

**Endpoint:** `GET /tests/discover/suites/{type}`

**Description:** Returns test suites filtered by type (UI or API).

**Parameters:**
- `type`: Either "ui" or "api"

**Examples:**
```bash
curl -s "http://localhost:8080/api/v1/tests/discover/suites/ui" | jq .
curl -s "http://localhost:8080/api/v1/tests/discover/suites/api" | jq .
```

## Test Execution APIs

### Execute Individual Test

**Endpoint:** `POST /tests/execute/individual`

**Description:** Executes a specific test method from a test class.

**Parameters:**
- `testClass` (required): Name of the test class
- `testMethod` (required): Name of the test method
- `environment` (optional): Target environment (default: "dev")
- `browserType` (optional): Browser type for UI tests (e.g., "chromium", "firefox", "webkit")
- `recordVideo` (optional): Record video of test execution (default: true)
- `captureScreenshots` (optional): Capture screenshots (default: true)
- `timeoutMinutes` (optional): Test timeout in minutes (default: 30)

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/individual?testClass=LoginApiTest&testMethod=login_api_should_return_token" \
  | jq .
```

### Execute Tests by Tags

**Endpoint:** `POST /tests/execute/tags`

**Description:** Executes all tests that have the specified tags.

**Parameters:**
- `tags` (required): Comma-separated list of tags (e.g., "smoke,api,authentication")
- `environment` (optional): Target environment (default: "dev")
- `browserType` (optional): Browser type for UI tests
- `recordVideo` (optional): Record video of test execution (default: true)
- `captureScreenshots` (optional): Capture screenshots (default: true)
- `timeoutMinutes` (optional): Test timeout in minutes (default: 30)

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke,api" \
  | jq .
```

### Execute Tests by Grep Pattern

**Endpoint:** `POST /tests/execute/grep`

**Description:** Executes tests matching the specified pattern.

**Parameters:**
- `pattern` (required): Maven-style test pattern (e.g., "*ApiTest", "Login*", "*UiTest")
- `environment` (optional): Target environment (default: "dev")
- `browserType` (optional): Browser type for UI tests
- `recordVideo` (optional): Record video of test execution (default: true)
- `captureScreenshots` (optional): Capture screenshots (default: true)
- `timeoutMinutes` (optional): Test timeout in minutes (default: 30)

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/grep?pattern=*ApiTest" \
  | jq .
```

### Execute Test Suite

**Endpoint:** `POST /tests/execute/suite`

**Description:** Executes a predefined test suite.

**Parameters:**
- `suiteName` (required): Name of the test suite to execute
- `environment` (optional): Target environment (default: "dev")
- `browserType` (optional): Browser type for UI tests
- `recordVideo` (optional): Record video of test execution (default: true)
- `captureScreenshots` (optional): Capture screenshots (default: true)
- `timeoutMinutes` (optional): Test timeout in minutes (default: 30)

**Example:**
```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/tests/execute/suite?suiteName=AuthenticationApiTestSuite" \
  | jq .
```

### Execute Custom Tests (Advanced)

**Endpoint:** `POST /tests/execute/custom`

**Description:** Execute tests with a custom request body for advanced configuration.

**Request Body:**
```json
{
  "executionType": "TAGS",
  "tags": ["smoke", "regression"],
  "environment": "staging",
  "browserType": "chromium",
  "recordVideo": true,
  "captureScreenshots": true,
  "timeoutMinutes": 45
}
```

**Example:**
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

## Test Execution Response

All execution endpoints return a response with the following structure:

```json
{
  "executionId": "20700bd4-0450-44bb-8a2c-4d86d93786d8",
  "status": "RUNNING",
  "executionType": "INDIVIDUAL_TEST",
  "description": "Individual test: LoginApiTest.login_api_should_return_token",
  "startTime": "2025-10-24T13:14:23.858494026",
  "timeoutMinutes": 30,
  "testsToExecute": ["LoginApiTest.login_api_should_return_token"],
  "environment": "dev",
  "browserType": null,
  "recordVideo": true,
  "captureScreenshots": true,
  "monitoringUrl": "/api/v1/test-execution/status?executionId=20700bd4-0450-44bb-8a2c-4d86d93786d8",
  "liveUpdatesUrl": "/api/v1/test-execution/live?executionId=20700bd4-0450-44bb-8a2c-4d86d93786d8",
  "resultsUrl": "/api/v1/test-execution/results/20700bd4-0450-44bb-8a2c-4d86d93786d8"
}
```

**Response Fields:**
- `executionId`: Unique identifier for the test execution
- `status`: Current status (RUNNING, COMPLETED, FAILED, ERROR)
- `executionType`: Type of execution (INDIVIDUAL_TEST, TAGS, GREP, SUITE, CUSTOM)
- `description`: Human-readable description of the execution
- `startTime`: When the execution started
- `timeoutMinutes`: Maximum execution time in minutes
- `testsToExecute`: List of tests that will be executed
- `environment`: Target environment
- `browserType`: Browser type for UI tests
- `recordVideo`: Whether video recording is enabled
- `captureScreenshots`: Whether screenshots are captured
- `monitoringUrl`: URL for polling execution status
- `liveUpdatesUrl`: URL for Server-Sent Events (SSE) updates
- `resultsUrl`: URL for retrieving test results

## Test Monitoring APIs

### Get Execution Status (Polling)

**Endpoint:** `GET /test-execution/status`

**Description:** Returns the current status of a test execution.

**Parameters:**
- `executionId` (required): The execution ID returned from the execution API

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/status?executionId=<executionId>" | jq .
```

### Get Live Updates (Server-Sent Events)

**Endpoint:** `GET /test-execution/live`

**Description:** Provides real-time updates for a test execution via Server-Sent Events.

**Parameters:**
- `executionId` (required): The execution ID returned from the execution API

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/live?executionId=<executionId>"
```

### Get Test Results

**Endpoint:** `GET /test-execution/results/{executionId}`

**Description:** Returns detailed results for a completed test execution.

**Parameters:**
- `executionId` (path parameter): The execution ID returned from the execution API

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/results/<executionId>" | jq .
```

## Error Responses

All APIs return standard HTTP status codes and error responses in the following format:

```json
{
  "timestamp": "2025-10-24T13:14:23.858494026",
  "status": 400,
  "error": "Bad Request",
  "message": "Required parameter 'testClass' is missing",
  "path": "/api/v1/tests/execute/individual"
}
```

## Rate Limiting

Currently, there are no rate limits imposed on the API endpoints. However, test executions are queued and executed asynchronously to prevent system overload.

## Authentication

The ATAS framework implements JWT-based authentication and authorization. Most API endpoints require authentication, except for public endpoints like health checks and authentication endpoints.

### Authentication Endpoints

#### Login

**Endpoint:** `POST /api/v1/auth/login`

**Description:** Authenticates a user and returns JWT access and refresh tokens.

**Request Body:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

#### Refresh Token

**Endpoint:** `POST /api/v1/auth/refresh`

**Description:** Refreshes an access token using a valid refresh token.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Get User Profile

**Endpoint:** `GET /api/v1/profile/me`

**Description:** Returns the authenticated user's profile information.

**Headers:**
- `Authorization: Bearer <accessToken>`

**Response:**
```json
{
  "username": "admin",
  "email": "admin@atas.local",
  "enabled": true,
  "accountNonLocked": true,
  "authorities": ["ROLE_ADMIN"]
}
```

**Example:**
```bash
curl -X GET http://localhost:8080/api/v1/profile/me \
  -H "Authorization: Bearer <accessToken>"
```

### Internal API Authentication

For internal API access, use the internal API authentication endpoint:

**Endpoint:** `POST /api/v1/internal/auth/token`

**Description:** Generates an internal API token using an API key.

**Headers:**
- `X-API-Key: <internal-api-key>`

**Example:**
```bash
curl -X POST http://localhost:8080/api/v1/internal/auth/token \
  -H "X-API-Key: internal-api-key-change-this-in-production"
```

### Using Authentication in API Calls

Include the JWT token in the Authorization header:

```bash
# Get access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')

# Use token in API calls
curl -X GET http://localhost:8080/api/v1/test-execution/status?executionId=<id> \
  -H "Authorization: Bearer $TOKEN"
```

### Default Credentials

The framework includes a default admin user:
- **Username:** `admin`
- **Password:** `admin123` (should be changed on first login)

**Security Note:** Change the default password immediately in production environments.

## Dashboard & Monitoring APIs

### Dashboard Overview

**Endpoint:** `GET /test-execution/dashboard/overview`

**Description:** Returns dashboard overview statistics including total executions, success rate, and recent activity.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/dashboard/overview" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Recent Executions

**Endpoint:** `GET /test-execution/dashboard/recent`

**Description:** Returns a list of recent test executions.

**Parameters:**
- `limit` (optional): Number of recent executions to return (default: 10)

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/dashboard/recent?limit=20" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Database Health

**Endpoint:** `GET /test-execution/dashboard/database-health`

**Description:** Returns database health information for the dashboard.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/dashboard/database-health" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Database Operations

**Endpoint:** `GET /test-execution/dashboard/database-operations`

**Description:** Returns database operation statistics.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/dashboard/database-operations" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Execution Trends

**Endpoint:** `GET /test-execution/dashboard/execution-trends`

**Description:** Returns execution trends over a specified number of days.

**Parameters:**
- `days` (optional): Number of days to analyze (default: 7)

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/dashboard/execution-trends?days=30" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Active Executions

**Endpoint:** `GET /test-execution/dashboard/active`

**Description:** Returns a list of currently active test executions.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/dashboard/active" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Active Executions Live (SSE)

**Endpoint:** `GET /test-execution/dashboard/active/live`

**Description:** Provides real-time updates for active test executions via Server-Sent Events.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/test-execution/dashboard/active/live" \
  -H "Authorization: Bearer <accessToken>"
```

## Database Management APIs

### Database Health

**Endpoint:** `GET /database/health`

**Description:** Returns database health information including connection status, pool statistics, and performance metrics.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/health" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Database Operations

**Endpoint:** `GET /database/operations`

**Description:** Returns database operation statistics including read/write counts and operation types.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/operations" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Database Live Updates (SSE)

**Endpoint:** `GET /database/live`

**Description:** Provides real-time database operation updates via Server-Sent Events.

**Parameters:**
- `clientId` (optional): Client identifier for the SSE connection (default: "default")

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/live?clientId=my-client" \
  -H "Authorization: Bearer <accessToken>"
```

### Browse Executions

**Endpoint:** `GET /database/browse/executions`

**Description:** Returns a paginated list of test executions with sorting options.

**Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sortBy` (optional): Field to sort by (default: "startTime")
- `sortDir` (optional): Sort direction - "asc" or "desc" (default: "desc")

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/browse/executions?page=0&size=50&sortBy=startTime&sortDir=desc" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Browse Results

**Endpoint:** `GET /database/browse/results`

**Description:** Returns a paginated list of test results with sorting options.

**Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sortBy` (optional): Field to sort by (default: "startTime")
- `sortDir` (optional): Sort direction - "asc" or "desc" (default: "desc")

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/browse/results?page=0&size=50" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Get Execution Detail

**Endpoint:** `GET /database/executions/{id}`

**Description:** Returns detailed information about a specific test execution including all results.

**Parameters:**
- `id` (path parameter): Execution ID (database primary key)

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/executions/123" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Get Result Detail

**Endpoint:** `GET /database/results/{id}`

**Description:** Returns detailed information about a specific test result including steps, attachments, and metrics.

**Parameters:**
- `id` (path parameter): Result ID (database primary key)

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/results/456" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

### Delete Execution

**Endpoint:** `DELETE /database/executions/{id}`

**Description:** Deletes a test execution and all associated results, steps, attachments, and metrics.

**Parameters:**
- `id` (path parameter): Execution ID (database primary key)

**Response:**
```json
{
  "success": true,
  "message": "Execution deleted successfully",
  "timestamp": "2025-12-14T12:00:00"
}
```

**Example:**
```bash
curl -X DELETE "http://localhost:8080/api/v1/database/executions/123" \
  -H "Authorization: Bearer <accessToken>"
```

### Delete Result

**Endpoint:** `DELETE /database/results/{id}`

**Description:** Deletes a test result and all associated steps, attachments, and metrics.

**Parameters:**
- `id` (path parameter): Result ID (database primary key)

**Response:**
```json
{
  "success": true,
  "message": "Result deleted successfully",
  "timestamp": "2025-12-14T12:00:00"
}
```

**Example:**
```bash
curl -X DELETE "http://localhost:8080/api/v1/database/results/456" \
  -H "Authorization: Bearer <accessToken>"
```

### Database Statistics

**Endpoint:** `GET /database/statistics`

**Description:** Returns overall database statistics including total counts for executions, results, steps, attachments, and metrics.

**Example:**
```bash
curl -s "http://localhost:8080/api/v1/database/statistics" \
  -H "Authorization: Bearer <accessToken>" | jq .
```

**Response:**
```json
{
  "totalExecutions": 150,
  "totalResults": 1250,
  "totalSteps": 5000,
  "totalAttachments": 300,
  "totalMetrics": 2500,
  "lastUpdated": "2025-12-14T12:00:00"
}
```

## Internal Playwright APIs

These APIs are for internal use and require internal API authentication. They provide browser automation capabilities for test execution.

### Create Browser Session

**Endpoint:** `POST /internal/playwright/sessions`

**Description:** Creates a new browser page session for automation.

**Headers:**
- `Authorization: Bearer <internal-api-token>`

**Request Body:**
```json
{
  "browserType": "chromium",
  "headless": true,
  "recordVideo": false,
  "viewportWidth": 1280,
  "viewportHeight": 720
}
```

**Response:**
```json
{
  "sessionId": "uuid-here",
  "success": true
}
```

### Navigate

**Endpoint:** `POST /internal/playwright/sessions/{sessionId}/navigate`

**Description:** Navigates the browser session to a URL.

**Request Body:**
```json
{
  "url": "https://example.com"
}
```

### Fill Input Field

**Endpoint:** `POST /internal/playwright/sessions/{sessionId}/fill`

**Description:** Fills an input field with a value.

**Request Body:**
```json
{
  "selector": "#username",
  "value": "testuser"
}
```

### Click Element

**Endpoint:** `POST /internal/playwright/sessions/{sessionId}/click`

**Description:** Clicks an element on the page.

**Request Body:**
```json
{
  "selector": "button.submit"
}
```

### Wait for Selector

**Endpoint:** `POST /internal/playwright/sessions/{sessionId}/wait-for-selector`

**Description:** Waits for a selector to be visible on the page.

**Request Body:**
```json
{
  "selector": "#content",
  "timeout": 30000
}
```

### Wait for URL

**Endpoint:** `POST /internal/playwright/sessions/{sessionId}/wait-for-url`

**Description:** Waits for the page URL to match a pattern.

**Request Body:**
```json
{
  "urlPattern": "/dashboard",
  "timeout": 30000
}
```

### Get Current URL

**Endpoint:** `GET /internal/playwright/sessions/{sessionId}/url`

**Description:** Returns the current URL of the page.

**Response:**
```json
{
  "success": true,
  "currentUrl": "https://example.com/page"
}
```

### Evaluate JavaScript

**Endpoint:** `POST /internal/playwright/sessions/{sessionId}/evaluate`

**Description:** Executes JavaScript in the browser context.

**Request Body:**
```json
{
  "script": "document.title"
}
```

**Response:**
```json
{
  "success": true,
  "result": "Page Title"
}
```

### Close Browser Session

**Endpoint:** `DELETE /internal/playwright/sessions/{sessionId}`

**Description:** Closes a browser session and releases resources.

### Create API Request Session

**Endpoint:** `POST /internal/playwright/api-sessions`

**Description:** Creates a new API request context session for API testing.

**Request Body:**
```json
{
  "baseUrl": "https://api.example.com"
}
```

### Close API Request Session

**Endpoint:** `DELETE /internal/playwright/api-sessions/{sessionId}`

**Description:** Closes an API request context session.

**Note:** Internal Playwright APIs require internal API authentication. Use the internal API token obtained from `/api/v1/internal/auth/token`.

## Test Discovery Behavior

The TestDiscoveryService discovers tests by scanning the test source files. If test files are not available or the test path is misconfigured, the service will return empty lists rather than mock data. This ensures that configuration issues are immediately visible and not masked by fake data.

**Important:** Always mount the `atas-tests` directory in the Docker container and configure the `atas.test.base.path` system property correctly to enable test discovery.
