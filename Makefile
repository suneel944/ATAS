# ATAS Project Makefile
# Advanced Testing As A Service - Easy command shortcuts

.PHONY: help build test clean compile docker-up docker-down docker-logs lint format check-all setup dev test-ui test-api test-unit test-integration test-by-type test-all report release

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[1;33m
RED := \033[0;31m
NC := \033[0m # No Color

# Load .env file and export variables to shell commands
# Docker Compose automatically loads .env from the current working directory (project root)
# This ensures Makefile variables and Docker Compose both respect .env settings
ifneq (,$(wildcard .env))
    # Extract SPRING_PROFILES_ACTIVE from .env if present (before setting default)
    ENV_PROFILE := $(shell grep '^SPRING_PROFILES_ACTIVE=' .env 2>/dev/null | cut -d'=' -f2 | tr -d ' "' || echo '')
endif

# Project variables
PROJECT_NAME := ATAS
MAVEN_WRAPPER := ./mvnw
# Docker Compose automatically reads .env from project root (where command is executed)
# Docker Compose commands are run from project root, so .env is automatically found
DOCKER_COMPOSE_LOCAL := docker compose -f docker/docker-compose-local-db.yml
DOCKER_COMPOSE_PROD := docker compose -f docker/docker-compose.prod.yml
DOCKER_COMPOSE := $(DOCKER_COMPOSE_LOCAL)
# Use SPRING_PROFILES_ACTIVE from .env if set, otherwise default to dev
# Command-line override still works: make dev SPRING_PROFILES_ACTIVE=stage
SPRING_PROFILES_ACTIVE ?= $(or $(ENV_PROFILE),dev)

# Export all variables to subprocesses (so Docker Compose and shell commands can use them)
# Docker Compose will automatically read .env file from project root for variable substitution
export

# Common warning messages
POSTGRES_WARNING := "⚠️  Note: This test requires PostgreSQL container to be running"

##@ Help
help: ## Display this help message
	@echo "$(BLUE)$(PROJECT_NAME) - Advanced Testing As A Service$(NC)"
	@echo ""
	@echo "$(GREEN)Available commands:$(NC)"
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Setup & Installation
setup: ## Initial project setup
	@echo "$(BLUE)🚀 Setting up $(PROJECT_NAME) project...$(NC)"
	@echo ""
	@echo "$(YELLOW)📋 Checking prerequisites...$(NC)"
	@command -v java >/dev/null 2>&1 || { echo "$(RED)❌ Java is not installed. Please install Java 21+$(NC)"; exit 1; }
	@command -v docker >/dev/null 2>&1 || { echo "$(RED)❌ Docker is not installed. Please install Docker$(NC)"; exit 1; }
	@docker compose version >/dev/null 2>&1 || { echo "$(RED)❌ Docker Compose is not installed. Please install Docker Compose$(NC)"; exit 1; }
	@echo "$(GREEN)✅ All prerequisites found$(NC)"
	@echo ""
	@echo "$(YELLOW)🔧 Configuring project...$(NC)"
	@chmod +x $(MAVEN_WRAPPER)
	@git config commit.template .gitmessage 2>/dev/null || echo "$(YELLOW)⚠️  Not in a git repository, skipping git config$(NC)"
	@echo "$(GREEN)✅ Project configuration complete$(NC)"
	@echo ""
	@echo "$(YELLOW)📦 Installing dependencies...$(NC)"
	@$(MAVEN_WRAPPER) -q -DskipTests install
	@echo "$(GREEN)✅ Dependencies installed$(NC)"
	@echo ""
	@echo ""
	@echo "$(GREEN)🎉 $(PROJECT_NAME) setup complete!$(NC)"
	@echo ""
	@echo "$(YELLOW)📚 Quick Start Guide:$(NC)"
	@echo "  make dev      - Start development environment (Docker services)"
	@echo "  make test     - Run all tests"
	@echo "  make build    - Build the project"
	@echo "  make help     - Show all available commands"
	@echo ""
	@echo "$(YELLOW)🌐 Development URLs:$(NC)"
	@echo "  ATAS Framework: http://localhost:8080"
	@echo "  Health Check:   http://localhost:8080/actuator/health"
	@echo "  PostgreSQL:     localhost:5433"
	@echo ""
	@echo "$(BLUE)💡 Tip: Run 'make dev' to start the development environment$(NC)"

compile: build ## Compile the project (alias for build)
	@echo "$(GREEN)✅ Compilation complete!$(NC)"

install: ## Install project artifacts to local Maven repository
	@echo "$(BLUE)Installing $(PROJECT_NAME) to local Maven repository...$(NC)"
	$(MAVEN_WRAPPER) install -DskipTests
	@echo "$(GREEN)✅ Installation to local repository complete!$(NC)"

##@ Building
build: ## Build the project
	@echo "$(BLUE)Building $(PROJECT_NAME)...$(NC)"
	$(MAVEN_WRAPPER) compile
	@echo "$(GREEN)✅ Build complete!$(NC)"

build-fast: ## Fast build (skip tests)
	@echo "$(BLUE)Fast building $(PROJECT_NAME)...$(NC)"
	$(MAVEN_WRAPPER) compile -DskipTests -q
	@echo "$(GREEN)✅ Fast build complete!$(NC)"

package: ## Package the project (create JARs)
	@echo "$(BLUE)Packaging $(PROJECT_NAME)...$(NC)"
	$(MAVEN_WRAPPER) clean package -DskipTests
	@echo "$(GREEN)✅ Packaging complete!$(NC)"

##@ Testing
test: ## Run all tests
	@echo "$(BLUE)Running all tests with profile: $(SPRING_PROFILES_ACTIVE)...$(NC)"
	@echo "$(YELLOW)⚠️  Note: Some UI tests require the ATAS framework service to be running$(NC)"
	@echo "$(YELLOW)   If tests fail with connection errors, run: make test-with-service$(NC)"
	SPRING_PROFILES_ACTIVE=$(SPRING_PROFILES_ACTIVE) $(MAVEN_WRAPPER) test
	@echo "$(GREEN)✅ Tests completed!$(NC)"

test-ui: ## Run UI tests only
	@echo "$(BLUE)Running UI tests...$(NC)"
	$(MAVEN_WRAPPER) test -Dtest="**/ui/**/*Test" -pl atas-tests
	@echo "$(GREEN)✅ UI tests completed!$(NC)"

test-api: ## Run API tests only
	@echo "$(BLUE)Running API tests...$(NC)"
	$(MAVEN_WRAPPER) test -Dtest="**/api/**/*Test" -pl atas-tests
	@echo "$(GREEN)✅ API tests completed!$(NC)"

test-unit: ## Run unit tests only
	@echo "$(BLUE)Running unit tests...$(NC)"
	$(MAVEN_WRAPPER) test -Dtest="**/*Test,!**/*IntegrationTest" -pl atas-framework
	@echo "$(GREEN)✅ Unit tests completed!$(NC)"

test-integration: ## Run integration tests only
	@echo "$(BLUE)Running integration tests...$(NC)"
	@echo "$(YELLOW)$(POSTGRES_WARNING)$(NC)"
	$(MAVEN_WRAPPER) test -Dtest="**/*IntegrationTest" -pl atas-framework
	@echo "$(GREEN)✅ Integration tests completed!$(NC)"


test-suite: ## Run specific test suite (usage: make test-suite SUITE=authentication-ui)
	@echo "$(BLUE)Running test suite: $(SUITE)...$(NC)"
	$(MAVEN_WRAPPER) test -Dtest="com.atas.suites.$(SUITE).*TestSuite"
	@echo "$(GREEN)✅ Test suite $(SUITE) completed!$(NC)"

test-all: test ## Alias for test (for backward compatibility)

test-by-type: ## Run all test types in sequence (unit, integration)
	@echo "$(BLUE)Running all test types in sequence...$(NC)"
	@$(MAKE) test-unit
	@$(MAKE) test-integration
	@echo "$(GREEN)✅ All test types completed!$(NC)"

test-with-service: ## Run all tests with framework service running
	@echo "$(BLUE)Starting framework service and running all tests...$(NC)"
	@echo "$(YELLOW)Note: This will start the ATAS framework service in the background$(NC)"
	@$(MAKE) docker-up
	@echo "$(BLUE)Waiting for service to be ready...$(NC)"
	@sleep 10
	$(MAVEN_WRAPPER) test
	@echo "$(GREEN)✅ All tests with service completed!$(NC)"

##@ Code Quality
lint: ## Run code quality checks
	@echo "$(BLUE)Running code quality checks...$(NC)"
	$(MAVEN_WRAPPER) spotbugs:check checkstyle:check
	@echo "$(GREEN)✅ Code quality checks completed!$(NC)"

format: ## Format code (if formatter is configured)
	@echo "$(BLUE)Formatting code...$(NC)"
	$(MAVEN_WRAPPER) com.spotify.fmt:fmt-maven-plugin:format
	@echo "$(GREEN)✅ Code formatting completed!$(NC)"

security: ## Run security checks
	@echo "$(BLUE)Running security checks...$(NC)"
	$(MAVEN_WRAPPER) org.owasp:dependency-check-maven:check
	@echo "$(GREEN)✅ Security checks completed!$(NC)"

check-all: ## Run all checks (build, test, lint, security)
	@echo "$(BLUE)Running all checks...$(NC)"
	@$(MAKE) build
	@$(MAKE) test
	@$(MAKE) lint
	@$(MAKE) security
	@echo "$(GREEN)✅ All checks completed!$(NC)"

##@ Docker & Services
docker-up: ## Start Docker services (respects environment variables from .env file)
	@echo "$(BLUE)Starting Docker services...$(NC)"
	@echo "$(YELLOW)Profile: $(SPRING_PROFILES_ACTIVE)$(NC)"
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | grep -v '^$$' | xargs) && \
		$(DOCKER_COMPOSE) up -d; \
	else \
		$(DOCKER_COMPOSE) up -d; \
	fi
	@echo "$(GREEN)✅ Docker services started!$(NC)"
	@echo "$(YELLOW)Services:$(NC)"
	@echo "  - ATAS Framework: http://localhost:8080"
	@echo "  - PostgreSQL: localhost:5433"
	@echo "  - Health Check: http://localhost:8080/actuator/health"

docker-down: ## Stop Docker services
	@echo "$(BLUE)Stopping Docker services...$(NC)"
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | grep -v '^$$' | xargs) && \
		$(DOCKER_COMPOSE) down; \
	else \
		$(DOCKER_COMPOSE) down; \
	fi
	@echo "$(GREEN)✅ Docker services stopped!$(NC)"

docker-logs: ## Show Docker service logs
	@echo "$(BLUE)Showing Docker service logs...$(NC)"
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | grep -v '^$$' | xargs) && \
		$(DOCKER_COMPOSE) logs -f; \
	else \
		$(DOCKER_COMPOSE) logs -f; \
	fi

docker-restart: ## Restart Docker services
	@echo "$(BLUE)Restarting Docker services...$(NC)"
	@$(MAKE) docker-down
	@$(MAKE) docker-up

docker-build: ## Build Docker images (respects environment variables from .env file)
	@echo "$(BLUE)Building Docker images...$(NC)"
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | grep -v '^$$' | xargs) && \
		$(DOCKER_COMPOSE) build; \
	else \
		$(DOCKER_COMPOSE) build; \
	fi
	@echo "$(GREEN)✅ Docker images built!$(NC)"

##@ Development
dev: ## Start development environment
	@echo "$(BLUE)Starting development environment with profile: $(SPRING_PROFILES_ACTIVE)...$(NC)"
	@$(MAKE) docker-up
	@echo "$(GREEN)✅ Development environment ready!$(NC)"
	@echo "$(YELLOW)Available commands:$(NC)"
	@echo "  make test     - Run tests"
	@echo "  make logs     - View logs"
	@echo "  make stop     - Stop services"

dev-stage: ## Start staging environment
	@echo "$(BLUE)Starting staging environment...$(NC)"
	SPRING_PROFILES_ACTIVE=stage $(MAKE) docker-up
	@echo "$(GREEN)✅ Staging environment ready!$(NC)"

dev-prod: ## Start production environment (for local testing - database port exposed)
	@echo "$(BLUE)Starting production environment...$(NC)"
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | grep -v '^$$' | xargs) && \
		$(DOCKER_COMPOSE_PROD) up -d; \
	else \
		$(DOCKER_COMPOSE_PROD) up -d; \
	fi
	@echo "$(GREEN)✅ Production environment ready!$(NC)"
	@echo "$(YELLOW)Services:$(NC)"
	@echo "  - ATAS Framework: http://localhost:8080"
	@echo "  - PostgreSQL: localhost:5433 (exposed for local testing)"
	@echo "  - Health Check: http://localhost:8080/actuator/health"
	@echo "$(YELLOW)Note:$(NC) Database port is exposed for local testing. Tests can record results."

run: ## Run the framework locally (without Docker)
	@echo "$(BLUE)Running ATAS framework locally with profile: $(SPRING_PROFILES_ACTIVE)...$(NC)"
	cd atas-framework && SPRING_PROFILES_ACTIVE=$(SPRING_PROFILES_ACTIVE) $(MAVEN_WRAPPER) spring-boot:run

logs: ## Show application logs
	@echo "$(BLUE)Showing application logs...$(NC)"
	$(DOCKER_COMPOSE) logs -f atas-service

stop: docker-down ## Alias for docker-down

##@ Reporting
report: ## Generate test reports
	@echo "$(BLUE)Generating test reports...$(NC)"
	$(MAVEN_WRAPPER) allure:report
	@echo "$(GREEN)✅ Test reports generated!$(NC)"
	@echo "$(YELLOW)Reports:$(NC)"
	@echo "  - Allure: atas-tests/target/site/allure-maven-plugin/index.html"
	@echo "  - Coverage: atas-tests/target/site/jacoco/index.html"

report-serve: ## Serve test reports locally
	@echo "$(BLUE)Serving test reports...$(NC)"
	$(MAVEN_WRAPPER) allure:serve


##@ Git & Workflow
commit: ## Commit with template (usage: make commit MESSAGE="feat: add new feature")
	@echo "$(BLUE)Committing changes...$(NC)"
	@if [ -z "$(MESSAGE)" ]; then \
		echo "$(RED)Error: Please provide a commit message$(NC)"; \
		echo "Usage: make commit MESSAGE=\"feat: add new feature\""; \
		exit 1; \
	fi
	git commit -m "$(MESSAGE)"
	@echo "$(GREEN)✅ Changes committed!$(NC)"

push: ## Push changes to remote
	@echo "$(BLUE)Pushing changes...$(NC)"
	git push
	@echo "$(GREEN)✅ Changes pushed!$(NC)"

pull: ## Pull latest changes
	@echo "$(BLUE)Pulling latest changes...$(NC)"
	git pull
	@echo "$(GREEN)✅ Latest changes pulled!$(NC)"

status: ## Show git status
	@echo "$(BLUE)Git status:$(NC)"
	git status

branch: ## Create new branch (usage: make branch NAME=feature/new-feature)
	@echo "$(BLUE)Creating new branch: $(NAME)...$(NC)"
	@if [ -z "$(NAME)" ]; then \
		echo "$(RED)Error: Please provide a branch name$(NC)"; \
		echo "Usage: make branch NAME=\"feature/new-feature\""; \
		exit 1; \
	fi
	git checkout -b $(NAME)
	@echo "$(GREEN)✅ Branch $(NAME) created and checked out!$(NC)"

##@ Release & Deployment
release: ## Create a release (usage: make release VERSION=1.0.0)
	@echo "$(BLUE)Creating release $(VERSION)...$(NC)"
	@if [ -z "$(VERSION)" ]; then \
		echo "$(RED)Error: Please provide a version$(NC)"; \
		echo "Usage: make release VERSION=\"1.0.0\""; \
		exit 1; \
	fi
	git tag -a v$(VERSION) -m "Release version $(VERSION)"
	git push origin v$(VERSION)
	@echo "$(GREEN)✅ Release v$(VERSION) created!$(NC)"

deploy: ## Deploy to staging
	@echo "$(BLUE)Deploying to staging...$(NC)"
	@$(MAKE) check-all
	@echo "$(GREEN)✅ Ready for deployment!$(NC)"

##@ Cleanup
clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	$(MAVEN_WRAPPER) clean
	@echo "$(GREEN)✅ Clean completed!$(NC)"

clean-all: ## Clean everything (build, Docker, logs, volumes)
	@echo "$(BLUE)Cleaning everything...$(NC)"
	@$(MAKE) clean
	@echo "$(YELLOW)Stopping Docker services and removing volumes...$(NC)"
	@$(DOCKER_COMPOSE_LOCAL) down -v 2>/dev/null || true
	@$(DOCKER_COMPOSE_PROD) down -v 2>/dev/null || true
	docker system prune -f
	docker volume prune -f
	@echo "$(YELLOW)Removing additional build artifacts...$(NC)"
	@rm -rf atas-tests/.allure
	@rm -rf atas-tests/target
	@rm -rf atas-framework/target
	@echo "$(GREEN)✅ Complete cleanup done!$(NC)"

##@ Utilities
deps: ## Show dependency tree
	@echo "$(BLUE)Dependency tree:$(NC)"
	$(MAVEN_WRAPPER) dependency:tree

deps-update: ## Update dependencies
	@echo "$(BLUE)Updating dependencies...$(NC)"
	$(MAVEN_WRAPPER) versions:display-dependency-updates
	@echo "$(YELLOW)Review updates above and run:$(NC)"
	@echo "$(YELLOW)$(MAVEN_WRAPPER) versions:use-latest-releases$(NC)"

info: ## Show project information
	@echo "$(BLUE)$(PROJECT_NAME) Project Information$(NC)"
	@echo ""
	@echo "$(GREEN)Project:$(NC) Advanced Testing As A Service"
	@echo "$(GREEN)Java Version:$(NC) $$(java -version 2>&1 | head -n 1)"
	@echo "$(GREEN)Maven Version:$(NC) $$($(MAVEN_WRAPPER) -version | head -n 1)"
	@echo "$(GREEN)Docker Version:$(NC) $$(docker --version 2>/dev/null || echo 'Not installed')"
	@echo "$(GREEN)Git Version:$(NC) $$(git --version)"
	@echo ""
	@echo "$(GREEN)Quick Commands:$(NC)"
	@echo "  make dev      - Start development environment"
	@echo "  make test     - Run tests"
	@echo "  make build    - Build project"
	@echo "  make help     - Show all commands"

##@ Workflow Shortcuts
ci: check-all ## Alias for check-all

pr-check: check-all ## Alias for check-all

quick-test: test-unit ## Alias for test-unit (for backward compatibility)
