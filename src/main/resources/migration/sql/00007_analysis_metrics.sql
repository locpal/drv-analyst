-- liquibase formatted sql
-- changeset sewaiper:7

CREATE TABLE analysis_metrics (
    analysis_id TEXT NOT NULL REFERENCES analysis_runs(id),
    scope TEXT NOT NULL CHECK (scope IN ('H1', 'D1', 'COMMON')),
    format_version INTEGER NOT NULL,
    calculation_version TEXT NOT NULL,
    calculated_at_utc TEXT NOT NULL,
    metrics_json TEXT NOT NULL,
    PRIMARY KEY (analysis_id, scope)
);
