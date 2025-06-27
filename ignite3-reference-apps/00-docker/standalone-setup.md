# Apache Ignite 3 Standalone Installation

**Alternative to Docker: Native Apache Ignite 3 cluster setup for reference applications**

## Prerequisites

- Java 11 or newer
- Download access to Apache Ignite 3 releases

## Installation Options

### Option 1: ZIP Archive Installation (Recommended)

Download the official Apache Ignite 3 distributions:

1. **Download distributions**:

   ```bash
   # Download from https://ignite.apache.org/download.cgi
   wget https://ignite.apache.org/download.cgi -O ignite3-db-3.0.0.zip
   wget https://ignite.apache.org/download.cgi -O ignite3-cli-3.0.0.zip
   
   # Extract both archives
   unzip ignite3-db-3.0.0.zip
   unzip ignite3-cli-3.0.0.zip
   ```

2. **Start single node cluster**:

   ```bash
   cd ignite3-db-3.0.0
   ./bin/ignite3db start node1
   ```

3. **Initialize cluster** (in separate terminal):

   ```bash
   cd ignite3-cli-3.0.0
   ./bin/ignite3
   
   # Connect to node
   connect http://127.0.0.1:10300
   
   # Initialize cluster
   cluster init --name=reference-cluster --meta-storage-node=node1 --cmg-node=node1
   ```

4. **Verify cluster status**:

   ```bash
   cluster status
   ```

### Option 2: Multi-Node Setup

For production-like testing with 3 nodes:

1. **Start three nodes**:

   ```bash
   # Terminal 1
   cd ignite3-db-3.0.0
   ./bin/ignite3db start node1 --config=etc/ignite-config.conf --work-dir=work1

   # Terminal 2  
   ./bin/ignite3db start node2 --config=etc/ignite-config.conf --work-dir=work2 --rest-port=10301 --client-port=10801

   # Terminal 3
   ./bin/ignite3db start node3 --config=etc/ignite-config.conf --work-dir=work3 --rest-port=10302 --client-port=10802
   ```

2. **Initialize multi-node cluster**:

   ```bash
   cd ignite3-cli-3.0.0
   ./bin/ignite3
   connect http://127.0.0.1:10300
   
   cluster init --name=reference-cluster \
     --meta-storage-node=node1,node2,node3 \
     --cmg-node=node1,node2,node3
   ```

### Option 3: DEB/RPM Package Installation

**DEB Installation**:

```bash
# Download packages
wget https://ignite.apache.org/download.cgi -O ignite3-db-3.0.0.deb
wget https://ignite.apache.org/download.cgi -O ignite3-cli-3.0.0.deb

# Install packages
sudo apt-get install ./ignite3-db-3.0.0.deb --no-install-recommends
sudo apt-get install ./ignite3-cli-3.0.0.deb --no-install-recommends

# Start as service
sudo systemctl start ignite3db
sudo systemctl enable ignite3db

# Initialize cluster
ignite3 connect http://127.0.0.1:10300
ignite3 cluster init --name=reference-cluster --meta-storage-node=node1 --cmg-node=node1
```

## Usage with Reference Applications

### Single Node Connection

For simple development with one node:

```java
IgniteClient client = IgniteClient.builder()
    .addresses("127.0.0.1:10800")
    .build();
```

### Multi-Node Connection

For performance testing with multiple nodes:

```java
IgniteClient client = IgniteClient.builder()
    .addresses("127.0.0.1:10800", "127.0.0.1:10801", "127.0.0.1:10802")
    .build();
```

## Required JVM Parameters

When running reference applications, include these JVM parameters:

```bash
export JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.invoke=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  --add-opens java.base/java.math=ALL-UNNAMED \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.time=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens java.base/jdk.internal.access=ALL-UNNAMED \
  --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
  -Dio.netty.tryReflectionSetAccessible=true"

mvn compile exec:java
```

## Cluster Management

### Check Cluster Status

```bash
# Using CLI
ignite3 connect http://127.0.0.1:10300
cluster status

# Using REST API
curl http://localhost:10300/management/v1/cluster/status
```

### SQL Access

```bash
# Connect to cluster
ignite3 connect http://127.0.0.1:10300

# Execute SQL
sql
CREATE TABLE Person (id int primary key, name varchar, age int);
INSERT INTO Person VALUES (1, 'John', 25);
SELECT * FROM Person;
```

### Stop Cluster

```bash
# Single node
./bin/ignite3db stop node1

# Multiple nodes
./bin/ignite3db stop node1
./bin/ignite3db stop node2  
./bin/ignite3db stop node3

# Service installation
sudo systemctl stop ignite3db
```

## Configuration

### Default Locations

**ZIP Installation**:

- Configuration: `etc/ignite-config.conf`
- Work directory: `work/`
- Logs: `work/log/`

**Package Installation**:

- Configuration: `/etc/ignite3db/`
- Work directory: `/var/lib/ignite3db/`
- Logs: `/var/log/ignite3db/`

### Memory Configuration

Edit configuration file to adjust memory settings:

```hocon
ignite {
  memory {
    default.size = 4GB
  }
}
```

## Troubleshooting

### Node Fails to Start

```bash
# Check Java version
java -version

# Verify JVM parameters
echo $JAVA_OPTS

# Check port availability
netstat -tulpn | grep 10800
```

### Cluster Initialization Fails

```bash
# Verify node is running
curl http://localhost:10300/management/v1/cluster/status

# Check logs
tail -f work/log/ignite3db.log
```

### Reference Applications Fail to Connect

```bash
# Verify cluster is initialized
curl http://localhost:10300/management/v1/cluster/status

# Test client connection
ignite3 connect http://127.0.0.1:10300
```

---

**⚠️ Important**: Ensure the Ignite 3 cluster is running and initialized before executing any reference applications in modules 01-10.
