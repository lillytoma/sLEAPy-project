# In ETL/extractors.py or new market_data.py
import yfinance as yf

def get_stock_price(ticker):
    data = yf.download(ticker, period="1d")
    return data['Close'].iloc[-1]