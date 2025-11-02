#!/bin/bash

# ATAS Docker Build Script
# This script builds optimized Docker images for different environments
# Note: Docker Compose will automatically read .env file from project root

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

# Default values
ENVIRONMENT="dev"
PUSH=false
CLEAN=false
TAG="latest"

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

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -e, --environment ENV    Environment to build for (dev|prod) [default: dev]"
    echo "  -t, --tag TAG           Docker tag to use [default: latest]"
    echo "  -p, --push              Push images to registry after building"
    echo "  -c, --clean             Clean up dangling images and build cache"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Build dev environment"
    echo "  $0 -e prod -t v1.0.0                # Build production with tag v1.0.0"
    echo "  $0 -e dev -p                         # Build dev and push to registry"
    echo "  $0 -c                                # Clean up and build dev"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -p|--push)
            PUSH=true
            shift
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate environment
if [[ "$ENVIRONMENT" != "dev" && "$ENVIRONMENT" != "prod" ]]; then
    print_error "Invalid environment: $ENVIRONMENT. Must be 'dev' or 'prod'"
    exit 1
fi

print_status "Building ATAS Docker images for $ENVIRONMENT environment with tag $TAG"

# Clean up if requested
if [[ "$CLEAN" == true ]]; then
    print_status "Cleaning up dangling images and build cache..."
    docker system prune -f
    docker builder prune -f
fi

# Set Dockerfile based on environment
if [[ "$ENVIRONMENT" == "prod" ]]; then
    DOCKERFILE="docker/Dockerfile.prod"
    COMPOSE_FILE="docker/docker-compose.prod.yml"
else
    DOCKERFILE="docker/Dockerfile"
    COMPOSE_FILE="docker/docker-compose-local-db.yml"
fi

# Note: Docker Compose will automatically read .env file from project root
# If .env doesn't exist, Docker Compose will use defaults defined in the compose files

# Build the image
print_status "Building image using $DOCKERFILE..."
IMAGE_NAME="atas-service:$TAG"

if [[ "$ENVIRONMENT" == "prod" ]]; then
    docker build -f "$DOCKERFILE" -t "$IMAGE_NAME" --build-arg BUILDKIT_INLINE_CACHE=1 .
else
    docker build -f "$DOCKERFILE" -t "$IMAGE_NAME" --build-arg BUILDKIT_INLINE_CACHE=1 .
fi

if [[ $? -eq 0 ]]; then
    print_success "Image built successfully: $IMAGE_NAME"
    
    # Show image size
    IMAGE_SIZE=$(docker images --format "table {{.Size}}" "$IMAGE_NAME" | tail -n 1)
    print_status "Image size: $IMAGE_SIZE"
else
    print_error "Failed to build image"
    exit 1
fi

# Push to registry if requested
if [[ "$PUSH" == true ]]; then
    print_status "Pushing image to registry..."
    docker push "$IMAGE_NAME"
    if [[ $? -eq 0 ]]; then
        print_success "Image pushed successfully"
    else
        print_error "Failed to push image"
        exit 1
    fi
fi

# Show build summary
print_status "Build Summary:"
echo "  Environment: $ENVIRONMENT"
echo "  Dockerfile: $DOCKERFILE"
echo "  Image: $IMAGE_NAME"
echo "  Size: $IMAGE_SIZE"
echo "  Compose file: $COMPOSE_FILE"

print_success "Build completed successfully!"
print_status "To run the application:"
echo "  docker compose -f $COMPOSE_FILE up -d"
echo "  # Or use: make docker-up"
