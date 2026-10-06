-- SonarQube Database Initialization Script
-- This script is automatically executed when PostgreSQL container starts
-- It creates the SonarQube user and database

-- Create the SonarQube database user
-- Password is substituted from environment variable during initialization
CREATE USER sonar_user WITH PASSWORD 'sleapy_for_sonar';

-- Create the SonarQube database
CREATE DATABASE sonar OWNER sonar_user;

-- Grant necessary permissions to sonar_user
GRANT CONNECT ON DATABASE sonar TO sonar_user;
GRANT CREATE ON DATABASE sonar TO sonar_user;

-- Connect to the sonar database and grant schema permissions
\c sonar

-- Grant all privileges on public schema
GRANT ALL PRIVILEGES ON SCHEMA public TO sonar_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sonar_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO sonar_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO sonar_user;
