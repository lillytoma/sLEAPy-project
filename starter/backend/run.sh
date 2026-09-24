#!/bin/bash
# Helper script to safely run the sLEAPy backend with proper cleanup

# Kill any lingering processes (just in case)
echo "Cleaning up any existing processes..."
pkill -9 java 2>/dev/null || true
sleep 2

# Load environment variables from .env
echo "Loading environment variables..."
export $(cat ../.env | xargs)

# Show what we're about to do
echo "Starting sLEAPy backend on port 8081..."
echo "Press Ctrl+C to stop the server cleanly"
echo ""

# Start the application
mvn spring-boot:run
