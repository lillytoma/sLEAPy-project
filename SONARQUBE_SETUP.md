# SonarQube Setup Guide for sLEAPy Project

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Step 1: Update Docker Compose](#step-1-update-docker-compose)
5. [Step 2: Create SonarQube Configuration Files](#step-2-create-sonarqube-configuration-files)
6. [Step 3: Configure Backend for Analysis](#step-3-configure-backend-for-analysis)
7. [Step 4: Configure Frontend for Analysis](#step-4-configure-frontend-for-analysis)
8. [Step 5: Initial Setup and Authentication](#step-5-initial-setup-and-authentication)
9. [Step 6: Running Local Analysis](#step-6-running-local-analysis)
10. [Step 7: CI/CD Integration](#step-7-cicd-integration)
11. [Best Practices](#best-practices)
12. [Troubleshooting](#troubleshooting)
13. [Team Instructions](#team-instructions)

---

## Overview

SonarQube is a comprehensive platform for continuous inspection of code quality. This setup provides:

- **Local Development**: Run SonarQube via Docker on your machine
- **Team Collaboration**: Centralized code quality metrics and standards
- **Multi-language Support**: Java (backend), TypeScript/Angular (frontend), Python (ETL)
- **CI/CD Integration**: Automated analysis on every commit
- **Quality Gates**: Prevent degradation of code quality

### Why SonarQube?
- Detects bugs and code smells
- Enforces code standards and security best practices
- Provides detailed reports and metrics
- Integrates with GitHub for PR analysis
- Supports multiple programming languages

---

## Architecture

```
┌─────────────────────────────────────────┐
│         Local Developer Machine         │
├─────────────────────────────────────────┤
│ Docker                                  │
│ ├─ SonarQube (Port 9000)               │
│ ├─ PostgreSQL (Port 5432)              │
│ └─ PgAdmin (Port 5050)                 │
│                                         │
│ SonarScanner CLI                        │
│ └─ Analyzes code & sends to SonarQube  │
└─────────────────────────────────────────┘
```

---

## Prerequisites

Before starting, ensure your development environment has:

### Required Software
- **Docker**: Version 20.10+
- **Docker Compose**: Version 2.0+
- **Java 21**: For backend analysis
- **Maven**: 3.8.1+
- **Node.js**: 18+ (for frontend analysis)
- **Git**: Latest version

### System Requirements
- **Disk Space**: At least 5 GB free
- **RAM**: Minimum 4 GB (8 GB recommended)
- **Ports Available**: 9000 (SonarQube), 5432 (PostgreSQL)

### Installation Verification
```bash
# Verify Docker
docker --version
docker-compose --version

# Verify Java
java -version

# Verify Maven
mvn --version

# Verify Node.js
node --version
npm --version
```

---

## Step 1: Update Docker Compose

Update your `starter/docker-compose.yml` to include the SonarQube service alongside existing services.

### Instructions:
1. Open `starter/docker-compose.yml`
2. Add the following SonarQube service to your services section:

```yaml
  sonarqube:
    image: sonarqube:lts-community
    container_name: sleapy-sonarqube
    restart: unless-stopped
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://postgres:5432/sonar
      SONAR_JDBC_USERNAME: sonar_user
      SONAR_JDBC_PASSWORD: sonar_password_changeme
      SONAR_ES_BOOTSTRAP_CHECKS_DISABLED: "true"
    ports:
      - "9000:9000"
    depends_on:
      - postgres
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_logs:/opt/sonarqube/logs
      - sonarqube_extensions:/opt/sonarqube/extensions
```

3. Add the SonarQube volumes at the bottom of the file:

```yaml
volumes:
  postgres_data:
  pgadmin_data:
  sonarqube_data:
  sonarqube_logs:
  sonarqube_extensions:
```

4. Update the PostgreSQL service to create a separate database for SonarQube by adding initialization SQL:
   - Create a file: `starter/database/sonar-init.sql`
   - See Step 2 below for content

---

## Step 2: Create SonarQube Configuration Files

### 2.1 Create PostgreSQL Initialization for SonarQube

Create `starter/database/sonar-init.sql`:

```sql
-- Create SonarQube database user and database
CREATE USER sonar_user WITH PASSWORD 'sonar_password_changeme';
CREATE DATABASE sonar OWNER sonar_user;
GRANT CONNECT ON DATABASE sonar TO sonar_user;
GRANT CREATE ON DATABASE sonar TO sonar_user;
```

**Important**: Add this file to your docker-compose PostgreSQL initialization:

Update the postgres service in `docker-compose.yml` to include:
```yaml
      - ./database/sonar-init.sql:/docker-entrypoint-initdb.d/00-sonar-init.sql
```

### 2.2 Create Project-Level SonarQube Configuration

Create `starter/sonar-project.properties` (for backend):

```properties
# SonarQube Project Configuration
# Backend - Java/Spring Boot Application

# Project identification
sonar.projectKey=com.sleapy:backend
sonar.projectName=sLEAPy Backend
sonar.projectVersion=1.0.0

# Source code location
sonar.sources=src/main

# Test coverage
sonar.tests=src/test
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# Exclusions
sonar.exclusions=**/config/**,**/dto/**

# Language specifications
sonar.language=java

# Code coverage tool
sonar.java.binaries=target/classes
sonar.java.libraries=~/.m2/repository/**/*.jar

# Analysis properties
sonar.login=CHANGEME_USE_TOKEN_FROM_SONARQUBE_UI
sonar.host.url=http://localhost:9000
```

### 2.3 Create Frontend SonarQube Configuration

Create `starter/frontend/sonar-project.properties`:

```properties
# SonarQube Project Configuration
# Frontend - Angular Application

# Project identification
sonar.projectKey=com.sleapy:frontend
sonar.projectName=sLEAPy Frontend
sonar.projectVersion=1.0.0

# Source code location
sonar.sources=src

# Test coverage
sonar.tests=src

# Exclusions
sonar.exclusions=**/*.spec.ts,node_modules/**

# Language specifications
sonar.language=typescript

# TypeScript specific settings
sonar.typescript.tslint.configPath=tslint.json

# Analysis properties
sonar.login=CHANGEME_USE_TOKEN_FROM_SONARQUBE_UI
sonar.host.url=http://localhost:9000
```

### 2.4 Create ETL Python Configuration

Create `starter/backend/etl/sonar-project.properties`:

```properties
# SonarQube Project Configuration
# ETL Pipeline - Python Application

# Project identification
sonar.projectKey=com.sleapy:etl
sonar.projectName=sLEAPy ETL
sonar.projectVersion=1.0.0

# Source code location
sonar.sources=.

# Exclusions
sonar.exclusions=venv/**,__pycache__/**,*.pyc

# Language specifications
sonar.language=py

# Analysis properties
sonar.login=CHANGEME_USE_TOKEN_FROM_SONARQUBE_UI
sonar.host.url=http://localhost:9000
```

---

## Step 3: Configure Backend for Analysis

### 3.1 Update Backend pom.xml

Add SonarQube Maven plugin to `starter/backend/pom.xml` in the `<build><plugins>` section:

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>
```

### 3.2 Configure Code Coverage (JaCoCo)

Add JaCoCo plugin to `starter/backend/pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 3.3 Update pom.xml SonarQube Properties

Ensure the following properties are in `starter/backend/pom.xml` `<properties>` section:

```xml
<sonar.projectKey>com.sleapy:backend</sonar.projectKey>
<sonar.projectName>sLEAPy Backend</sonar.projectName>
<sonar.host.url>http://localhost:9000</sonar.host.url>
<sonar.java.binaries>${project.build.directory}/classes</sonar.java.binaries>
```

---

## Step 4: Configure Frontend for Analysis

### 4.1 Install ESLint (If Not Already Installed)

```bash
cd starter/frontend
npm install --save-dev @angular-eslint/eslint-plugin @angular-eslint/eslint-plugin-template eslint typescript

# Fix any known vulnerabilities in dependencies
npm audit fix
```

### 4.2 Create or Update ESLint Configuration

Ensure `.eslintrc.json` exists in `starter/frontend/`:

```json
{
  "root": true,
  "ignorePatterns": ["projects/**/*"],
  "overrides": [
    {
      "files": ["*.ts"],
      "extends": [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended"
      ],
      "rules": {
        "@angular-eslint/directive-selector": [
          "error",
          {
            "type": "attribute",
            "prefix": "app",
            "style": "camelCase"
          }
        ],
        "@angular-eslint/component-selector": [
          "error",
          {
            "type": "element",
            "prefix": "app",
            "style": "kebab-case"
          }
        ]
      }
    }
  ]
}
```

---

## Step 5: Initial Setup and Authentication

### 5.1 Start Docker Containers

```bash
cd starter
docker-compose up -d
```

Verify all containers are running:
```bash
docker-compose ps
```

### 5.2 Access SonarQube Dashboard

1. Open browser: `http://localhost:9000`
2. Default login credentials:
   - Username: `admin`
   - Password: `admin`
3. **IMPORTANT**: Change default password immediately when prompted
4. Store new password securely (in your local password manager, NOT in Git)

### 5.3 Generate Authentication Token

This token is used by analysis tools to authenticate with SonarQube.

**Steps**:
1. Log in to SonarQube (`http://localhost:9000`)
2. Click on user profile (top-right corner)
3. Select **My Account** → **Security**
4. Under "Tokens" section, click **Generate**
5. Name it: `sleapy-local-analysis`
6. Select expiration (90 days recommended)
7. Copy the generated token
8. **DO NOT COMMIT THIS TOKEN TO GIT**

### 5.4 Store Token Securely

#### Option 1: Local Environment Variables (Recommended)

**Linux/Mac**:
```bash
# Add to ~/.bashrc, ~/.zshrc, or ~/.bash_profile
export SONAR_TOKEN="your_generated_token_here"
```

Then reload:
```bash
source ~/.bashrc  # or ~/.zshrc
```

**Windows**:
```cmd
setx SONAR_TOKEN "your_generated_token_here"
```

#### Option 2: Maven settings.xml

If not already present, create `~/.m2/settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <servers>
    <server>
      <id>sonarqube</id>
      <username>CHANGEME_YOUR_TOKEN</username>
      <password></password>
    </server>
  </servers>
</settings>
```

---

## Step 6: Running Local Analysis

### 6.1 Backend Java/Spring Boot Analysis

```bash
cd starter/backend

# Build the project with test coverage
mvn clean verify

# Run SonarQube analysis
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN
```

Or using the project key approach:
```bash
mvn sonar:sonar \
  -Dsonar.projectKey=com.sleapy:backend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN
```

### 6.2 Frontend Angular Analysis

```bash
cd starter/frontend

# Install dependencies
npm install

# Run linting
npm run lint

# Run SonarQube analysis using sonarqube-scanner
npx sonar-scanner \
  -Dsonar.projectKey=com.sleapy:frontend \
  -Dsonar.sources=src \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN
```

### 6.3 ETL Python Analysis

First, install SonarQube Scanner for Python:

```bash
cd starter/backend/etl

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
pip install sonarqube-scanner
```

Run analysis:
```bash
sonar-scanner \
  -Dsonar.projectKey=com.sleapy:etl \
  -Dsonar.sources=. \
  -Dsonar.exclusions="venv/**,__pycache__/**" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN
```

### 6.4 View Analysis Results

1. Go to `http://localhost:9000`
2. Navigate to **Projects**
3. Click on your project to view:
   - Code metrics (complexity, coverage, duplication)
   - Issues (bugs, vulnerabilities, code smells)
   - Security hotspots
   - Test coverage reports
   - Code quality gates status

---

## Step 7: CI/CD Integration

### 7.1 GitHub Actions Integration

Create `.github/workflows/sonarqube.yml`:

```yaml
name: SonarQube Analysis

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  sonarqube:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_USER: sonar_user
          POSTGRES_PASSWORD: ${{ secrets.SONAR_DB_PASSWORD }}
          POSTGRES_DB: sonar
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      sonarqube:
        image: sonarqube:latest-community
        env:
          SONAR_JDBC_URL: jdbc:postgresql://postgres:5432/sonar
          SONAR_JDBC_USERNAME: sonar_user
          SONAR_JDBC_PASSWORD: ${{ secrets.SONAR_DB_PASSWORD }}
          SONAR_ES_BOOTSTRAP_CHECKS_DISABLED: "true"
        options: >-
          --health-cmd "curl -f http://localhost:9000/api/system/health || exit 1"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 9000:9000
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Build and analyze backend
      working-directory: starter/backend
      run: |
        mvn clean verify sonar:sonar \
          -Dsonar.projectKey=com.sleapy:backend \
          -Dsonar.host.url=http://localhost:9000 \
          -Dsonar.login=${{ secrets.SONAR_TOKEN }}
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
    
    - name: Analyze frontend
      working-directory: starter/frontend
      run: |
        npm install
        npm run lint
        npx sonar-scanner \
          -Dsonar.projectKey=com.sleapy:frontend \
          -Dsonar.sources=src \
          -Dsonar.host.url=http://localhost:9000 \
          -Dsonar.login=${{ secrets.SONAR_TOKEN }}
    
    - name: Analyze ETL
      working-directory: starter/backend/etl
      run: |
        python -m pip install --upgrade pip
        pip install -r requirements.txt
        pip install sonar-scanner
        sonar-scanner \
          -Dsonar.projectKey=com.sleapy:etl \
          -Dsonar.sources=. \
          -Dsonar.exclusions="venv/**,__pycache__/**" \
          -Dsonar.host.url=http://localhost:9000 \
          -Dsonar.login=${{ secrets.SONAR_TOKEN }}
```

### 7.2 Set GitHub Secrets

Add these to your GitHub repository settings (**Settings** → **Secrets and variables** → **Actions**):

1. **SONAR_TOKEN**: Your SonarQube authentication token
2. **SONAR_DB_PASSWORD**: Password for the sonar_user database user (same as in docker-compose.yml)

**Important**: Never expose these in your workflow files or documentation.

### 7.3 Jenkins Integration (If Using Jenkins)

Create `starter/Jenkinsfile` additions:

```groovy
stage('SonarQube Analysis') {
    environment {
        SONAR_TOKEN = credentials('sonarqube-token')
    }
    steps {
        script {
            sh '''
                cd starter/backend
                mvn clean verify sonar:sonar \
                  -Dsonar.projectKey=com.sleapy:backend \
                  -Dsonar.host.url=http://localhost:9000 \
                  -Dsonar.login=$SONAR_TOKEN
            '''
        }
    }
}
```

---

## Best Practices

### 1. Regular Code Analysis
- Run analysis on every commit (local)
- Automated analysis on every PR (CI/CD)
- Schedule nightly builds for comprehensive analysis

### 2. Quality Gates
Set up quality gates to prevent code degradation:
- Minimum code coverage: 50% → 80%
- Maximum code duplication: 3%
- Zero critical security vulnerabilities
- Zero blocker issues

**To configure**:
1. Go to SonarQube dashboard
2. **Administration** → **Quality Gates**
3. Create "sLEAPy" quality gate with conditions

### 3. Security Best Practices
- **Never commit tokens** to Git
- Rotate tokens every 90 days
- Use environment variables for sensitive data
- Limit token scope to necessary permissions
- Review token usage in GitHub Actions logs

### 4. Code Review Process
- Require SonarQube quality gate checks before merging
- Review SonarQube issues as part of PR review
- Address security hotspots immediately
- Document exceptions to rules

### 5. Team Standards
- Configure consistent rule sets for all projects
- Use SonarQube profiles to enforce team standards
- Regular team reviews of dashboard metrics
- Celebrate improvements in code quality

### 6. Performance Optimization
- Use exclusion patterns to skip analysis of generated code
- Exclude node_modules, venv, target directories
- Schedule heavy analysis during off-peak hours
- Cache Maven and npm dependencies

### 7. Documentation
- Maintain this setup guide in the repository
- Document any custom quality gates or rules
- Keep team informed of SonarQube updates
- Record any troubleshooting solutions

---

## Troubleshooting

### Issue: SonarQube Container Won't Start

**Symptoms**: Docker container crashes or fails to start

**Solutions**:
```bash
# Check logs
docker-compose logs sonarqube

# Ensure sufficient system resources
# Increase Docker memory allocation to at least 4GB

# Common fix: Bootstrap checks failure
# Ensure this in docker-compose.yml:
SONAR_ES_BOOTSTRAP_CHECKS_DISABLED: "true"

# Restart containers
docker-compose down
docker-compose up -d
```

### Issue: Analysis Fails with "Connection Refused"

**Symptoms**: `java.net.ConnectException: Connection refused`

**Solutions**:
```bash
# Verify SonarQube is running
docker-compose ps

# Check if port 9000 is accessible
curl http://localhost:9000

# Wait for SonarQube to fully start (can take 30-60 seconds)
docker-compose logs sonarqube | grep "SonarQube is operational"

# Ensure Docker networking is correct
docker network ls
docker inspect sleapy-sonarqube
```

### Issue: Authentication Token Not Working

**Symptoms**: `401 Unauthorized` or `403 Forbidden` errors

**Solutions**:
```bash
# Verify token is set
echo $SONAR_TOKEN

# Check token hasn't expired in SonarQube UI
# Generate new token if needed

# Ensure token is passed correctly
mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN  # Correct
mvn sonar:sonar -Dsonar.login=<token>        # Wrong - don't hardcode
```

### Issue: Out of Memory Errors

**Symptoms**: `OutOfMemoryError` during analysis

**Solutions**:
```bash
# Increase memory in docker-compose.yml
environment:
  SONARQUBE_JAVA_OPTS: "-Xmx2g -Xms1g"

# For Maven analysis
export MAVEN_OPTS="-Xmx2g -Xms1g"
mvn sonar:sonar ...
```

### Issue: Database Connection Errors

**Symptoms**: PostgreSQL connection timeouts or failures

**Solutions**:
```bash
# Verify PostgreSQL is running
docker-compose ps postgres

# Check database and user exist
docker exec sleapy-postgres psql -U postgres -l

# Recreate database
docker-compose down -v
docker-compose up -d
```

### Issue: Coverage Reports Not Generated

**Symptoms**: 0% coverage reported by SonarQube

**Solutions**:
```bash
# Ensure JaCoCo plugin is in pom.xml
# Verify test execution
mvn test

# Check coverage report location
ls -la starter/backend/target/site/jacoco/jacoco.xml

# Ensure sonar.coverage.jacoco.xmlReportPaths is set in sonar-project.properties
```

---

## Team Instructions

### For New Team Members

1. **Initial Setup** (First Time Only)
   ```bash
   # Clone repository
   git clone <repo-url>
   cd sLEAPy-sprint1-project/starter
   
   # Start SonarQube
   docker-compose up -d
   
   # Wait 60 seconds for startup, then verify
   curl http://localhost:9000
   ```

2. **Access SonarQube**
   - Open browser: `http://localhost:9000`
   - Ask team lead for login credentials
   - Change password if using shared account

3. **Request Personal Token**
   - Follow Step 5.3 above
   - Store in environment variable (not in Git)
   - Never share token with teammates

4. **Run Your First Analysis**
   - Follow Step 6 instructions for your codebase
   - Review results in dashboard
   - Understand and fix any critical issues

### Daily Workflow

1. **Before Committing**
   ```bash
   # Run analysis locally
   cd starter/backend
   mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN
   
   # Review issues before pushing
   ```

2. **During Code Review**
   - Check SonarQube results in GitHub PR
   - Ensure quality gate passes
   - Discuss any new issues with author

3. **When Quality Gate Fails**
   - Identify issues in SonarQube dashboard
   - Create issues/improvements on local branch
   - Re-run analysis to confirm fixes
   - Request re-review

### Stopping and Starting SonarQube

```bash
# Stop all containers (preserves data)
docker-compose down

# Start containers again
docker-compose up -d

# Stop and remove volumes (full reset - loses all data!)
docker-compose down -v
```

### Getting Help

1. **Check logs**:
   ```bash
   docker-compose logs sonarqube
   docker-compose logs postgres
   ```

2. **Restart services**:
   ```bash
   docker-compose restart
   ```

3. **Ask team lead** if issues persist

### Common Commands Reference

```bash
# View SonarQube dashboard
open http://localhost:9000

# View container status
docker-compose ps

# View container logs
docker-compose logs -f sonarqube

# SSH into container (for debugging)
docker exec -it sleapy-sonarqube bash

# Stop all services
docker-compose down

# Start all services
docker-compose up -d

# Full reset (DESTRUCTIVE)
docker-compose down -v
docker-compose up -d
```

---

## Security Checklist

Before sharing this setup with the team:

- [ ] Do NOT commit `.env` files with secrets
- [ ] Do NOT hardcode tokens in configuration files
- [ ] Do NOT share SonarQube password via email
- [ ] Do NOT expose tokens in GitHub Actions logs
- [ ] Do update default PostgreSQL password in docker-compose.yml
- [ ] Do use GitHub Secrets for CI/CD tokens
- [ ] Do rotate tokens every 90 days
- [ ] Do audit token usage regularly
- [ ] Do document password storage method for team
- [ ] Do review access controls monthly

---

## Additional Resources

- [SonarQube Official Documentation](https://docs.sonarqube.org/)
- [SonarQube with Docker](https://docs.sonarqube.org/latest/setup-and-upgrade/install-the-server/)
- [Maven SonarQube Plugin](https://docs.sonarqube.org/latest/analyzing-source-code/scanners/sonarscanner-for-maven/)
- [SonarQube Scanner for JavaScript](https://docs.sonarqube.org/latest/analyzing-source-code/scanners/sonarscanner-for-npm/)
- [GitHub Actions Best Practices](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

---

## Support and Questions

For questions or issues with SonarQube setup:
1. Check the Troubleshooting section above
2. Review SonarQube official documentation
3. Ask the team lead
4. Create an issue in the repository with `[SonarQube]` prefix

---

**Last Updated**: October 2026  
**Maintained By**: DevOps/Quality Team  
**Version**: 1.0
