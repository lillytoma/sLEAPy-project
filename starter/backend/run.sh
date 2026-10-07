#!/bin/bash
# Helper script to safely run the sLEAPy backend with proper cleanup

# Set database credentials (must match docker-compose.yml)
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=sleapy_neueda
export POSTGRES_DB=sleapy_db

# Show what we're about to do
echo "Starting sLEAPy backend on port 8081..."
echo "Database: $POSTGRES_DB"
echo "User: $POSTGRES_USER"
echo "Press Ctrl+C to stop the server cleanly"
echo ""

# Start the application
java -jar target/project-0.0.1-SNAPSHOT.jar
