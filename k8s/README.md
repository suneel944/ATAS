# Kubernetes Deployment Guide for ATAS

## Overview

This directory contains Kubernetes manifests for deploying ATAS in a cloud environment with horizontal scaling capabilities.

ATAS includes performance optimizations for dashboard and reporting:
- **Redis Caching**: Dashboard queries cached with different TTLs (60s overview, 30s recent, 5min trends)
- **Database Indexes**: Optimized indexes for fast dashboard queries
- **SQL Aggregation**: Native SQL queries instead of in-memory processing
- **Redis Pub/Sub**: SSE (Server-Sent Events) scaling across multiple pods
- **Pagination**: Date-range filtering and pagination for large datasets

## Architecture

```
                    ┌───────────────┐
                    │   Ingress     │
                    │  (NGINX/ALB)  │
                    └───────┬───────┘
                            │
           ┌────────────────┼──────────────┐
           │                │              │
    ┌──────▼───────┐ ┌──────▼─────┐ ┌──────▼──────┐
    │ ATAS Pod 1  │ │ ATAS Pod 2│ │ ATAS Pod 3 │
    │   (HPA)      │ │   (HPA)    │ │   (HPA)     │
    └───────┬──────┘ └─────┬──────┘ └─────┬───────┘
            │              │              │      
            └──────────────┼──────────────┘
                           │
                    ┌──────▼───────┐
                    │  PostgreSQL  │
                    │ (Managed DB) │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │    Redis     │
                    │(Cache/PubSub)│
                    └──────────────┘
    ```

## Prerequisites

- Kubernetes cluster (EKS, GKE, AKS, or self-managed)
- kubectl configured
- Container registry with ATAS image
- Metrics Server (for HPA)
- **Redis 8.4+** (for caching and SSE Pub/Sub) - can be in-cluster or managed service

### Visualization Tools (Optional but Recommended)

- **k9s** - Terminal UI for Kubernetes (recommended for quick checks)
  ```bash
  sudo snap install k9s
  # Or: brew install k9s (macOS)
  ```
- **Lens** - Desktop app for Kubernetes management
- **Kubernetes Dashboard** - Web UI for cluster management

See [VISUALIZATION.md](VISUALIZATION.md) for detailed setup and usage.

## Quick Start

### 1. Build and Push Image

```bash
# Build image
docker build -f docker/Dockerfile.prod -t your-registry/atas-service:latest .

# Push to registry
docker push your-registry/atas-service:latest
```

### 2. Update Image Reference

Edit `deployment.yaml`:
```yaml
image: your-registry/atas-service:latest
```

### 3. Configure Secrets

Edit `secrets.yaml` with your production values:
```bash
# Generate secure secrets
openssl rand -base64 32  # For JWT secrets
```

### 4. Deploy

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create secrets and config
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml

# Deploy database (or use managed service)
kubectl apply -f k8s/database.yaml

# Deploy Redis for caching and Pub/Sub
kubectl apply -f k8s/redis.yaml

# Deploy service
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Deploy HPA for auto-scaling
kubectl apply -f k8s/hpa.yaml

# Deploy Ingress (if using)
kubectl apply -f k8s/ingress.yaml
```

## Horizontal Scaling

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment atas-service -n atas --replicas=5

# Check current replicas
kubectl get deployment atas-service -n atas
```

### Auto-Scaling (HPA)

The HPA automatically scales based on:
- **CPU**: Target 70% utilization
- **Memory**: Target 80% utilization
- **Min Replicas**: 3
- **Max Replicas**: 10

```bash
# Check HPA status
kubectl get hpa atas-service-hpa -n atas

# View HPA events
kubectl describe hpa atas-service-hpa -n atas
```

### Custom Metrics (Advanced)

For scaling based on test execution queue:
1. Install Prometheus Adapter
2. Expose custom metrics from ATAS
3. Update `hpa.yaml` with custom metrics

## Cloud Provider Specifics

### AWS (EKS)

```yaml
# Use AWS Load Balancer Controller
# Update ingress.yaml annotations:
alb.ingress.kubernetes.io/scheme: internet-facing
alb.ingress.kubernetes.io/target-type: ip
alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'

# Use RDS for database (recommended)
# Update DB_URL in deployment.yaml:
DB_URL: "jdbc:postgresql://your-rds-endpoint:5432/atasdb"

# Use ElastiCache for Redis (recommended)
# Update REDIS_HOST in configmap.yaml:
REDIS_HOST: "your-elasticache-endpoint.cache.amazonaws.com"
# Update REDIS_PASSWORD in secrets.yaml with ElastiCache auth token
```

### GCP (GKE)

```yaml
# Use GCP Load Balancer
# Update service.yaml:
type: LoadBalancer
annotations:
  cloud.google.com/load-balancer-type: "External"

# Use Cloud SQL for database (recommended)
# Use Cloud SQL Proxy sidecar or update connection string

# Use Memorystore for Redis (recommended)
# Update REDIS_HOST in configmap.yaml with Memorystore Private IP
# Update network policies to allow access to Memorystore
```

### Azure (AKS)

```yaml
# Use Azure Application Gateway
# Update ingress.yaml with Azure-specific annotations

# Use Azure Database for PostgreSQL (recommended)

# Use Azure Cache for Redis (recommended)
# Update REDIS_HOST in configmap.yaml with Azure Cache connection string
# Update REDIS_PASSWORD in secrets.yaml with Azure Cache access key
```

## Monitoring

### Check Pod Status

```bash
# List all pods
kubectl get pods -n atas

# View pod logs
kubectl logs -f deployment/atas-service -n atas

# View logs from specific pod
kubectl logs atas-service-xxxxx -n atas
```

### Check Service

```bash
# Service endpoints
kubectl get endpoints atas-service -n atas

# Service details
kubectl describe service atas-service -n atas
```

### Resource Usage

```bash
# Pod resource usage
kubectl top pods -n atas

# Node resource usage
kubectl top nodes
```

### Performance Monitoring

**Check Redis Cache Performance:**
```bash
# Connect to Redis pod
kubectl exec -it deployment/atas-redis -n atas -- redis-cli

# Check cache stats
INFO stats
INFO memory

# Monitor cache keys
KEYS atas:cache:*
```

**Check Database Performance:**
```bash
# View database query performance (if enabled)
kubectl logs -f deployment/atas-service -n atas | grep "query"
```

**Monitor Cache Hit Rates:**
- Check application logs for cache hit/miss metrics
- Monitor Redis memory usage for cache size
- Dashboard response times should be <500ms with cache hits

## Redis Configuration

Redis is **required** for optimal performance. It provides:
- **Caching**: Dashboard queries cached to reduce database load
  - Dashboard Overview: 60s TTL
  - Recent Executions: 30s TTL
  - Execution Trends: 5min TTL
  - Execution Status: 10s TTL
- **Pub/Sub**: SSE (Server-Sent Events) scaling across multiple pods
  - Execution updates broadcast to all connected clients
  - Works seamlessly with horizontal scaling

### Option 1: In-Cluster Redis (Development/Testing)

Use `redis.yaml` for development/testing. The manifest includes:
- Redis 8.4-alpine deployment
- Persistent storage (2Gi PVC)
- Health checks
- Resource limits (256Mi-512Mi memory)

**Deployment:**
```bash
kubectl apply -f k8s/redis.yaml
```

**Verify:**
```bash
kubectl get pods -n atas -l app=atas-redis
kubectl get svc atas-redis -n atas
```

### Option 2: Managed Redis (Recommended for Production)

**AWS ElastiCache:**
```yaml
# Update deployment.yaml REDIS_HOST:
REDIS_HOST: "your-elasticache-endpoint.cache.amazonaws.com"
# Update secrets.yaml with Redis password
```

**GCP Memorystore:**
- Use Private IP connection
- Update REDIS_HOST in configmap

**Azure Cache for Redis:**
- Use connection string from Azure Portal
- Update REDIS_HOST and REDIS_PASSWORD

**Redis Sentinel/Cluster (High Availability):**
For production HA, deploy Redis Sentinel or use managed Redis Cluster:
```yaml
# Update redis.yaml with Sentinel configuration
# Or use Redis Operator (e.g., Redis Enterprise Operator)
```

### Redis Configuration Details

The application automatically configures Redis based on environment variables:
- `REDIS_HOST`: Redis service hostname (default: `atas-redis`)
- `REDIS_PORT`: Redis port (default: `6379`)
- `REDIS_PASSWORD`: Optional password (set in secrets)
- `REDIS_TIMEOUT`: Connection timeout (default: `2000ms`)
- `REDIS_POOL_MAX_ACTIVE`: Connection pool max active (default: `8`)
- `REDIS_POOL_MAX_IDLE`: Connection pool max idle (default: `8`)
- `REDIS_POOL_MIN_IDLE`: Connection pool min idle (default: `0`)

**Note:** If Redis is unavailable, the application will still work but:
- Caching will be disabled (all queries hit database)
- SSE updates will only work within a single pod (no cross-pod updates)

## Database Configuration

The database includes performance optimizations:
- **Indexes**: Optimized indexes on `test_executions` and `test_results` tables
- **Materialized Views**: Pre-computed dashboard metrics (optional)
- **Query Optimization**: Native SQL aggregation instead of in-memory processing
- **Pagination**: Date-range filtering for large datasets

These optimizations are automatically applied via Flyway migrations on application startup.

## Database Options

### Option 1: Managed Database (Recommended)

**AWS RDS:**
```yaml
# Update deployment.yaml DB_URL:
DB_URL: "jdbc:postgresql://your-rds-endpoint.region.rds.amazonaws.com:5432/atasdb"
```

**GCP Cloud SQL:**
- Use Cloud SQL Proxy sidecar
- Or use Private IP connection

**Azure Database:**
- Use connection string from Azure Portal

### Option 2: In-Cluster PostgreSQL

Use `database.yaml` for development/testing (not recommended for production).

## Storage

ATAS uses S3 for videos/screenshots, so no persistent volumes needed for the service pods.

If you need local storage for any reason:
```yaml
# Add to deployment.yaml
volumeMounts:
- name: temp-storage
  mountPath: /tmp
volumes:
- name: temp-storage
  emptyDir: {}
```

## Security

### Secrets Management

For production, use:
- **AWS**: Secrets Manager + External Secrets Operator
- **GCP**: Secret Manager + External Secrets Operator
- **Azure**: Key Vault + External Secrets Operator

### Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: atas-network-policy
  namespace: atas
spec:
  podSelector:
    matchLabels:
      app: atas-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: atas
    ports:
    - protocol: TCP
      port: 5432  # Database
    - protocol: TCP
      port: 6379  # Redis
  - {}  # Allow all outbound (for S3, external APIs)
```

## Troubleshooting

### Pods Not Starting

```bash
# Check pod events
kubectl describe pod <pod-name> -n atas

# Check logs
kubectl logs <pod-name> -n atas
```

### HPA Not Scaling

```bash
# Check metrics server
kubectl get apiservice v1beta1.metrics.k8s.io

# Check HPA status
kubectl describe hpa atas-service-hpa -n atas
```

### Database Connection Issues

```bash
# Test database connectivity from pod
kubectl exec -it <pod-name> -n atas -- \
  sh -c "echo 'SELECT 1;' | psql -h atas-db -U atas -d atasdb"
```

### Redis Connection Issues

```bash
# Test Redis connectivity from pod
kubectl exec -it <pod-name> -n atas -- \
  sh -c "redis-cli -h atas-redis ping"

# Check Redis service
kubectl get svc atas-redis -n atas

# View Redis logs
kubectl logs -f deployment/atas-redis -n atas

# Check Redis pod status
kubectl describe pod -l app=atas-redis -n atas

# Test Redis from application pod
kubectl exec -it <pod-name> -n atas -- \
  sh -c "echo 'PING' | nc atas-redis 6379"
```

**Common Issues:**
- **Connection refused**: Check if Redis pod is running and service is configured
- **Timeout**: Check network policies allow traffic on port 6379
- **Authentication failed**: Verify `REDIS_PASSWORD` in secrets matches Redis configuration
- **Cache not working**: Check application logs for Redis connection errors

## Performance Tuning

### Redis Cache Tuning

Adjust cache TTLs in `configmap.yaml` based on your needs:
- **Lower TTL** (30s): More real-time data, higher database load
- **Higher TTL** (5min): Better performance, slightly stale data

For high-traffic environments:
- Increase `REDIS_POOL_MAX_ACTIVE` to handle more concurrent connections
- Monitor Redis memory usage and adjust `maxmemory` policy
- Consider Redis Cluster for horizontal scaling

### Database Tuning

The application includes optimized queries, but you can further tune:
- Monitor slow query logs
- Adjust connection pool size in deployment resources
- Consider read replicas for dashboard queries
- Schedule materialized view refresh (if using)

### Horizontal Scaling Considerations

With Redis Pub/Sub, SSE works across all pods:
- All pods subscribe to Redis channels
- Execution updates broadcast to all connected clients
- No single point of failure for real-time updates

**Scaling Recommendations:**
- Start with 3 replicas (HPA minimum)
- Monitor CPU/Memory metrics
- Adjust HPA thresholds based on actual load
- Consider separate scaling for read-heavy vs write-heavy operations

## Production Checklist

- [ ] Update image registry in deployment.yaml
- [ ] Configure production secrets
- [ ] Set up managed database (RDS/Cloud SQL/Azure DB)
- [ ] Set up managed Redis (ElastiCache/Memorystore/Azure Cache) or configure Redis HA
- [ ] Verify Redis connectivity and cache performance
- [ ] Configure Ingress with TLS
- [ ] Set up monitoring (Prometheus/Grafana) with Redis metrics
- [ ] Configure log aggregation (CloudWatch/Stackdriver)
- [ ] Set up backup strategy for database and Redis (if using persistence)
- [ ] Configure resource limits based on load testing
- [ ] Set up alerting for HPA scaling events and Redis memory usage
- [ ] Review and adjust HPA min/max replicas
- [ ] Configure network policies (including Redis port 6379)
- [ ] Test SSE functionality across multiple pods
- [ ] Verify cache hit rates and dashboard performance
- [ ] Set up CI/CD pipeline for deployments



