-- liquibase formatted sql
-- changeset sewaiper:4

CREATE TABLE instrument_passports_new (
    id INTEGER PRIMARY KEY,
    name TEXT,
    ticker TEXT NOT NULL,
    asset_type TEXT NOT NULL,
    provider TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    figi TEXT,
    exchange TEXT,
    UNIQUE(provider, provider_id)
);

INSERT INTO instrument_passports_new (id, name, ticker, asset_type, provider, provider_id, figi, exchange)
SELECT id, name, ticker, asset_type, provider, provider_id, figi, exchange
FROM instrument_passports;

DROP TABLE instrument_passports;

ALTER TABLE instrument_passports_new RENAME TO instrument_passports;

CREATE INDEX passport_provider_idx ON instrument_passports (provider, asset_type, ticker);
