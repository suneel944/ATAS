# Getting Started with ATAS

A step-by-step guide for new contributors to get up and running with the ATAS project.

## 🚀 Step-by-Step Onboarding

### Step 1: Explore Available Commands

```bash
make help
```

This shows all available commands with descriptions. Use this anytime you need to remember a command.

---

### Step 2: One-Time Project Setup

```bash
make setup
```

**What this does:**
- ✅ Checks prerequisites (Java 21+, Docker, Docker Compose)
- 🔧 Configures project (Maven wrapper permissions, git commit template)
- 📦 Downloads and installs all dependencies
- 🏗️ Builds and installs the project to local Maven repository

**When to use:** First time only, or after pulling major changes.

**Time:** ~2-5 minutes (depending on internet speed for dependency download)

---

### Step 3: Start Development Environment

```bash
make dev
```

**What this does:**
- 🐳 Starts Docker containers (PostgreSQL + ATAS Framework)
- 🧩 Auto-migrates database schema
- 🌍 Configures environment (dev profile by default)

**Verify it's working:**
```bash
# Check health
curl http://localhost:8080/actuator/health

# Access monitoring dashboard (requires authentication)
open http://localhost:8080/monitoring/dashboard

# Login page
open http://localhost:8080/login
```

**Default Credentials:**
- Username: `admin`
- Password: `admin123` (change on first login)

**When to use:** Every time you start working on the project.

---

### Step 4: Verify Everything Works

**Quick verification (fastest):**
```bash
make test-unit
```
- Runs framework unit tests only (no external dependencies)
- Uses H2 in-memory database
- Takes ~10-30 seconds

**Full verification:**
```bash
make test
```
- Runs all tests (unit + integration + UI + API)
- Requires PostgreSQL running (which `make dev` started)
- Takes longer (~2-5 minutes)

---

## 📋 Daily Development Workflow

Once you've completed the setup above, here's your typical daily workflow:

### Morning / Starting Work

```bash
# 1. Pull latest changes
make pull

# 2. Start services
make dev

# 3. Quick test to verify everything works
make test-unit
```

### During Development

```bash
# Compile after making code changes
make compile   # or `make build` (they're the same)

# Run specific tests while developing
make test-unit         # Framework unit tests (fastest)
make test-ui           # UI tests only
make test-api          # API tests only

# Check code quality
make lint              # Run code quality checks
make format            # Format code (if needed)
```

### Before Committing

```bash
# Run all checks
make pr-check          # Runs: build, test, lint, security

# Or run individually:
make build             # Compile
make test              # Run all tests
make lint              # Code quality
make security          # Security checks
```

### When You Need Fresh Start

```bash
# Clean everything and start over
make clean-all         # Removes build artifacts, Docker volumes, etc.
make setup             # Re-setup from scratch
make dev               # Start fresh environment
```

---

## 🎯 Command Quick Reference by Goal

### First Time Setup
1. `make help` - See all commands
2. `make setup` - Complete one-time setup
3. `make dev` - Start development environment
4. `make test-unit` - Verify it works

### Daily Development
- `make dev` - Start services
- `make compile` - Compile code
- `make test-unit` - Quick test feedback
- `make logs` - View service logs
- `make stop` - Stop services when done

### Running Tests
- `make test-unit` - Fastest (framework unit tests)
- `make test-integration` - Integration tests
- `make test-ui` - UI tests
- `make test-api` - API tests
- `make test` - All tests
- `make test-by-type` - Run test types in sequence

### Building
- `make compile` - Quick compile (alias for build)
- `make build` - Compile source code
- `make install` - Install to local Maven repository
- `make package` - Create JAR files

### Quality Checks
- `make lint` - Code quality checks
- `make format` - Format code
- `make security` - Security vulnerability scan
- `make check-all` - Run all checks

### Monitoring & Debugging
- `make logs` - View application logs
- `make docker-logs` - View Docker service logs
- Visit http://localhost:8080/monitoring - Monitoring dashboard
- Visit http://localhost:8080/database - Database management dashboard

---

## 🔄 Understanding the Command Flow

### Why `setup` → `dev` → `test`?

1. **`make setup`** - One-time setup that:
   - Checks you have the right tools
   - Downloads all dependencies
   - Configures git
   - Installs everything to local Maven repo
   
2. **`make dev`** - Starts your development environment:
   - Launches PostgreSQL database
   - Starts ATAS Framework service
   - Makes everything accessible at localhost:8080

3. **`make test-*`** - Validates everything works:
   - Tests your code changes
   - Verifies the system is functioning

### When Do I Use Each Command?

| Situation | Command |
|-----------|---------|
| First time ever | `make setup` then `make dev` |
| Starting work session | `make dev` |
| After code changes | `make compile` or `make build` |
| Quick test | `make test-unit` |
| Before committing | `make pr-check` |
| Need to install to local repo | `make install` |
| Starting fresh | `make clean-all` then `make setup` |

---

## ⚠️ Common Mistakes to Avoid

1. **Don't skip `make setup`** - You'll miss dependencies and configuration
2. **Don't forget `make dev`** - Services won't be running
3. **Don't run `make test` without `make dev`** - Integration tests need PostgreSQL
4. **Use `make compile` not `make install`** - For daily compilation (install is only when you need artifacts in local repo)

---

## 💡 Pro Tips

- **Fast feedback:** Use `make test-unit` during development (fastest)
- **Full validation:** Use `make pr-check` before pushing (runs everything)
- **Quick build:** `make build-fast` skips tests for even faster compilation
- **Check status:** `make info` shows versions of all tools
- **View logs:** `make logs` for real-time application logs

---

## 🆘 Troubleshooting

If something doesn't work:

1. **Check prerequisites:**
   ```bash
   make info    # Shows versions
   ```

2. **Clean and restart:**
   ```bash
   make clean-all
   make setup
   make dev
   ```

3. **Check service health:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

4. **View logs:**
   ```bash
   make logs    # Application logs
   make docker-logs    # Docker service logs
   ```

5. **Check database connection (if recording tests):**
   ```bash
   # When running tests with ATAS_RECORD_LOCAL=true, the system auto-detects database
   # Check logs for: "Environment detection", "✅ Matched", "Connecting to database"
   
   # Verify which containers are running
   docker ps --format "{{.Names}}" | grep atas-db
   
   # Check database port
   nc -zv localhost 5433  # Local Docker Compose
   ```

6. **Check help:**
   ```bash
   make help    # See all available commands
   ```

---

**Welcome to ATAS! Happy testing! 🧪**

