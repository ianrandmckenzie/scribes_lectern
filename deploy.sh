#!/bin/bash

# Load environment variables
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
else
    echo -e "${RED}Error: .env file not found. Copy .env.example to .env and configure it.${NC}"
    exit 1
fi

# Configuration (defaults if not set in .env)
SERVER_IP="${SERVER_IP:-}"
SERVER_USER="${SERVER_USER:-root}"
REMOTE_PATH="${REMOTE_PATH:-/root/Server/mods/}"
JAR_NAME="${JAR_NAME:-scribes_lectern-0.1.0.jar}"
HYTALE_JAR_PATH="${HYTALE_JAR_PATH:-../HytaleServer.jar}"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}Starting build and deployment for Scribes' Lectern...${NC}"

# 1. Ensure HytaleServer.jar is installed in local Maven repo (if not already)
if [ -f "$HYTALE_JAR_PATH" ]; then
    echo "Installing HytaleServer.jar to local Maven repository..."
    mvn install:install-file \
      -Dfile="$HYTALE_JAR_PATH" \
      -DgroupId=com.hypixel.hytale \
      -DartifactId=HytaleServer-parent \
      -Dversion=1.0-SNAPSHOT \
      -Dpackaging=jar \
      -DgeneratePom=true \
      -q
else
    echo -e "${RED}Error: HytaleServer.jar not found at $HYTALE_JAR_PATH${NC}"
    exit 1
fi

# 2. Build the plugin
echo "Building the plugin..."
mvn clean package
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi

# 3. Deploy Plugin
echo "Deploying $JAR_NAME to $SERVER_IP..."
scp "target/$JAR_NAME" "$SERVER_USER@$SERVER_IP:$REMOTE_PATH"
if [ $? -ne 0 ]; then
    echo -e "${RED}Deployment failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Deployment successful! Restarting server...${NC}"
bash ../restart_server.sh

