#!/bin/bash

# ATAS Kubernetes Deployment Test Script
# This script tests the Kubernetes deployment step by step

set -e

# Colors for output
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="atas"
TIMEOUT=300  # 5 minutes timeout for deployments

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ATAS Kubernetes Deployment Test${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

# Check kubectl
if ! command -v kubectl &> /dev/null; then
    # Try common locations
    if [ -f /usr/local/bin/kubectl ]; then
        export PATH="/usr/local/bin:$PATH"
    elif [ -f /usr/bin/kubectl ]; then
        export PATH="/usr/bin:$PATH"
    else
        echo -e "${RED}❌ kubectl is not installed${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✅ kubectl found at $(which kubectl)${NC}"

# Check if kubectl is configured
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}❌ kubectl is not configured or cluster is not accessible${NC}"
    echo ""
    echo -e "${YELLOW}Current context:${NC}"
    kubectl config current-context 2>/dev/null || echo "  (none)"
    echo ""
    echo -e "${YELLOW}Error details:${NC}"
    kubectl cluster-info 2>&1 | grep -i "error\|dial\|lookup" | head -2 || echo "  Cluster connection failed"
    echo ""
    echo -e "${YELLOW}Possible solutions:${NC}"
    echo "  1. Check network/VPN connection to AWS"
    echo "  2. Verify EKS cluster is running: aws eks describe-cluster --name <cluster-name>"
    echo "  3. Use local cluster instead: make k8s-test-local"
    echo "  4. Set up minikube: minikube start"
    echo ""
    exit 1
fi
echo -e "${GREEN}✅ kubectl is configured${NC}"

# Check cluster connection
CLUSTER_NAME=$(kubectl config current-context 2>/dev/null || echo "unknown")
echo -e "${GREEN}✅ Connected to cluster: ${CLUSTER_NAME}${NC}"
echo ""

# Step 1: Create namespace
echo -e "${BLUE}Step 1: Creating namespace...${NC}"
kubectl apply -f namespace.yaml
kubectl wait --for=condition=Active namespace/${NAMESPACE} --timeout=30s || true
echo -e "${GREEN}✅ Namespace created${NC}"
echo ""

# Step 2: Create secrets
echo -e "${BLUE}Step 2: Creating secrets...${NC}"
kubectl apply -f secrets.yaml
echo -e "${GREEN}✅ Secrets created${NC}"
echo ""

# Step 3: Create configmap
echo -e "${BLUE}Step 3: Creating configmap...${NC}"
kubectl apply -f configmap.yaml
echo -e "${GREEN}✅ ConfigMap created${NC}"
echo ""

# Step 4: Deploy database
echo -e "${BLUE}Step 4: Deploying database...${NC}"
kubectl apply -f database.yaml
echo -e "${YELLOW}Waiting for database to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=atas-db -n ${NAMESPACE} --timeout=${TIMEOUT}s || {
    echo -e "${RED}❌ Database pod not ready within ${TIMEOUT}s${NC}"
    echo -e "${YELLOW}Checking database pod status...${NC}"
    kubectl get pods -n ${NAMESPACE} -l app=atas-db
    kubectl describe pod -l app=atas-db -n ${NAMESPACE} | tail -20
    exit 1
}
echo -e "${GREEN}✅ Database is ready${NC}"
echo ""

# Step 5: Deploy Redis
echo -e "${BLUE}Step 5: Deploying Redis...${NC}"
kubectl apply -f redis.yaml
echo -e "${YELLOW}Waiting for Redis to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=atas-redis -n ${NAMESPACE} --timeout=${TIMEOUT}s || {
    echo -e "${RED}❌ Redis pod not ready within ${TIMEOUT}s${NC}"
    echo -e "${YELLOW}Checking Redis pod status...${NC}"
    kubectl get pods -n ${NAMESPACE} -l app=atas-redis
    kubectl describe pod -l app=atas-redis -n ${NAMESPACE} | tail -20
    exit 1
}
echo -e "${GREEN}✅ Redis is ready${NC}"

# Test Redis connectivity
echo -e "${YELLOW}Testing Redis connectivity...${NC}"
REDIS_POD=$(kubectl get pod -l app=atas-redis -n ${NAMESPACE} -o jsonpath='{.items[0].metadata.name}')
if kubectl exec -n ${NAMESPACE} ${REDIS_POD} -- redis-cli ping &> /dev/null; then
    echo -e "${GREEN}✅ Redis is responding to ping${NC}"
else
    echo -e "${RED}❌ Redis is not responding${NC}"
    exit 1
fi
echo ""

# Step 6: Deploy ATAS service
echo -e "${BLUE}Step 6: Deploying ATAS service...${NC}"
echo -e "${YELLOW}Note: Make sure to update deployment.yaml with your image registry${NC}"
kubectl apply -f deployment.yaml
echo -e "${YELLOW}Waiting for ATAS service to be ready...${NC}"
kubectl wait --for=condition=ready pod -l app=atas-service -n ${NAMESPACE} --timeout=${TIMEOUT}s || {
    echo -e "${RED}❌ ATAS service pod not ready within ${TIMEOUT}s${NC}"
    echo -e "${YELLOW}Checking ATAS service pod status...${NC}"
    kubectl get pods -n ${NAMESPACE} -l app=atas-service
    kubectl describe pod -l app=atas-service -n ${NAMESPACE} | tail -30
    echo -e "${YELLOW}Checking logs...${NC}"
    kubectl logs -l app=atas-service -n ${NAMESPACE} --tail=50
    exit 1
}
echo -e "${GREEN}✅ ATAS service is ready${NC}"
echo ""

# Step 7: Create service
echo -e "${BLUE}Step 7: Creating service...${NC}"
kubectl apply -f service.yaml
echo -e "${GREEN}✅ Service created${NC}"
echo ""

# Step 8: Verify all pods are running
echo -e "${BLUE}Step 8: Verifying all pods are running...${NC}"
sleep 5
kubectl get pods -n ${NAMESPACE}

ALL_RUNNING=true
for POD in $(kubectl get pods -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}'); do
    STATUS=$(kubectl get pod ${POD} -n ${NAMESPACE} -o jsonpath='{.status.phase}')
    if [ "$STATUS" != "Running" ]; then
        echo -e "${RED}❌ Pod ${POD} is not running (status: ${STATUS})${NC}"
        ALL_RUNNING=false
    fi
done

if [ "$ALL_RUNNING" = true ]; then
    echo -e "${GREEN}✅ All pods are running${NC}"
else
    echo -e "${RED}❌ Some pods are not running${NC}"
    exit 1
fi
echo ""

# Step 9: Test service connectivity
echo -e "${BLUE}Step 9: Testing service connectivity...${NC}"
SERVICE_POD=$(kubectl get pod -l app=atas-service -n ${NAMESPACE} -o jsonpath='{.items[0].metadata.name}')

# Test health endpoint
echo -e "${YELLOW}Testing health endpoint...${NC}"
if kubectl exec -n ${NAMESPACE} ${SERVICE_POD} -- wget -q -O- http://localhost:8080/actuator/health &> /dev/null; then
    echo -e "${GREEN}✅ Health endpoint is accessible${NC}"
else
    echo -e "${YELLOW}⚠️  Health endpoint test skipped (wget may not be available)${NC}"
fi

# Test Redis connection from service pod
echo -e "${YELLOW}Testing Redis connection from service pod...${NC}"
if kubectl exec -n ${NAMESPACE} ${SERVICE_POD} -- sh -c "echo 'PING' | nc atas-redis 6379" &> /dev/null; then
    echo -e "${GREEN}✅ Redis is accessible from service pod${NC}"
else
    echo -e "${YELLOW}⚠️  Redis connectivity test skipped (nc may not be available)${NC}"
fi

# Test database connection from service pod
echo -e "${YELLOW}Testing database connection from service pod...${NC}"
if kubectl exec -n ${NAMESPACE} ${SERVICE_POD} -- sh -c "echo 'SELECT 1;' | psql -h atas-db -U atas -d atasdb" &> /dev/null; then
    echo -e "${GREEN}✅ Database is accessible from service pod${NC}"
else
    echo -e "${YELLOW}⚠️  Database connectivity test skipped (psql may not be available)${NC}"
fi
echo ""

# Step 10: Check services
echo -e "${BLUE}Step 10: Checking services...${NC}"
kubectl get svc -n ${NAMESPACE}
echo ""

# Step 11: Set up local access (for local clusters)
echo -e "${BLUE}Step 11: Setting up local access...${NC}"
# Check for local clusters by node name or context
if kubectl get nodes 2>/dev/null | grep -qiE "kind|minikube|k3s|control-plane" || \
   kubectl config current-context 2>/dev/null | grep -qiE "kind|minikube|k3s"; then
    echo -e "${YELLOW}Local cluster detected - setting up port-forward...${NC}"
    # Check if port-forward is already running
    if pgrep -f "kubectl port-forward.*atas-service.*8080" > /dev/null; then
        echo -e "${GREEN}✅ Port-forward already running${NC}"
    else
        echo -e "${BLUE}Starting port-forward in background...${NC}"
        kubectl port-forward -n ${NAMESPACE} svc/atas-service 8080:8080 > /tmp/k8s-port-forward.log 2>&1 &
        sleep 2
        if pgrep -f "kubectl port-forward.*atas-service.*8080" > /dev/null; then
            echo -e "${GREEN}✅ Port-forward started${NC}"
        else
            echo -e "${YELLOW}⚠️  Port-forward failed to start (check /tmp/k8s-port-forward.log)${NC}"
        fi
    fi
else
    echo -e "${YELLOW}Remote cluster detected - port-forward not started automatically${NC}"
    echo -e "${YELLOW}Run manually: kubectl port-forward svc/atas-service 8080:8080 -n ${NAMESPACE}${NC}"
fi
echo ""

# Step 12: Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✅ Deployment test completed successfully!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Access Information:${NC}"
if pgrep -f "kubectl port-forward.*atas-service.*8080" > /dev/null; then
    echo -e "${GREEN}✅ Service available at: http://localhost:8080${NC}"
    echo -e "${GREEN}✅ Dashboard: http://localhost:8080/monitoring/dashboard${NC}"
    echo -e "${GREEN}✅ Health: http://localhost:8080/actuator/health${NC}"
else
    echo -e "${YELLOW}To access the service, run:${NC}"
    echo "  kubectl port-forward svc/atas-service 8080:8080 -n ${NAMESPACE}"
    echo "  Then visit: http://localhost:8080/monitoring/dashboard"
fi
echo ""
echo -e "${YELLOW}Other commands:${NC}"
echo "1. Check pod logs: kubectl logs -f deployment/atas-service -n ${NAMESPACE}"
echo "2. Stop port-forward: pkill -f 'kubectl port-forward.*atas-service'"
echo "3. Deploy HPA: kubectl apply -f hpa.yaml"
echo "4. Deploy Ingress: kubectl apply -f ingress.yaml"
echo ""

