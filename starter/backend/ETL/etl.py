"""Main ETL (Extract, Transform, Load) pipeline for the sLEAPy trading analytics.

This module orchestrates the extraction of trading data from the source database,
processing it, and loading it into the analytics warehouse schema for reporting
and analysis.
"""

from config import load_db_config
from database import create_engine_connection
from extractors import (
    extract_transactions_last_6_months,
    extract_transactions_by_client,
    extract_most_expensive_trades
)
from loaders import load_to_warehouse, save_to_csv

def main():
    """Execute the complete ETL pipeline.
    
    This function:
    1. Loads database configuration from Docker Compose file
    2. Establishes database connection
    3. Extracts multiple datasets using different queries
    4. Saves data to CSV for inspection
    5. Loads processed data into analytics warehouse schema
    """
    try:
        # Path to Docker Compose configuration file
        CONFIG_FILE = "../../docker-compose.yml"

        # Step 1: Load database configuration from YAML
        print("LOADING ETL CONFIG")
        config = load_db_config(CONFIG_FILE)
        
        # Step 2: Create database connection with connection pooling
        print("Creating database connection")
        engine = create_engine_connection(config)
        
        # Step 3: Extract data from source database using different queries
        print("\nExtracting data from source database...")
        
        # Extract all transactions from the last 6 months
        transactions_6months = extract_transactions_last_6_months(engine)
        
        # Extract transactions for specific client (example: client_id = 1)
        transactions_client = extract_transactions_by_client(engine, client_id=1)
        
        # Extract the 100 most expensive trades by total transaction value
        expensive_trades = extract_most_expensive_trades(engine, limit=100)
        
        # Display extraction summary
        print("\nDataframes created successfully:")
        print(f"- Transactions (last 6 months): {len(transactions_6months)} rows")
        print(f"- Transactions (client 1): {len(transactions_client)} rows")
        print(f"- Most expensive trades: {len(expensive_trades)} rows")
        
        # Step 4: Organize extracted data into a dictionary for processing
        dataframes = {
            "Transactions_6Months": transactions_6months,
            "Transactions_Client": transactions_client,
            "Expensive_Trades": expensive_trades
        }
        
        # Step 5: Save extracted data to CSV files for manual inspection and backup
        save_to_csv(dataframes)
        
        # Step 6: Load processed data into analytics warehouse schema for reporting
        print("\nLoading data to warehouse schema...")
        load_to_warehouse(dataframes, engine, schema_name="sleapy_analytics")
        
        print("\nETL pipeline finished successfully.")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()