#!/bin/bash
# Complete startup script for sLEAPy system
# This script starts all services in the correct order

set -e

STARTER_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "=========================================="
echo "  sLEAPy Trading System - Full Startup"
echo "=========================================="
echo ""

# STEP 1: Start Docker containers (PostgreSQL + Redis)
echo "STEP 1/5: Starting Docker containers..."
cd "$STARTER_DIR"
docker-compose up -d

echo "Waiting 5 seconds for containers to be ready..."
sleep 5

if docker-compose ps | grep -q "sleapy-postgres.*Up"; then
  echo "✓ PostgreSQL running on port 8101"
else
  echo "✗ PostgreSQL failed to start"
  exit 1
fi

if docker-compose ps | grep -q "sleapy-redis.*Up"; then
  echo "✓ Redis running on port 6379"
else
  echo "✗ Redis failed to start"
  exit 1
fi
echo ""

# STEP 2: Start Backend
echo "STEP 2/5: Starting Backend API server..."
cd "$STARTER_DIR/backend"

# Set database credentials (MUST match docker-compose.yml)
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=sleapy_neueda
export POSTGRES_DB=sleapy_db

java -jar target/project-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

echo "Waiting 25 seconds for startup..."
sleep 25

if grep -q "Tomcat started" backend.log; then
  echo "✓ Backend started on port 8081"
else
  echo "✗ Backend failed to start"
  echo "Check backend.log for details:"
  tail -20 backend.log
  kill $BACKEND_PID 2>/dev/null || true
  exit 1
fi
echo ""

# STEP 3: Start Price Streamer
echo "STEP 3/5: Starting Price Streamer..."
source /tmp/price_venv/bin/activate 2>/dev/null || python3 -m venv /tmp/price_venv && source /tmp/price_venv/bin/activate
pip install -q yfinance redis 2>/dev/null || true

python3 price_streamer.py > price_streamer.log 2>&1 &
STREAMER_PID=$!
echo "Price Streamer PID: $STREAMER_PID"

echo "Waiting 8 seconds for first price update..."
sleep 8

if grep -q "Updated" price_streamer.log; then
  echo "✓ Price Streamer running (updating prices every 5s)"
  tail -3 price_streamer.log | grep "Updated" | head -2
else
  echo "✗ Price Streamer failed"
  echo "Check price_streamer.log for details"
fi
echo ""

# STEP 4: Start Frontend
echo "STEP 4/5: Starting Frontend (Angular)..."
cd "$STARTER_DIR/frontend"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
  echo "Installing npm dependencies..."
  npm install -q 2>/dev/null || true
fi

ng serve --host 0.0.0.0 --port 4200 > frontend.log 2>&1 &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"

echo "Waiting 20 seconds for startup..."
sleep 20

if netstat -tlnp 2>/dev/null | grep -q 4200; then
  echo "✓ Frontend running on port 4200"
else
  echo "✗ Frontend failed to start"
  echo "Check frontend.log for details"
fi
echo ""

# STEP 5: Summary
echo "=========================================="
echo "  ✅ SYSTEM READY"
echo "=========================================="
echo ""
echo "📍 Access URLs:"
echo "  Frontend:     http://localhost:4200"
echo "  Backend API:  http://localhost:8081"
echo "  PgAdmin:      http://localhost:5050"
echo ""
echo "💾 Database Credentials:"
echo "  Username: postgres"
echo "  Password: sleapy_neueda"
echo "  Database: sleapy_db"
echo ""
echo "🎯 Test Account (auto-created):"
echo "  Email:    testuser@example.com"
echo "  Password: TestPassword123"
echo ""
echo "🧪 Test It:"
echo "  1. Open http://localhost:4200"
echo "  2. Click 'Sign Up' or use test account"
echo "  3. Log in with credentials"
echo "  4. Navigate to Markets"
echo "  5. View live stock prices"
echo ""
echo "📋 Running Processes:"
echo "  Backend:  PID $BACKEND_PID"
echo "  Streamer: PID $STREAMER_PID"
echo "  Frontend: PID $FRONTEND_PID"
echo ""
echo "❌ To Stop All Services:"
echo "  pkill -9 java && pkill -9 python3 && pkill -9 'ng serve' && docker-compose down"
echo ""
echo "📖 View Logs:"
echo "  Backend:  tail -f backend/backend.log"
echo "  Streamer: tail -f backend/price_streamer.log"
echo "  Frontend: tail -f frontend/frontend.log"
echo ""
