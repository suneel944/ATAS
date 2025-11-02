# GitHub Workflows

This directory contains GitHub Actions workflows for the ATAS project, including CI/CD pipelines, testing, and deployment automation.

## Workflow Overview

### Core Workflows

- **`pr-checks.yml`** - Main PR validation workflow
- **`test-layers.yml`** - New test layer structure (unit, integration, production, version consistency)
- **`test-suites.yml`** - Legacy test suite execution
- **`_test-matrix.yml`** - Reusable test matrix configuration
- **`build-and-push.yml`** - Docker image building and pushing
- **`release.yml`** - Release automation

## Test Architecture

The project uses a multi-layer testing approach:

### 1. Unit Tests (H2-based)
- **Location**: `atas-framework/src/test/java/`
- **Database**: H2 in-memory database
- **Profile**: `unit-test`
- **Purpose**: Fast, isolated tests for individual components
- **Command**: `mvn test -pl atas-framework -Dtest="**/*Test" -Dspring.profiles.active=unit-test -Dtest="!**/*IntegrationTest"`

### 2. Integration Tests (PostgreSQL with Testcontainers)
- **Location**: `atas-framework/src/test/java/com/atas/framework/integration/`
- **Database**: PostgreSQL via Testcontainers
- **Profile**: `integration-test`
- **Purpose**: Test component interactions with real database
- **Command**: `mvn test -pl atas-framework -Dtest="**/*IntegrationTest" -Dspring.profiles.active=integration-test`

### 3. Version Consistency Tests
- **Location**: `atas-framework/src/test/java/com/atas/framework/integration/VersionConsistencyTest.java`
- **Purpose**: Ensure version consistency between Docker Compose and integration tests
- **Command**: `mvn test -pl atas-framework -Dtest="**/*VersionConsistencyTest"`

## Workflow Triggers

### PR Checks (`pr-checks.yml`)
- **Trigger**: Pull requests to `main`/`master` branches
- **Features**:
  - Change detection (framework, tests, docker, workflows)
  - Code quality checks
  - Build framework
  - Run all test layers
  - Generate comprehensive PR summary

### Test Layers (`test-layers.yml`)
- **Trigger**: Pull requests or manual dispatch
- **Features**:
  - Selective test layer execution
  - Environment-specific testing
  - Comprehensive test reporting
  - Artifact collection

### Test Suites (`test-suites.yml`)
- **Trigger**: Pull requests or manual dispatch
- **Features**:
  - Legacy test suite support
  - Browser matrix testing
  - Allure report generation
  - PR commenting

## Environment Configuration

### Test Profiles

#### Unit Tests (`application-unit-test.yml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:atas_unit_test_db;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

#### Integration Tests (`application-integration-test.yml`)
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:18:///atas_integration_test_db
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

#### Test Environment Profiles

Tests use environment-specific profiles:
- `application-dev.yml` - Development test settings (default)
- `application-stage.yml` - Staging test settings
- `application-prod.yml` - Production test settings

Tests are environment-agnostic and use environment variables:
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/atas_test}
    username: ${DB_USERNAME:atas}
    password: ${DB_PASSWORD:ataspass}
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## Database Versions

- **Docker Compose**: PostgreSQL 18
- **Integration Tests**: PostgreSQL 18 (via Testcontainers)
- **Production Tests**: PostgreSQL 18
- **Version Consistency**: Automatically validated

## Running Tests Locally

### All Tests
```bash
# Run all test layers
mvn test -pl atas-framework,atas-tests

# Run with specific profiles
mvn test -pl atas-framework -Dspring.profiles.active=unit-test
mvn test -pl atas-framework -Dspring.profiles.active=integration-test
mvn test -pl atas-tests -Dspring.profiles.active=dev    # Development (default)
mvn test -pl atas-tests -Dspring.profiles.active=stage  # Staging
mvn test -pl atas-tests -Dspring.profiles.active=prod   # Production
```

### Specific Test Types
```bash
# Unit tests only
mvn test -pl atas-framework -Dtest="**/*Test" -Dtest="!**/*IntegrationTest"

# Integration tests only
mvn test -pl atas-framework -Dtest="**/*IntegrationTest"

# Version consistency tests
mvn test -pl atas-framework -Dtest="**/*VersionConsistencyTest"
```

## Artifacts

The workflows generate and upload the following artifacts:

- **Test Reports**: Surefire reports for each test layer
- **Coverage Reports**: JaCoCo coverage reports
- **Allure Reports**: Detailed test execution reports
- **Test Results**: XML test result files

## Monitoring and Notifications

- **PR Comments**: Automatic test result comments on pull requests
- **Workflow Summaries**: Detailed execution summaries
- **Artifact Retention**: 7-30 days depending on artifact type
- **Status Checks**: Required for PR merging

## Troubleshooting

### Common Issues

1. **Test Failures**: Check the specific test layer logs
2. **Database Connection**: 
   - For integration tests: Testcontainers handles PostgreSQL automatically
   - For local testing: Ensure PostgreSQL is running or use Testcontainers
   - Check environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
3. **Version Mismatches**: Run version consistency tests to detect issues
4. **Dependency Issues**: Clear Maven cache and rebuild
5. **Profile Issues**: Ensure correct Spring profile is set (`dev`, `stage`, or `prod`)

### Debug Commands

```bash
# Clean and rebuild
make clean
make install  # Installs to local Maven repository
# Or for quick compilation:
make compile  # Compiles only

# Run with debug logging
mvn test -Dlogging.level.com.atas=DEBUG

# Check test profiles
mvn test -X -Dspring.profiles.active=unit-test
```

## Contributing

When adding new tests:

1. **Unit Tests**: Add to `atas-framework/src/test/java/`
2. **Integration Tests**: Add to `atas-framework/src/test/java/com/atas/framework/integration/`
3. **Product Tests**: Add to `atas-tests/src/test/java/com/atas/products/{product-name}/features/`
   - **UI Tests**: Add to `features/{feature-name}/ui/`
   - **API Tests**: Add to `features/{feature-name}/api/`
   - **Page Objects**: Add to `products/{product-name}/pages/`
4. **Test Structure**:
   - Use `TestTags` for categorization (e.g., `@Tag(TestTags.UI)`, `@Tag(TestTags.SMOKE)`)
   - Use `TestConfiguration` for environment-agnostic configuration
   - Use `ApiTestHooks` or `UiTestHooks` for test lifecycle management
5. **Update Workflows**: Modify relevant workflow files if needed
6. **Test Locally**: Ensure tests pass before creating PR

## Test Organization

The test layer uses a product-based structure:

```
atas-tests/src/test/java/com/atas/
├── config/
│   └── TestConfiguration.java          # Environment-agnostic test config
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
    └── utils/
        └── TestUtils.java             # Common test utilities
```

## Best Practices

- Use appropriate test layers for different scenarios
- Keep unit tests fast and isolated
- Use integration tests for component interactions
- Use product-based organization for test structure
- Tag tests appropriately using `TestTags` constants
- Use environment-agnostic configuration via `TestConfiguration`
- Maintain version consistency across environments
- Document test requirements and setup