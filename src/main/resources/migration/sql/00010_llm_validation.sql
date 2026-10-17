-- liquibase formatted sql
-- changeset sewaiper:10

ALTER TABLE llm_exchanges ADD COLUMN validation_json TEXT;
