#!/usr/bin/env python3

import redis
import yfinance as yf
import logging
import time
import signal
import sys

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

# Redis configuration
redis_client = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

# Symbols to track
SYMBOLS = ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'NVDA', 'TSLA']

# Price expiry (24 hours in seconds)
PRICE_EXPIRY = 86400

def update_prices():
    """Fetch prices from yfinance and update Redis cache"""
    try:
        # Download last day of data for all symbols
        data = yf.download(SYMBOLS, period='1d', progress=False, threads=False)
        
        # Extract close price for each symbol
        for symbol in SYMBOLS:
            try:
                # Handle single vs multiple symbols
                if len(SYMBOLS) == 1:
                    close_price = data['Close'].iloc[-1]
                else:
                    close_price = data['Close'][symbol].iloc[-1]
                
                # Store in Redis with 24-hour expiry
                redis_client.setex(
                    f'price:{symbol}',
                    PRICE_EXPIRY,
                    str(close_price)
                )
                logging.info(f'Updated {symbol}: ${close_price:.2f}')
            except Exception as e:
                logging.error(f'Error updating {symbol}: {e}')
        
        return True
    except Exception as e:
        logging.error(f'Failed to fetch prices: {e}')
        return False

def signal_handler(sig, frame):
    """Handle graceful shutdown"""
    logging.info('Shutting down price streamer...')
    sys.exit(0)

def main():
    """Main loop for price streaming"""
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    logging.info('Starting price streamer daemon...')
    logging.info(f'Tracking symbols: {", ".join(SYMBOLS)}')
    logging.info('Polling every 5 seconds...')
    
    # Initial price fetch
    update_prices()
    
    # Poll prices every 5 seconds
    while True:
        try:
            time.sleep(5)
            update_prices()
        except KeyboardInterrupt:
            logging.info('Interrupted, shutting down...')
            break
        except Exception as e:
            logging.error(f'Error in main loop: {e}')
            time.sleep(5)

if __name__ == '__main__':
    main()
