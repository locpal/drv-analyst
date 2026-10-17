package ru.sewaiper.derivative.analyst.service;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BaseBarSeriesBuilder;
import ru.sewaiper.derivative.analyst.dao.MarketSnapshotDao;
import ru.sewaiper.derivative.analyst.dao.AnalysisStepDao;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;
import ru.sewaiper.derivative.analyst.service.analyzer.IndicatorAnalysisService;
import ru.sewaiper.derivative.analyst.service.market.MarketDataProvider;
import ru.sewaiper.derivative.analyst.service.schedule.TradingHorizonService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

class AnalysisCoordinatorTest {

    @Test
    void assemblesContextAndHorizonsFromTheSameSnapshot() {
        var indicators = mock(IndicatorAnalysisService.class);
        var provider = mock(MarketDataProvider.class);
        var horizons = mock(TradingHorizonService.class);
        var snapshots = mock(MarketSnapshotDao.class);
        var coordinator = new AnalysisCoordinator(provider, indicators, horizons, snapshots,
                new AnalysisStepJournal(mock(AnalysisStepDao.class)));
        var analysisId = UUID.randomUUID();
        var locator = new InstrumentLocator("TEST", AssetType.FUTURE, Provider.T_INVEST);
        var depth = new SnapshotDepth(10);
        Instant capturedAt = Instant.parse("2026-10-13T12:00:00Z");
        var passport = new InstrumentPassport("TEST", "Test future", AssetType.FUTURE,
                Provider.T_INVEST, "uid-test", "figi-test", "forts_futures_weekend");
        var snapshot = new InstrumentSnapshot("TEST", capturedAt,
                passport, Map.of(Timeframe.H1, new TInvestCandleData(Timeframe.H1, List.of())), null, null, null);
        var context = new MarketContext(snapshot, new BaseBarSeriesBuilder().build());
        var targets = List.of(new TradingHorizon(1, LocalDate.parse("2026-10-14"),
                Instant.parse("2026-10-14T15:54:59Z")));
        when(provider.getSnapshot(locator, depth)).thenReturn(snapshot);
        when(indicators.analyze(snapshot, Set.of())).thenReturn(context);
        when(horizons.resolve("forts_futures_weekend", capturedAt)).thenReturn(targets);

        var preparation = coordinator.prepare(analysisId, locator, depth);

        assertSame(locator, preparation.locator());
        assertSame(depth, preparation.depth());
        assertSame(context, preparation.marketContext());
        assertEquals(targets, preparation.horizons());
        verify(provider).getSnapshot(locator, depth);
        var order = inOrder(snapshots, indicators);
        order.verify(snapshots).save(analysisId, snapshot);
        order.verify(indicators).analyze(snapshot, Set.of());
        verify(indicators).analyze(snapshot, Set.of());
        verify(horizons).resolve("forts_futures_weekend", capturedAt);
    }

    @Test
    void doesNotCreatePreparationWhenScheduleFails() {
        var indicators = mock(IndicatorAnalysisService.class);
        var provider = mock(MarketDataProvider.class);
        var horizons = mock(TradingHorizonService.class);
        var snapshots = mock(MarketSnapshotDao.class);
        var locator = new InstrumentLocator("TEST", AssetType.FUTURE, Provider.T_INVEST);
        var depth = new SnapshotDepth(10);
        Instant capturedAt = Instant.parse("2026-10-13T12:00:00Z");
        var passport = new InstrumentPassport("TEST", "Test future", AssetType.FUTURE,
                Provider.T_INVEST, "uid-test", "figi-test", "forts_futures_weekend");
        var snapshot = new InstrumentSnapshot("TEST", capturedAt, passport,
                Map.of(Timeframe.H1, new TInvestCandleData(Timeframe.H1, List.of())), null, null, null);
        when(provider.getSnapshot(locator, depth)).thenReturn(snapshot);
        when(indicators.analyze(snapshot, Set.of())).thenReturn(new MarketContext(snapshot, new BaseBarSeriesBuilder().build()));
        when(horizons.resolve("forts_futures_weekend", capturedAt))
                .thenThrow(new IllegalStateException("schedule unavailable"));

        assertThrows(IllegalStateException.class,
                () -> new AnalysisCoordinator(provider, indicators, horizons, snapshots,
                        new AnalysisStepJournal(mock(AnalysisStepDao.class)))
                        .prepare(UUID.randomUUID(), locator, depth));
    }
}
