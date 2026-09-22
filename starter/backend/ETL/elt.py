import sys
import yaml
import os
from sqlalchemy import create_engine
import pandas as pd

def load_db_config(yml_path):
    """Load database configuration from YAML file"""
    if not os.path.exists(yml_path):
        raise FileNotFoundError(f"YAML config file not found: {yml_path}")
    try:
        with open(yml_path, "r") as f:
            config = yaml.safe_load(f)
        # required_keys = ["POSTGRES_USER", "POSTGRES_PASSWORD",]
        if "services" in config and "postgres" in config["services"]:
            env = config["services"]["postgres"].get("environment", {})
            return {
                "dbname": env.get("POSTGRES_DB"),
                "user": env.get("POSTGRES_USER"),
                "password": env.get("POSTGRES_PASSWORD"),
                "host": env.get("POSTGRES_HOST", "localhost"),
                "port": env.get("POSTGRES_PORT", 8101)
            }
        return config
    except Exception as e:
        print(f"Error loading config: {e}")
        raise

def extract_from_postgres(tables, db_config):
    """Extract data from PostgreSQL table into a pandas dataframe."""
    try:
        engine = create_engine(
            f"postgresql+pg8000://{db_config['user']}:{db_config['password']}@{db_config['host']}:{db_config['port']}/{db_config['dbname']}"
        )
        df = pd.read_sql_table(tables, engine)
        print(tables)
        print(df)
        if df.empty:
            raise ValueError(f"Table '{tables}' is empty.")
        return df
       
    except Exception as e:
        print(f"Error extracting data: {e}")
        raise

if __name__ == "__main__":
    try:
        CONFIG_FILE = "../../docker-compose.yml"

        print("LOADING ETL CONFIG")
        config = load_db_config(CONFIG_FILE)
        print(config)
        print("Extracting data from the DataBase")
        raw_data = extract_from_postgres("Clients",config)
        print(df)
        print("ETL pipeline finished successfully.")
    except Exception as e:
        print(f"Error: {e}")