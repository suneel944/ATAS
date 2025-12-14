#!/bin/bash

# Setup local Kubernetes cluster using kind

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

CLUSTER_NAME="atas-local"

echo -e "${BLUE}Setting up local Kubernetes cluster with kind...${NC}"
echo ""

# Check if kind is installed
if ! command -v kind &> /dev/null; then
    echo -e "${YELLOW}kind is not installed. Installing to ~/.local/bin...${NC}"
    mkdir -p ~/.local/bin
    curl -Lo ~/.local/bin/kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
    chmod +x ~/.local/bin/kind
    export PATH="$HOME/.local/bin:$PATH"
    echo -e "${GREEN}✅ kind installed${NC}"
fi

# Ensure kind is in PATH
export PATH="$HOME/.local/bin:$PATH"

# Check if cluster already exists
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
    echo -e "${GREEN}Cluster ${CLUSTER_NAME} already exists${NC}"
    kubectl cluster-info --context kind-${CLUSTER_NAME} &>/dev/null || {
        echo -e "${YELLOW}Cluster exists but context not set. Setting context...${NC}"
        kubectl config use-context kind-${CLUSTER_NAME}
    }
    echo -e "${GREEN}✅ Using existing cluster${NC}"
    exit 0
fi

# Create cluster
echo -e "${BLUE}Creating kind cluster: ${CLUSTER_NAME}...${NC}"
cat <<EOF | kind create cluster --name ${CLUSTER_NAME} --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 8080
    hostPort: 8080
    protocol: TCP
EOF

echo -e "${GREEN}✅ Cluster created${NC}"

# Set kubectl context
kubectl cluster-info --context kind-${CLUSTER_NAME}

# Install metrics-server for CPU/RAM stats in k9s
echo -e "${BLUE}Installing metrics-server...${NC}"
kubectl apply -f metrics-server.yaml 2>/dev/null || {
    echo -e "${YELLOW}⚠️  Could not install metrics-server from k8s/metrics-server.yaml$(NC)"
    echo -e "${YELLOW}   Installing via kubectl apply from official manifest...$(NC)"
    kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml 2>/dev/null || \
    kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.7.0/components.yaml || {
        echo -e "${YELLOW}⚠️  Metrics-server installation failed. CPU/RAM stats in k9s may not work.$(NC)"
    }
}

echo -e "${GREEN}Waiting for metrics-server to be ready...${NC}"
kubectl wait --namespace kube-system \
  --for=condition=ready pod \
  --selector=k8s-app=metrics-server \
  --timeout=90s 2>/dev/null || echo -e "${YELLOW}⚠️  Metrics-server may still be starting$(NC)"

echo ""
echo -e "${GREEN}✅ Local Kubernetes cluster is ready!${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Build and load image: make k8s-test-local"
echo "2. Or manually: docker build -f docker/Dockerfile.prod -t atas-service:local ."
echo "3. Then: kind load docker-image atas-service:local --name ${CLUSTER_NAME}"
echo "4. Deploy: make k8s-deploy"
echo ""

