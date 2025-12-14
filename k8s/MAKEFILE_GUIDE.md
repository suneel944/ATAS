# Makefile Guide for Kubernetes Deployment

This guide explains the proper way to use the Makefile to run the ATAS project via Kubernetes.

## Quick Start (Recommended)

For **first-time setup** or **complete local deployment**, use:

```bash
make k8s-test-local
```

This single command will:
1. ✅ Check if `kind` is installed (installs if missing)
2. ✅ Check if local cluster exists (creates if missing)
3. ✅ Build the Docker image (`atas-service:local`)
4. ✅ Load the image into the kind cluster
5. ✅ Deploy all Kubernetes resources (namespace, secrets, configmap, database, Redis, service)
6. ✅ Set up port-forward to `localhost:8080`
7. ✅ Verify the service is accessible
8. ✅ Install metrics-server (for CPU/RAM stats in k9s)

**After running this, your service will be available at:**
- **API**: http://localhost:8080
- **Dashboard**: http://localhost:8080/monitoring/dashboard
- **Health Check**: http://localhost:8080/actuator/health

---

## Step-by-Step Workflow

### 1. Initial Setup (One-time)

```bash
# Set up local Kubernetes cluster (kind)
make k8s-setup-local
```

This creates a local `kind` cluster named `atas-local` and installs metrics-server.

**Note**: `k8s-test-local` does this automatically, so you can skip this step.

---

### 2. Deploy Everything (Recommended)

```bash
# Complete deployment (builds image, loads into cluster, deploys everything)
make k8s-test-local
```

**OR** if you already have the image built and cluster ready:

```bash
# Deploy Kubernetes resources only (assumes image is already in cluster)
make k8s-deploy
```

**Difference:**
- `k8s-test-local`: Builds image + loads into cluster + deploys
- `k8s-deploy`: Only deploys (assumes image exists in cluster or registry)

---

### 3. Check Status

```bash
# View pods, services, and HPA status
make k8s-status
```

**Output shows:**
- Pod status (Running, Pending, etc.)
- Service endpoints
- HPA (Horizontal Pod Autoscaler) metrics

---

### 4. View Logs

```bash
# Stream logs from the ATAS service
make k8s-logs
```

**To view logs for specific pods:**
```bash
kubectl logs -n atas <pod-name> -f
```

---

### 5. Access Services Locally

The port-forward is automatically set up by `k8s-test-local` or `k8s-deploy` for local clusters.

**If port-forward is not running:**

```bash
# Start port-forward (runs in background)
make k8s-port-forward
```

**To stop port-forward:**

```bash
make k8s-stop-port-forward
```

---

### 6. Visualize Cluster

```bash
# Open k9s terminal UI (installs k9s if missing)
make k8s-visualize
```

**In k9s:**
- Press `0` to view pods
- Press `s` to view services
- Press `d` to describe a resource
- Press `l` to view logs
- Press `s` (on a pod) to shell into it
- Press `?` for help

---

### 7. Cleanup

**Quick cleanup (keeps cluster):**
```bash
# Delete namespace and stop port-forwards
make k8s-clean
```

**Deep cleanup (removes everything):**
```bash
# Delete namespace, kind cluster, and Docker volumes
make k8s-clean-deep
```

---

## Common Workflows

### Workflow 1: First-Time Setup

```bash
# 1. Complete setup and deployment
make k8s-test-local

# 2. Check status
make k8s-status

# 3. View logs (if needed)
make k8s-logs

# 4. Access dashboard
# Open: http://localhost:8080/monitoring/dashboard
```

---

### Workflow 2: Rebuild and Redeploy After Code Changes

```bash
# 1. Clean up old deployment
make k8s-clean

# 2. Rebuild and redeploy
make k8s-test-local

# 3. Verify
make k8s-status
```

---

### Workflow 3: Update Configuration Only

If you only changed `k8s/configmap.yaml` or `k8s/secrets.yaml`:

```bash
# 1. Update configmap
kubectl apply -f k8s/configmap.yaml

# 2. Update secrets
kubectl apply -f k8s/secrets.yaml

# 3. Restart pods to pick up changes
kubectl rollout restart deployment/atas-service -n atas

# 4. Check status
make k8s-status
```

---

### Workflow 4: Debugging

```bash
# 1. Check status
make k8s-status

# 2. View logs
make k8s-logs

# 3. Visualize in k9s
make k8s-visualize

# 4. Check pod details
kubectl describe pod <pod-name> -n atas

# 5. Shell into pod
kubectl exec -it <pod-name> -n atas -- /bin/bash
```

---

## Available Makefile Targets

| Target | Description |
|--------|-------------|
| `k8s-test-local` | **Recommended**: Complete local deployment (builds, loads, deploys) |
| `k8s-setup-local` | Set up local kind cluster (one-time) |
| `k8s-deploy` | Deploy Kubernetes resources (assumes image exists) |
| `k8s-status` | Check deployment status (pods, services, HPA) |
| `k8s-logs` | View ATAS service logs |
| `k8s-port-forward` | Port-forward service to localhost:8080 |
| `k8s-stop-port-forward` | Stop port-forward |
| `k8s-clean` | Clean up namespace and port-forwards |
| `k8s-clean-deep` | Deep clean (namespace, cluster, volumes) |
| `k8s-verify-access` | Verify service is accessible |
| `k8s-visualize` | Open k9s for cluster visualization |

---

## Prerequisites

1. **Docker**: For building images
2. **kubectl**: Kubernetes CLI (usually auto-installed with kind)
3. **kind**: Kubernetes in Docker (auto-installed by `k8s-setup-local`)
4. **k9s** (optional): For visualization (auto-installed by `k8s-visualize`)

---

## Troubleshooting

### Issue: "kubectl is not configured"

**Solution:**
```bash
# Create local cluster
make k8s-setup-local
```

---

### Issue: "Image pull error"

**Solution:**
```bash
# Rebuild and load image
make k8s-test-local
```

---

### Issue: "Port-forward not working"

**Solution:**
```bash
# Stop existing port-forwards
make k8s-stop-port-forward

# Start fresh
make k8s-port-forward
```

---

### Issue: "Service not accessible"

**Solution:**
```bash
# Check if port-forward is running
pgrep -f "kubectl port-forward"

# Restart port-forward
make k8s-port-forward

# Verify access
make k8s-verify-access
```

---

### Issue: "CPU/RAM stats showing n/a in k9s"

**Solution:**
```bash
# Metrics-server should be installed automatically
# Check if it's running:
kubectl get pods -n kube-system | grep metrics-server

# If not running, install manually:
kubectl apply -f k8s/metrics-server.yaml

# Wait 30-60 seconds for metrics to be available
```

---

## Production Deployment

For production (EKS, GKE, AKS), use:

```bash
# 1. Build and push image to registry
docker build -f docker/Dockerfile.prod -t your-registry/atas-service:latest .
docker push your-registry/atas-service:latest

# 2. Update k8s/deployment.yaml with your image
#    image: your-registry/atas-service:latest

# 3. Update k8s/secrets.yaml with production values

# 4. Deploy
make k8s-deploy
```

**Note**: For production, you'll need:
- Managed database (RDS, Cloud SQL, etc.)
- Managed Redis (ElastiCache, Memorystore, etc.)
- Ingress controller configured
- Proper secrets management (Secrets Manager, etc.)

---

## Summary

**For local development, always use:**
```bash
make k8s-test-local
```

This is the simplest and most reliable way to get everything running.

**For quick checks:**
- `make k8s-status` - Check what's running
- `make k8s-logs` - View logs
- `make k8s-visualize` - Open k9s

**For cleanup:**
- `make k8s-clean` - Quick cleanup
- `make k8s-clean-deep` - Complete cleanup

