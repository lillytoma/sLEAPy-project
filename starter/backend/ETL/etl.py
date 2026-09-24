from config import load_db_config
from database import create_engine_connection
from extractors import (
    extract_transactions_last_6_months,
    extract_transactions_by_client,
    extract_most_expensive_trades
)
from loaders import load_to_warehouse, save_to_csv

def main():
    try:
        CONFIG_FILE = "../../docker-compose.yml"

        print("LOADING ETL CONFIG")
        config = load_db_config(CONFIG_FILE)
        
        print("Creating database connection")
        engine = create_engine_connection(config)
        
        print("\nExtracting data from source database...")
        
        # Extract transactions from last 6 months
        transactions_6months = extract_transactions_last_6_months(engine)
        
        # Extract transactions for specific client (example: client_id = 1)
        transactions_client = extract_transactions_by_client(engine, client_id=1)
        
        # Extract most expensive trades
        expensive_trades = extract_most_expensive_trades(engine, limit=100)
        
        print("\nDataframes created successfully:")
        print(f"- Transactions (last 6 months): {len(transactions_6months)} rows")
        print(f"- Transactions (client 1): {len(transactions_client)} rows")
        print(f"- Most expensive trades: {len(expensive_trades)} rows")
        
        # Prepare dataframes dictionary
        dataframes = {
            "Transactions_6Months": transactions_6months,
            "Transactions_Client": transactions_client,
            "Expensive_Trades": expensive_trades
        }
        
        # Save to CSV for inspection
        save_to_csv(dataframes)
        
        # Load to warehouse ( i will uncomment when destination DB is ready)
        # destination_config = load_db_config("path/to/destination/docker-compose.yml")
        # load_to_warehouse(dataframes, destination_config)
        
        print("\nETL pipeline finished successfully.")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()