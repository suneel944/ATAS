# Development Guide

This guide provides detailed instructions for setting up and working with the ATAS development environment.

## 🚀 Quick Setup

### Prerequisites

- **Java 21** (LTS) - [Install via SDKMAN](https://sdkman.io/)
- **Maven 3.9+** - [Install via SDKMAN](https://sdkman.io/)
- **Docker & Docker Compose** - [Install Docker](https://docs.docker.com/get-docker/)
- **Git** - [Install Git](https://git-scm.com/downloads)

### One-Command Setup

```bash
# Clone and setup
git clone https://github.com/YOUR_USERNAME/ATAS.git
cd ATAS
./mvnw clean compile
```

## 🔧 Development Environment

### Git Configuration

The project uses:

- **Commit message template** (`.gitmessage`) for consistent formatting
- **GitHub Actions** for automated validation
- **Maven plugins** for code quality (Checkstyle, SpotBugs)
- **Conventional Commits** format for commit messages

### IDE Setup

#### IntelliJ IDEA

1. **Import project** as Maven project
2. **Configure Java SDK** to Java 21
3. **Install plugins**:
   - Lombok Plugin
   - Spring Boot Plugin
   - Docker Plugin
4. **Configure code style**:
   - File → Settings → Editor → Code Style → Java
   - Import from `.idea/codeStyles/Project.xml` (if available)

#### VS Code

1. **Install extensions**:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Docker Extension
   - GitLens
2. **Configure settings** in `.vscode/settings.json`

#### Eclipse

1. **Import as Maven project**
2. **Configure Java Build Path** to Java 21
3. **Install Spring Tools** plugin

### Environment Variables

Create `.env` file for local development:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/atasdb
SPRING_DATASOURCE_USERNAME=atas
SPRING_DATASOURCE_PASSWORD=ataspass

# AWS S3 (optional)
ATAS_STORAGE_BUCKET=your-bucket-name
ATAS_STORAGE_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key

# Development flags
ATAS_SKIP_QUALITY_CHECKS=false
ATAS_DEBUG_MODE=true
```

## 🏗️ Project Structure

```
ATAS/
├── atas-framework/          # Core Spring Boot service
│   ├── src/main/java/      # Framework source code
│   ├── src/main/resources/ # Configuration files
│   └── src/test/java/      # Framework tests
├── atas-tests/             # Test implementations
│   └── src/test/java/      # UI/API tests
├── docker/                 # Docker configuration
├── scripts/                # Development scripts
├── .github/                # GitHub workflows
└── docs/                   # Documentation
```

## 🧪 Testing

### Running Tests

```bash
# All tests
./mvnw test

# Specific module
./mvnw -pl atas-tests test

# Specific test class
./mvnw test -Dtest=LoginUiTest

# With coverage
./mvnw test jacoco:report
```

### Test Categories

- **Unit Tests**: `**/*Test.java`
- **Integration Tests**: `**/*IT.java`
- **UI Tests**: `**/*UiTest.java`
- **API Tests**: `**/*ApiTest.java`

### Test Data

- **Test HTML files**: `atas-tests/src/test/resources/`
- **Test configurations**: `application-test.yml`
- **Test data builders**: `**/data/*TestData.java`

## 🐳 Docker Development

### Local Development

```bash
# Start services
docker-compose -f docker/docker-compose-local-db.yml up -d

# View logs
docker-compose -f docker/docker-compose-local-db.yml logs -f

# Stop services
docker-compose -f docker/docker-compose-local-db.yml down
```

### Service URLs

- **ATAS Framework**: http://localhost:8080
- **PostgreSQL**: localhost:5433
- **Health Check**: http://localhost:8080/actuator/health

## 🔍 Code Quality

### Static Analysis

```bash
# SpotBugs (bug detection)
./mvnw spotbugs:check

# Checkstyle (code style)
./mvnw checkstyle:check

# OWASP dependency check
./mvnw org.owasp:dependency-check-maven:check
```

### Code Coverage

```bash
# Generate coverage report
./mvnw test jacoco:report

# View report
open atas-tests/target/site/jacoco/index.html
```

## 📝 Commit Workflow

### 1. Create Feature Branch

```bash
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Write code following [Code Style](#code-style)
- Add tests for new functionality
- Update documentation if needed

### 3. Commit Changes

```bash
# Stage changes
git add .

# Commit with conventional format
git commit -m "feat(auth): add OAuth2 login support"
```

### 4. Push and Create PR

```bash
git push origin feature/your-feature-name
# Create PR via GitHub UI
```

## 🎨 Code Style

### Java Style Guide

- **Google Java Style Guide** compliance
- **4 spaces** for indentation
- **120 character** line limit
- **Meaningful names** for variables and methods
- **Javadoc** for public APIs

### Example

```java
/**
 * Service for managing test executions with real-time monitoring.
 * 
 * @author Your Name
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestExecutionService {
    
    private final TestExecutionRepository executionRepository;
    
    /**
     * Starts a new test execution with the given configuration.
     *
     * @param config the test execution configuration
     * @return the created test execution
     * @throws IllegalArgumentException if config is invalid
     */
    public TestExecution startExecution(TestExecutionConfig config) {
        log.info("Starting test execution with config: {}", config);
        // Implementation
    }
}
```

### Test Style

```java
@Test
@DisplayName("Should successfully login with valid credentials")
void loginWithValidCredentialsShouldSucceed() {
    // Given
    LoginPage loginPage = new LoginPage(page);
    
    // When
    DashboardPage dashboard = loginPage.navigate()
        .enterUsername("admin@test.com")
        .enterPassword("password123")
        .clickLogin();
    
    // Then
    assertThat(dashboard.isLoaded()).isTrue();
    assertThat(dashboard.getWelcomeMessage()).contains("Welcome");
}
```

## 🔧 Debugging

### Local Debugging

```bash
# Run with debug port
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Attach debugger to port 5005
```

### Test Debugging

```bash
# Run specific test with debug
./mvnw test -Dtest=LoginUiTest -Dmaven.surefire.debug
```

### Docker Debugging

```bash
# View container logs
docker logs atas-service

# Execute commands in container
docker exec -it atas-service bash

# View database
docker exec -it atas-db psql -U atas -d atasdb
```

## 📊 Monitoring

### Application Metrics

- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Info**: http://localhost:8080/actuator/info

### Test Execution Monitoring

```bash
# Check test execution status
curl http://localhost:8080/api/v1/test-execution/status?executionId=your-id

# Live updates (SSE)
curl http://localhost:8080/api/v1/test-execution/live?executionId=your-id
```

## 🚀 Performance

### Build Optimization

```bash
# Parallel build
./mvnw -T 1C clean compile

# Skip tests for faster build
./mvnw clean compile -DskipTests

# Offline mode
./mvnw dependency:go-offline
```

### Test Optimization

```bash
# Run tests in parallel
./mvnw test -T 1C

# Skip slow tests
./mvnw test -Dtest="!**/*IT"
```

## 🐛 Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| `mvnw: Permission denied` | `chmod +x mvnw` |
| `Port 8080 already in use` | Change `server.port` in `application.yml` |
| `Chrome not found (Playwright)` | Run any test once to download browsers |
| `Connection refused to DB` | Ensure PostgreSQL is running |
| `Maven build fails` | Check Java version (should be 21) |

### Debug Commands

```bash
# Check Java version
java -version

# Check Maven version
./mvnw -version

# Check Docker
docker --version
docker-compose --version

# Check git hooks
ls -la .git/hooks/

# Test commit message
echo "feat(test): add new test" | .git/hooks/commit-msg
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Playwright Java Documentation](https://playwright.dev/java/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Allure Framework](https://docs.qameta.io/allure/)
- [Conventional Commits](https://www.conventionalcommits.org/)

## 🤝 Getting Help

- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For questions and general discussion
- **Pull Requests**: For code contributions
- **Documentation**: Check existing docs first

Happy coding! 🚀
