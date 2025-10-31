# Environment Configuration Guide

ATAS is fully environment-agnostic and supports multiple deployment environments through Spring profiles and environment variables.

## 🌍 Environment Profiles

ATAS supports three main environment profiles:

- **`dev`** - Development environment (default)
- **`stage`** - Staging environment
- **`prod`** - Production environment

## ⚙️ Configuration Files

### Framework Configuration
- `atas-framework/src/main/resources/application-dev.yml` - Development settings
- `atas-framework/src/main/resources/application-stage.yml` - Staging settings
- `atas-framework/src/main/resources/application-prod.yml` - Production settings

### Test Configuration
- `atas-tests/src/test/resources/application-dev.yml` - Test development settings
- `atas-tests/src/test/resources/application-stage.yml` - Test staging settings
- `atas-tests/src/test/resources/application-prod.yml` - Test production settings

## 🔧 Environment Variables

### Core Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | `dev`, `stage`, `prod` |

### Database Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `DB_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/atasdb` | `jdbc:postgresql://prod-db:5432/atas` |
| `DB_USERNAME` | Database username | `atas` | `atas_prod` |
| `DB_PASSWORD` | Database password | `ataspass` | `secure_password` |

### AWS S3 Storage Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `S3_BUCKET` | S3 bucket for media storage | `atas-videos` | `atas-prod-videos` |
| `S3_REGION` | AWS region | `us-east-1` | `us-west-2` |
| `S3_VIDEO_FOLDER` | Video storage folder | `videos` | `prod-videos` |
| `S3_SCREENSHOT_FOLDER` | Screenshot storage folder | `screenshots` | `prod-screenshots` |

### Framework-Specific Configuration

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `ATAS_TEST_BASE_PATH` | Test discovery base path | `atas-tests/src/test/java/com/atas` | `/app/tests` |
| `ATAS_TEST_ENVIRONMENT` | Default test environment | `dev` | `stage` |
| `ATAS_TEST_TIMEOUT_MINUTES` | Test timeout in minutes | `30` | `60` |
| `PLAYWRIGHT_VIDEO_DIR` | Playwright video directory | `videos` | `/tmp/videos` |

## 🚀 Usage Examples

### Local Development

**Default development environment:**
```bash
make dev
```

**Custom development environment:**
```bash
export DB_URL="jdbc:postgresql://localhost:5432/atasdb"
export S3_BUCKET="my-dev-bucket"
make dev
```

**Run tests with development profile:**
```bash
make test  # Uses dev profile by default
```

### Staging Environment

**Start staging environment:**
```bash
make dev-stage
# or
SPRING_PROFILES_ACTIVE=stage make dev
```

**Run tests with staging profile:**
```bash
SPRING_PROFILES_ACTIVE=stage make test
```

**Custom staging configuration:**
```bash
export SPRING_PROFILES_ACTIVE="stage"
export DB_URL="jdbc:postgresql://stage-db:5432/atas"
export DB_USERNAME="atas_stage"
export DB_PASSWORD="stage_password"
export S3_BUCKET="atas-stage-videos"
export S3_REGION="us-east-2"
make dev
```

### Production Environment

**Start production environment:**
```bash
make dev-prod
# or
SPRING_PROFILES_ACTIVE=prod make dev
```

**Run tests with production profile:**
```bash
SPRING_PROFILES_ACTIVE=prod make test
```

**Custom production configuration:**
```bash
export SPRING_PROFILES_ACTIVE="prod"
export DB_URL="jdbc:postgresql://prod-db:5432/atas"
export DB_USERNAME="atas_prod"
export DB_PASSWORD="secure_prod_password"
export S3_BUCKET="atas-prod-videos"
export S3_REGION="us-east-1"
make dev
```

## 🐳 Docker Configuration

### Docker Compose

**Development (default):**
```bash
make dev
```

**Staging:**
```bash
SPRING_PROFILES_ACTIVE=stage make dev
```

**Production:**
```bash
SPRING_PROFILES_ACTIVE=prod make dev
```

### Environment-Specific Docker Compose

**Local database:**
```bash
docker compose -f docker/docker-compose-local-db.yml up
```

**System database:**
```bash
docker compose -f docker/docker-compose-system-db.yml up
```

**Production:**
```bash
docker compose -f docker/docker-compose.prod.yml up
```

### Docker Environment Variables

Create a `.env` file for Docker Compose:

```bash
# .env file
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://atas-db:5432/atasdb
DB_USERNAME=atas
DB_PASSWORD=ataspass
S3_BUCKET=atas-videos
S3_REGION=us-east-1
S3_VIDEO_FOLDER=videos
S3_SCREENSHOT_FOLDER=screenshots
```

## 🔄 CI/CD Integration

### GitHub Actions

```yaml
name: Deploy to Staging
on:
  push:
    branches: [develop]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Deploy to Staging
        env:
          SPRING_PROFILES_ACTIVE: stage
          DB_URL: ${{ secrets.STAGE_DB_URL }}
          DB_USERNAME: ${{ secrets.STAGE_DB_USERNAME }}
          DB_PASSWORD: ${{ secrets.STAGE_DB_PASSWORD }}
          S3_BUCKET: atas-stage-videos
          S3_REGION: us-east-2
        run: |
          make dev-stage
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    environment {
        SPRING_PROFILES_ACTIVE = 'prod'
        DB_URL = credentials('prod-db-url')
        DB_USERNAME = credentials('prod-db-user')
        DB_PASSWORD = credentials('prod-db-pass')
        S3_BUCKET = 'atas-prod-videos'
        S3_REGION = 'us-east-1'
    }
    
    stages {
        stage('Deploy') {
            steps {
                sh 'make dev-prod'
            }
        }
    }
}
```

## 🔍 Environment-Specific Features

### Development Environment
- **Logging:** DEBUG level with full SQL queries
- **Actuator:** All endpoints exposed
- **Health:** Full details shown
- **Database:** DDL auto-update enabled
- **Security:** Minimal restrictions

### Staging Environment
- **Logging:** INFO level with moderate detail
- **Actuator:** Limited endpoints (health, info, metrics)
- **Health:** Details shown when authorized
- **Database:** DDL validation only
- **Security:** Moderate restrictions

### Production Environment
- **Logging:** WARN/ERROR level only
- **Actuator:** Minimal endpoints (health, info only)
- **Health:** Basic status only
- **Database:** DDL validation only
- **Security:** Maximum restrictions

## 🛠️ Troubleshooting

### Common Issues

**1. Profile not loading:**
```bash
# Check active profile
echo $SPRING_PROFILES_ACTIVE

# Set profile explicitly
export SPRING_PROFILES_ACTIVE=dev
```

**2. Database connection failed:**
```bash
# Check database variables
echo $DB_URL
echo $DB_USERNAME
echo $DB_PASSWORD

# Test connection
psql $DB_URL -U $DB_USERNAME
```

**3. S3 access denied:**
```bash
# Check S3 variables
echo $S3_BUCKET
echo $S3_REGION

# Verify AWS credentials
aws s3 ls s3://$S3_BUCKET
```

**4. Configuration not applied:**
```bash
# Check if profile files exist
ls atas-framework/src/main/resources/application-*.yml
ls atas-tests/src/test/resources/application-*.yml

# Verify environment variables
env | grep -E "(SPRING|DB_|S3_)"
```

### Debug Mode

Enable debug logging to troubleshoot configuration:

```bash
# Framework debug
SPRING_PROFILES_ACTIVE=dev make run

# Test debug
SPRING_PROFILES_ACTIVE=dev make test
```

## 📋 Best Practices

### Security
- Never commit secrets to version control
- Use CI/CD secret management
- Rotate credentials regularly
- Use least-privilege access

### Configuration Management
- Use environment variables for all external dependencies
- Keep configuration files in version control
- Document environment-specific requirements
- Test configuration in all environments

### Deployment
- Use different S3 buckets for different environments
- Monitor configuration changes in production
- Validate configuration before deployment
- Use infrastructure as code for consistency

## 🔄 Migration Guide

### From Hardcoded Values

1. **Identify hardcoded values** in configuration files
2. **Replace with environment variables** using `${VARIABLE:default}` syntax
3. **Create profile-specific files** for environment differences
4. **Update deployment scripts** to set environment variables
5. **Test in all environments** to ensure configuration works

### Configuration Validation

The framework includes validation to ensure all required settings are present. Check the application logs for configuration validation messages.

---

**Need help?** Check the main [README.md](../README.md) for project setup or the [API Reference](API_REFERENCE.md) for detailed endpoint information.
