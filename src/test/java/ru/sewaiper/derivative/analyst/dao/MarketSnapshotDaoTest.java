package ru.sewaiper.derivative.analyst.dao;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.TradingMechanics;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.*;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MarketSnapshotDaoTest {
    @TempDir Path tempDir;

    @Test
    void restoresTheSavedSnapshotAndRejectsOverwrite() throws Exception {
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("snapshots.sqlite"));
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setResourceLoader(new DefaultResourceLoader());
        liquibase.setChangeLog("classpath:migration/changelog.xml");
        liquibase.afterPropertiesSet();
        var jdbc = new JdbcTemplate(dataSource);
        var analysisId = UUID.randomUUID();
        new AnalysisRunDao(jdbc).create(analysisId,
                new InstrumentLocator("uid-1", AssetType.FUTURE, Provider.T_INVEST),
                Set.of(), Instant.parse("2026-10-16T10:00:00Z"));
        var price = new Quotation(null, "123", 456_789_123);
        var candle = new Candle(price, price, price, price, "5",
                Instant.parse("2026-10-16T10:00:00Z"), true, null, null, null);
        var snapshot = new InstrumentSnapshot("TEST", Instant.parse("2026-10-16T11:00:00Z"),
                new InstrumentPassport("TEST", "Test", AssetType.FUTURE, Provider.T_INVEST,
                        "uid-1", "figi-1", "forts_futures"),
                Map.of(Timeframe.H1, new TInvestCandleData(Timeframe.H1, List.of(candle)),
                        Timeframe.D1, new TInvestCandleData(Timeframe.D1, List.of())),
                new OrderBookSnapshot(Instant.parse("2026-10-16T11:00:00Z"),
                        List.of(new PriceLevel(new java.math.BigDecimal("1.5"), 2)), List.of()),
                new MarketTick(Instant.parse("2026-10-16T10:59:00Z"),
                        new java.math.BigDecimal("1.5"), new java.math.BigDecimal("1.4"), 10L,
                        Instant.parse("2026-10-15T20:00:00Z"), Instant.parse("2026-10-16T10:58:00Z")),
                new TradingMechanics(1, 0.01, "rub", true, true, false,
                        null, null, 1.0, 100.0, 100.0));
        var dao = new MarketSnapshotDao(jdbc);

        dao.save(analysisId, snapshot);

        assertEquals(snapshot, dao.find(analysisId).orElseThrow());
        assertThrows(Exception.class, () -> dao.save(analysisId, snapshot));

        var metricsOnlyId = UUID.randomUUID();
        new AnalysisRunDao(jdbc).create(metricsOnlyId,
                new InstrumentLocator("uid-1", AssetType.FUTURE, Provider.T_INVEST),
                Set.of(), Instant.parse("2026-10-16T10:00:00Z"));
        var withoutLiveInputs = new InstrumentSnapshot(snapshot.ticker(), snapshot.snapshotTimeUtc(),
                snapshot.passport(), snapshot.candleData(), null, null, snapshot.tradingMechanics());
        dao.save(metricsOnlyId, withoutLiveInputs);
        var restored = dao.find(metricsOnlyId).orElseThrow();
        assertNull(restored.orderBook());
        assertNull(restored.currentTick());
        assertEquals(snapshot.candleData(), restored.candleData());
    }
}
