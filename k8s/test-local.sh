#!/bin/bash

# ATAS Kubernetes Local Testing Script
# For testing with minikube, kind, or k3s

set -e

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

NAMESPACE="atas"

echo -e "${BLUE}ATAS Kubernetes Local Test Setup${NC}"
echo ""

# Detect local Kubernetes cluster
if kubectl get nodes | grep -q "minikube"; then
    CLUSTER_TYPE="minikube"
    echo -e "${GREEN}Detected: Minikube${NC}"
elif kubectl get nodes | grep -q "kind"; then
    CLUSTER_TYPE="kind"
    echo -e "${GREEN}Detected: Kind${NC}"
elif kubectl get nodes | grep -q "k3s"; then
    CLUSTER_TYPE="k3s"
    echo -e "${GREEN}Detected: K3s${NC}"
else
    CLUSTER_TYPE="unknown"
    echo -e "${YELLOW}Unknown cluster type${NC}"
fi

# Build and load image for local cluster
echo -e "${BLUE}Building Docker image...${NC}"
cd ..
docker build -f docker/Dockerfile.prod -t atas-service:local .

case $CLUSTER_TYPE in
    minikube)
        echo -e "${BLUE}Loading image into minikube...${NC}"
        minikube image load atas-service:local
        ;;
    kind)
        echo -e "${BLUE}Loading image into kind...${NC}"
        kind load docker-image atas-service:local
        ;;
    k3s)
        echo -e "${YELLOW}For k3s, you may need to push to a registry or use k3d image import${NC}"
        ;;
esac

cd k8s

# Update deployment to use local image
echo -e "${BLUE}Updating deployment to use local image...${NC}"
sed -i.bak 's|image:.*atas-service.*|image: atas-service:local|' deployment.yaml

# Run deployment test
echo -e "${BLUE}Running deployment test...${NC}"
chmod +x test-deployment.sh
./test-deployment.sh

# Restore original deployment
if [ -f deployment.yaml.bak ]; then
    mv deployment.yaml.bak deployment.yaml
fi

echo -e "${GREEN}Local test setup complete!${NC}"

