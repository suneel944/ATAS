# Contributing to ATAS

Thank you for your interest in contributing to the Advanced Testing As A Service (ATAS) project! This document provides guidelines for contributing code, documentation, and other improvements.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [GitHub Workflows](#github-workflows)
- [Development Workflow](#development-workflow)
- [Code Style](#code-style)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)

## 🤝 Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you agree to uphold this code.

## 🚀 Getting Started

### Prerequisites

- **Java 21** (LTS)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **Git**

### Setup

1. **Fork the repository**
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/ATAS.git
   cd ATAS
   ```

3. **Set up upstream remote**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/ATAS.git
   ```

4. **Setup and build**:
   ```bash
   make setup    # Initial setup
   make build    # Build project
   ```

5. **Run tests**:
   ```bash
   make test     # Run all tests
   ```

## 🛠️ Makefile Commands

The project includes a comprehensive Makefile with easy-to-remember commands:

### Essential Commands
```bash
make help       # Show all available commands
make setup      # Initial project setup
make build      # Build the project
make test       # Run all tests
make dev        # Start development environment
make clean      # Clean build artifacts
```

### Development Commands
```bash
make test-ui    # Run UI tests only
make test-api   # Run API tests only
make lint       # Run code quality checks
make security   # Run security checks
make report     # Generate test reports
```

### Docker Commands
```bash
make docker-up     # Start Docker services
make docker-down   # Stop Docker services
make docker-logs   # Show service logs
make docker-build  # Build Docker images
```

### Git Commands
```bash
make commit MESSAGE="feat: add new feature"  # Commit with message
make branch NAME=feature/new-feature         # Create new branch
make push                                     # Push changes
make status                                   # Show git status
```

### Quality Assurance
```bash
make check-all   # Run all checks (build, test, lint, security)
make pr-check    # Run PR checks locally
make ci          # Run CI pipeline locally
```

For a complete list of commands, run `make help`.

## 📝 Commit Guidelines

### Commit Message Format

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Commit Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | A new feature | `feat(auth): add OAuth2 login support` |
| `fix` | A bug fix | `fix(ui): resolve login button click issue` |
| `docs` | Documentation changes | `docs: update API documentation` |
| `style` | Code style changes (formatting, etc.) | `style: fix code formatting issues` |
| `refactor` | Code refactoring without feature changes | `refactor(api): simplify test execution logic` |
| `perf` | Performance improvements | `perf(db): optimize test result queries` |
| `test` | Adding or updating tests | `test(ui): add login validation tests` |
| `chore` | Maintenance tasks, dependencies | `chore: update Maven dependencies` |
| `ci` | CI/CD pipeline changes | `ci: add security scanning workflow` |
| `build` | Build system changes | `build: update Docker configuration` |
| `revert` | Revert previous commits | `revert: revert "feat: add new feature"` |

### Scopes

Use scopes to indicate the area of the codebase affected:

- `framework` - Core framework changes
- `tests` - Test implementations
- `ui` - UI-related changes
- `api` - API-related changes
- `auth` - Authentication features
- `monitoring` - Monitoring features
- `docker` - Docker/containerization
- `ci` - CI/CD workflows
- `docs` - Documentation

### Examples

#### ✅ Good Commit Messages

```bash
feat(auth): add multi-factor authentication support
fix(ui): resolve login page responsive layout issues
docs(api): update test execution endpoint documentation
test(monitoring): add dashboard UI test coverage
ci: implement smart workflow execution based on file changes
refactor(framework): extract common test execution logic
chore: update Spring Boot to version 3.5.6
```

#### ❌ Bad Commit Messages

```bash
fix stuff
update code
WIP
changes
bug fix
new feature
```

### Commit Body

For complex changes, provide a detailed explanation:

```bash
feat(monitoring): add real-time test execution dashboard

- Implement WebSocket connection for live updates
- Add test execution status indicators
- Include performance metrics visualization
- Support multiple concurrent test sessions

Closes #123
```

### Breaking Changes

Use `!` after the type/scope to indicate breaking changes:

```bash
feat(api)!: change test execution response format

BREAKING CHANGE: The test execution API now returns results in a different format.
Update your client code to handle the new response structure.
```

## 🔄 Pull Request Process

### Before Submitting

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

2. **Make your changes** following the [Code Style](#code-style) guidelines

3. **Write tests** for new functionality

4. **Update documentation** if needed

5. **Run the full test suite**:
   ```bash
   make test     # Run all tests
   ```

6. **Check code quality**:
   ```bash
   make lint     # Run code quality checks
   make pr-check # Run all PR checks locally
   ```

### PR Guidelines

1. **Use descriptive titles** that follow commit message conventions
2. **Provide detailed descriptions** explaining what and why
3. **Link related issues** using `Closes #123` or `Fixes #456`
4. **Include screenshots** for UI changes
5. **Update documentation** if your changes affect user-facing features

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)

## Related Issues
Closes #123
```

## 🔧 GitHub Workflows

Our CI/CD pipeline includes several automated checks. Ensure your changes pass all workflows:

### Workflow Overview

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| **PR Checks** | Pull Request | Quality gates with smart execution |
| **Test Suites** | PR/Manual | Comprehensive test execution |
| **Security Scan** | Daily/Manual | Vulnerability scanning |
| **Build & Push** | Push to main | Build and deploy to GHCR |
| **Release** | Tag/Manual | Create releases and artifacts |

### Smart Execution

The PR Checks workflow uses intelligent execution based on file changes:

- **Framework changes** → Run code quality, unit tests, integration tests, security scan
- **Test changes** → Run code quality, unit tests, security scan
- **Docker changes** → Run Docker build test
- **Documentation changes** → Skip most checks (faster feedback)

### Workflow Requirements

Your PR must pass:

1. **Code Quality** (SpotBugs, Checkstyle)
2. **Unit Tests** (Matrix execution across modules)
3. **Integration Tests** (Database integration)
4. **Security Scan** (OWASP dependency check)
5. **Docker Build** (If framework/Docker changes)

### Manual Workflow Execution

You can manually trigger workflows with custom parameters:

```bash
# Run full test suite
gh workflow run "Test Suites" -f test-suite=all -f environment=test

# Run specific test suite
gh workflow run "Test Suites" -f test-suite=authentication-ui -f browser-matrix=chromium

# Security scan
gh workflow run "Security Scan" -f scan-type=all -f severity-threshold=HIGH
```

## 🛠 Development Workflow

### Branch Naming

Use descriptive branch names:

```bash
feature/auth-oauth2-support
fix/login-button-responsive
docs/api-documentation-update
refactor/test-execution-service
chore/update-dependencies
```

### Development Process

1. **Start from main**:
   ```bash
   git checkout main
   git pull upstream main
   ```

2. **Create feature branch**:
   ```bash
   git checkout -b feature/your-feature
   ```

3. **Make incremental commits** with clear messages

4. **Keep commits focused** - one logical change per commit

5. **Rebase before PR**:
   ```bash
   git rebase main
   ```

6. **Push and create PR**:
   ```bash
   git push origin feature/your-feature
   ```

### Code Review Process

1. **Self-review** your changes before requesting review
2. **Request review** from maintainers
3. **Address feedback** promptly
4. **Keep PRs focused** and reasonably sized
5. **Respond to comments** constructively

## 🎨 Code Style

### Java Code Style

- Follow **Google Java Style Guide**
- Use **4 spaces** for indentation
- **Maximum line length**: 120 characters
- Use **meaningful variable names**
- Add **Javadoc** for public methods
- Use **Lombok** annotations where appropriate

### Example

```java
/**
 * Service for managing test executions with real-time monitoring.
 * Provides methods to start, stop, and track test execution progress.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestExecutionService {
    
    private final TestExecutionRepository executionRepository;
    private final TestResultRepository resultRepository;
    
    /**
     * Starts a new test execution with the given configuration.
     *
     * @param config the test execution configuration
     * @return the created test execution
     * @throws IllegalArgumentException if config is invalid
     */
    public TestExecution startExecution(TestExecutionConfig config) {
        // Implementation
    }
}
```

### Test Code Style

- Use **descriptive test method names**
- Follow **Given-When-Then** structure
- Use **AssertJ** for assertions
- Add **@DisplayName** for complex tests

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

## 🧪 Testing Guidelines

### Test Organization

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test component interactions
- **UI Tests**: Test user interface functionality
- **API Tests**: Test REST API endpoints

### Test Naming

```java
// Good
@Test
void loginWithInvalidCredentialsShouldFail() { }

@Test
void shouldReturnTestExecutionStatusWhenValidIdProvided() { }

// Bad
@Test
void test1() { }

@Test
void loginTest() { }
```

### Test Data

- Use **test data builders** for complex objects
- Create **test-specific data** in test resources
- Use **@TestPropertySource** for test-specific configuration

### Coverage Requirements

- **Minimum 80%** line coverage for new code
- **100% coverage** for critical business logic
- **Integration tests** for all public APIs

## 📚 Documentation

### Code Documentation

- **Javadoc** for all public classes and methods
- **README updates** for user-facing changes
- **API documentation** for new endpoints
- **Architecture decisions** in ADR format

### Documentation Types

1. **User Documentation**: README, setup guides
2. **Developer Documentation**: API docs, architecture
3. **Contributor Documentation**: This file, code style guides
4. **Deployment Documentation**: Docker, CI/CD guides

### Documentation Standards

- Use **clear, concise language**
- Include **code examples**
- Provide **step-by-step instructions**
- Keep **up-to-date** with code changes

## 🚨 Common Issues and Solutions

### Build Failures

```bash
# Clean and rebuild
./mvnw clean compile

# Check for dependency issues
./mvnw dependency:tree

# Run specific module
./mvnw -pl atas-framework clean compile
```

### Test Failures

```bash
# Run tests with debug output
./mvnw test -X

# Run specific test class
./mvnw test -Dtest=LoginUiTest

# Run tests in specific module
./mvnw -pl atas-tests test
```

### Workflow Failures

1. **Check workflow logs** in GitHub Actions
2. **Run locally** to reproduce issues
3. **Update dependencies** if needed
4. **Check file paths** and permissions

## 📞 Getting Help

- **GitHub Issues**: For bugs and feature requests
- **Discussions**: For questions and general discussion
- **Pull Requests**: For code contributions
- **Documentation**: Check existing docs first

## 🎉 Recognition

Contributors will be recognized in:
- **CONTRIBUTORS.md** file
- **Release notes** for significant contributions
- **GitHub contributors** page

Thank you for contributing to ATAS! 🚀
