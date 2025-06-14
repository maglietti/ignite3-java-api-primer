# Apache Ignite 3 Cluster Setup

**Docker-based 3-node Ignite cluster for reference applications**

## Quick Start

### Option 1: Automated Setup (Recommended)

1. **Run the initialization script**:
   ```bash
   ./init-cluster.sh
   ```

   This script will:
   - Start the 3-node cluster
   - Wait for containers to be healthy
   - Initialize the cluster automatically
   - Verify everything is working

### Option 2: Manual Setup

1. **Start the cluster**:
   ```bash
   docker-compose up -d
   ```

2. **Initialize the cluster** (required before first use):
   ```bash
   # Wait for containers to be healthy (30-60 seconds)
   docker-compose ps
   
   # Initialize using REST API
   curl -X POST http://localhost:10300/management/v1/cluster/init \
     -H "Content-Type: application/json" \
     -d '{
       "metaStorageNodes": ["node1", "node2", "node3"],
       "cmgNodes": ["node1", "node2", "node3"],
       "clusterName": "ignite3-reference-cluster"
     }'
   ```

3. **Verify cluster initialization**:
   ```bash
   # Check cluster status (should show "started" state)
   curl http://localhost:10300/management/v1/cluster/status
   ```

4. **View logs** (optional):
   ```bash
   docker-compose logs -f
   ```

5. **Stop the cluster** (when done):
   ```bash
   docker-compose down
   ```

## Cluster Configuration

This setup provides a **3-node Apache Ignite cluster** with the following configuration:

### Node Details
- **Node 1**: `localhost:10800` (primary client connection)
- **Node 2**: `localhost:10801` (alternative client connection)  
- **Node 3**: `localhost:10802` (alternative client connection)

### Resource Allocation
- **Memory**: 4GB JVM heap per node
- **Network**: Custom bridge network for inter-node communication
- **Health Checks**: Automatic monitoring of node status

## Usage with Reference Applications

**Prerequisites**: The cluster must be running before executing any reference applications.

### Default Connection
Most reference applications connect to: `127.0.0.1:10800` (Node 1)

### Alternative Connections
For load balancing or failover testing:
- Node 2: `127.0.0.1:10801`
- Node 3: `127.0.0.1:10802`

## Troubleshooting

### Cluster Not Starting
```bash
# Check container status
docker-compose ps

# View detailed logs
docker-compose logs node1
```

### Cluster Not Initialized
If applications fail with "Cluster is not initialized" errors:

**Check cluster status**:
```bash
curl http://localhost:10300/management/v1/cluster/status
```

**If status shows "not_started", run initialization**:
```bash
curl -X POST http://localhost:10300/management/v1/cluster/init \
  -H "Content-Type: application/json" \
  -d '{
    "metaStorageNodes": ["node1", "node2", "node3"],
    "cmgNodes": ["node1", "node2", "node3"], 
    "clusterName": "ignite3-reference-cluster"
  }'
```

### Memory Issues
If you encounter memory issues, reduce JVM settings in `docker-compose.yml`:
```yaml
environment:
  JVM_MAX_MEM: "2g"
  JVM_MIN_MEM: "2g"
```

### Port Conflicts
If ports are already in use, modify the port mappings in `docker-compose.yml`.

## Cluster Management

### Health Check
```bash
curl http://localhost:10800/management/v1/cluster/status
```

### Clean Restart
```bash
docker-compose down -v  # Remove volumes
docker-compose up -d    # Start fresh
```

---

**⚠️ Important**: Keep this cluster running while working with any reference applications in modules 01-10.