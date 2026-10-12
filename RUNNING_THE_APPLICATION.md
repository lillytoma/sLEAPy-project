# Running the sLEAPy Trading Platform

## Quick Start (All-in-One)

```bash
cd ~/sLEAPy/sLEAPy-sprint1-project/starter

# 1. Start the database and services
docker-compose up -d postgres pgadmin redis

# 2. Wait 5 seconds for database to initialize
sleep 5

# 3. Initialize the database schema (create market_prices table)
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "
CREATE TABLE IF NOT EXISTS Market_Prices (
    Symbol VARCHAR(20) PRIMARY KEY,
    Current_Price NUMERIC(15,2) NOT NULL,
    High_Price NUMERIC(15,2),
    Low_Price NUMERIC(15,2),
    Last_Updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);"

# 4. In Terminal 1: Start the backend (runs on port 8081)
cd backend
mvn spring-boot:run

# 5. In Terminal 2: Start the frontend (runs on port 4200)
cd frontend
ng serve --port 4200

# 6. Open browser and navigate to http://localhost:4200
```

## System Architecture

### Backend (Spring Boot)
- **Port:** 8081
- **Database:** PostgreSQL on localhost:8101
- **Database:** sleapy_db (user: postgres, password: sleapy_neueda)
- **Stack:** Java 21, Spring Boot 4.1.1, MyBatis, WebSocket

### Frontend (Angular)
- **Port:** 4200
- **API Server:** http://localhost:8081
- **Stack:** Angular standalone components, TypeScript, RxJS, Tailwind CSS

### Database (PostgreSQL)
- **Port:** 8101 (external) → 5432 (internal)
- **Container:** sleapy-postgres
- **Credentials:** postgres / sleapy_neueda
- **Databases:** sleapy_db (main), sleapy_analyst (analytics)

## Testing the Application

### 1. Verify Backend API is Running

```bash
# Check if backend responds
curl -s http://localhost:8081/api/instruments | jq '.[] | {symbol: .symbol, name: .symbolName}'

# Expected output: List of 16 instruments (AAPL, AMZN, BTC, ETH, GOOGL, IWM, JPM, META, MSFT, NVDA, QQQ, SPY, TSLA, US10Y, V, XOM)
```

### 2. Test Signup Flow

**Step 1:** Visit http://localhost:4200/signup

**Step 2:** Fill in the form with:
```
First Name: John
Last Name: Doe
Date of Birth: 01/01/1990 (age must be >= 18)
Phone: 555-0123
Street: 123 Main St
City: New York
Zip: 10001
Region: NY
State: New York
SSN: 123-45-6789
Email: john.doe@example.com
Password: SecurePass123! (must be strong: 4/4 score)
```

**Step 3:** Click "Sign Up"

**Expected Result:**
- Success message displayed
- Redirects to /login after 2 seconds

**API Call Details:**
- **Endpoint:** POST /api/auth/signup
- **Request Body:**
```json
{
  "firstname": "John",
  "lastname": "Doe",
  "dob": "1990-01-01",
  "phoneNumber": "555-0123",
  "street": "123 Main St",
  "city": "New York",
  "zip": "10001",
  "region": "NY",
  "state": "New York",
  "ssn": "123-45-6789",
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

- **Response:**
```json
{
  "message": "Signup successful",
  "clientId": 123,
  "token": "eyJhbGc..."
}
```

### 3. Test Login Flow

**Step 1:** Visit http://localhost:4200/login

**Step 2:** Enter credentials from signup:
```
Email: john.doe@example.com
Password: SecurePass123!
```

**Step 3:** Click "Login"

**Expected Result:**
- No error message
- JWT token stored in localStorage as 'auth_token'
- User ID stored in localStorage as 'user_id'
- Redirects to /dashboard/home

**Verification:**
```bash
# Open browser DevTools Console and run:
localStorage.getItem('auth_token')  # Should return JWT token
localStorage.getItem('user_id')     # Should return client ID
localStorage.getItem('user_email')  # Should return user email
```

**API Call Details:**
- **Endpoint:** POST /api/auth/login
- **Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

- **Response:**
```json
{
  "message": "Login successful",
  "clientId": 123,
  "token": "eyJhbGc..."
}
```

### 4. Test Dashboard Navigation

After successful login, you should see:
- **Left Sidebar:** Navigation menu with 6 tabs
  - Dashboard (Home)
  - Portfolio
  - Markets
  - Watchlist
  - History
  - Settings
- **Top Header:** 
  - Theme toggle button
  - Sign Out button
- **Main Content:** Portfolio summary (initially empty)

### 5. Test Market Data API

```bash
# Get all instruments
curl -s http://localhost:8081/api/instruments | jq '.[0]'

# Expected response:
# {
#   "symbol": "AAPL",
#   "symbolName": "Apple Inc.",
#   "instrumentType": {
#     "instrumentType_id": 1,
#     "name": "Stock"
#   },
#   "currentPrice": null,
#   "highPrice": null,
#   "lowPrice": null,
#   "lastUpdated": null
# }

# Search for a specific instrument
curl -s "http://localhost:8081/api/instruments/search?q=APPLE" | jq '.'

# Get a specific instrument
curl -s http://localhost:8081/api/instruments/AAPL | jq '.'
```

### 6. Test Order Placement

**Prerequisites:** 
- Must be logged in
- Must have cash balance in your account

**Step 1:** Navigate to /dashboard/markets (click "Markets" in sidebar)

**Step 2:** Click "Buy" button on any stock (e.g., AAPL)

**Step 3:** In the Buy Modal:
- Select order type: "Market" or "Limit"
- Enter number of shares: 10
- If "Limit", enter limit price: 150.00
- Click "Place Buy Order"

**Step 4:** Verify in database:
```bash
# Check order was created
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT * FROM Orders ORDER BY Order_ID DESC LIMIT 1;"

# Check holdings were updated
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT * FROM Holdings WHERE Instrument_ID IN (SELECT Instrument_ID FROM Instruments WHERE Symbol='AAPL');"

# Check cash balance was debited
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT Client_ID, cash_balance FROM Clients WHERE Client_ID=YOUR_CLIENT_ID;"
```

**API Call Details:**
- **Endpoint:** POST /api/trades/buy
- **Headers:** 
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

- **Request Body:**
```json
{
  "clientId": 123,
  "instrumentSymbol": "AAPL",
  "quantity": 10,
  "orderType": "MARKET",
  "limitPrice": null
}
```

- **Response:**
```json
{
  "orderId": 456,
  "clientId": 123,
  "quantity": 10,
  "orderStatus": "FILLED",
  "totalCost": 1600.50,
  "timestamp": "2024-10-08T15:45:30.123Z"
}
```

### 7. Test Sell Order

**Step 1:** Navigate to /dashboard/portfolio

**Step 2:** Click "Sell" button next to holdings you own

**Step 3:** In the Sell Modal:
- Select order type: "Market" or "Limit"
- Enter number of shares to sell: 5
- If "Limit", enter limit price: 160.00
- Click "Place Sell Order"

**Step 4:** Verify:
- Holding quantity decreased
- Cash balance increased
- Order record created in database

### 8. Test WebSocket Real-Time Updates

**Prerequisites:** Backend must be running with @Scheduled task enabled

**Step 1:** Open browser DevTools → Network tab

**Step 2:** Filter by "WS" to see WebSocket connections

**Step 3:** Navigate to /dashboard/markets

**Expected:**
- WebSocket connection established: ws://localhost:8081/ws/prices
- Messages arriving every 5 seconds
- Each message contains price updates for all instruments

**Message Format:**
```json
{
  "symbol": "AAPL",
  "currentPrice": 150.25,
  "highPrice": 151.00,
  "lowPrice": 149.50,
  "lastUpdated": "2024-10-08T15:45:30.123Z"
}
```

### 9. Test CORS Configuration

CORS is configured to allow requests from frontend (http://localhost:4200) to backend (http://localhost:8081).

**Verify CORS headers:**
```bash
curl -i -X OPTIONS http://localhost:8081/api/instruments \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: GET"

# Expected headers in response:
# Access-Control-Allow-Origin: http://localhost:4200
# Access-Control-Allow-Methods: GET, HEAD, POST, PUT, DELETE, PATCH, OPTIONS
```

## Database Management

### View All Instruments
```bash
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT * FROM Instruments;"
```

### View All Clients
```bash
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT * FROM Clients;"
```

### View All Orders
```bash
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT * FROM Orders;"
```

### View All Holdings
```bash
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT * FROM Holdings;"
```

### View Market Prices Cache
```bash
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT * FROM Market_Prices;"
```

### Access PGAdmin
- **URL:** http://localhost:5050
- **Email:** admin@sleapy.local
- **Password:** admin123

## Troubleshooting

### Backend won't start: Database Connection Failed
```bash
# 1. Check if database container is running
docker ps | grep postgres

# 2. If not running, start it
docker-compose up -d postgres

# 3. Wait 5 seconds for initialization
sleep 5

# 4. Verify database is accessible
psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT 1"
```

### Frontend won't connect to backend (CORS error)
```bash
# 1. Verify backend is running on port 8081
lsof -i :8081 | grep java

# 2. Verify environment.ts points to correct backend
cat frontend/src/environments/environment.ts

# 3. Should show: apiUrl: 'http://localhost:8081'

# 4. Rebuild frontend if needed
cd frontend
ng build
```

### Signup/Login endpoints return 403 Forbidden
```bash
# Issue: CORS not enabled on controller
# Solution: Ensure all controllers have @CrossOrigin(origins = "http://localhost:4200")

# Verify:
grep -r "@CrossOrigin" backend/src/main/java/com/sleapy/project/controllers/
```

### Market prices not updating (WebSocket not connecting)
```bash
# Check if @EnableScheduling is on Main class
grep -A2 "public class Main" backend/src/main/java/com/sleapy/project/Main.java

# Check if ScheduledPriceRefreshTask is running
# Look for log message: "Scheduled task running..."
```

## CI/CD and Deployment

### Build Production JAR
```bash
cd backend
mvn clean package -DskipTests

# JAR location: target/project-0.0.1-SNAPSHOT.jar
```

### Build Production Angular Bundle
```bash
cd frontend
ng build --configuration production

# Output: dist/frontend/
```

### Docker Build and Run
```bash
# Build backend image
docker build -t sleapy-backend backend/

# Run backend in container
docker run -p 8081:8081 -e POSTGRES_PASSWORD=sleapy_neueda sleapy-backend

# Build frontend image
docker build -t sleapy-frontend frontend/

# Run frontend in container
docker run -p 4200:4200 sleapy-frontend
```

## Key Endpoints Reference

### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Login user, receive JWT token

### Instruments
- `GET /api/instruments` - Get all instruments with prices
- `GET /api/instruments/{symbol}` - Get specific instrument
- `GET /api/instruments/search?q={query}` - Search instruments

### Trading
- `POST /api/trades/buy` - Place buy order
- `POST /api/trades/sell` - Place sell order

### Portfolio
- `GET /api/holdings/{clientId}` - Get client holdings
- `POST /api/holdings` - Create holding
- `PUT /api/holdings/{holdingId}` - Update holding
- `DELETE /api/holdings/{holdingId}` - Delete holding

### Orders
- `GET /api/orders?clientId={id}` - Get client orders
- `GET /api/orders/{orderId}` - Get specific order

### WebSocket
- `WS /ws/prices` - Real-time price updates (STOMP /topic/prices)

## Environment Variables

### Backend (application.yaml)
```yaml
POSTGRES_DB: sleapy_db
POSTGRES_USER: postgres
POSTGRES_PASSWORD: sleapy_neueda
JWT_SECRET: your_super_secret_key_change_this_in_production_at_least_32_chars
JWT_EXPIRATION: 86400000  # 24 hours in milliseconds
```

### Frontend (environment.ts)
```typescript
apiUrl: 'http://localhost:8081'
```

## Summary

✅ Backend running on http://localhost:8081  
✅ Frontend running on http://localhost:4200  
✅ Database running on localhost:8101  
✅ CORS configured for cross-origin requests  
✅ JWT authentication working  
✅ WebSocket real-time updates enabled  
✅ Trading system fully integrated  

**Ready for testing!**
