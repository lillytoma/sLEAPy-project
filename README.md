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


## Notes
This README will evolve throughout the sprint as more details are finalized.
