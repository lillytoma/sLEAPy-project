#!/bin/bash
# Helper script to safely run the sLEAPy backend with proper cleanup

# Load environment variables from .env
echo "Loading environment variables..."
export $(cat ../.env | xargs)

# Show what we're about to do
echo "Starting sLEAPy backend on port 8081..."
echo "Press Ctrl+C to stop the server cleanly"
echo ""

# Start the application
./mvnw spring-boot:run
