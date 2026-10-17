-- liquibase formatted sql
-- changeset sewaiper:1

-- Таблица для исторических свечей
CREATE TABLE IF NOT EXISTS candles (
    ticker        TEXT    NOT NULL,
    timestamp     INTEGER NOT NULL,
    open          REAL    NOT NULL,
    high          REAL    NOT NULL,
    low           REAL    NOT NULL,
    close         REAL    NOT NULL,
    volume        REAL    NOT NULL,
    open_interest REAL,
    PRIMARY KEY (ticker, timestamp)
);

-- Создаем индекс для быстрого поиска срезов истории при бэктесте
CREATE INDEX IF NOT EXISTS idx_candles_ts ON candles (ticker, timestamp DESC);
