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

The backend also accepts `POSTGRES_HOST` (default `localhost`) and
`POSTGRES_PORT` (default `8101`). For a database accessed through an SSH tunnel,
these must identify the tunnel's **local** listening address and port.

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

4. If you already had a PostgreSQL volume from an older checkout, refresh the analytics schema used by the analyst dashboard:
   ```bash
   psql -h localhost -U postgres -d sleapy_db -p 8101 -f database/analystSchema.sql
   ```

### Testing the Connection Locally

The backend automatically applies the non-destructive analyst-role and client-profile migrations on
startup, including for existing database volumes. Existing accounts default to
non-analyst access; existing analyst roles are preserved. PostgreSQL must already
contain the operational tables, and the configured database user must be able to
alter `public.clients`. Missing profile fields on legacy accounts remain null
until supplied; existing profile values and legacy addresses are not overwritten.

To apply the same login fix to an already-running backend without restarting it,
run this from `starter` using your configured database credentials:

```bash
psql -h localhost -U postgres -d sleapy_db -p 8101 -v ON_ERROR_STOP=1 \
  -f backend/src/main/resources/db/migrations/001-add-analyst-role.sql \
  -f backend/src/main/resources/db/migrations/002-add-client-profile.sql
```

Do not rerun the mock-data seed or delete the database volume to fix a missing
column; those operations can erase existing accounts and trades.

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

### Running the app against an existing remote database

Do not start the local PostgreSQL container when your accounts live in a remote
database reached through port `8101`. Both Docker and SSH cannot own that port;
an empty local database will reject otherwise correct account credentials.

If the local container is occupying the tunnel port, stop it without deleting
its data:

```bash
docker stop sleapy-postgres
```

Then open a new tunnel and leave it running:

```bash
ssh -N -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 -o ServerAliveCountMax=3 \
  -L 127.0.0.1:8101:localhost:8101 ec2-user@YOUR_DATABASE_HOST
```

If an older SSH session failed to bind the port, restart that session after
stopping Docker; it will not automatically acquire the freed port.
Verify the connection before starting the backend:

```bash
psql -h localhost -U postgres -d sleapy_db -p 8101 -W \
  -c 'SELECT current_database(), COUNT(*) AS accounts FROM public.clients;'
```

Restart the backend after restoring or changing the database target so pooled
connections cannot keep pointing at the previous instance. Do not reseed the
database or reset passwords to repair a tunnel/port conflict.

The analyst summary reads committed filled orders (`status = 2`) directly from
`public.orders`, excluding admin and analyst accounts. Instrument types are
looked up from `public.instruments` and `public.instrument_type`; missing type
lookups are shown as `Other`. There is no top-100 cap on dashboard totals.
The dashboard refreshes every 10 seconds, including when a client is selected;
the refresh button fetches immediately. New committed filled orders appear on
the next successful refresh. Pending, cancelled, and rolled-back orders do not
contribute to trading totals.

Day, Week, Month, and Year buttons filter all summary totals and instrument
breakdowns using calendar-to-date ranges in the backend server's time zone.
Day begins at midnight, Week begins on Monday, Month on the first, and Year on
January 1. The default is Day. The API accepts
`GET /api/analyst/summary?period=DAY|WEEK|MONTH|YEAR` plus optional `clientId`.
Unknown periods return HTTP 400. No trades in a range returns zero totals and an
empty breakdown. Polling cancels outdated requests when filters change and retries
on the next interval after displaying request errors.

The Python ETL remains a manual batch-reporting pipeline, not an order-placement
hook: `etl.py` extracts datasets and replaces warehouse tables when invoked.
Its `expensive_trades` dataset contains only the top 100 orders, and previously
kept the dashboard stale. The dashboard no longer depends on that snapshot or
on an unpublished `/topic/analyst` WebSocket topic. This change does not schedule
ETL or change its batch exports.
The analyst mapper explicitly maps the instrument name, type, trade count, and
volume to API fields rather than relying on global underscore-to-camel-case mapping.
Analyst client searches match usernames and emails case-insensitively and exclude
any account marked as an admin or analyst. Legacy null role flags are treated as
unset. Startup adds the admin role column if missing without changing existing
role assignments.
The analyst client field suggests matches after a short typing delay and cancels
outdated searches. Select a suggestion to fill the field and filter analytics;
Arrow keys and Enter select a match, and Escape dismisses suggestions.

The aggregate trading activity chart uses actual historical filled-order values,
with a gradient fill, value axis, and hover tooltip. Day groups orders by hour
and labels times of day; Week groups by date starting Monday; Month groups by day
of the month; Year groups by calendar month. Each range runs from its calendar
start through the current interval in the server's time zone. Empty elapsed
intervals show zero, future intervals are omitted, and the current interval is
partial. Values are per-interval trade volume, not cumulative totals. The chart
refreshes every 10 seconds, respects the shared client and instrument filters,
and does not require waiting on session samples. Straight segments avoid
inventing negative activity between zero-value intervals.
Orders recorded with only a purchase date appear in that day's midnight bucket;
the chart cannot reconstruct intraday times that were not stored.
The top toolbar's period, instrument, and client controls apply to every metric,
the activity chart, the instrument-type donut, and the most-active instrument list.
Customer search supports multiple selections. Add customers using the search
suggestions, remove individual selection chips, or use Clear all to return to all
customers. Every summary query uses the selected customer IDs without counting
duplicate selections; administrators and analysts remain excluded.
The summary API accepts comma-separated or repeated `clientIds` query parameters
and retains support for the original single `clientId` parameter.

With All instruments selected, the donut shows the share of filled orders by
instrument type. Select a legend button or slice to see that type's most-active
instruments alongside it, ranked by order count, then traded dollar value, with
five symbols per page.

With a specific instrument type selected, the donut instead shows its top five
symbols by filled-order count for the selected customers and period. Percentages
are order shares within those five; the legend and hover include traded dollar
volume. Clicking a symbol shows its current owners, ranked by summed shares held,
five per page. The highest-ranked symbol is initially selected. Ownership refreshes
every 10 seconds, uses the selected customer IDs, excludes admins/analysts, and
omits zero/negative holdings. Ownership is current, not limited to period trades.
The protected `/api/analyst/instrument-owners?symbol=...&clientIds=...` endpoint
provides these owners. Changing symbols or customer filters cancels old owner
requests; errors are displayed with automatic retries. Empty results display
explicit messages. The extra instrument-details and portfolio-summary views remain removed.

Selected customers are always displayed below the search controls as green name
chips, independently of whether search suggestions are open. Each chip removes
that customer; Clear all returns to all customers. The search field retains its
fixed layout while chips wrap on a separate row.

Migration `004-add-portfolio-snapshots.sql` creates persistent snapshot storage.
The backend records customer cash and per-type holdings values at startup and
every five minutes while running, independently of dashboard visits. Snapshots
are not backfilled or deleted on restart. Day/Week/Month/Year selects recorded
snapshots within that calendar-to-date range. Snapshot collection and API history
remain available, but are not displayed in the restored instrument panels.

Before recording begins, historical values are unavailable; gaps while the
backend is stopped are not interpolated.
Holdings without cached market prices make affected valuations unavailable,
not zero, while cash and unaffected instrument values remain visible. Prices
are the cache values at capture time, not independently reconstructed historical
quotes. Snapshot collection respects `spring.task.scheduling.enabled=false` in
tests. The new migration and collection begin when you next start the backend.

The analyst header's account avatar includes sign out and the shared persistent
appearance, text-size, and color-vision preferences. Controls support keyboard
navigation; Escape closes the panel and restores focus to the avatar.
Night mode is available under Appearance. Text-size scaling applies to both the
trader dashboard and the analyst page; color-vision palettes are grouped under
the Color themes heading.

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
