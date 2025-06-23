#!/bin/bash

#
# Apache Ignite 3 JAR Deployment Script
#
# Deploys JAR files containing compute job classes to an Ignite 3 cluster
# using the REST API. Provides fallback options for CLI-based deployment.
#

set -e

# Default configuration
DEFAULT_CLUSTER_HOST="localhost"
DEFAULT_REST_PORT="10300"
DEFAULT_DEPLOY_MODE="MAJORITY"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_info() {
    echo -e "${BLUE}>>> $1${NC}"
}

print_success() {
    echo -e "${GREEN}>>> $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}!!! $1${NC}"
}

print_error() {
    echo -e "${RED}!!! $1${NC}"
}

# Display usage information
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS] <unit-id> <unit-version> <jar-file>

Deploy JAR files containing compute job classes to Apache Ignite 3 cluster.

ARGUMENTS:
    unit-id         Unique identifier for the deployment unit
    unit-version    Version string for the deployment unit (e.g., 1.0.0)
    jar-file        Path to the JAR file to deploy

OPTIONS:
    -h, --host HOST     Ignite cluster host (default: $DEFAULT_CLUSTER_HOST)
    -p, --port PORT     Ignite REST API port (default: $DEFAULT_REST_PORT)
    -m, --mode MODE     Deployment mode: MAJORITY|ALL (default: $DEFAULT_DEPLOY_MODE)
    -c, --check         Check deployment status after upload
    -r, --remove        Remove existing deployment unit before deploying
    -v, --verbose       Enable verbose output
    --validate          Validate cluster state before deployment
    --monitor           Monitor compute jobs after deployment
    --metrics           Enable compute metrics
    --help              Show this help message

EXAMPLES:
    # Deploy compute jobs JAR
    $0 compute-jobs 1.0.0 target/my-jobs.jar

    # Deploy to specific cluster with status check
    $0 -h 192.168.1.100 -p 10300 -c my-jobs 2.1.0 /path/to/jobs.jar

    # Deploy to all nodes with removal of existing unit
    $0 -m ALL -r data-processors 1.5.2 processors.jar

    # Deploy with full validation and monitoring
    $0 --validate --check --monitor --metrics compute-jobs 1.0.0 target/jobs.jar

    # Check deployment status only
    $0 -c compute-jobs 1.0.0 ""

DEPLOYMENT WORKFLOW:
    1. Validates JAR file exists (unless checking status only)
    2. Attempts REST API deployment to cluster
    3. Optionally checks deployment status
    4. Provides fallback CLI instructions on failure

REST API ENDPOINT:
    POST http://HOST:PORT/management/v1/deployment/units/UNIT_ID/VERSION?deployMode=MODE

FALLBACK OPTIONS:
    If REST API fails, manual deployment options are provided:
    - Local CLI: ignite deployment deploy UNIT_ID JAR_FILE
    - Docker CLI: docker run --rm -it --network=host apacheignite/ignite:3.0.0 cli

EOF
}

# Parse command line arguments
parse_arguments() {
    CLUSTER_HOST="$DEFAULT_CLUSTER_HOST"
    REST_PORT="$DEFAULT_REST_PORT"
    DEPLOY_MODE="$DEFAULT_DEPLOY_MODE"
    CHECK_STATUS=false
    REMOVE_EXISTING=false
    VERBOSE=false
    VALIDATE_CLUSTER=false
    MONITOR_JOBS=false
    ENABLE_METRICS=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--host)
                CLUSTER_HOST="$2"
                shift 2
                ;;
            -p|--port)
                REST_PORT="$2"
                shift 2
                ;;
            -m|--mode)
                DEPLOY_MODE="$2"
                if [[ "$DEPLOY_MODE" != "MAJORITY" && "$DEPLOY_MODE" != "ALL" ]]; then
                    print_error "Invalid deploy mode: $DEPLOY_MODE. Use MAJORITY or ALL."
                    exit 1
                fi
                shift 2
                ;;
            -c|--check)
                CHECK_STATUS=true
                shift
                ;;
            -r|--remove)
                REMOVE_EXISTING=true
                shift
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            --validate)
                VALIDATE_CLUSTER=true
                shift
                ;;
            --monitor)
                MONITOR_JOBS=true
                shift
                ;;
            --metrics)
                ENABLE_METRICS=true
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            -*)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
            *)
                break
                ;;
        esac
    done
    
    # Validate required arguments
    if [[ $# -lt 2 ]]; then
        print_error "Missing required arguments"
        show_usage
        exit 1
    fi
    
    UNIT_ID="$1"
    UNIT_VERSION="$2"
    JAR_FILE="$3"
    
    # Validate unit ID and version
    if [[ -z "$UNIT_ID" || -z "$UNIT_VERSION" ]]; then
        print_error "Unit ID and version are required"
        exit 1
    fi
    
    # Validate JAR file (unless checking status only)
    if [[ "$CHECK_STATUS" == false && -z "$JAR_FILE" ]]; then
        print_error "JAR file path is required for deployment"
        exit 1
    fi
    
    if [[ "$CHECK_STATUS" == false && ! -f "$JAR_FILE" ]]; then
        print_error "JAR file not found: $JAR_FILE"
        exit 1
    fi
}

# Check if required tools are available
check_dependencies() {
    if ! command -v curl >/dev/null 2>&1; then
        print_error "curl is required but not installed"
        exit 1
    fi
    
    # Check for jq if using advanced features
    if [[ "$VALIDATE_CLUSTER" == true || "$MONITOR_JOBS" == true ]] && ! command -v jq >/dev/null 2>&1; then
        print_warning "jq not found - some advanced features will be limited"
    fi
}

# Validate cluster state before deployment
validate_cluster() {
    print_info "=== [1/4] Validating Cluster State ==="
    
    # Check cluster initialization
    print_info "Checking cluster initialization..."
    local response
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" \
                  "http://${CLUSTER_HOST}:${REST_PORT}/management/v1/cluster/state" 2>/dev/null || echo "HTTPSTATUS:000")
    
    local http_code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    if [[ "$http_code" != "200" ]]; then
        print_error "Cannot connect to cluster management API (HTTP $http_code)"
        print_error "Verify cluster is running at ${CLUSTER_HOST}:${REST_PORT}"
        return 1
    fi
    
    local body
    body=$(echo "$response" | sed 's/HTTPSTATUS:[0-9]*$//')
    
    # Parse cluster state with or without jq
    if command -v jq >/dev/null 2>&1; then
        local cmg_nodes
        cmg_nodes=$(echo "$body" | jq -r '.cmgNodes | length' 2>/dev/null || echo "unknown")
        if [[ "$cmg_nodes" == "0" || "$cmg_nodes" == "null" ]]; then
            print_error "Cluster is not properly initialized (CMG nodes: $cmg_nodes)"
            return 1
        fi
        print_success "Cluster initialized with $cmg_nodes CMG nodes"
        
        # Check cluster topology
        print_info "Validating cluster topology..."
        local topology_response
        topology_response=$(curl -s "http://${CLUSTER_HOST}:${REST_PORT}/management/v1/cluster/topology/physical" 2>/dev/null)
        local node_count
        node_count=$(echo "$topology_response" | jq -r 'length' 2>/dev/null || echo "unknown")
        print_success "Cluster has $node_count active nodes"
        
        if [[ "$VERBOSE" == true ]]; then
            echo "$topology_response" | jq -r '.[] | "  >>> Node: \(.name) at \(.address.host):\(.address.port)"' 2>/dev/null || echo "  >>> Node details unavailable"
        fi
    else
        print_success "Cluster API accessible (detailed validation requires jq)"
    fi
    
    return 0
}

# Enable compute metrics for monitoring
enable_compute_metrics() {
    print_info "=== [2/4] Enabling Compute Metrics ==="
    
    local response
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" \
                  -X POST \
                  -H "Content-Type: text/plain" \
                  -d "compute" \
                  "http://${CLUSTER_HOST}:${REST_PORT}/management/v1/metric/cluster/enable" 2>/dev/null || echo "HTTPSTATUS:000")
    
    local http_code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    case $http_code in
        200|201)
            print_success "Compute metrics enabled successfully"
            ;;
        000)
            print_warning "Failed to enable metrics - cluster unreachable"
            ;;
        *)
            print_warning "Metrics enablement returned HTTP $http_code"
            ;;
    esac
}

# Monitor compute jobs after deployment
monitor_compute_jobs() {
    print_info "=== [4/4] Monitoring Compute Jobs ==="
    
    local response
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" \
                  "http://${CLUSTER_HOST}:${REST_PORT}/management/v1/compute/jobs" 2>/dev/null || echo "HTTPSTATUS:000")
    
    local http_code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    if [[ "$http_code" != "200" ]]; then
        print_warning "Cannot retrieve compute jobs (HTTP $http_code)"
        return 1
    fi
    
    local body
    body=$(echo "$response" | sed 's/HTTPSTATUS:[0-9]*$//')
    
    if command -v jq >/dev/null 2>&1; then
        local job_count
        job_count=$(echo "$body" | jq -r 'length' 2>/dev/null || echo "0")
        
        if [[ "$job_count" == "0" ]]; then
            print_info "No active compute jobs"
        else
            print_info "Found $job_count active compute jobs:"
            echo "$body" | jq -r '.[] | "  >>> Job \(.id): \(.status) (\(.createTime))"' 2>/dev/null || print_warning "Job details parsing failed"
        fi
    else
        print_info "Compute jobs endpoint accessible (detailed info requires jq)"
    fi
}

# Enhanced deployment status check with per-node verification
check_enhanced_deployment_status() {
    print_info "=== [3/4] Enhanced Deployment Verification ==="
    
    # Check cluster-wide deployment status
    print_info "Checking cluster-wide deployment status..."
    local cluster_url="http://${CLUSTER_HOST}:${REST_PORT}/management/v1/deployment/cluster/units/${UNIT_ID}"
    local response
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$cluster_url" 2>/dev/null || echo "HTTPSTATUS:000")
    
    local http_code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    case $http_code in
        200)
            local body
            body=$(echo "$response" | sed 's/HTTPSTATUS:[0-9]*$//')
            
            if command -v jq >/dev/null 2>&1; then
                local version_status
                version_status=$(echo "$body" | jq -r --arg version "$UNIT_VERSION" '.versionToStatus[]? | select(.version == $version) | .status' 2>/dev/null)
                
                if [[ -n "$version_status" ]]; then
                    print_success "Deployment unit ${UNIT_ID}:${UNIT_VERSION} status: $version_status"
                    
                    # Check node-level deployment
                    print_info "Verifying node-level deployment..."
                    local node_url="http://${CLUSTER_HOST}:${REST_PORT}/management/v1/deployment/node/units/${UNIT_ID}"
                    local node_response
                    node_response=$(curl -s "$node_url" 2>/dev/null)
                    
                    if [[ -n "$node_response" ]]; then
                        echo "$node_response" | jq -r --arg version "$UNIT_VERSION" '.[] | select(.version == $version) | "  >>> Node \(.nodeName): \(.status)"' 2>/dev/null || print_info "Node status details unavailable"
                    fi
                else
                    print_warning "Version $UNIT_VERSION not found in deployment status"
                fi
            else
                print_success "Deployment status retrieved (detailed info requires jq)"
            fi
            ;;
        404)
            print_warning "Deployment unit not found: ${UNIT_ID}"
            ;;
        000)
            print_error "Failed to connect to cluster at ${CLUSTER_HOST}:${REST_PORT}"
            ;;
        *)
            print_error "Status check failed with HTTP $http_code"
            ;;
    esac
}

# Remove existing deployment unit
remove_deployment() {
    local url="http://${CLUSTER_HOST}:${REST_PORT}/management/v1/deployment/units/${UNIT_ID}/${UNIT_VERSION}"
    
    print_info "Removing existing deployment unit: ${UNIT_ID}:${UNIT_VERSION}"
    
    local response
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X DELETE "$url" 2>/dev/null || echo "HTTPSTATUS:000")
    
    local http_code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    case $http_code in
        200|204)
            print_success "Deployment unit removed successfully"
            ;;
        404)
            print_warning "Deployment unit not found (may not exist)"
            ;;
        000)
            print_warning "Failed to connect to cluster at ${CLUSTER_HOST}:${REST_PORT}"
            ;;
        *)
            print_warning "Remove operation returned HTTP $http_code"
            ;;
    esac
}

# Deploy JAR file via REST API
deploy_jar() {
    local url="http://${CLUSTER_HOST}:${REST_PORT}/management/v1/deployment/units/${UNIT_ID}/${UNIT_VERSION}?deployMode=${DEPLOY_MODE}"
    
    print_info "Deploying JAR to cluster: ${CLUSTER_HOST}:${REST_PORT}"
    print_info "Unit: ${UNIT_ID}:${UNIT_VERSION}"
    print_info "JAR: ${JAR_FILE}"
    print_info "Mode: ${DEPLOY_MODE}"
    
    # Remove existing deployment if requested
    if [[ "$REMOVE_EXISTING" == true ]]; then
        remove_deployment
    fi
    
    # Deploy JAR file
    local response
    if [[ "$VERBOSE" == true ]]; then
        print_info "Uploading JAR file..."
        response=$(curl -w "HTTPSTATUS:%{http_code}" \
                       -X POST \
                       -H "Content-Type: multipart/form-data" \
                       -F "unitContent=@${JAR_FILE}" \
                       "$url" 2>/dev/null || echo "HTTPSTATUS:000")
    else
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" \
                       -X POST \
                       -H "Content-Type: multipart/form-data" \
                       -F "unitContent=@${JAR_FILE}" \
                       "$url" 2>/dev/null || echo "HTTPSTATUS:000")
    fi
    
    local body
    body=$(echo "$response" | sed 's/HTTPSTATUS:[0-9]*$//')
    
    local http_code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    case $http_code in
        200|201)
            print_success "JAR deployed successfully (HTTP $http_code)"
            return 0
            ;;
        409)
            print_warning "Deployment unit already exists (HTTP 409)"
            print_info "Use -r/--remove flag to replace existing deployment"
            return 0
            ;;
        000)
            print_error "Failed to connect to cluster at ${CLUSTER_HOST}:${REST_PORT}"
            print_error "Verify cluster is running and REST API is accessible"
            return 1
            ;;
        *)
            print_error "Deployment failed with HTTP $http_code"
            if [[ -n "$body" && "$VERBOSE" == true ]]; then
                print_error "Response: $body"
            fi
            return 1
            ;;
    esac
}

# Check deployment status
check_deployment_status() {
    local url="http://${CLUSTER_HOST}:${REST_PORT}/management/v1/deployment/cluster/units/${UNIT_ID}"
    
    print_info "Checking deployment status for: ${UNIT_ID}"
    
    local response
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X GET "$url" 2>/dev/null || echo "HTTPSTATUS:000")
    
    local body
    body=$(echo "$response" | sed 's/HTTPSTATUS:[0-9]*$//')
    
    local http_code
    http_code=$(echo "$response" | grep -o "HTTPSTATUS:[0-9]*" | cut -d: -f2)
    
    case $http_code in
        200)
            print_success "Deployment status retrieved successfully"
            if [[ "$VERBOSE" == true ]]; then
                echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
            else
                # Extract basic status information
                if command -v python3 >/dev/null 2>&1; then
                    local status
                    status=$(echo "$body" | python3 -c "
import json, sys
try:
    data = json.load(sys.stdin)
    if 'versionToStatus' in data and data['versionToStatus']:
        for version_status in data['versionToStatus']:
            if version_status.get('version') == '$UNIT_VERSION':
                print(f\"Version {version_status['version']}: {version_status['status']}\")
                break
        else:
            print('Version $UNIT_VERSION not found')
    else:
        print('No deployment status available')
except:
    print('Status parsing failed')
" 2>/dev/null)
                    print_info "$status"
                else
                    print_info "Status data available (use -v for details)"
                fi
            fi
            ;;
        404)
            print_warning "Deployment unit not found: ${UNIT_ID}"
            ;;
        000)
            print_error "Failed to connect to cluster at ${CLUSTER_HOST}:${REST_PORT}"
            ;;
        *)
            print_error "Status check failed with HTTP $http_code"
            ;;
    esac
}

# Provide fallback deployment instructions
show_fallback_options() {
    cat << EOF

=== Fallback Deployment Options ===

If REST API deployment fails, use these manual alternatives:

1. Local Ignite CLI:
   ignite deployment deploy ${UNIT_ID} ${JAR_FILE}

2. Docker CLI:
   docker run --rm -it --network=host -e LANG=C.UTF-8 -e LC_ALL=C.UTF-8 \\
     apacheignite/ignite:3.0.0 cli
   # Then inside container:
   deployment deploy ${UNIT_ID} /path/to/${JAR_FILE##*/}

3. Manual REST API (if script fails):
   curl -X POST \\
     "http://${CLUSTER_HOST}:${REST_PORT}/management/v1/deployment/units/${UNIT_ID}/${UNIT_VERSION}?deployMode=${DEPLOY_MODE}" \\
     -H "Content-Type: multipart/form-data" \\
     -F "unitContent=@${JAR_FILE}"

EOF
}

# Main execution
main() {
    parse_arguments "$@"
    check_dependencies
    
    print_info "=== Apache Ignite 3 JAR Deployment Script ==="
    
    # Handle status check only
    if [[ "$CHECK_STATUS" == true && -z "$JAR_FILE" ]]; then
        if [[ "$VALIDATE_CLUSTER" == true || "$MONITOR_JOBS" == true ]]; then
            # Run advanced status checks
            [[ "$VALIDATE_CLUSTER" == true ]] && validate_cluster
            check_enhanced_deployment_status
            [[ "$MONITOR_JOBS" == true ]] && monitor_compute_jobs
        else
            # Basic status check
            check_deployment_status
        fi
        exit 0
    fi
    
    # Pre-deployment validation
    if [[ "$VALIDATE_CLUSTER" == true ]]; then
        if ! validate_cluster; then
            print_error "Cluster validation failed - aborting deployment"
            exit 1
        fi
        echo
    fi
    
    # Enable metrics if requested
    if [[ "$ENABLE_METRICS" == true ]]; then
        enable_compute_metrics
        echo
    fi
    
    # Deploy JAR file
    if deploy_jar; then
        echo
        
        # Enhanced status check if requested
        if [[ "$CHECK_STATUS" == true ]]; then
            check_enhanced_deployment_status
            echo
        fi
        
        # Monitor compute jobs if requested
        if [[ "$MONITOR_JOBS" == true ]]; then
            monitor_compute_jobs
            echo
        fi
        
        print_success "Deployment process completed successfully"
        
        cat << EOF

=== Next Steps ===

1. Verify deployment status:
   $0 -c ${UNIT_ID} ${UNIT_VERSION} ""

2. Use in compute jobs:
   JobDescriptor.builder(YourJobClass.class)
       .units(List.of(new DeploymentUnit("${UNIT_ID}", "${UNIT_VERSION}")))
       .build()

EOF
    else
        print_error "REST API deployment failed"
        show_fallback_options
        exit 1
    fi
}

# Execute main function with all arguments
main "$@"