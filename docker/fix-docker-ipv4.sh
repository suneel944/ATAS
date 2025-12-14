#!/bin/bash
# Script to configure Docker to prefer IPv4 over IPv6
# This fixes the "network is unreachable" error when pulling images

echo "🔧 Configuring Docker to prefer IPv4..."

# Create docker config directory if it doesn't exist
sudo mkdir -p /etc/docker

# Check if daemon.json exists
if [ -f /etc/docker/daemon.json ]; then
    echo "📋 Backing up existing /etc/docker/daemon.json..."
    sudo cp /etc/docker/daemon.json /etc/docker/daemon.json.backup
fi

# Create or update daemon.json with IPv4 preference
sudo tee /etc/docker/daemon.json > /dev/null <<EOF
{
  "ipv6": false,
  "fixed-cidr-v6": "",
  "experimental": false,
  "dns": ["8.8.8.8", "8.8.4.4"]
}
EOF

echo "✅ Docker configuration updated!"
echo ""
echo "🔄 Restarting Docker service..."
sudo systemctl restart docker

echo ""
echo "✅ Docker has been configured to use IPv4 only."
echo "💡 You can now try: make dev"

