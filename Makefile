# ATAS Project Makefile
# Advanced Testing As A Service - Easy command shortcuts

.PHONY: help build test clean compile docker-up docker-down docker-logs lint format check-all setup dev test-ui test-api test-unit test-integration test-by-type test-all report release install build-fast package test-suite test-with-service docker-restart docker-build docker-stop-all dev-stage dev-prod run logs stop report-serve commit push pull status branch deploy clean-all deps deps-update info ci pr-check quick-test k8s-test k8s-setup-local k8s-test-local k8s-deploy k8s-status k8s-logs k8s-port-forward k8s-stop-port-forward k8s-clean k8s-clean-deep k8s-verify-access k8s-visualize

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

# Docker Compose reads .env from project root (where command is executed)
# Explicitly specify --env-file to ensure .env is always found, even when using -f with paths
DOCKER_COMPOSE_LOCAL := docker compose -f docker/docker-compose-local-db.yml --env-file .env
DOCKER_COMPOSE_PROD := docker compose -f docker/docker-compose.prod.yml --env-file .env
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
	@echo "$(BLUE)╔════════════════════════════════════════════════════════════╗$(NC)"
	@echo "$(BLUE)║  $(PROJECT_NAME) - Mezepay Testing As A Service$(NC)$(BLUE)                    ║$(NC)"
	@echo "$(BLUE)╚════════════════════════════════════════════════════════════╝$(NC)"
	@echo ""
	@echo "$(GREEN)📚 Quick Start:$(NC)"
	@echo "  $(YELLOW)make setup$(NC)     - Initial project setup"
	@echo "  $(YELLOW)make dev$(NC)       - Start development environment"
	@echo "  $(YELLOW)make test$(NC)      - Run all tests"
	@echo "  $(YELLOW)make build$(NC)     - Build the project"
	@echo ""
	@echo "$(GREEN)🌐 Development URLs:$(NC)"
	@echo "  • ATAS Framework: $(BLUE)http://localhost:8080$(NC)"
	@echo "  • Health Check:   $(BLUE)http://localhost:8080/actuator/health$(NC)"
	@echo "  • Dashboard:      $(BLUE)http://localhost:8080/monitoring/dashboard$(NC)"
	@echo "  • PostgreSQL:     $(BLUE)localhost:5433$(NC)"
	@echo "  • Redis:           $(BLUE)localhost:6379$(NC)"
	@echo ""
	@echo "$(GREEN)📋 Available Commands:$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; section=""} \
	/^##@/ { \
		section = substr($$0, 5); \
		if (section != "") { \
			printf "$(YELLOW)%s$(NC)\n", section; \
		} \
	} \
	/^[a-zA-Z_0-9-]+:.*?##/ { \
		if ($$2 != "") { \
			printf "  $(BLUE)%-20s$(NC) %s\n", $$1, $$2; \
		} \
	}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(GREEN)💡 Tips:$(NC)"
	@echo "  • Use $(YELLOW)make help$(NC) to see this message again"
	@echo "  • For Kubernetes: $(YELLOW)make k8s-test-local$(NC) to test locally"
	@echo "  • For logs: $(YELLOW)make logs$(NC) (Docker) or $(YELLOW)make k8s-logs$(NC) (Kubernetes)"
	@echo ""

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
	@echo "  Redis:          localhost:6379"
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
	@if $(MAVEN_WRAPPER) spotbugs:check >/dev/null 2>&1; then \
		echo "$(GREEN)✅ SpotBugs check passed$(NC)"; \
	elif $(MAVEN_WRAPPER) spotbugs:check 2>&1 | grep -q "No plugin found"; then \
		echo "$(YELLOW)⚠️  SpotBugs plugin not configured, skipping...$(NC)"; \
	else \
		echo "$(YELLOW)⚠️  SpotBugs found issues (non-blocking)$(NC)"; \
	fi
	@if $(MAVEN_WRAPPER) checkstyle:check >/dev/null 2>&1; then \
		echo "$(GREEN)✅ Checkstyle check passed$(NC)"; \
	elif $(MAVEN_WRAPPER) checkstyle:check 2>&1 | grep -q "No plugin found"; then \
		echo "$(YELLOW)⚠️  Checkstyle plugin not configured, skipping...$(NC)"; \
	else \
		echo "$(YELLOW)⚠️  Checkstyle found issues (non-blocking)$(NC)"; \
	fi
	@echo "$(GREEN)✅ Code quality checks completed!$(NC)"

format: ## Format code (if formatter is configured)
	@echo "$(BLUE)Formatting code...$(NC)"
	$(MAVEN_WRAPPER) com.spotify.fmt:fmt-maven-plugin:format
	@echo "$(GREEN)✅ Code formatting completed!$(NC)"

check-all: ## Run all checks (build, test, lint)
	@echo "$(BLUE)Running all checks...$(NC)"
	@$(MAKE) build
	@$(MAKE) test
	@$(MAKE) lint
	@echo "$(GREEN)✅ All checks completed!$(NC)"

##@ Docker & Services

docker-up: ## Start Docker services (respects environment variables from .env file)
	@echo "$(BLUE)Starting Docker services...$(NC)"
	@echo "$(YELLOW)Profile: $(SPRING_PROFILES_ACTIVE)$(NC)"
	@$(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)✅ Docker services started!$(NC)"
	@echo "$(YELLOW)Services:$(NC)"
	@echo "  - ATAS Framework: http://localhost:8080"
	@echo "  - ATAS PostgreSQL: localhost:5433"
	@echo "  - Redis: localhost:6379"
	@echo "  - Health Check: http://localhost:8080/actuator/health"

docker-down: ## Stop Docker services
	@echo "$(BLUE)Stopping Docker services...$(NC)"
	@$(DOCKER_COMPOSE) down
	@echo "$(GREEN)✅ Docker services stopped!$(NC)"

docker-logs: ## Show Docker service logs
	@echo "$(BLUE)Showing Docker service logs...$(NC)"
	@$(DOCKER_COMPOSE) logs -f

docker-restart: ## Restart Docker services
	@echo "$(BLUE)Restarting Docker services...$(NC)"
	@$(MAKE) docker-down
	@$(MAKE) docker-up

docker-stop-all: ## Stop and remove all Docker containers
	@echo "$(BLUE)Stopping and removing all Docker containers...$(NC)"
	@if [ -n "$$(docker ps -aq)" ]; then \
		docker stop $$(docker ps -aq) && docker rm $$(docker ps -aq); \
		echo "$(GREEN)✅ All containers stopped and removed!$(NC)"; \
	else \
		echo "$(YELLOW)No containers to stop$(NC)"; \
	fi

docker-build: ## Build Docker images (respects environment variables from .env file)
	@echo "$(BLUE)Building Docker images...$(NC)"
	@$(DOCKER_COMPOSE) build
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
	@$(DOCKER_COMPOSE_PROD) up -d
	@echo "$(GREEN)✅ Production environment ready!$(NC)"
	@echo "$(YELLOW)Services:$(NC)"
	@echo "  - ATAS Framework: http://localhost:8080"
	@echo "  - PostgreSQL: localhost:5433 (exposed for local testing)"
	@echo "  - Redis: localhost:6379 (exposed for local testing)"
	@echo "  - Health Check: http://localhost:8080/actuator/health"
	@echo "$(YELLOW)Note:$(NC) Database and Redis ports are exposed for local testing. Tests can record results."

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
	@echo "$(YELLOW)Removing Allure results...$(NC)"
	@rm -rf atas-tests/allure-results
	@rm -rf atas-tests/allure-report
	@rm -rf atas-tests/target/allure-results
	@rm -rf atas-tests/target/allure-report
	@rm -rf atas-framework/allure-results
	@rm -rf atas-framework/allure-report
	@rm -rf atas-framework/target/allure-results
	@rm -rf atas-framework/target/allure-report
	@rm -rf atas-tests/.allure
	@rm -rf atas-framework/.allure
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
	@rm -rf atas-tests/allure-results
	@rm -rf atas-tests/allure-report
	@rm -rf atas-tests/target/allure-results
	@rm -rf atas-tests/target/allure-report
	@rm -rf atas-framework/.allure
	@rm -rf atas-framework/allure-results
	@rm -rf atas-framework/allure-report
	@rm -rf atas-framework/target/allure-results
	@rm -rf atas-framework/target/allure-report
	@rm -rf atas-tests/target
	@rm -rf atas-framework/target
	@echo "$(GREEN)✅ Complete cleanup done!$(NC)"

##@ Kubernetes

k8s-test: ## Test Kubernetes deployment (requires kubectl and cluster)
	@echo "$(BLUE)Testing Kubernetes deployment...$(NC)"
	@if ! kubectl cluster-info &>/dev/null; then \
		echo "$(RED)❌ kubectl is not configured or cluster is not accessible$(NC)"; \
		echo "$(YELLOW)For local testing, run: make k8s-test-local$(NC)"; \
		exit 1; \
	fi
	@cd k8s && PATH="/usr/local/bin:/usr/bin:$$PATH" ./test-deployment.sh

k8s-setup-local: ## Set up local Kubernetes cluster with kind
	@echo "$(BLUE)Setting up local Kubernetes cluster...$(NC)"
	@cd k8s && ./setup-local-cluster.sh

k8s-test-local: ## Test Kubernetes deployment with local cluster (kind)
	@echo "$(BLUE)Testing Kubernetes deployment with local cluster...$(NC)"
	@export PATH="$$HOME/.local/bin:$$PATH" && \
	if ! command -v kind &> /dev/null; then \
		echo "$(YELLOW)kind is not installed. Setting up...$(NC)"; \
		$(MAKE) k8s-setup-local; \
	fi && \
	if ! kind get clusters 2>/dev/null | grep -q "atas-local"; then \
		echo "$(YELLOW)Local cluster not found. Creating...$(NC)"; \
		$(MAKE) k8s-setup-local; \
	fi && \
	echo "$(BLUE)Setting kubectl context to kind-atas-local...$(NC)" && \
	kubectl config use-context kind-atas-local > /dev/null 2>&1 || true && \
	echo "$(BLUE)Building and loading image...$(NC)" && \
	docker build -f docker/Dockerfile.prod -t atas-service:local . || \
		(echo "$(RED)❌ Docker build failed$(NC)" && exit 1) && \
	kind load docker-image atas-service:local --name atas-local || \
		(echo "$(RED)❌ Failed to load image into kind$(NC)" && exit 1) && \
	echo "$(BLUE)Updating deployment to use local image...$(NC)" && \
	cd k8s && \
	sed -i.bak 's|image:.*atas-service.*|image: atas-service:local|' deployment.yaml && \
	PATH="$$HOME/.local/bin:/usr/local/bin:/usr/bin:$$PATH" ./test-deployment.sh; \
	EXIT_CODE=$$?; \
	if [ -f deployment.yaml.bak ]; then mv deployment.yaml.bak deployment.yaml; fi; \
	cd .. && \
	echo "$(BLUE)Setting up local service access...$(NC)" && \
	kubectl apply -f k8s/service-local.yaml 2>/dev/null || kubectl apply -f k8s/service.yaml && \
	echo "$(BLUE)Waiting for service to be ready...$(NC)" && \
	for i in 1 2 3 4 5; do \
		if kubectl get svc atas-service -n atas &>/dev/null; then \
			break; \
		fi; \
		echo "$(YELLOW)Waiting for service... (attempt $$i/5)$(NC)"; \
		sleep 1; \
	done && \
	$(MAKE) k8s-port-forward && \
	sleep 3 && \
	$(MAKE) k8s-verify-access && \
	echo "$(GREEN)✅ Local deployment complete!$(NC)" && \
	echo "$(GREEN)✅ Dashboard: http://localhost:8080/monitoring/dashboard$(NC)" && \
	exit $$EXIT_CODE

k8s-deploy: ## Deploy all Kubernetes resources
	@echo "$(BLUE)Deploying to Kubernetes...$(NC)"
	@if ! kubectl cluster-info &>/dev/null; then \
		echo "$(RED)❌ kubectl is not configured or cluster is not accessible$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Installing metrics-server (for CPU/RAM stats)...$(NC)"
	@kubectl apply -f k8s/metrics-server.yaml 2>/dev/null || \
		kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml 2>/dev/null || \
		echo "$(YELLOW)⚠️  Metrics-server installation skipped or failed$(NC)"
	@echo "$(BLUE)Creating namespace...$(NC)"
	@kubectl apply -f k8s/namespace.yaml
	@echo "$(BLUE)Deploying secrets...$(NC)"
	@kubectl apply -f k8s/secrets.yaml
	@echo "$(BLUE)Deploying configmap...$(NC)"
	@kubectl apply -f k8s/configmap.yaml
	@echo "$(BLUE)Deploying database...$(NC)"
	@kubectl apply -f k8s/database.yaml
	@echo "$(BLUE)Deploying Redis...$(NC)"
	@kubectl apply -f k8s/redis.yaml
	@echo "$(BLUE)Deploying ATAS service...$(NC)"
	@kubectl apply -f k8s/deployment.yaml
	@if kubectl get nodes 2>/dev/null | grep -qiE "kind|minikube|k3s|control-plane" || \
	   kubectl config current-context 2>/dev/null | grep -qiE "kind|minikube|k3s"; then \
		echo "$(BLUE)Local cluster detected - using NodePort service...$(NC)"; \
		kubectl apply -f k8s/service-local.yaml 2>/dev/null || kubectl apply -f k8s/service.yaml; \
		echo "$(BLUE)Setting up port-forward...$(NC)"; \
		$(MAKE) k8s-port-forward; \
		sleep 3; \
		$(MAKE) k8s-verify-access || true; \
	else \
		kubectl apply -f k8s/service.yaml; \
		echo "$(YELLOW)For local access, run: make k8s-port-forward$(NC)"; \
	fi
	@echo "$(GREEN)✅ Kubernetes resources deployed!$(NC)"
	@echo "$(YELLOW)Check status: kubectl get pods -n atas$(NC)"

k8s-status: ## Check Kubernetes deployment status
	@echo "$(BLUE)Kubernetes deployment status:$(NC)"
	@if ! kubectl cluster-info &>/dev/null; then \
		echo "$(RED)❌ kubectl is not configured or cluster is not accessible$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Pods:$(NC)"
	@kubectl get pods -n atas
	@echo ""
	@echo "$(BLUE)Services:$(NC)"
	@kubectl get svc -n atas
	@echo ""
	@if kubectl get hpa -n atas &>/dev/null; then \
		echo "$(BLUE)Horizontal Pod Autoscaler:$(NC)"; \
		kubectl get hpa -n atas; \
		echo ""; \
	fi
	@if pgrep -f "kubectl port-forward.*atas-service.*8080" > /dev/null; then \
		echo "$(GREEN)✅ Port-forward is active$(NC)"; \
		echo "$(GREEN)✅ Service: http://localhost:8080$(NC)"; \
	else \
		echo "$(YELLOW)⚠️  Port-forward is not running$(NC)"; \
		echo "$(YELLOW)Start with: make k8s-port-forward$(NC)"; \
	fi

k8s-logs: ## View ATAS service logs
	@echo "$(BLUE)Viewing ATAS service logs...$(NC)"
	@if ! kubectl get deployment atas-service -n atas &>/dev/null; then \
		echo "$(RED)❌ Deployment atas-service not found in namespace atas$(NC)"; \
		echo "$(YELLOW)Deploy first: make k8s-deploy$(NC)"; \
		exit 1; \
	fi
	@kubectl logs -f deployment/atas-service -n atas

k8s-port-forward: ## Port-forward ATAS service to localhost:8080 (runs in background)
	@echo "$(BLUE)Setting up port-forward to localhost:8080...$(NC)"
	@export PATH="$$HOME/.local/bin:$$PATH" && \
	if kubectl get nodes 2>/dev/null | grep -qiE "kind|minikube|k3s|control-plane" || \
	   kubectl config current-context 2>/dev/null | grep -qiE "kind"; then \
		kubectl config use-context kind-atas-local > /dev/null 2>&1 || true; \
	fi && \
	for i in 1 2 3 4 5; do \
		if kubectl get svc atas-service -n atas &>/dev/null 2>&1; then \
			echo "$(GREEN)✅ Service found$(NC)"; \
			break; \
		fi; \
		if [ $$i -eq 5 ]; then \
			echo "$(RED)❌ Service atas-service not found in namespace atas after 5 attempts$(NC)"; \
			echo "$(YELLOW)Current context: $$(kubectl config current-context 2>/dev/null || echo 'none')$(NC)"; \
			echo "$(YELLOW)Available services:$$(kubectl get svc -n atas 2>/dev/null | head -5 || echo ' (none)')$(NC)"; \
			echo "$(YELLOW)Deploy first: make k8s-deploy$(NC)"; \
			exit 1; \
		fi; \
		echo "$(YELLOW)Waiting for service to be available... (attempt $$i/5)$(NC)"; \
		sleep 1; \
	done
	@if pgrep -f "kubectl port-forward.*atas-service.*8080" > /dev/null; then \
		echo "$(YELLOW)Port-forward already running$(NC)"; \
		echo "$(GREEN)✅ Service available at: http://localhost:8080$(NC)"; \
		echo "$(GREEN)✅ Dashboard: http://localhost:8080/monitoring/dashboard$(NC)"; \
	else \
		echo "$(BLUE)Starting port-forward in background (localhost only)...$(NC)"; \
		kubectl port-forward --address localhost -n atas svc/atas-service 8080:8080 > /tmp/k8s-port-forward.log 2>&1 & \
		sleep 3 && \
		if pgrep -f "kubectl port-forward.*atas-service.*8080" > /dev/null; then \
			echo "$(GREEN)✅ Port-forward started!$(NC)"; \
			echo "$(GREEN)✅ Service available at: http://localhost:8080$(NC)"; \
			echo "$(GREEN)✅ Dashboard: http://localhost:8080/monitoring/dashboard$(NC)"; \
			echo "$(YELLOW)To stop: make k8s-stop-port-forward$(NC)"; \
		else \
			echo "$(RED)❌ Failed to start port-forward$(NC)"; \
			echo "$(YELLOW)Check logs: cat /tmp/k8s-port-forward.log$(NC)"; \
			exit 1; \
		fi; \
	fi

k8s-stop-port-forward: ## Stop port-forward for ATAS service
	@echo "$(BLUE)Stopping port-forward...$(NC)"
	@pkill -f "kubectl port-forward.*atas-service.*8080" || true
	@echo "$(GREEN)✅ Port-forward stopped!$(NC)"

k8s-clean: ## Clean up Kubernetes deployment (namespace, port-forwards, and optionally cluster)
	@echo "$(BLUE)Cleaning up Kubernetes deployment...$(NC)"
	@echo "$(YELLOW)Stopping port-forwards...$(NC)"
	@pkill -f "kubectl port-forward.*atas-service.*8080" || true
	@pkill -f "kubectl port-forward.*atas-db.*5433" || true
	@echo "$(BLUE)Deleting namespace...$(NC)"
	@kubectl delete namespace atas --ignore-not-found=true || true
	@echo "$(GREEN)✅ Kubernetes namespace cleaned$(NC)"
	@echo "$(YELLOW)Note: This only cleans the namespace. For deeper cleanup:$(NC)"
	@echo "$(YELLOW)  - Delete kind cluster: kind delete cluster --name atas-local$(NC)"
	@echo "$(YELLOW)  - Remove volumes: docker volume prune -f$(NC)"
	@echo "$(YELLOW)  - Remove images: docker image prune -a -f$(NC)"

k8s-clean-deep: ## Deep clean: delete namespace, kind cluster, and Docker resources
	@echo "$(BLUE)Performing deep Kubernetes cleanup...$(NC)"
	@echo "$(YELLOW)1. Stopping port-forwards...$(NC)"
	@ps aux | grep -E "kubectl port-forward.*atas-service.*8080" | grep -v grep | awk '{print $$2}' | xargs -r kill -9 2>/dev/null || true
	@ps aux | grep -E "kubectl port-forward.*atas-db.*5433" | grep -v grep | awk '{print $$2}' | xargs -r kill -9 2>/dev/null || true
	@echo "$(YELLOW)2. Deleting namespace...$(NC)"
	@kubectl delete namespace atas --ignore-not-found=true 2>/dev/null || true
	@echo "$(YELLOW)3. Deleting kind cluster...$(NC)"
	@if kind get clusters 2>/dev/null | grep -q "atas-local"; then \
		kind delete cluster --name atas-local 2>/dev/null && \
		echo "$(GREEN)✅ Kind cluster deleted$(NC)" || \
		echo "$(YELLOW)⚠️  Kind cluster not found or already deleted$(NC)"; \
	else \
		echo "$(YELLOW)⚠️  Kind cluster not found$(NC)"; \
	fi
	@echo "$(YELLOW)4. Removing stopped containers...$(NC)"
	@docker ps -a --filter "name=atas-local" -q 2>/dev/null | xargs -r docker rm -f 2>/dev/null || true
	@echo "$(YELLOW)5. Cleaning up Docker volumes...$(NC)"
	@docker volume prune -f 2>/dev/null || true
	@echo "$(GREEN)✅ Deep cleanup completed!$(NC)"
	@echo "$(YELLOW)Note: Docker images are preserved. Use 'docker image prune -a' to remove them.$(NC)"

k8s-verify-access: ## Verify that the ATAS service is accessible
	@echo "$(BLUE)Verifying service access...$(NC)"
	@if ! pgrep -f "kubectl port-forward.*atas-service.*8080" > /dev/null; then \
		echo "$(YELLOW)⚠️  Port-forward is not running. Starting...$(NC)"; \
		$(MAKE) k8s-port-forward; \
		sleep 2; \
	fi
	@for i in 1 2 3 4 5; do \
		if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then \
			echo "$(GREEN)✅ Service is accessible at http://localhost:8080$(NC)"; \
			echo "$(GREEN)✅ Health check passed$(NC)"; \
			exit 0; \
		fi; \
		echo "$(YELLOW)Waiting for service to be ready... (attempt $$i/5)$(NC)"; \
		sleep 2; \
	done; \
	echo "$(RED)❌ Service is not accessible after 10 seconds$(NC)"; \
	echo "$(YELLOW)Check logs: kubectl logs -n atas deployment/atas-service$(NC)"; \
	exit 1

k8s-visualize: ## Open k9s for cluster visualization (requires k9s)
	@echo "$(BLUE)Opening k9s for ATAS namespace...$(NC)"
	@if ! command -v k9s &> /dev/null; then \
		echo "$(YELLOW)k9s is not installed. Installing...$(NC)"; \
		if command -v snap &> /dev/null; then \
			sudo snap install k9s; \
		else \
			echo "$(RED)Please install k9s manually: https://k9scli.io/topics/install/$(NC)"; \
			exit 1; \
		fi \
	fi
	@k9s -n atas

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
	@echo "$(GREEN)Project:$(NC) Mezepay Testing As A Service"
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
