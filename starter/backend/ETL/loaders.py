from sqlalchemy import text

def load_to_warehouse(dataframes, engine, schema_name="sleapy_analytics"):
    """Load dataframes to a different schema in the same database"""
    try:
        # Create schema if it doesn't exist
        with engine.connect() as connection:
            connection.execute(text(f"CREATE SCHEMA IF NOT EXISTS {schema_name}"))
            connection.commit()
        
        # Load each dataframe to the schema
        for name, df in dataframes.items():
            table_name = name.lower()
            df.to_sql(
                table_name, 
                engine, 
                schema=schema_name,
                if_exists='replace', 
                index=False
            )
            print(f"Loaded dataframe '{name}' to {schema_name}.{table_name}")
        
    except Exception as e:
        print(f"Error loading data to warehouse: {e}")
        raise

def save_to_csv(dataframes, output_dir="./"):
    """Save dataframes to CSV files"""
    try:
        for name, df in dataframes.items():
            filename = f"{name.lower()}.csv"
            filepath = f"{output_dir}/{filename}"
            df.to_csv(filepath, index=False)
            print(f"Saved dataframe '{name}' to {filepath}")
            
    except Exception as e:
        print(f"Error saving dataframes to CSV: {e}")
        raise