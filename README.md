# sLEAPy-project

## Overview
This repository establishes the core workflow, collaboration structure, and early project scaffolding that the team will build upon in future sprints.

---

## Project Description
Sprint 1 focuses on setting up the essential groundwork for the sLEAPy project. Key objectives include:

- [Placeholder]
  
More detailed feature descriptions will be added as the project evolves.

---

## Branching Strategy
We follow **trunk‑based development** with **short‑lived feature branches**:

- All stable code lives in `main`  
- New work is done in small, focused feature branches  
- Branches are merged quickly through pull requests  
- Frequent integration reduces merge conflicts and keeps progress continuous  

This approach supports fast iteration and strong team collaboration.

---

## Team Members
This project is developed by a team of five:

- **Lilly Toma**  
- **Robbie Thurston**  
- **Rishav Mohanty**  
- **Edosa Aigbuza**  
- **Ian Jackson**

---

## Initial Setup

1. Clone the repository:
   ```
   git clone <repo-url>
   cd sLEAPy-sprint1-project
   ```
2. Make sure you have installed:
   - Java 21
   - Maven
   - Docker

---

## PostgreSQL Database Setup

### Environment (.env)
Create the .env in the starter folder and
create a username, password, and database name 
of your choosing. For example:

POSTGRES_USER=[username]
POSTGRES_PASSWORD=[password]
POSTGRES_DB=[db_name]

### Starting the Database

1. Navigate to the `starter` directory:
   ```bash
   cd starter
   ```

2. Start PostgreSQL in Docker:
   ```bash
   docker-compose up -d
   ```
   This starts the `sleapy-postgres` container with the `sleapy_db` database.

3. Verify the container is running:
   ```bash
   docker-compose ps
   ```
   You should see `sleapy-postgres` with status `Up`.

### Testing the Connection Locally

From the EC2 instance, test the connection:
```bash
psql -h localhost -U postgres -d sleapy_db -p 8101 -W
```

If you see the `psql` prompt (`sleapy_db=>`), the database is working.

### Connecting from Windows via SSH Tunnel

To access PostgreSQL from your Windows machine securely:

1. **Open an SSH tunnel** (keep this terminal open while using the database):
   ```powershell
   ssh -L 8101:localhost:8101 ec2-user@YOUR_EC2_IP
   ```
   Replace `YOUR_EC2_IP` with your EC2 instance IP.

2. **Connect via pgAdmin** (or any PostgreSQL client):
   - **Host:** `localhost`
   - **Port:** `8101`
   - **Database:**
   - **Username:**
   - **Password:**

   The SSH tunnel transparently routes your connection through the encrypted SSH channel.

---

## Running the Program

### Backend (Java)

1. Navigate to the backend directory:
   ```bash
   cd starter/backend
   ```

2. Build the jar:
   ```bash
   mvn clean package
   ```

3. Build the Docker image:
   ```bash
   docker build -t sleapy-backend .
   ```

4. Run a container:
   ```bash
   docker run --name sleapy-backend sleapy-backend
   ```

View logs with `docker logs sleapy-backend` and stop with `docker rm -f sleapy-backend`.

### Backend — Running Locally for Development

The steps above build a deployable Docker image. Day to day, it's faster to just run the backend directly with Maven and point it at the real database. From `starter/backend`:

1. Make sure `mvnw` is executable (only needed once per machine):
   ```bash
   chmod +x mvnw
   ```

2. Load the real database credentials from the `.env` file (the same one described under [PostgreSQL Database Setup](#postgresql-database-setup)) into your shell:
   ```bash
   set -a && source ../.env && set +a
   ```
   `.env` files aren't automatically picked up by Maven/Java the way they are by `docker-compose` — this line explicitly loads the values as real environment variables for your current terminal session. You'll need to re-run it in every new terminal.

3. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```
   If you see `HikariPool-1 - Start completed.` and `Tomcat started on port(s): 8080`, it's up and connected to the real database.

   If port `8080` is already taken on your machine (e.g. Jenkins uses it on some of our EC2 boxes), run on a different port instead of fighting for `8080`:
   ```bash
   SERVER_PORT=8081 ./mvnw spring-boot:run
   ```

### Frontend (Angular)

1. Navigate to the frontend directory:
   ```bash
   cd starter/frontend
   ```

2. Install dependencies (if not already installed):
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```

4. Open your browser to `http://localhost:4200`

---

## Testing the API

While the frontend is still catching up, endpoints can be tested directly with `curl` once the backend is running (see [Backend — Running Locally for Development](#backend--running-locally-for-development)).

**Login** (`POST /api/auth/login`) — put the request body in a file first rather than inlining it, to avoid shell-quoting headaches:
```bash
cat > login.json << 'EOF'
{
  "email": "test@example.com",
  "password": "yourpassword"
}
EOF

curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d @login.json
```
Expect `200 OK` for correct credentials, and `401 Unauthorized` for either a wrong password or an email that doesn't exist — both cases return the same response on purpose, so a failed attempt never reveals which part was wrong.

**Client balance** (`GET /api/clients/{clientId}/balance`):
```bash
curl http://localhost:8080/api/clients/1/balance
```

(swap `8080` for `8081`, or whatever port you're actually running on, if you overrode it)

---

## Cloning the Database

1. In pgAdmin, right-click Servers in the left panel and select Register > Server.

2. Fill in the connection details:

  - Name: sleapy-postgres
  - Host: localhost
  - Port: 5432 (or your PostgreSQL port)
  - Username/Password: Your local PostgreSQL credentials

3. Click Save. Once connected, right-click your database and select Query Tool.

4. Copy the entire contents of tables.sql (starter\database\tables.sql) and paste it into the Query Tool. Click Execute (or press F5).

5. Repeat step 4 with mockdata.sql (starter\database\mockdata.sql) to seed test data.

Your local database is now ready to use!

---

## Notes
This README will evolve throughout the sprint as more details are finalized.
