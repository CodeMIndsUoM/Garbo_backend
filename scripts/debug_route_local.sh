#!/usr/bin/env bash
# debug_route_local.sh
# Run ON your local Mac to trigger remote diagnostics on the EC2 instance via AWS SSM.

set -euo pipefail

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

EC2_INSTANCE_ID="i-0cc6c206c6c34456c"
AWS_PROFILE="garbo"
AWS_REGION="ap-south-1"
SCRIPT_PATH="$(dirname "$0")/debug_route_production.sh"

if [ ! -f "$SCRIPT_PATH" ]; then
    echo -e "${RED}ERROR: debug_route_production.sh not found at ${SCRIPT_PATH}${NC}"
    exit 1
fi

echo -e "${BLUE}=== Launching Remote Diagnostics via AWS SSM ===${NC}"
echo "Instance ID: ${EC2_INSTANCE_ID}"
echo "AWS Profile: ${AWS_PROFILE}"
echo "AWS Region:  ${AWS_REGION}"

# Read local production script contents
SCRIPT_CONTENT=$(cat "$SCRIPT_PATH")

# Construct parameter JSON safely using jq to avoid escaping issues
PARAMETERS_JSON=$(jq -n \
  --arg script "$SCRIPT_CONTENT" \
  '{"commands": [ "cat << '\''EOF'\'' > /tmp/debug_route.sh", $script, "EOF", "chmod +x /tmp/debug_route.sh", "/tmp/debug_route.sh" ]}')

# Run the command via SSM
echo -e "\n${BLUE}Sending SSM command to EC2...${NC}"
COMMAND_ID=$(aws ssm send-command \
  --profile "$AWS_PROFILE" \
  --region "$AWS_REGION" \
  --instance-ids "$EC2_INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters "$PARAMETERS_JSON" \
  --query "Command.CommandId" \
  --output text)

echo "SSM Command Sent. ID: ${COMMAND_ID}"
echo "Waiting for execution to complete..."

# Poll for completion (up to 30 seconds)
for i in $(seq 1 30); do
    STATUS=$(aws ssm get-command-invocation \
      --profile "$AWS_PROFILE" \
      --region "$AWS_REGION" \
      --command-id "$COMMAND_ID" \
      --instance-id "$EC2_INSTANCE_ID" \
      --query "Status" \
      --output text 2>/dev/null || echo "Pending")
    
    if [[ "$STATUS" == "Success" || "$STATUS" == "Failed" || "$STATUS" == "Cancelled" || "$STATUS" == "TimedOut" ]]; then
        echo -e "Execution status: ${STATUS}\n"
        
        # Get standard output and error
        echo -e "${GREEN}--- STANDARD OUTPUT ---${NC}"
        aws ssm get-command-invocation \
          --profile "$AWS_PROFILE" \
          --region "$AWS_REGION" \
          --command-id "$COMMAND_ID" \
          --instance-id "$EC2_INSTANCE_ID" \
          --query "StandardOutputContent" \
          --output text
          
        echo -e "\n${RED}--- STANDARD ERROR ---${NC}"
        aws ssm get-command-invocation \
          --profile "$AWS_PROFILE" \
          --region "$AWS_REGION" \
          --command-id "$COMMAND_ID" \
          --instance-id "$EC2_INSTANCE_ID" \
          --query "StandardErrorContent" \
          --output text
          
        if [[ "$STATUS" == "Success" ]]; then
            exit 0
        else
            exit 1
        fi
    fi
    sleep 2
done

echo -e "${RED}ERROR: SSM Command timed out waiting for execution.${NC}"
exit 1
