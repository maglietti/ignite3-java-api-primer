#!/bin/bash

# Initialize Apache Ignite 3 Cluster
# This script starts and initializes the 3-node cluster for reference applications

set -e

# Function to detect Docker Compose command
detect_docker_compose() {
    if command -v docker-compose >/dev/null 2>&1; then
        echo "docker-compose"
    elif docker compose version >/dev/null 2>&1; then
        echo "docker compose"
    else
        echo "ERROR: Neither 'docker-compose' nor 'docker compose' command found." >&2
        echo "Please install Docker Compose version 2.23.1 or newer." >&2
        echo "Installation guide: https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker" >&2
        exit 1
    fi
}

# Function to check Docker Compose version
check_docker_compose_version() {
    local compose_cmd="$1"
    
    if [[ "$compose_cmd" == "docker compose" ]]; then
        version_output=$(docker compose version 2>/dev/null | head -n1)
    else
        version_output=$(docker-compose --version 2>/dev/null)
    fi
    
    if [[ -z "$version_output" ]]; then
        echo "WARNING: Could not determine Docker Compose version" >&2
        return 0
    fi
    
    # Extract version number (works for both docker-compose and docker compose)
    version=$(echo "$version_output" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1)
    
    if [[ -n "$version" ]]; then
        echo ">>> Detected Docker Compose version: $version"
        
        # Check if version is >= 2.23.1
        required_version="2.23.1"
        if printf '%s\n%s\n' "$required_version" "$version" | sort -V -C 2>/dev/null; then
            echo ">>> Version check passed (>= $required_version)"
        else
            echo "WARNING: Docker Compose version $version detected, but $required_version or newer is recommended" >&2
            echo "Consider upgrading: https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker" >&2
        fi
    fi
}

# Detect Docker and Docker Compose
echo "=== Checking Docker environment..."

if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: Docker command not found." >&2
    echo "Please install Docker first: https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker" >&2
    exit 1
fi

docker_version_output=$(docker --version)
echo ">>> Docker found: $docker_version_output"

# Check Docker version
docker_version=$(echo "$docker_version_output" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1)
if [[ -n "$docker_version" ]]; then
    required_docker_version="20.10.0"
    if printf '%s\n%s\n' "$required_docker_version" "$docker_version" | sort -V -C 2>/dev/null; then
        echo ">>> Docker version check passed (>= $required_docker_version)"
    else
        echo "WARNING: Docker version $docker_version detected, but $required_docker_version or newer is recommended" >&2
        echo "Consider upgrading: https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker" >&2
    fi
fi

# Detect Docker Compose command
COMPOSE_CMD=$(detect_docker_compose)
echo ">>> Using Docker Compose command: $COMPOSE_CMD"

# Check Docker Compose version
check_docker_compose_version "$COMPOSE_CMD"

echo "=== Starting Apache Ignite 3 cluster..."

# Start the cluster
$COMPOSE_CMD up -d

# Check container status
echo "--- Container status:"
$COMPOSE_CMD ps

# Wait for port to be available
echo "--- Waiting for cluster API to be available..."
timeout=120
counter=0
while ! curl -s http://localhost:10300/management/v1/cluster/state > /dev/null 2>&1; do
    sleep 5
    counter=$((counter + 5))
    if [ $counter -ge $timeout ]; then
        echo "❌ Timeout waiting for cluster API"
        echo "Check logs with: $COMPOSE_CMD logs"
        exit 1
    fi
done

# Initialize the cluster using REST API
echo "=== Initializing cluster using REST API..."
init_response=$(curl -s -X POST http://localhost:10300/management/v1/cluster/init \
  -H "Content-Type: application/json" \
  -d '{
    "metaStorageNodes": ["node1", "node2", "node3"],
    "cmgNodes": ["node1", "node2", "node3"],
    "clusterName": "ignite3-reference-cluster"
  }')
echo "REST API response: $init_response"

echo "--- Waiting for cluster to initialize..."
sleep 10

# Verify initialization
echo "=== Verifying cluster status..."
status_response=$(curl -s http://localhost:10300/management/v1/cluster/state)
echo "Cluster status: $status_response"

if echo "$status_response" | grep -q '"cmgNodes"'; then
    echo "Cluster successfully initialized and ready!"
    echo ""
    echo "Next steps:"
    echo "  Run reference apps:  cd ../01-sample-data-setup && mvn compile exec:java"
    echo "  Start Ignite CLI:    $COMPOSE_CMD run --rm cli connect http://node1:10300"
    echo ""
    echo "To stop cluster: $COMPOSE_CMD down"
else
    echo "Cluster initialization may have failed"
    echo "Check the response above and try manual initialization"
    exit 1
fi