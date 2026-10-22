-- Analytics schema bootstrap for analyst dashboard queries.
-- Safe to rerun: it recreates the expensive_trades snapshot from current Orders data.

CREATE SCHEMA IF NOT EXISTS sleapy_analytics;

CREATE TABLE IF NOT EXISTS sleapy_analytics.expensive_trades (
    order_id INTEGER PRIMARY KEY,
    client_id INTEGER NOT NULL,
    symbol TEXT NOT NULL,
    symbol_name TEXT NOT NULL,
    instrument_type TEXT NOT NULL,
    quantity NUMERIC(15,4) NOT NULL,
    purchase_price NUMERIC(15,2) NOT NULL,
    total_value NUMERIC(18,2) NOT NULL,
    time_purchased TIMESTAMP NOT NULL
);

TRUNCATE TABLE sleapy_analytics.expensive_trades;

INSERT INTO sleapy_analytics.expensive_trades (
    order_id,
    client_id,
    symbol,
    symbol_name,
    instrument_type,
    quantity,
    purchase_price,
    total_value,
    time_purchased
)
SELECT
    o.order_id,
    o.client_id,
    i.symbol,
    i.symbol_name,
    COALESCE(it.instrumenttype, 'Other') AS instrument_type,
    o.quantity,
    o.purchase_price,
    ROUND((o.quantity * o.purchase_price)::numeric, 2) AS total_value,
    o.time_purchased
FROM orders o
JOIN instruments i ON i.instrument_id = o.instrument_id
JOIN instrument_type it ON it.instrumenttype_id = i.instrumenttype_id
WHERE o.status = 2
ORDER BY (o.quantity * o.purchase_price) DESC
LIMIT 100;
