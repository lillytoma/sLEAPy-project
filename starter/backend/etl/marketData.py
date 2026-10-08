"""Market data extraction from Yahoo Finance.

This module fetches stock prices from Yahoo Finance and returns them
as pandas DataFrames. The data is meant to be cached in Redis, not stored
in the database.

Usage:
    prices_df = fetch_current_stock_prices(["AAPL", "GOOGL", "MSFT"])
    # Returns DataFrame with columns: Symbol, current_price, high_price, low_price
"""

import yfinance as yf
import pandas as pd
from datetime import datetime
import logging

# Configure logging to show what's happening
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def fetch_current_stock_prices(tickers):
    """
    Fetch current market prices for a list of stock tickers from Yahoo Finance.
    
    This function does the following:
    1. Takes a list of stock symbols (e.g., ["AAPL", "GOOGL"])
    2. For each symbol, downloads today's price data from Yahoo Finance
    3. Extracts: current price, high price, low price
    4. Returns a pandas DataFrame
    
    Args:
        tickers (list): List of stock symbols, e.g., ["AAPL", "GOOGL", "MSFT"]
    
    Returns:
        pd.DataFrame: Table with columns:
            - Symbol: The stock ticker
            - current_price: Current closing price
            - high_price: Today's high price
            - low_price: Today's low price
            - last_updated: Timestamp when fetched
    
    Example:
        >>> prices = fetch_current_stock_prices(["AAPL", "GOOGL"])
        >>> print(prices)
           Symbol  current_price  high_price  low_price          last_updated
        0   AAPL           150.25      151.50      149.80 2026-10-06 10:30:00
        1  GOOGL          140.00      142.10      139.50 2026-10-06 10:30:00
    """
    prices_data = []
    
    # Loop through each stock ticker
    for ticker in tickers:
        try:
            logger.info(f"Fetching price for {ticker}...")
            
            # Download 1 day of price data from Yahoo Finance
            # This returns data with: Open, High, Low, Close, Volume
            data = yf.download(ticker, period="1d", progress=False)
            
            # Extract the specific prices we need
            current_price = float(data['Close'].iloc[-1])  # Last closing price
            high_price = float(data['High'].iloc[-1])      # Today's high
            low_price = float(data['Low'].iloc[-1])        # Today's low
            
            # Create a dictionary for this stock
            prices_data.append({
                'Symbol': ticker,
                'current_price': current_price,
                'high_price': high_price,
                'low_price': low_price,
                'last_updated': datetime.now()
            })
            
            logger.info(f"✓ {ticker}: ${current_price}")
            
        except Exception as e:
            # If something fails (network error, invalid ticker), log it and continue
            logger.error(f"✗ Error fetching {ticker}: {e}")
            continue
    
    # Convert list of dictionaries to a pandas DataFrame (table format)
    df = pd.DataFrame(prices_data)
    logger.info(f"\nSuccessfully fetched {len(df)} stock prices")
    return df


def fetch_historical_data(ticker, period="1mo"):
    """
    Fetch historical price data for a single stock.
    
    Useful for: price charts, trend analysis, moving averages, etc.
    
    Args:
        ticker (str): Stock symbol
        period (str): "1d", "5d", "1mo", "3mo", "6mo", "1y", "2y", "5y", "10y", "ytd", "max"
    
    Returns:
        pd.DataFrame: Historical price data with columns: Date, Open, High, Low, Close, Volume
    
    Example:
        >>> history = fetch_historical_data("AAPL", period="3mo")
        >>> print(history.head())  # Show first 5 rows
    """
    try:
        logger.info(f"Fetching {period} historical data for {ticker}...")
        data = yf.download(ticker, period=period, progress=False)
        # Reset index to convert Date from index to a regular column
        data = data.reset_index()
        logger.info(f"✓ Got {len(data)} days of historical data")
        return data
    except Exception as e:
        logger.error(f"Error fetching historical data for {ticker}: {e}")
        return pd.DataFrame()


def get_single_stock_price(ticker):
    """
    Get the current price for a single stock (simple version).
    
    Usage when you just need one quick price lookup.
    
    Args:
        ticker (str): Stock symbol (e.g., "AAPL")
    
    Returns:
        float: Current price, or None if error
    
    Example:
        >>> price = get_single_stock_price("AAPL")
        >>> print(f"AAPL is ${price}")
    """
    try:
        data = yf.download(ticker, period="1d", progress=False)
        return float(data['Close'].iloc[-1])
    except Exception as e:
        logger.error(f"Error fetching price for {ticker}: {e}")
        return None
