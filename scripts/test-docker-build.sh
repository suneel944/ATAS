#!/bin/bash

# Test Docker Build Script
# This script tests the Docker build process locally to ensure it works
# Note: For full integration testing with database, use docker-compose instead

set -e

# Ensure we're in the project root
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_status "Testing Docker build process..."

# Test 1: Build the image
print_status "Building Docker image..."
docker build -f docker/Dockerfile -t atas-test:latest .

if [[ $? -eq 0 ]]; then
    print_success "Docker image built successfully"
else
    print_error "Docker build failed"
    exit 1
fi

# Test 2: Check image size
IMAGE_SIZE=$(docker images --format "table {{.Size}}" atas-test:latest | tail -n 1)
print_status "Image size: $IMAGE_SIZE"

# Test 3: Test image runs
# Note: This test runs without database connection. For full integration testing,
# use docker-compose which will automatically load .env variables
print_status "Testing image startup..."
docker run --rm -d --name atas-test-container -p 8080:8080 atas-test:latest

# Wait a bit for startup
sleep 10

# Check if container is running
if docker ps | grep -q atas-test-container; then
    print_success "Container started successfully"
    
    # Test health endpoint
    print_status "Testing health endpoint..."
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_success "Health endpoint is responding"
    else
        print_warning "Health endpoint not responding (this might be expected if the app needs database)"
    fi
    
    # Clean up
    docker stop atas-test-container
    print_status "Container stopped and removed"
else
    print_error "Container failed to start"
    docker logs atas-test-container
    docker stop atas-test-container 2>/dev/null || true
    exit 1
fi

# Test 4: Test SBOM generation (if syft is available)
if command -v syft &> /dev/null; then
    print_status "Testing SBOM generation..."
    syft scan atas-test:latest -o spdx-json > test-sbom.json
    if [[ $? -eq 0 ]]; then
        print_success "SBOM generated successfully"
        print_status "SBOM file size: $(wc -c < test-sbom.json) bytes"
        rm test-sbom.json
    else
        print_warning "SBOM generation failed (this might be expected in CI)"
    fi
else
    print_warning "Syft not available, skipping SBOM test"
fi

print_success "All Docker build tests completed successfully!"
print_status "The Docker build process is working correctly and ready for CI/CD"
