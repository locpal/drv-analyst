-- liquibase formatted sql
-- changeset sewaiper:5

CREATE TABLE analysis_runs (
    id TEXT PRIMARY KEY,
    provider TEXT NOT NULL,
    asset_type TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    methodology_version TEXT NOT NULL,
    skip_json TEXT NOT NULL DEFAULT '[]',
    status TEXT NOT NULL,
    created_at_utc TEXT NOT NULL,
    started_at_utc TEXT,
    finished_at_utc TEXT,
    snapshot_at_utc TEXT,
    exchange TEXT,
    error_message TEXT
);

CREATE TABLE analysis_run_horizons (
    analysis_id TEXT NOT NULL REFERENCES analysis_runs(id),
    trading_days INTEGER NOT NULL,
    trading_date TEXT NOT NULL,
    target_at_utc TEXT NOT NULL,
    PRIMARY KEY (analysis_id, trading_days)
);
