CREATE TABLE IF NOT EXISTS public.analyst_portfolio_snapshots (
    snapshot_at TIMESTAMP NOT NULL,
    client_id BIGINT NOT NULL,
    instrument_type VARCHAR(100) NOT NULL,
    cash_balance NUMERIC(24,4) NOT NULL,
    holdings_value NUMERIC(28,6) NOT NULL,
    missing_prices BIGINT NOT NULL,
    PRIMARY KEY (snapshot_at, client_id, instrument_type)
);
CREATE INDEX IF NOT EXISTS analyst_portfolio_snapshots_client_time
    ON public.analyst_portfolio_snapshots (client_id, snapshot_at);
