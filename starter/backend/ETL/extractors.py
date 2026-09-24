import pandas as pd

def extract_transactions_last_6_months(engine):
    """Extract all transactions from the last 6 months"""
    try:
        query = """
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
        WHERE o.time_purchased >= NOW() - INTERVAL '6 months'
        ORDER BY o.time_purchased DESC
        """
        df = pd.read_sql_query(query, engine)
        
        if df.empty:
            raise ValueError("No transactions found in the last 6 months")
        
        print(f"Extracted {len(df)} transactions from last 6 months")
        return df
        
    except Exception as e:
        print(f"Error extracting transactions from last 6 months: {e}")
        raise

def extract_transactions_by_client(engine, client_id):
    """Extract all transactions for a specific client"""
    try:
        query = """
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
        WHERE c.client_ID = %s
        ORDER BY o.time_purchased DESC
        """
        df = pd.read_sql_query(query, engine, params=(client_id,))
        
        if df.empty:
            raise ValueError(f"No transactions found for client {client_id}")
        
        print(f"Extracted {len(df)} transactions for client {client_id}")
        return df
        
    except Exception as e:
        print(f"Error extracting transactions for client {client_id}: {e}")
        raise

def extract_most_expensive_trades(engine, limit=100):
    """Extract the most expensive trades (by total transaction value)"""
    try:
        query = """
        SELECT 
            o.order_ID,
            c.client_ID,
            c.username,
            i.symbol,
            i.symbol_Name,
            o.quantity,
            o.purchase_price,
            (o.quantity * o.purchase_price) as Total_Value,
            o.time_purchased,
            o.time_filled,
            os.orderstatus_name as status
        FROM orders o
        JOIN clients c ON o.client_ID = c.client_ID
        JOIN instruments i ON o.instrument_id = i.instrument_id
        JOIN order_status os ON o.status = os.orderstatus_id
        ORDER BY (o.quantity * o.purchase_price) DESC
        LIMIT %s
        """
        df = pd.read_sql_query(query, engine, params=(limit,))
        
        if df.empty:
            raise ValueError("No trades found")
        
        print(f"Extracted top {len(df)} most expensive trades")
        return df
        
    except Exception as e:
        print(f"Error extracting most expensive trades: {e}")
        raise