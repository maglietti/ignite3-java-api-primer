#!/bin/bash

# Initialize Apache Ignite 3 Cluster
# This script starts and initializes the 3-node cluster for reference applications

set -e

echo "=== Starting Apache Ignite 3 cluster..."

# Start the cluster
docker-compose up -d

# Check container status
echo "--- Container status:"
docker-compose ps

# Wait for port to be available
echo "--- Waiting for cluster API to be available..."
timeout=120
counter=0
while ! curl -s http://localhost:10300/management/v1/cluster/state > /dev/null 2>&1; do
    sleep 5
    counter=$((counter + 5))
    if [ $counter -ge $timeout ]; then
        echo "❌ Timeout waiting for cluster API"
        echo "Check logs with: docker-compose logs"
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
    echo "1. cd ../01-sample-data-setup"
    echo "2. mvn exec:java"
    echo ""
    echo "To stop cluster: docker compose down"
else
    echo "Cluster initialization may have failed"
    echo "Check the response above and try manual initialization"
    exit 1
fi