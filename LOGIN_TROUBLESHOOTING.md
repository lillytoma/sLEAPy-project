# Login Troubleshooting Guide

## Issue: Unable to Login
**Root Cause**: Database connection failed due to missing environment variables

### What Was Wrong
When the backend started, it couldn't connect to PostgreSQL because the environment variables weren't being passed to the Java process:
- `POSTGRES_USER` was being treated as literal string instead of `postgres`
- `POSTGRES_PASSWORD` was being treated as literal string instead of `sleapy_neueda`
- Backend threw: `FATAL: password authentication failed for user "${POSTGRES_USER}"`

### The Fix
Database credentials must be exported **before** running the Java JAR:

```bash
# Set the environment variables
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=sleapy_neueda
export POSTGRES_DB=sleapy_db

# Then run the backend
java -jar target/project-0.0.1-SNAPSHOT.jar
```

### How to Start Backend Correctly

**Option 1: Using the updated run.sh script**
```bash
cd backend
./run.sh
```

**Option 2: Manual startup with environment variables**
```bash
cd backend
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=sleapy_neueda
export POSTGRES_DB=sleapy_db
java -jar target/project-0.0.1-SNAPSHOT.jar
```

**Option 3: Using the complete startup script**
```bash
cd starter
chmod +x COMPLETE_STARTUP.sh
./COMPLETE_STARTUP.sh
```

### Verify Backend is Working

Test the login endpoint:
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"testuser@example.com","password":"TestPassword123"}'
```

Should return a JSON response with a token (not an error).

### Test Account (Auto-Created)
```
Email:    testuser@example.com
Password: TestPassword123
```

### Database Credentials (for reference)
```
Host:     localhost:8101
User:     postgres
Password: sleapy_neueda
Database: sleapy_db
```

### Complete System Check

After starting all services, verify:

1. **Docker containers running**
   ```bash
   docker-compose ps
   ```
   Should show: postgres (8101) and redis (6379)

2. **Backend responding**
   ```bash
   curl http://localhost:8081/api/prices/all
   ```
   Should return JSON with prices

3. **Frontend accessible**
   ```bash
   curl http://localhost:4200
   ```
   Should return HTML (or no error)

4. **Price streamer updating**
   ```bash
   tail backend/price_streamer.log
   ```
   Should show "Updated AAPL:", "Updated MSFT:", etc.

### Troubleshooting Steps

If login still doesn't work:

1. **Check backend logs**
   ```bash
   tail -50 backend/backend.log | grep -i "error\|exception"
   ```

2. **Check database connection**
   ```bash
   psql -h localhost -p 8101 -U postgres -d sleapy_db -c "SELECT 1"
   ```
   Password: `sleapy_neueda`

3. **Restart backend with correct credentials**
   ```bash
   pkill -9 java
   cd backend
   export POSTGRES_USER=postgres POSTGRES_PASSWORD=sleapy_neueda POSTGRES_DB=sleapy_db
   java -jar target/project-0.0.1-SNAPSHOT.jar
   sleep 25
   # Try login again
   ```

4. **Clear browser cache**
   - Clear localStorage: Open DevTools → Application → LocalStorage → Clear All
   - Hard refresh: Ctrl+Shift+R (Chrome) or Cmd+Shift+R (Mac)

### Frontend Debug Console

Check browser console for errors:
1. Open http://localhost:4200
2. Press F12 to open DevTools
3. Go to Console tab
4. Check for red error messages
5. Go to Network tab
6. Try to login
7. Check the `/api/auth/login` request
   - Status should be 200 (success) or 401 (user not found)
   - Should NOT show CORS errors or "Cannot reach server"

### Quick Fix Summary

**The issue was**: Environment variables weren't passed to Java  
**The solution**: Export credentials before running backend  
**Prevention**: Use COMPLETE_STARTUP.sh or run.sh (now updated)  
