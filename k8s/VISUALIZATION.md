# Kubernetes Cluster Visualization Tools

## k9s (Terminal UI) - Recommended

k9s is a terminal-based UI for Kubernetes that provides a fast and easy way to navigate, observe, and manage your clusters.

### Installation

```bash
# Linux (using snap)
sudo snap install k9s

# Or download binary
wget https://github.com/derailed/k9s/releases/latest/download/k9s_Linux_amd64.tar.gz
tar -xzf k9s_Linux_amd64.tar.gz
sudo mv k9s /usr/local/bin/

# Or using package manager
# Arch Linux
yay -S k9s

# macOS
brew install k9s
```

### Usage

```bash
# Start k9s (connects to current kubectl context)
k9s

# Start k9s in specific namespace
k9s -n atas

# Start k9s with all namespaces
k9s -A
```

### k9s Key Bindings

- `0-9` - View different resources (0=pods, 1=deployments, 2=services, etc.)
- `/` - Filter/search
- `:` - Command mode
- `d` - Describe resource
- `e` - Edit resource
- `l` - View logs
- `s` - Shell into pod
- `x` - Delete resource
- `?` - Help
- `q` - Quit

### k9s Views for ATAS

```bash
# View pods
k9s -n atas
# Press '0' for pods view

# View deployments
# Press '1' for deployments

# View services
# Press '2' for services

# View configmaps
# Press 'c' then select configmap

# View secrets
# Press 's' then select secret
```

## Other Visualization Tools

### 1. Kubernetes Dashboard (Web UI)

**Installation:**
```bash
# Install dashboard
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# Create service account
kubectl create serviceaccount dashboard-admin-sa -n kubernetes-dashboard
kubectl create clusterrolebinding dashboard-admin-sa --clusterrole=cluster-admin --serviceaccount=kubernetes-dashboard:dashboard-admin-sa

# Get token
kubectl -n kubernetes-dashboard create token dashboard-admin-sa

# Access dashboard
kubectl proxy
# Open: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/
```

### 2. Lens (Desktop App)

**Installation:**
```bash
# Download from: https://k8slens.dev/
# Or using snap
sudo snap install kontena-lens --classic
```

**Features:**
- Beautiful desktop UI
- Multi-cluster support
- Resource monitoring
- Terminal access
- Log viewing

### 3. Octant (Web UI)

**Installation:**
```bash
# Download from: https://github.com/vmware-tanzu/octant/releases
# Or using snap
sudo snap install octant
```

**Usage:**
```bash
octant
# Opens browser at http://localhost:7777
```

### 4. kubectl with Aliases

**Quick aliases:**
```bash
# Add to ~/.bashrc or ~/.zshrc
alias kgp='kubectl get pods'
alias kgs='kubectl get svc'
alias kgd='kubectl get deployment'
alias kdp='kubectl describe pod'
alias klf='kubectl logs -f'
alias kga='kubectl get all'
```

**kubectl plugins:**
```bash
# Install krew (kubectl plugin manager)
(
  set -x; cd "$(mktemp -d)" &&
  OS="$(uname | tr '[:upper:]' '[:lower:]')" &&
  ARCH="$(uname -m | sed -e 's/x86_64/amd64/' -e 's/\(arm\)\(64\)\?.*/\1\2/' -e 's/aarch64$/arm64/')" &&
  KREW="krew-${OS}_${ARCH}" &&
  curl -fsSLO "https://github.com/kubernetes-sigs/krew/releases/latest/download/${KREW}.tar.gz" &&
  tar zxvf "${KREW}.tar.gz" &&
  ./"${KREW}" install krew
)

# Add to PATH
export PATH="${KREW_ROOT:-$HOME/.krew}/bin:$PATH"

# Install useful plugins
kubectl krew install tree
kubectl krew install view-utilization
kubectl krew install resource-capacity
```

## Quick Commands for ATAS Visualization

### Using kubectl

```bash
# Watch all resources in namespace
watch kubectl get all -n atas

# Tree view of resources
kubectl tree deployment atas-service -n atas

# Get all resources with wide output
kubectl get all -n atas -o wide

# Describe all resources
kubectl describe all -n atas

# Get resources as YAML
kubectl get all -n atas -o yaml
```

### Using k9s

```bash
# Start k9s in ATAS namespace
k9s -n atas

# Then:
# - Press '0' to see pods
# - Press '1' to see deployments
# - Press '2' to see services
# - Press 'c' then select configmap to see configmaps
# - Press 's' then select secret to see secrets (values hidden)
# - Select a pod and press 'l' to view logs
# - Select a pod and press 's' to shell into it
# - Press '/' to filter/search
```

## Monitoring Tools

### 1. kubectl top

```bash
# Resource usage
kubectl top pods -n atas
kubectl top nodes

# Watch resource usage
watch kubectl top pods -n atas
```

### 2. Prometheus + Grafana

For production monitoring, set up Prometheus and Grafana:
- Prometheus for metrics collection
- Grafana for visualization
- Both can be installed via Helm charts

## Recommended Setup for ATAS

**For Development:**
- **k9s** - Fast terminal UI for quick checks
- **kubectl aliases** - Quick commands

**For Production:**
- **Lens** or **Kubernetes Dashboard** - Full web UI
- **Prometheus + Grafana** - Metrics and monitoring
- **k9s** - Quick terminal access

## Quick Start with k9s

```bash
# 1. Install k9s
sudo snap install k9s

# 2. Start k9s in ATAS namespace
k9s -n atas

# 3. Navigate:
#    - Press '0' for pods
#    - Select a pod, press 'l' for logs
#    - Press 'd' to describe
#    - Press 's' to shell into pod
#    - Press '?' for help
```

## Custom k9s Configuration

Create `~/.config/k9s/config.yml`:

```yaml
k9s:
  liveViewAutoRefresh: true
  refreshRate: 2
  maxConnRetry: 5
  readOnly: false
  noExitOnCtrlC: false
  ui:
    enableMouse: true
    headless: false
    logoless: false
    crumbsless: false
    reactive: false
    noIcons: false
  skipLatestRevCheck: false
  disablePodCounting: false
  shellPod:
    image: busybox:1.35.0
    namespace: default
    limits:
      cpu: 100m
      memory: 100Mi
```

## Tips

1. **Use k9s for quick checks** - Fastest way to see what's happening
2. **Use Lens for detailed analysis** - Better for complex debugging
3. **Use kubectl for automation** - Scripts and CI/CD
4. **Combine tools** - Use k9s for daily work, Lens for deep dives

