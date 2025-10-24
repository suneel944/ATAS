# ATAS Test Structure Documentation

## Overview

The ATAS (Advanced Testing As A Service) project has been reorganized to follow a feature-based test structure with proper separation of UI and API tests, comprehensive test suites, and full containerization support.

## Test Organization

### Feature-Based Structure

```
atas-tests/src/test/java/com/atas/
├── features/                    # Feature-specific test implementations
│   ├── authentication/
│   │   ├── api/                # Authentication API tests
│   │   │   └── LoginApiTest.java
│   │   ├── ui/                 # Authentication UI tests
│   │   │   ├── LoginUiTest.java
│   │   │   └── LoginValidationUiTest.java
│   │   ├── config/             # Feature-specific configuration
│   │   ├── data/               # Test data for the feature
│   │   └── pages/              # Page Object Model classes
│   └── monitoring/
│       ├── api/                # Monitoring API tests
│       │   ├── TestExecutionStatusApiTest.java
│       │   └── TestMonitoringApiTest.java
│       ├── ui/                 # Monitoring UI tests
│       │   └── MonitoringDashboardUiTest.java
│       ├── config/             # Feature-specific configuration
│       └── data/               # Test data for the feature
├── suites/                     # Test suite organization
│   ├── authentication/
│   │   ├── ui/
│   │   │   └── AuthenticationUiTestSuite.java
│   │   └── api/
│   │       └── AuthenticationApiTestSuite.java
│   ├── monitoring/
│   │   ├── ui/
│   │   │   └── MonitoringUiTestSuite.java
│   │   └── api/
│   │       └── MonitoringApiTestSuite.java
│   └── AllTestSuites.java      # Master suite for all tests
└── shared/                     # Shared utilities and base classes
    ├── pages/
    └── utils/
```

### Test Suites

Each feature has dedicated test suites for UI and API tests:

- **AuthenticationUiTestSuite**: Groups all authentication UI tests
- **AuthenticationApiTestSuite**: Groups all authentication API tests
- **MonitoringUiTestSuite**: Groups all monitoring UI tests
- **MonitoringApiTestSuite**: Groups all monitoring API tests
- **AllTestSuites**: Master suite that includes all feature test suites

## Running Tests

### Individual Test Suites

```bash
# Run all tests
mvn test -Dtest="com.atas.suites.AllTestSuites"

# Run authentication UI tests only
mvn test -Dtest="com.atas.suites.authentication.ui.AuthenticationUiTestSuite"

# Run authentication API tests only
mvn test -Dtest="com.atas.suites.authentication.api.AuthenticationApiTestSuite"

# Run monitoring UI tests only
mvn test -Dtest="com.atas.suites.monitoring.ui.MonitoringUiTestSuite"

# Run monitoring API tests only
mvn test -Dtest="com.atas.suites.monitoring.api.MonitoringApiTestSuite"
```

### Feature-Specific Tests

```bash
# Run all authentication tests (UI + API)
mvn test -Dtest="com.atas.features.authentication.**"

# Run all monitoring tests (UI + API)
mvn test -Dtest="com.atas.features.monitoring.**"
```

## Docker Support

### Docker Compose

The project includes comprehensive Docker support:

- **docker-compose.yml**: Main compose file with database and service
- **docker-compose.override.yml**: Development overrides
- **docker-compose-local-db.yml**: Local database setup
- **docker-compose-system-db.yml**: System database setup

### Running with Docker

```bash
# Start the full stack (database + service)
docker-compose up -d

# Start only the database
docker-compose up -d atas-db

# Build and start the service
docker-compose up --build atas-service

# View logs
docker-compose logs -f atas-service

# Stop all services
docker-compose down
```

### Docker Development

```bash
# Enable debug mode (port 5005 exposed)
docker-compose -f docker-compose.yml -f docker-compose.override.yml up

# Run tests in container
docker-compose exec atas-service mvn test
```

## GitHub Actions

### CI/CD Workflows

1. **PR Checks** (`.github/workflows/pr-checks.yml`):
   - Code quality checks (SpotBugs, CheckStyle)
   - Unit tests
   - Integration tests
   - Security scans (OWASP dependency check)
   - Docker build and test

2. **Build and Push** (`.github/workflows/build-and-push.yml`):
   - Builds and pushes Docker images to GHCR on merge to main/master
   - Includes security scanning and SBOM generation
   - Multi-platform support

3. **Test Suites** (`.github/workflows/test-suites.yml`):
   - Manual workflow for running specific test suites
   - Supports different environments (test, staging, production)
   - Generates Allure reports

### Workflow Triggers

- **PR Checks**: Automatically runs on pull requests to main/master
- **Build and Push**: Runs on push to main/master branches
- **Test Suites**: Manual trigger with configurable options

## Best Practices

### Adding New Features

1. Create feature directory under `features/`
2. Add both `ui/` and `api/` subdirectories
3. Create corresponding test suite classes
4. Update `AllTestSuites.java` to include new suites
5. Add feature-specific configuration and test data

### Test Organization

- **UI Tests**: Use Page Object Model pattern
- **API Tests**: Use dedicated API client classes
- **Test Data**: Centralize in feature-specific data classes
- **Configuration**: Feature-specific config classes

### Docker Development

- Use `docker-compose.override.yml` for development-specific settings
- Mount source code for hot reload during development
- Use health checks for proper service dependencies
- Leverage Docker layer caching for faster builds

## Environment Configuration

### Profiles

- **test**: Default test environment
- **docker**: Docker-specific configuration
- **debug**: Development debugging mode

### Environment Variables

- `SPRING_DATASOURCE_URL`: Database connection URL
- `ATAS_STORAGE_BUCKET`: S3 bucket for test artifacts
- `ATAS_STORAGE_REGION`: AWS region for storage
- `SPRING_PROFILES_ACTIVE`: Active Spring profiles

## Monitoring and Reporting

### Allure Reports

Test results are automatically generated as Allure reports:

```bash
# Generate Allure report
mvn allure:report

# Serve Allure report locally
mvn allure:serve
```

### Test Artifacts

- Test execution videos and screenshots
- Allure test reports
- Security scan results
- Docker build logs

## Security

### Security Scanning

- **Trivy**: Container vulnerability scanning
- **OWASP Dependency Check**: Dependency vulnerability scanning
- **SARIF**: Security results in standardized format

### Secrets Management

- Use GitHub Secrets for sensitive data
- Never commit secrets to repository
- Use environment variables for configuration
