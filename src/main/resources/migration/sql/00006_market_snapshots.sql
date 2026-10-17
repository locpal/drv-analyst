-- liquibase formatted sql
-- changeset sewaiper:6

CREATE TABLE market_snapshots (
    analysis_id TEXT PRIMARY KEY REFERENCES analysis_runs(id),
    ticker TEXT NOT NULL,
    snapshot_at_utc TEXT NOT NULL,
    provider TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    asset_type TEXT NOT NULL,
    passport_json TEXT NOT NULL,
    order_book_json TEXT NOT NULL,
    market_tick_json TEXT NOT NULL,
    trading_mechanics_json TEXT NOT NULL
);

-- The quote and order book belong to one snapshot; candle histories are independent per timeframe.
-- An empty candle list is valid when the provider has no closed candles for a timeframe.
CREATE TABLE market_snapshot_candles (
    analysis_id TEXT NOT NULL REFERENCES market_snapshots(analysis_id),
    timeframe TEXT NOT NULL CHECK (timeframe IN ('H1', 'D1')),
    candle_data_json TEXT NOT NULL,
    PRIMARY KEY (analysis_id, timeframe)
);
