def load_to_warehouse(dataframes, destination_config):
    """Load dataframes to the destination warehouse database"""
    from database import create_engine_connection
    
    try:
        engine = create_engine_connection(destination_config)
        
        for name, df in dataframes.items():
            table_name = name.lower()
            df.to_sql(table_name, engine, if_exists='replace', index=False)
            print(f"Loaded dataframe '{name}' to table '{table_name}'")
        
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