-- liquibase formatted sql
-- changeset sewaiper:8

CREATE TABLE analysis_steps (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    analysis_id TEXT NOT NULL REFERENCES analysis_runs(id),
    step_name TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('STARTED', 'COMPLETED', 'FAILED')),
    started_at_utc TEXT NOT NULL,
    finished_at_utc TEXT,
    input_json TEXT NOT NULL,
    output_json TEXT,
    error_message TEXT
);

CREATE INDEX idx_analysis_steps_analysis_id ON analysis_steps(analysis_id, id);
