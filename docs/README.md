# ATAS Documentation

Welcome to the ATAS (Advanced Testing As A Service) documentation. This directory contains comprehensive guides and references for using the ATAS framework.

## 📚 Documentation Index

### Core Documentation

- **[API Reference](API_REFERENCE.md)** - Complete REST API documentation with endpoints, parameters, and examples
- **[Test Execution Guide](TEST_EXECUTION_GUIDE.md)** - Step-by-step guide for executing and monitoring tests

### Quick Links

- **Getting Started:** See the main [README.md](../README.md) for project setup
- **API Endpoints:** All available endpoints are documented in [API_REFERENCE.md](API_REFERENCE.md)
- **Test Execution:** Learn how to execute tests in [TEST_EXECUTION_GUIDE.md](TEST_EXECUTION_GUIDE.md)

## 🚀 Quick Start

1. **Start the Framework:**
   ```bash
   make dev
   ```

2. **Run Tests (Choose your approach):**
   ```bash
   # Quick unit tests (fastest, no external dependencies)
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

3. **Discover Available Tests (API):**
   ```bash
   curl -s "http://localhost:8080/api/v1/tests/discover" | jq .
   ```

4. **Execute Tests via API:**
   ```bash
   curl -s -X POST \
     "http://localhost:8080/api/v1/tests/execute/tags?tags=smoke" \
     | jq .
   ```

5. **Monitor Execution:**
   ```bash
   curl -s "http://localhost:8080/api/v1/test-execution/status?executionId=<executionId>" | jq .
   ```

## 📖 Documentation Structure

```
docs/
├── README.md                    # This file - documentation index
├── API_REFERENCE.md            # Complete API documentation
└── TEST_EXECUTION_GUIDE.md     # Test execution guide
```

## 🔧 API Overview

The ATAS framework provides REST APIs for:

### Test Discovery
- Discover all available tests, suites, and tags
- Filter tests by type (UI/API)
- Get detailed information about test classes and methods

### Test Execution
- Execute individual tests
- Execute tests by tags
- Execute tests by pattern matching
- Execute predefined test suites
- Custom execution with advanced configuration

### Test Monitoring
- Real-time status monitoring
- Server-Sent Events for live updates
- Detailed test results and reporting

## 🎯 Use Cases

### Development
- Run individual tests during development
- Debug specific test failures
- Validate changes before committing

### CI/CD Integration
- Execute smoke tests in deployment pipelines
- Run regression tests after releases
- Automated test execution with monitoring

### Quality Assurance
- Execute comprehensive test suites
- Run tests across different environments
- Monitor test execution performance

## 🔍 Key Features

- **Asynchronous Execution:** Tests run in the background without blocking
- **Multiple Execution Types:** Individual, tags, patterns, and suites
- **Real-time Monitoring:** Live updates via Server-Sent Events
- **Flexible Configuration:** Environment, browser, timeout, and recording options
- **Comprehensive Discovery:** Dynamic test discovery with detailed metadata

## 📞 Support

For questions, issues, or contributions:

1. Check the [API Reference](API_REFERENCE.md) for detailed endpoint information
2. Follow the [Test Execution Guide](TEST_EXECUTION_GUIDE.md) for step-by-step instructions
3. Review the main [README.md](../README.md) for project setup and configuration
4. Check the troubleshooting sections in the guides for common issues

## 🔄 Updates

This documentation is updated regularly to reflect the latest features and improvements. Check back frequently for updates and new capabilities.

---

**Happy Testing! 🧪**
