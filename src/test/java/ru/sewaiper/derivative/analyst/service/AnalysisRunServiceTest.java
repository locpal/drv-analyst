package ru.sewaiper.derivative.analyst.service;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BaseBarSeriesBuilder;
import ru.sewaiper.derivative.analyst.dao.AnalysisRunDao;
import ru.sewaiper.derivative.analyst.dao.AnalysisStepDao;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisPreparation;
import ru.sewaiper.derivative.analyst.domain.analysis.StartAnalysisRequest;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisRunServiceTest {
    private final AnalysisRunDao dao = mock(AnalysisRunDao.class);
    private final AnalysisCoordinator coordinator = mock(AnalysisCoordinator.class);
    private final List<Runnable> queued = new ArrayList<>();
    private final Executor executor = queued::add;
    private final AnalysisStepDao stepDao = mock(AnalysisStepDao.class);
    private final ForecastService forecastService = mock(ForecastService.class);
    private final AnalysisRunService service = new AnalysisRunService(dao, coordinator, executor,
            new AnalysisStepJournal(stepDao), stepDao, forecastService);
    private final StartAnalysisRequest request = new StartAnalysisRequest("uid-1", AssetType.FUTURE);

    @Test
    void returnsIdBeforePreparationAndPersistsItsResultInWorker() {
        var id = service.start(request);

        verify(dao).create(eq(id), argThat(locator -> locator.providerId().equals("uid-1")
                && locator.type() == AssetType.FUTURE), eq(Set.of()), any(Instant.class));
        verifyNoInteractions(coordinator);
        assertEquals(1, queued.size());

        var snapshotAt = Instant.parse("2026-10-16T12:00:00Z");
        var passport = new InstrumentPassport("TEST", "Test", AssetType.FUTURE,
                Provider.T_INVEST, "uid-1", "figi-1", "forts_futures");
        var snapshot = new InstrumentSnapshot("TEST", snapshotAt, passport,
                Map.of(Timeframe.H1, new TInvestCandleData(Timeframe.H1, List.of())), null, null, null);
        var horizon = new TradingHorizon(1, LocalDate.parse("2026-10-19"),
                Instant.parse("2026-10-19T15:00:00Z"));
        when(coordinator.prepare(eq(id), any(), any()))
                .thenAnswer(invocation -> {
                    var context = new MarketContext(snapshot, new BaseBarSeriesBuilder().build());
                    var nums = DecimalNumFactory.getInstance();
                    context.addTimeframeMetrics(Timeframe.H1, Map.of(MetricName.ATR,
                            new AtrMetrics(nums.numOf("39"), nums.numOf("0.27"),
                                    28, 200, snapshotAt)));
                    return new AnalysisPreparation(invocation.getArgument(1), invocation.getArgument(2),
                            context, List.of(horizon));
                });

        queued.getFirst().run();

        verify(dao).markPreparing(eq(id), any(Instant.class));
        verify(dao).markPrepared(eq(id), any(Instant.class), eq(snapshotAt),
                eq("forts_futures"), eq(List.of(horizon)),
                eq(Map.of()), argThat(metrics -> metrics.get(Timeframe.H1).containsKey(MetricName.ATR)), eq(true));
        verify(forecastService).forecast(eq(id), any(AnalysisPreparation.class));
        verify(dao, never()).markFailed(eq(id), any(), any());
    }

    @Test
    void recordsWorkerFailureWithoutThrowingOnRequestThread() {
        var id = service.start(request);
        when(coordinator.prepare(eq(id), any(), any()))
                .thenThrow(new IllegalStateException("schedule unavailable"));

        assertDoesNotThrow(() -> queued.getFirst().run());

        verify(dao).markFailed(eq(id), any(Instant.class), contains("schedule unavailable"));
    }

    @Test
    void skipsForecastAndPreservesRequestedOptions() {
        var skip = Set.of(SkippableStep.MARKET_TICK, SkippableStep.ORDER_BOOK, SkippableStep.FORECAST);
        var id = service.start(new StartAnalysisRequest("uid-1", AssetType.FUTURE, skip));
        verify(dao).create(eq(id), any(), eq(skip), any(Instant.class));
        when(coordinator.prepare(eq(id), any(), any())).thenAnswer(invocation -> {
            var depth = (ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth) invocation.getArgument(2);
            assertEquals(skip, depth.skip());
            var snapshot = new InstrumentSnapshot("TEST", Instant.parse("2026-10-16T12:00:00Z"),
                    new InstrumentPassport("TEST", "Test", AssetType.FUTURE, Provider.T_INVEST,
                            "uid-1", "figi-1", "forts_futures"),
                    Map.of(Timeframe.H1, new TInvestCandleData(Timeframe.H1, List.of())), null, null, null);
            var context = new MarketContext(snapshot, new BaseBarSeriesBuilder().build());
            var nums = DecimalNumFactory.getInstance();
            context.addTimeframeMetrics(Timeframe.H1, Map.of(MetricName.ATR,
                    new AtrMetrics(nums.numOf("39"), nums.numOf("0.27"),
                            28, 200, snapshot.snapshotTimeUtc())));
            return new AnalysisPreparation(invocation.getArgument(1), depth, context,
                    List.of(new TradingHorizon(1, LocalDate.parse("2026-10-19"),
                            Instant.parse("2026-10-19T15:00:00Z"))));
        });

        queued.getFirst().run();

        verify(dao).markPrepared(eq(id), any(Instant.class), any(Instant.class),
                eq("forts_futures"), any(), any(), any(), eq(false));
        verifyNoInteractions(forecastService);
    }
}
