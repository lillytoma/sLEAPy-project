import os
import yaml

def load_db_config(yml_path):
    """Load database configuration from YAML file"""
    if not os.path.exists(yml_path):
        raise FileNotFoundError(f"YAML config file not found: {yml_path}")
    try:
        with open(yml_path, "r") as f:
            config = yaml.safe_load(f)
        if "services" in config and "postgres" in config["services"]:
            env = config["services"]["postgres"].get("environment", {})
            return {
                "dbname": env.get("POSTGRES_DB"),
                "user": env.get("POSTGRES_USER"),
                "password": env.get("POSTGRES_PASSWORD"),
                "host": "localhost",
                "port": env.get("POSTGRES_PORT", 8101)
            }
        return config
    except Exception as e:
        print(f"Error loading config: {e}")
        raise