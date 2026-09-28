import sys
sys.path.insert(0, '/home/ec2-user/sLEAPy-project/starter/backend/ETL')

from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import PythonOperator

from config import load_db_config
from database import create_engine_connection
from extractors import (
    extract_transactions_last_6_months,
    extract_transactions_by_client,
    extract_most_expensive_trades
)
from loaders import load_to_warehouse, save_to_csv

# Default arguments for the DAG
default_args = {
    'owner': 'sleapy',
    'retries': 2,
    'retry_delay': timedelta(minutes=5),
    'start_date': datetime.now() - timedelta(days=1),
}

# Define the DAG
dag = DAG(
    'sleapy_etl_pipeline',
    default_args=default_args,
    description='ETL pipeline for sLEAPy trading data',
    schedule='0 10 * * *',  # Run daily at 10 AM
    catchup=False,
)

# Task functions
def extract_data(**context):
    """Extract all data from source database"""
    try:
        CONFIG_FILE = "../../docker-compose.yml"
        
        print("LOADING ETL CONFIG")
        config = load_db_config(CONFIG_FILE)
        
        print("Creating database connection")
        engine = create_engine_connection(config)
        
        print("\nExtracting data from source database...")
        
        transactions_6months = extract_transactions_last_6_months(engine)
        transactions_client = extract_transactions_by_client(engine, client_id=1)
        expensive_trades = extract_most_expensive_trades(engine, limit=100)
        
        # Push data to XCom for next task
        context['task_instance'].xcom_push(
            key='transactions_6months',
            value=transactions_6months.to_json()
        )
        context['task_instance'].xcom_push(
            key='transactions_client',
            value=transactions_client.to_json()
        )
        context['task_instance'].xcom_push(
            key='expensive_trades',
            value=expensive_trades.to_json()
        )
        
        print("\nData extraction completed successfully")
        
    except Exception as e:
        print(f"Error in extract_data: {e}")
        raise

def load_data(**context):
    """Load extracted data to warehouse schema"""
    try:
        import pandas as pd
        
        CONFIG_FILE = "../../docker-compose.yml"
        config = load_db_config(CONFIG_FILE)
        engine = create_engine_connection(config)
        
        # Pull data from XCom
        ti = context['task_instance']
        transactions_6months = pd.read_json(ti.xcom_pull(key='transactions_6months'))
        transactions_client = pd.read_json(ti.xcom_pull(key='transactions_client'))
        expensive_trades = pd.read_json(ti.xcom_pull(key='expensive_trades'))
        
        dataframes = {
            "Transactions_6Months": transactions_6months,
            "Transactions_Client": transactions_client,
            "Expensive_Trades": expensive_trades
        }
        
        print("\nLoading data to warehouse schema...")
        load_to_warehouse(dataframes, engine, schema_name="sleapy_analytics")
        
        print("\nData loading completed successfully")
        
    except Exception as e:
        print(f"Error in load_data: {e}")
        raise

def save_data(**context):
    """Save data to CSV backup"""
    try:
        import pandas as pd
        
        ti = context['task_instance']
        transactions_6months = pd.read_json(ti.xcom_pull(key='transactions_6months'))
        transactions_client = pd.read_json(ti.xcom_pull(key='transactions_client'))
        expensive_trades = pd.read_json(ti.xcom_pull(key='expensive_trades'))
        
        dataframes = {
            "Transactions_6Months": transactions_6months,
            "Transactions_Client": transactions_client,
            "Expensive_Trades": expensive_trades
        }
        
        print("\nSaving data to CSV...")
        save_to_csv(dataframes, output_dir="/home/ec2-user/sLEAPy-project/data/backups")
        
        print("\nCSV backup completed successfully")
        
    except Exception as e:
        print(f"Error in save_data: {e}")
        raise

# Define tasks
extract_task = PythonOperator(
    task_id='extract_data',
    python_callable=extract_data,
    dag=dag,
)

load_task = PythonOperator(
    task_id='load_data',
    python_callable=load_data,
    dag=dag,
)

save_task = PythonOperator(
    task_id='save_data',
    python_callable=save_data,
    dag=dag,
)

# Set task dependencies
extract_task >> [load_task, save_task]