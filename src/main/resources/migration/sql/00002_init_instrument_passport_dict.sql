-- liquibase formatted sql
-- changeset sewaiper:2

CREATE TABLE IF NOT EXISTS instrument_passports (
    id INTEGER PRIMARY KEY ,
    name TEXT ,
    ticker TEXT NOT NULL ,
    asset_type TEXT NOT NULL ,
    provider TEXT NOT NULL ,
    provider_id TEXT NOT NULL ,

    UNIQUE(provider, asset_type, ticker)
);

CREATE INDEX IF NOT EXISTS passport_provider_idx on instrument_passports (provider, asset_type, ticker);
