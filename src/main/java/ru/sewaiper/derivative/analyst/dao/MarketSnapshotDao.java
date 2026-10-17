package ru.sewaiper.derivative.analyst.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sewaiper.derivative.analyst.domain.market.TradingMechanics;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.MarketTick;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.OrderBookSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MarketSnapshotDao {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcTemplate jdbc;

    public MarketSnapshotDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** One immutable input snapshot for each run. Called before indicator calculation. */
    @Transactional
    public void save(UUID analysisId, InstrumentSnapshot snapshot) {
        if (snapshot.passport().provider() != Provider.T_INVEST) {
            throw new IllegalArgumentException("Unsupported snapshot provider: " + snapshot.passport().provider());
        }
        jdbc.update("""
                INSERT INTO market_snapshots (analysis_id, ticker, snapshot_at_utc, provider, provider_id,
                    asset_type, passport_json, order_book_json, market_tick_json,
                    trading_mechanics_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, analysisId.toString(), snapshot.ticker(), snapshot.snapshotTimeUtc().toString(),
                snapshot.passport().provider().name(), snapshot.passport().providerId(),
                snapshot.passport().assetType().name(), json(snapshot.passport()),
                json(snapshot.orderBook()), json(snapshot.currentTick()),
                json(snapshot.tradingMechanics()));
        snapshot.candleData().forEach((timeframe, data) -> jdbc.update("""
                INSERT INTO market_snapshot_candles (analysis_id, timeframe, candle_data_json)
                VALUES (?, ?, ?)
                """, analysisId.toString(), timeframe.name(), data.serialize()));
    }

    public Optional<InstrumentSnapshot> find(UUID analysisId) {
        List<InstrumentSnapshot> snapshots = jdbc.query("""
                SELECT * FROM market_snapshots WHERE analysis_id = ?
                """, (rs, row) -> {
                    if (Provider.valueOf(rs.getString("provider")) != Provider.T_INVEST) {
                        throw new IllegalStateException("Unsupported stored snapshot provider");
                    }
                    InstrumentPassport passport = read(rs.getString("passport_json"), InstrumentPassport.class);
                    if (!passport.providerId().equals(rs.getString("provider_id"))
                            || !passport.assetType().name().equals(rs.getString("asset_type"))) {
                        throw new IllegalStateException("Stored snapshot identity does not match its passport");
                    }
                    return new InstrumentSnapshot(rs.getString("ticker"),
                            Instant.parse(rs.getString("snapshot_at_utc")), passport,
                            readCandles(analysisId),
                            read(rs.getString("order_book_json"), OrderBookSnapshot.class),
                            read(rs.getString("market_tick_json"), MarketTick.class),
                            read(rs.getString("trading_mechanics_json"), TradingMechanics.class));
                }, analysisId.toString());
        return snapshots.stream().findFirst();
    }

    private Map<Timeframe, TInvestCandleData> readCandles(UUID analysisId) {
        Map<Timeframe, TInvestCandleData> result = new EnumMap<>(Timeframe.class);
        jdbc.query("SELECT timeframe, candle_data_json FROM market_snapshot_candles WHERE analysis_id = ?",
                rs -> {
                    Timeframe timeframe = Timeframe.valueOf(rs.getString("timeframe"));
                    TInvestCandleData data = TInvestCandleData.restore(rs.getString("candle_data_json"));
                    if (timeframe != data.timeframe()) {
                        throw new IllegalStateException("Stored candle timeframe does not match its row");
                    }
                    result.put(timeframe, data);
                }, analysisId.toString());
        return result;
    }

    private String json(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize market snapshot", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return JSON.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot restore market snapshot", e);
        }
    }
}
