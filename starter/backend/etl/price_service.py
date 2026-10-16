"""Small HTTP service that wraps yfinance for live stock quotes.

The Spring Boot backend (YahooFinancePriceService) calls this service instead of
embedding yfinance directly, since yfinance is a Python library. Run it with:

    uvicorn price_service:app --port 8000

Endpoints:
    GET /quote/{symbol}        -> single quote, 404 if the symbol has no price
    GET /quotes?symbols=A,B,C  -> {"A": {...}, "B": {...}, ...} (symbols with no
                                   price are omitted rather than erroring the batch)
    GET /search?query=apple     -> matching Yahoo Finance symbols
    GET /history/{symbol}?period=6mo -> historical closing prices
"""

import logging
from datetime import datetime
from typing import Optional

import yfinance as yf
from fastapi import FastAPI, HTTPException

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="sLEAPy Price Service")


def _quote_for(symbol: str) -> Optional[dict]:
    """Fetch a single quote via yfinance's fast_info (cheap, no history download)."""
    info = yf.Ticker(symbol).fast_info
    current_price = info.get("lastPrice") if hasattr(info, "get") else info["lastPrice"]
    if current_price is None:
        return None

    return {
        "symbol": symbol,
        "currentPrice": current_price,
        "highPrice": info.get("dayHigh"),
        "lowPrice": info.get("dayLow"),
        "lastUpdated": datetime.now().isoformat(),
    }


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/quote/{symbol}")
def get_quote(symbol: str):
    symbol = symbol.strip().upper()
    try:
        quote = _quote_for(symbol)
    except Exception as e:
        logger.error(f"Failed to fetch quote for {symbol}: {e}")
        raise HTTPException(status_code=502, detail=f"Failed to fetch price for {symbol}") from e

    if quote is None:
        raise HTTPException(status_code=404, detail=f"No price available for {symbol}")
    return quote


@app.get("/quotes")
def get_quotes(symbols: str):
    tickers = list(dict.fromkeys(s.strip().upper() for s in symbols.split(",") if s.strip()))
    if not tickers:
        raise HTTPException(status_code=400, detail="symbols query param is required")

    result = {}
    for symbol in tickers:
        try:
            quote = _quote_for(symbol)
            if quote is not None:
                result[symbol] = quote
            else:
                logger.warning(f"No price available for {symbol}, skipping")
        except Exception as e:
            logger.error(f"Failed to fetch quote for {symbol}: {e}")
            continue

    return result


@app.get("/search")
def search(query: str):
    query = query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="query parameter is required")

    try:
        search = yf.Search(query, max_results=20, news_count=0)
        results = []
        for quote in search.quotes or []:
            symbol = quote.get("symbol")
            if not symbol:
                continue
            results.append({
                "symbol": symbol,
                "symbolName": quote.get("longname") or quote.get("shortname") or symbol,
                "exchange": quote.get("exchange"),
                "quoteType": quote.get("quoteType"),
                "currentPrice": quote.get("regularMarketPrice"),
            })
        return results
    except Exception as e:
        logger.error(f"Failed to search Yahoo Finance for {query}: {e}")
        raise HTTPException(status_code=502, detail="Yahoo Finance search failed") from e


@app.get("/history/{symbol}")
def get_history(symbol: str, period: str = "1mo"):
    allowed_periods = {"6mo", "1mo", "2wk", "1wk", "3d"}
    if period not in allowed_periods:
        raise HTTPException(status_code=400, detail="Unsupported history period")
    try:
        interval = "5m" if period == "3d" else "1d"
        history = yf.Ticker(symbol.strip().upper()).history(period=period, interval=interval, auto_adjust=False)
        if history.empty:
            raise HTTPException(status_code=404, detail=f"No history available for {symbol}")
        return [
            {"date": index.isoformat(), "close": float(row["Close"])}
            for index, row in history.iterrows()
            if row["Close"] is not None
        ]
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to fetch history for {symbol}: {e}")
        raise HTTPException(status_code=502, detail=f"Failed to fetch history for {symbol}") from e
