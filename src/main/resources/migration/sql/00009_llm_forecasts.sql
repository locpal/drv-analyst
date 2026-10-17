-- liquibase formatted sql
-- changeset sewaiper:9

CREATE TABLE llm_exchanges (
    analysis_id TEXT PRIMARY KEY REFERENCES analysis_runs(id),
    requested_at_utc TEXT NOT NULL,
    request_json TEXT NOT NULL,
    responded_at_utc TEXT,
    response_json TEXT,
    model TEXT,
    finish_reason TEXT,
    usage_json TEXT,
    content TEXT,
    validation_error TEXT
);

CREATE TABLE market_hypotheses (
    analysis_id TEXT NOT NULL REFERENCES analysis_runs(id),
    trading_days INTEGER NOT NULL,
    type TEXT NOT NULL,
    lower_bound TEXT NOT NULL,
    upper_bound TEXT NOT NULL,
    reasoning TEXT NOT NULL,
    alternative_scenario TEXT NOT NULL,
    uncertainty_factors_json TEXT NOT NULL,
    PRIMARY KEY (analysis_id, trading_days)
);
