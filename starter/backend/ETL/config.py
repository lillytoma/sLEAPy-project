"""Configuration module for loading database settings from YAML files.

This module provides utilities to extract database connection parameters from
Docker Compose YAML configuration files, enabling dynamic configuration management.
"""

import os
import yaml

def load_db_config(yml_path):
    """Load database configuration from YAML file.
    
    Parses a Docker Compose YAML file to extract PostgreSQL database
    configuration including credentials, host, and port information.
    
    Args:
        yml_path (str): Path to the docker-compose.yml configuration file.
    
    Returns:
        dict: Dictionary containing database connection parameters:
              - dbname: Database name
              - user: Database user
              - password: Database password
              - host: Database host (defaults to localhost)
              - port: Database port (defaults to 8101)
    
    Raises:
        FileNotFoundError: If the YAML config file doesn't exist.
        Exception: If there's an error parsing the YAML file.
    """
    # Check if the configuration file exists
    if not os.path.exists(yml_path):
        raise FileNotFoundError(f"YAML config file not found: {yml_path}")
    
    try:
        # Load YAML file safely
        with open(yml_path, "r") as f:
            config = yaml.safe_load(f)
        
        # Extract PostgreSQL service configuration from Docker Compose format
        if "services" in config and "postgres" in config["services"]:
            # Get environment variables from postgres service definition
            env = config["services"]["postgres"].get("environment", {})
            # Build database configuration dictionary with extracted values
            return {
                "dbname": env.get("POSTGRES_DB"),
                "user": env.get("POSTGRES_USER"),
                "password": env.get("POSTGRES_PASSWORD"),
                "host": "localhost",
                "port": env.get("POSTGRES_PORT", 8101)
            }
        # Fallback: return entire config if postgres service not found
        return config
    except Exception as e:
        print(f"Error loading config: {e}")
        raise