#!/usr/bin/env bash
# debug_route_production.sh
# Run ON the EC2 instance to check the backend status and route optimization dependencies.

set -euo pipefail

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting Production Route Optimization Diagnostics ===${NC}"

CONTAINER_NAME="compose-backend-1"

# 1. Check if the container is running
echo -e "\n${BLUE}[1/5] Checking Docker container state...${NC}"
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo -e "${RED}ERROR: Container '${CONTAINER_NAME}' is not running!${NC}"
    echo "Running containers:"
    docker ps
    exit 1
else
    STATUS=$(docker inspect --format='{{.State.Status}}' "${CONTAINER_NAME}")
    HEALTH=$(docker inspect --format='{{.State.Health.Status}}' "${CONTAINER_NAME}" 2>/dev/null || echo "unsupported")
    echo -e "${GREEN}SUCCESS: Container is running (Status: ${STATUS}, Health: ${HEALTH})${NC}"
fi

# 2. Check JNI Library Dependencies (libgomp) inside container
echo -e "\n${BLUE}[2/5] Checking libgomp1 in container...${NC}"
if docker exec "${CONTAINER_NAME}" find /usr -name "*libgomp.so.1*" >/dev/null 2>&1; then
    echo -e "${GREEN}SUCCESS: libgomp.so.1 is present in the container.${NC}"
else
    echo -e "${RED}ERROR: libgomp.so.1 is missing! OR-Tools will fail to load native libraries.${NC}"
fi

# 3. Check OSRM Routing Server Connectivity from inside the container
echo -e "\n${BLUE}[3/5] Testing OSRM network access from container...${NC}"
OSRM_URL="http://router.project-osrm.org/table/v1/driving/"
echo "Curling ${OSRM_URL}..."
if docker exec "${CONTAINER_NAME}" curl -I -s --connect-timeout 5 "${OSRM_URL}" > /dev/null; then
    echo -e "${GREEN}SUCCESS: Container has internet access and can reach the OSRM server.${NC}"
else
    echo -e "${RED}ERROR: Container failed to connect to OSRM server (${OSRM_URL}). Check internet access / security group egress rules.${NC}"
fi

# 4. Check for UnsatisfiedLinkError in container logs
echo -e "\n${BLUE}[4/5] Scanning logs for native library loading errors...${NC}"
LATEST_LOGS=$(docker logs --tail 200 "${CONTAINER_NAME}" 2>&1)

if echo "${LATEST_LOGS}" | grep -q -i "UnsatisfiedLinkError"; then
    echo -e "${RED}ERROR: Found UnsatisfiedLinkError in recent logs!${NC}"
    echo -e "${YELLOW}Relevant log output:${NC}"
    echo "${LATEST_LOGS}" | grep -C 3 -i "UnsatisfiedLinkError" || true
    echo -e "${YELLOW}This usually means Loader.loadNativeLibraries() was called after Spring initialized OR-Tools classes, or libgomp1 is missing.${NC}"
else
    echo -e "${GREEN}SUCCESS: No UnsatisfiedLinkError found in recent 200 log lines.${NC}"
fi

# 5. Check other database connection errors
echo -e "\n${BLUE}[5/5] Scanning logs for database and DDL errors...${NC}"
if echo "${LATEST_LOGS}" | grep -q -i -E "connection refused|cannot execute|foreign key|constraint"; then
    echo -e "${YELLOW}WARNING: Found potential database or constraint errors in logs:${NC}"
    echo "${LATEST_LOGS}" | grep -i -E "connection refused|cannot execute|foreign key|constraint" | tail -n 10 || true
else
    echo -e "${GREEN}SUCCESS: No database errors found in recent logs.${NC}"
fi

echo -e "\n${BLUE}=== Diagnostics Completed ===${NC}"
