-- ============================================================================
-- ANALYST SCHEMA FOR SLEAPY TRADING PLATFORM
-- ============================================================================
-- Purpose: Store aggregated and processed data for analysts
-- Source: Python ETL pipeline reads from operational sleapy schema
-- Populated: Nightly via scheduled ETL jobs
--
-- Tables:
-- - daily_trades_summary: Market-wide trading volume aggregates
-- - client_portfolio_metrics: Per-client daily portfolio snapshots
-- - instrument_performance: Per-instrument daily metrics
-- - client_risk_metrics: Risk analysis per client
-- - analyst_queries_log: Audit trail of analyst activities
-- ============================================================================

-- Create analytics schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS sleapy_analytics;

-- TABLE 1: Daily trades summary (market-wide aggregates)
-- Used by: Market analysts, trading reports, performance dashboards
-- Updated by: ETL pipeline (nightly aggregation of orders table)
CREATE TABLE sleapy_analytics.daily_trades_summary (
    summary_id SERIAL PRIMARY KEY,
    trade_date DATE NOT NULL UNIQUE,
    total_trades INT DEFAULT 0 CHECK (total_trades >= 0),
    total_buy_volume DECIMAL(15,2) DEFAULT 0.00,
    total_sell_volume DECIMAL(15,2) DEFAULT 0.00,
    total_buy_value DECIMAL(15,2) DEFAULT 0.00,
    total_sell_value DECIMAL(15,2) DEFAULT 0.00,
    avg_trade_value DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- TABLE 2: Client portfolio metrics (daily snapshots)
-- Used by: Client performance reports, portfolio analysis, advisor dashboard
-- Updated by: ETL pipeline (daily snapshot from holdings + orders)
CREATE TABLE sleapy_analytics.client_portfolio_metrics (
    metric_id SERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES public.clients(client_id) ON DELETE CASCADE,
    portfolio_date DATE NOT NULL,
    total_holdings_value DECIMAL(15,2),
    cash_balance DECIMAL(15,2),
    total_portfolio_value DECIMAL(15,2),
    daily_return DECIMAL(15,4),
    daily_return_pct DECIMAL(10,4),
    number_of_positions INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id, portfolio_date)
);

-- TABLE 3: Instrument performance tracking (daily metrics)
-- Used by: Price analysis, market trends, instrument reports
-- Updated by: ETL pipeline (daily aggregation of instrument trades)
CREATE TABLE sleapy_analytics.instrument_performance (
    perf_id SERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL REFERENCES public.instruments(instrument_id) ON DELETE CASCADE,
    performance_date DATE NOT NULL,
    daily_return DECIMAL(10,4),
    daily_return_pct DECIMAL(10,4),
    volume_traded INT DEFAULT 0,
    num_trades INT DEFAULT 0,
    avg_price DECIMAL(10,2),
    high_price DECIMAL(10,2),
    low_price DECIMAL(10,2),
    open_price DECIMAL(10,2),
    close_price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(instrument_id, performance_date)
);

-- TABLE 4: Client risk metrics (risk analysis)
-- Used by: Risk assessment, compliance reporting, portfolio recommendations
-- Updated by: ETL pipeline (daily risk calculation)
CREATE TABLE sleapy_analytics.client_risk_metrics (
    risk_id SERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES public.clients(client_id) ON DELETE CASCADE,
    metric_date DATE NOT NULL,
    portfolio_volatility DECIMAL(10,4),
    max_concentration DECIMAL(10,4),
    diversification_score DECIMAL(10,4),
    value_at_risk DECIMAL(15,2),
    sharpe_ratio DECIMAL(10,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id, metric_date)
);

-- TABLE 5: Analyst queries log (audit trail)
-- Used by: Usage analytics, audit compliance, performance monitoring
-- Populated by: Application logging on analyst queries
CREATE TABLE sleapy_analytics.analyst_queries_log (
    log_id SERIAL PRIMARY KEY,
    analyst_user_id BIGINT,
    query_type VARCHAR(50),
    query_description TEXT,
    filters_applied JSONB,
    result_count INT,
    execution_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- Daily trades indexes
CREATE INDEX idx_daily_trades_date ON sleapy_analytics.daily_trades_summary(trade_date DESC);

-- Portfolio metrics indexes
CREATE INDEX idx_portfolio_metrics_client_date ON sleapy_analytics.client_portfolio_metrics(client_id, portfolio_date DESC);
CREATE INDEX idx_portfolio_metrics_date ON sleapy_analytics.client_portfolio_metrics(portfolio_date DESC);

-- Instrument performance indexes
CREATE INDEX idx_instrument_perf_date ON sleapy_analytics.instrument_performance(performance_date DESC);
CREATE INDEX idx_instrument_perf_instrument_date ON sleapy_analytics.instrument_performance(instrument_id, performance_date DESC);

-- Risk metrics indexes
CREATE INDEX idx_risk_metrics_client_date ON sleapy_analytics.client_risk_metrics(client_id, metric_date DESC);

-- Queries log indexes
CREATE INDEX idx_queries_log_analyst ON sleapy_analytics.analyst_queries_log(analyst_user_id, created_at DESC);

-- ============================================================================
-- ANALYST USER (Read-Only Access)
-- ============================================================================
-- Note: Uncomment and set secure password before deploying

-- CREATE USER analyst_user WITH PASSWORD 'CHANGE_ME_SECURE_PASSWORD';
-- GRANT CONNECT ON DATABASE sleapy TO analyst_user;
-- GRANT USAGE ON SCHEMA sleapy_analytics TO analyst_user;
-- GRANT SELECT ON ALL TABLES IN SCHEMA sleapy_analytics TO analyst_user;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA sleapy_analytics GRANT SELECT ON TABLES TO analyst_user;