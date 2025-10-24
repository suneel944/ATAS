# GitHub Workflows Documentation

This document describes the GitHub Actions workflows used in the ATAS project.

## Workflow Overview

| Workflow | Purpose | Trigger | Status |
|----------|---------|---------|--------|
| [PR Checks](.github/workflows/pr-checks.yml) | Quality gates for pull requests | PR to main/master | ![PR Checks](https://github.com/suneel/ATAS/workflows/PR%20Checks/badge.svg) |
| [Test Suites](.github/workflows/test-suites.yml) | Comprehensive test execution | Manual dispatch, PR | ![Test Suites](https://github.com/suneel/ATAS/workflows/Test%20Suites/badge.svg) |
| [Build and Push](.github/workflows/build-and-push.yml) | Build and deploy to GHCR | Push to main/master | ![Build and Push](https://github.com/suneel/ATAS/workflows/Build%20and%20Push%20to%20GHCR/badge.svg) |
| [Release](.github/workflows/release.yml) | Create releases and artifacts | Tag push, manual | ![Release](https://github.com/suneel/ATAS/workflows/Release/badge.svg) |

## Workflow Details

### PR Checks (`pr-checks.yml`)

**Purpose**: Comprehensive quality gates for pull requests with intelligent job execution based on file changes.

**Features**:
- **Smart Execution**: Only runs relevant jobs based on changed files
- **Matrix Strategy**: Parallel execution of unit tests across modules
- **Comprehensive Coverage**: Code quality, unit tests, integration tests, Docker builds
- **Enhanced Reporting**: Detailed PR summaries with change detection

**Jobs**:
- `changes`: Detects which parts of the codebase changed
- `code-quality`: SpotBugs and Checkstyle analysis
- `unit-tests`: Matrix execution across `atas-framework` and `atas-tests` modules
- `integration-tests`: Database integration tests with PostgreSQL
- `docker-build-test`: Docker image build and health check
- `pr-summary`: Comprehensive status summary

**Manual Inputs**:
- `skip-tests`: Skip test execution
- `run-full-suite`: Force execution of all checks regardless of changes

### Test Suites (`test-suites.yml`)

**Purpose**: Comprehensive test execution with browser matrix support and environment flexibility.

**Features**:
- **Browser Matrix**: Parallel execution across Chromium, Firefox, and WebKit
- **Environment Support**: Test, staging, and production environments
- **Reusable Components**: Uses `_test-matrix.yml` for consistent execution
- **Enhanced Reporting**: Detailed test results with artifact management

**Manual Inputs**:
- `test-suite`: Choose specific test suite (all, authentication-ui, authentication-api, monitoring-ui, monitoring-api)
- `environment`: Target environment (test, staging, production)
- `browser-matrix`: Custom browser selection
- `parallel-jobs`: Number of parallel execution jobs

### Build and Push (`build-and-push.yml`)

**Purpose**: Build and push Docker images to GitHub Container Registry.

**Features**:
- **Multi-arch Support**: Builds for multiple architectures
- **SBOM Generation**: Software Bill of Materials for transparency
- **Smart Tagging**: Branch-based and SHA-based tagging
- **Cache Optimization**: GitHub Actions cache for faster builds

### Release (`release.yml`)

**Purpose**: Automated release creation with comprehensive artifact management.

**Features**:
- **Multi-format Artifacts**: JAR files, Docker images, SBOMs
- **GitHub Integration**: Automated release creation with assets
- **Version Management**: Tag-based and manual version specification

## Reusable Workflows

### Common Setup (`_common-setup.yml`)

**Purpose**: Shared setup steps for Java/Maven projects.

**Features**:
- **Configurable Java Version**: Support for different JDK versions
- **Maven Caching**: Optimized dependency caching
- **Flexible Goals**: Configurable Maven execution goals

### Test Matrix (`_test-matrix.yml`)

**Purpose**: Reusable test execution with browser matrix support.

**Features**:
- **Browser Matrix**: Parallel execution across multiple browsers
- **Environment Support**: Configurable test environments
- **Service Integration**: PostgreSQL service for integration tests
- **Artifact Management**: Comprehensive test result collection

## Best Practices

### 1. **Conditional Execution**
- Jobs only run when relevant files are changed
- Reduces CI/CD costs and execution time
- Improves developer experience

### 2. **Matrix Strategies**
- Parallel execution across modules and browsers
- Faster feedback loops
- Better resource utilization

### 3. **Transparency**
- SBOM generation for supply chain transparency

### 4. **Comprehensive Reporting**
- Detailed workflow summaries
- Artifact management with retention policies
- PR integration with status reporting

### 5. **Cache Optimization**
- Maven dependency caching
- Docker layer caching
- GitHub Actions cache utilization

## Usage Examples

### Running Full Test Suite
```bash
# Via GitHub CLI
gh workflow run "Test Suites" -f test-suite=all -f environment=test

# Via GitHub UI
# Go to Actions → Test Suites → Run workflow
```

### Creating a Release
```bash
# Via GitHub CLI
gh workflow run "Release" -f version=v1.2.0 -f release-notes="Bug fixes and improvements"

# Via GitHub UI
# Go to Actions → Release → Run workflow
```


## Monitoring and Maintenance

### Workflow Status
- Monitor workflow success rates
- Review failed runs promptly
- Update dependencies regularly

### Performance Optimization
- Review execution times
- Optimize cache hit rates
- Consider parallel job limits

### Maintenance
- Update base images regularly

## Troubleshooting

### Common Issues

1. **Cache Misses**: Clear caches if builds are slow
2. **Test Failures**: Check environment-specific configurations
3. **Docker Build Issues**: Verify Dockerfile and context paths

### Debug Mode
Enable debug logging by adding `ACTIONS_STEP_DEBUG: true` to repository secrets.

## Contributing

When adding new workflows:
1. Follow the established naming conventions
2. Include comprehensive documentation
3. Add appropriate status badges
4. Test thoroughly before merging
5. Update this README with new workflow information
