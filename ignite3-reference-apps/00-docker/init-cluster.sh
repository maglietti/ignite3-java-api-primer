#!/bin/bash

# Initialize Apache Ignite 3 Cluster
# Starts a 3-node cluster via Docker Compose, waits for all nodes to boot,
# initializes the cluster, and verifies the result.
#
# Usage: ./init-cluster.sh
#
# Prerequisites:
#   - Docker >= 20.10.0
#   - Docker Compose >= 2.23.1
#   - curl

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

REST_BASE="http://localhost:10300"
REST_PORTS=(10300 10301 10302)
NODE_NAMES=("node1" "node2" "node3")
CLUSTER_NAME="ignite3-reference-cluster"

# --- Docker Environment ---

detect_docker_compose() {
    if docker compose version >/dev/null 2>&1; then
        echo "docker compose"
    elif command -v docker-compose >/dev/null 2>&1; then
        echo "docker-compose"
    else
        echo "ERROR: Neither 'docker compose' nor 'docker-compose' found." >&2
        echo "Install Docker Compose 2.23.1 or newer." >&2
        echo "Guide: https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker" >&2
        exit 1
    fi
}

check_version() {
    local name="$1" current="$2" required="$3"
    if [[ -n "$current" ]]; then
        if printf '%s\n%s\n' "$required" "$current" | sort -V -C 2>/dev/null; then
            echo "    $name $current (>= $required)"
        else
            echo "    $name $current (WARNING: $required or newer recommended)" >&2
        fi
    fi
}

echo "=== [1/6] Checking prerequisites"

if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: Docker not found." >&2
    echo "Guide: https://ignite.apache.org/docs/ignite3/latest/installation/installing-using-docker" >&2
    exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
    echo "ERROR: curl not found." >&2
    exit 1
fi

docker_version=$(docker --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1)
check_version "Docker" "$docker_version" "20.10.0"

COMPOSE_CMD=$(detect_docker_compose)
if [[ "$COMPOSE_CMD" == "docker compose" ]]; then
    compose_version=$(docker compose version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1)
else
    compose_version=$(docker-compose --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -n1)
fi
check_version "Docker Compose" "$compose_version" "2.23.1"
echo "    Compose command: $COMPOSE_CMD"

# --- Start Containers ---

echo ""
echo "=== [2/6] Starting containers"

(cd "$SCRIPT_DIR" && $COMPOSE_CMD up -d)

echo ""
echo "--- Container status:"
(cd "$SCRIPT_DIR" && $COMPOSE_CMD ps)

# --- Wait for REST API on all nodes ---

echo ""
echo "=== [3/6] Waiting for nodes to boot"

TIMEOUT=120
for i in "${!REST_PORTS[@]}"; do
    port="${REST_PORTS[$i]}"
    name="${NODE_NAMES[$i]}"
    counter=0

    printf "    %s (port %s) " "$name" "$port"
    while ! curl -sf "http://localhost:$port/management/v1/node/state" >/dev/null 2>&1; do
        sleep 3
        counter=$((counter + 3))
        if [[ $counter -ge $TIMEOUT ]]; then
            echo "TIMEOUT"
            echo "ERROR: $name did not respond within ${TIMEOUT}s." >&2
            echo "Check logs: (cd \"$SCRIPT_DIR\" && $COMPOSE_CMD logs $name)" >&2
            exit 1
        fi
        printf "."
    done
    echo "UP"
done

# --- Verify node discovery ---

echo ""
echo "=== [4/6] Verifying node discovery"

echo "--- Node versions:"
for port in "${REST_PORTS[@]}"; do
    version_json=$(curl -sf "http://localhost:$port/management/v1/node/version" 2>/dev/null || echo '{}')
    info_json=$(curl -sf "http://localhost:$port/management/v1/node/info" 2>/dev/null || echo '{}')
    node_name=$(echo "$info_json" | python3 -c "import sys,json; print(json.load(sys.stdin).get('name','unknown'))" 2>/dev/null || echo "unknown")
    product=$(echo "$version_json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(f\"{d.get('product','?')} {d.get('version','?')}\")" 2>/dev/null || echo "unknown")
    echo "    $node_name: $product (REST port $port)"
done

echo ""
echo "--- Physical topology (from node1):"
topo_json=$(curl -sf "$REST_BASE/management/v1/cluster/topology/physical" 2>/dev/null || echo '[]')
topo_count=$(echo "$topo_json" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
echo "$topo_json" | python3 -c "
import sys, json
nodes = json.load(sys.stdin)
for n in sorted(nodes, key=lambda x: x.get('name','')):
    addr = n.get('address', {})
    meta = n.get('metadata', {})
    print(f\"    {n['name']}: {addr.get('host','?')}:{addr.get('port','?')} (REST {meta.get('httpPort','?')})\")
" 2>/dev/null || echo "    (could not parse topology)"

if [[ "$topo_count" -lt "${#NODE_NAMES[@]}" ]]; then
    echo ""
    echo "WARNING: Expected ${#NODE_NAMES[@]} nodes in topology but found $topo_count." >&2
    echo "Waiting 10s for remaining nodes to join discovery..." >&2
    sleep 10
    topo_count=$(curl -sf "$REST_BASE/management/v1/cluster/topology/physical" 2>/dev/null | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
    if [[ "$topo_count" -lt "${#NODE_NAMES[@]}" ]]; then
        echo "ERROR: Still only $topo_count nodes discovered. Check network configuration." >&2
        exit 1
    fi
    echo "    All ${#NODE_NAMES[@]} nodes now visible."
fi

# --- Initialize Cluster ---

echo ""
echo "=== [5/6] Initializing cluster"

echo ">>> POST /management/v1/cluster/init"
echo "    Cluster name: $CLUSTER_NAME"
echo "    MetaStorage nodes: ${NODE_NAMES[*]}"

init_http_code=$(curl -s -o /tmp/ignite3-init-response.txt -w "%{http_code}" \
    -X POST "$REST_BASE/management/v1/cluster/init" \
    -H "Content-Type: application/json" \
    -d "{
        \"metaStorageNodes\": [\"node1\", \"node2\", \"node3\"],
        \"cmgNodes\": [\"node1\", \"node2\", \"node3\"],
        \"clusterName\": \"$CLUSTER_NAME\"
    }")

init_response=$(cat /tmp/ignite3-init-response.txt 2>/dev/null)
rm -f /tmp/ignite3-init-response.txt

if [[ "$init_http_code" == "200" ]]; then
    echo "<<< 200 OK"
else
    echo "<<< $init_http_code $init_response"
    echo ""
    echo "ERROR: Cluster initialization failed (HTTP $init_http_code)." >&2
    if echo "$init_response" | grep -q '"detail"'; then
        detail=$(echo "$init_response" | python3 -c "import sys,json; print(json.load(sys.stdin).get('detail',''))" 2>/dev/null)
        echo "    Detail: $detail" >&2
    fi
    exit 1
fi

# --- Verify All Nodes STARTED ---

echo ""
echo "=== [6/6] Verifying cluster"

echo "--- Waiting for all nodes to reach STARTED..."
STARTED_TIMEOUT=60
for i in "${!REST_PORTS[@]}"; do
    port="${REST_PORTS[$i]}"
    name="${NODE_NAMES[$i]}"
    counter=0

    printf "    %s " "$name"
    while true; do
        state=$(curl -sf "http://localhost:$port/management/v1/node/state" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('state',''))" 2>/dev/null || echo "")
        if [[ "$state" == "STARTED" ]]; then
            echo "STARTED"
            break
        fi
        sleep 3
        counter=$((counter + 3))
        if [[ $counter -ge $STARTED_TIMEOUT ]]; then
            echo "TIMEOUT (state: ${state:-unreachable})"
            echo "WARNING: $name did not reach STARTED within ${STARTED_TIMEOUT}s." >&2
            break
        fi
        printf "."
    done
done

echo ""
echo "--- Node status:"
# Check if health endpoints are available (Ignite 3.x may or may not have them)
has_health=$(curl -s -o /dev/null -w "%{http_code}" "$REST_BASE/health/readiness" 2>/dev/null)
for i in "${!REST_PORTS[@]}"; do
    port="${REST_PORTS[$i]}"
    name="${NODE_NAMES[$i]}"
    state=$(curl -s "http://localhost:$port/management/v1/node/state" 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin).get('state','unknown'))" 2>/dev/null || echo "unreachable")
    if [[ "$has_health" != "404" && "$has_health" != "409" ]]; then
        readiness_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:$port/health/readiness" 2>/dev/null)
        echo "    $name: $state (readiness: $readiness_code)"
    else
        echo "    $name: $state"
    fi
done

echo ""
echo "--- Cluster state:"
state_response=$(curl -sf "$REST_BASE/management/v1/cluster/state" 2>/dev/null || echo "")
if echo "$state_response" | grep -q '"cmgNodes"'; then
    echo "$state_response" | python3 -c "
import sys, json
s = json.load(sys.stdin)
tag = s.get('clusterTag', {})
print(f\"    Name:    {tag.get('clusterName', '?')}\")
print(f\"    ID:      {tag.get('clusterId', '?')}\")
print(f\"    Version: {s.get('igniteVersion', '?')}\")
print(f\"    CMG:     {', '.join(s.get('cmgNodes', []))}\")
print(f\"    MS:      {', '.join(s.get('msNodes', []))}\")
" 2>/dev/null
    echo ""
    echo "Cluster is ready."
    echo ""
    echo "Endpoints:"
    echo "    REST API:    http://localhost:10300"
    echo "    JDBC node1:  jdbc:ignite:thin://localhost:10800"
    echo "    JDBC node2:  jdbc:ignite:thin://localhost:10801"
    echo "    JDBC node3:  jdbc:ignite:thin://localhost:10802"
    echo ""
    echo "Commands:"
    echo "    Run reference apps:  cd ../01-sample-data-setup && mvn compile exec:java"
    echo "    Start Ignite CLI:    (cd \"$SCRIPT_DIR\" && $COMPOSE_CMD run --rm cli connect http://node1:10300)"
    echo "    Cluster state:       curl -s $REST_BASE/management/v1/cluster/state | python3 -m json.tool"
    echo "    Cluster config:      curl -s $REST_BASE/management/v1/configuration/cluster"
    echo "    Node logs:           (cd \"$SCRIPT_DIR\" && $COMPOSE_CMD logs -f node1)"
    echo "    Stop cluster:        (cd \"$SCRIPT_DIR\" && $COMPOSE_CMD down)"
else
    echo "    ERROR: Could not retrieve cluster state after initialization." >&2
    echo "    Response: $state_response" >&2
    echo ""
    echo "    Check logs: (cd \"$SCRIPT_DIR\" && $COMPOSE_CMD logs)" >&2
    exit 1
fi
