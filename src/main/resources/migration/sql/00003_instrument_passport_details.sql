-- liquibase formatted sql
-- changeset sewaiper:3

ALTER TABLE instrument_passports ADD COLUMN figi TEXT;
ALTER TABLE instrument_passports ADD COLUMN exchange TEXT;
