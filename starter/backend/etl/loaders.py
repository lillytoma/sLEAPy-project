"""Data loading module for persisting processed data to the analytics warehouse.

This module provides functions to load extracted and transformed data into
database tables and export to CSV files for reporting and archival purposes.
"""

from sqlalchemy import text
import pandas as pd

def load_to_warehouse(dataframes, engine, schema_name="sleapy_analytics"):
    """Load dataframes to a different schema in the same database.
    
    Creates tables in the analytics warehouse schema and populates them with
    data from the provided dataframes. Automatically handles schema creation
    and table structure based on dataframe column types.
    
    Args:
        dataframes (dict): Dictionary mapping table names to pandas DataFrames.
        engine: SQLAlchemy engine object for database connection.
        schema_name (str): Target schema name in the database.
                          Defaults to 'sleapy_analytics'.
    
    Raises:
        Exception: If there's an error creating schema, tables, or inserting data.
    """
    try:
        # Step 1: Create the analytics schema if it doesn't already exist
        with engine.begin() as connection:
            connection.execute(text(f"CREATE SCHEMA IF NOT EXISTS {schema_name}"))
        
        # Step 2: Load each dataframe to the schema using manual inserts
        for name, df in dataframes.items():
            # Convert table name to lowercase for consistency
            table_name = name.lower()
            
            # Step 3a: Create table with proper schema and columns
            with engine.begin() as connection:
                # Drop table if it already exists to ensure clean load
                connection.execute(text(f"DROP TABLE IF EXISTS {schema_name}.{table_name}"))
                
                # Step 3b: Map dataframe columns to SQL types
                columns = []
                for col in df.columns:
                    # Analyze column data type and values
                    dtype = df[col].dtype
                    col_max = df[col].max()
                    col_min = df[col].min()
                    
                    # Map Python/pandas types to PostgreSQL types
                    if 'int' in str(dtype):
                        # Check if value fits in INTEGER (32-bit) or needs BIGINT (64-bit)
                        if col_max > 2147483647 or col_min < -2147483648:
                            sql_type = "BIGINT"
                        else:
                            sql_type = "INTEGER"
                    elif 'float' in str(dtype):
                        sql_type = "DOUBLE PRECISION"
                    else:
                        # Default to TEXT for strings and other types
                        sql_type = "TEXT"
                    columns.append(f'"{col}" {sql_type}')
                
                # Step 3c: Create the table with mapped columns
                create_table_sql = f"CREATE TABLE {schema_name}.{table_name} ({', '.join(columns)})"
                connection.execute(text(create_table_sql))
                
                # Step 3d: Insert data row by row into the new table
                if not df.empty:
                    for idx, row in df.iterrows():
                        # Build column list with quoted identifiers
                        cols = ', '.join([f'"{c}"' for c in df.columns])
                        # Build values list, converting None to NULL
                        values_str = ', '.join([f"'{str(v)}'" if pd.notna(v) else 'NULL' for v in row.values])
                        # Execute INSERT statement
                        insert_sql = f"INSERT INTO {schema_name}.{table_name} ({cols}) VALUES ({values_str})"
                        connection.execute(text(insert_sql))
            
            # Log successful table load
            print(f"Loaded dataframe '{name}' to {schema_name}.{table_name}")
        
    except Exception as e:
        print(f"Error loading data to warehouse: {e}")
        raise

def save_to_csv(dataframes, output_dir="./"):
    """Save dataframes to CSV files.
    
    Exports each dataframe to a CSV file for data inspection, backup,
    and integration with external tools.
    
    Args:
        dataframes (dict): Dictionary mapping filenames to pandas DataFrames.
        output_dir (str): Directory where CSV files will be saved.
                         Defaults to current directory.
    
    Raises:
        Exception: If there's an error writing to the CSV files.
    """
    try:
        # Iterate through each dataframe and save as CSV
        for name, df in dataframes.items():
            # Create filename from dataframe name (lowercase)
            filename = f"{name.lower()}.csv"
            # Construct full filepath
            filepath = f"{output_dir}/{filename}"
            # Save dataframe to CSV without row indices
            df.to_csv(filepath, index=False)
            # Log successful save
            print(f"Saved dataframe '{name}' to {filepath}")
            
    except Exception as e:
        print(f"Error saving dataframes to CSV: {e}")
        raise