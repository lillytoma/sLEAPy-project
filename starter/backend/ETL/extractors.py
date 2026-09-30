"""Data extraction module for querying specific datasets from the source database.

This module contains functions to extract various trading datasets using SQL queries,
returning results as pandas DataFrames for further processing and analysis.
"""

import pandas as pd
from sqlalchemy import text, create_engine

def extract_transactions_last_6_months(engine):
    """Extract all transactions from the last 6 months.
    
    Retrieves order data for the past 6 months with associated client information,
    instrument details, and order status, ordered by most recent first.
    
    Args:
        engine: SQLAlchemy engine object for database connection.
    
    Returns:
        pd.DataFrame: DataFrame with columns:
                     - order_ID, client_ID, username, symbol, symbol_Name,
                       quantity, purchase_price, time_purchased, time_filled, status
    
    Raises:
        ValueError: If no transactions are found in the last 6 months.
        Exception: If there's an error executing the query.
    """
    try:
        # SQL query to extract recent transactions with full details
        query = text("""
        SELECT 
            o.order_ID,
            c.client_ID,
            c.username,
            i.symbol,
            i.symbol_Name,
            o.quantity,
            o.purchase_price,
            o.time_purchased,
            o.time_filled,
            os.orderstatus_name as status
        FROM orders o
        JOIN clients c ON o.client_ID = c.client_ID
        JOIN instruments i ON o.instrument_id = i.instrument_id
        JOIN order_status os ON o.status = os.orderstatus_id
        -- Filter for transactions within the last 6 months
        WHERE o.time_purchased >= NOW() - INTERVAL '6 months'
        -- Order by most recent transactions first
        ORDER BY o.time_purchased DESC
        """)
        
        # Execute query and convert result to DataFrame
        with engine.connect() as connection:
            result = connection.execute(query)
            df = pd.DataFrame(result.fetchall(), columns=result.keys())
        
        # Validate that data was returned
        if df.empty:
            raise ValueError("No transactions found in the last 6 months")
        
        print(f"Extracted {len(df)} transactions from last 6 months")
        return df
        
    except Exception as e:
        print(f"Error extracting transactions from last 6 months: {e}")
        raise

def extract_transactions_by_client(engine, client_id):
    """Extract all transactions for a specific client.
    
    Retrieves all historical orders for a given client including instrument
    details and order status information.
    
    Args:
        engine: SQLAlchemy engine object for database connection.
        client_id (int): The client ID to filter transactions by.
    
    Returns:
        pd.DataFrame: DataFrame with columns:
                     - order_ID, client_ID, username, symbol, symbol_name,
                       quantity, purchase_price, time_purchased, time_filled, status
    
    Raises:
        ValueError: If no transactions are found for the specified client.
        Exception: If there's an error executing the query.
    """
    try:
        # SQL query to get all transactions for a specific client
        query = text("""
        SELECT 
            o.order_ID,
            c.client_ID,
            c.username,
            i.symbol,
            i.symbol_name,
            o.quantity,
            o.purchase_price,
            o.time_purchased,
            o.time_filled,
            os.orderstatus_name as status
        FROM orders o
        JOIN clients c ON o.client_ID = c.client_ID
        JOIN instruments i ON o.Instrument_ID = i.Instrument_ID
        JOIN order_Status os ON o.status = os.orderStatus_ID
        -- Filter for specific client
        WHERE c.client_ID = :client_id
        -- Order by most recent transactions first
        ORDER BY o.time_purchased DESC
        """)
        
        # Execute query with client_id parameter and convert to DataFrame
        with engine.connect() as connection:
            result = connection.execute(query, {"client_id": client_id})
            df = pd.DataFrame(result.fetchall(), columns=result.keys())
        
        # Validate that data was returned for the client
        if df.empty:
            raise ValueError(f"No transactions found for client {client_id}")
        
        print(f"Extracted {len(df)} transactions for client {client_id}")
        return df
        
    except Exception as e:
        print(f"Error extracting transactions for client {client_id}: {e}")
        raise

def extract_most_expensive_trades(engine, limit=100):
    """Extract the most expensive trades (by total transaction value).
    
    Retrieves the highest value trades based on quantity * purchase_price,
    useful for identifying significant trading activity and clients.
    
    Args:
        engine: SQLAlchemy engine object for database connection.
        limit (int): Maximum number of trades to return. Defaults to 100.
    
    Returns:
        pd.DataFrame: DataFrame with columns:
                     - order_ID, client_ID, username, symbol, symbol_Name,
                       quantity, purchase_price, Total_Value, time_purchased,
                       time_filled, status
    
    Raises:
        ValueError: If no trades are found in the database.
        Exception: If there's an error executing the query.
    """
    try:
        # SQL query to get the most expensive trades by total value
        query = text("""
        SELECT 
            o.order_ID,
            c.client_ID,
            c.username,
            i.symbol,
            i.symbol_Name,
            o.quantity,
            o.purchase_price,
            -- Calculate total transaction value (quantity * price)
            (o.quantity * o.purchase_price) as Total_Value,
            o.time_purchased,
            o.time_filled,
            os.orderstatus_name as status
        FROM orders o
        JOIN clients c ON o.client_ID = c.client_ID
        JOIN instruments i ON o.instrument_id = i.instrument_id
        JOIN order_status os ON o.status = os.orderstatus_id
        -- Sort by total value in descending order to get most expensive trades first
        ORDER BY (o.quantity * o.purchase_price) DESC
        -- Limit results to specified number of top trades
        LIMIT :limit
        """)
        
        # Execute query with limit parameter and convert to DataFrame
        with engine.connect() as connection:
            result = connection.execute(query, {"limit": limit})
            df = pd.DataFrame(result.fetchall(), columns=result.keys())
        
        # Validate that trades were found
        if df.empty:
            raise ValueError("No trades found")
        
        print(f"Extracted top {len(df)} most expensive trades")
        return df
        
    except Exception as e:
        print(f"Error extracting most expensive trades: {e}")
        raise