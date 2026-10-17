package ru.sewaiper.derivative.analyst.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.dao.AnalysisRunDao;
import ru.sewaiper.derivative.analyst.dao.AnalysisStepDao;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisPreparation;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisRun;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStep;
import ru.sewaiper.derivative.analyst.domain.analysis.LlmExchange;
import ru.sewaiper.derivative.analyst.domain.analysis.StartAnalysisRequest;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.Executor;

@Service
public class AnalysisRunService {
    private static final Logger log = LoggerFactory.getLogger(AnalysisRunService.class);

    private final AnalysisRunDao dao;
    private final AnalysisCoordinator coordinator;
    private final Executor executor;
    private final AnalysisStepJournal journal;
    private final AnalysisStepDao stepDao;
    private final ForecastService forecastService;

    public AnalysisRunService(AnalysisRunDao dao, AnalysisCoordinator coordinator,
                              @Qualifier("analysisExecutor") Executor executor,
                              AnalysisStepJournal journal, AnalysisStepDao stepDao,
                              ForecastService forecastService) {
        this.dao = dao;
        this.coordinator = coordinator;
        this.executor = executor;
        this.journal = journal;
        this.stepDao = stepDao;
        this.forecastService = forecastService;
    }

    public UUID start(StartAnalysisRequest request) {
        InstrumentLocator locator = new InstrumentLocator(request.providerId(), request.assetType(), Provider.T_INVEST);
        UUID id = UUID.randomUUID();
        dao.create(id, locator, request.skip(), Instant.now());
        log.info("Analysis {} created for {} / {}", id, locator.type(), locator.providerId());
        try {
            executor.execute(() -> prepare(id, locator, new SnapshotDepth(10, request.skip())));
        } catch (RuntimeException e) {
            dao.markFailed(id, Instant.now(), "Worker unavailable: " + e.getClass().getSimpleName());
            log.error("Analysis {} could not be scheduled", id, e);
        }
        return id;
    }

    public Optional<AnalysisRun> find(UUID id) {
        return dao.find(id);
    }

    public List<AnalysisStep> steps(UUID id) {
        return stepDao.findByAnalysisId(id);
    }

    public Optional<LlmExchange> forecast(UUID id) {
        return forecastService.find(id);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedRuns() {
        Instant now = Instant.now();
        stepDao.failInterruptedSteps(now);
        dao.failInterruptedRuns(now);
    }

    private void prepare(UUID id, InstrumentLocator locator, SnapshotDepth depth) {
        try {
            dao.markPreparing(id, Instant.now());
            log.info("Analysis {} preparation started", id);
            AnalysisPreparation preparation = coordinator.prepare(id, locator, depth);
            saveResults(id, preparation);
            log.info("Analysis {} preparation completed with {} horizons", id, preparation.horizons().size());
            if (forecastPlanned(locator, depth.skip())) {
                forecastService.forecast(id, preparation);
                log.info("Analysis {} forecast completed", id);
            }
        } catch (Exception e) {
            log.error("Analysis {} preparation failed", id, e);
            try {
                dao.markFailed(id, Instant.now(), failureMessage(e));
            } catch (RuntimeException persistenceError) {
                log.error("Analysis {} failure could not be persisted", id, persistenceError);
            }
        }
    }

    private void saveResults(UUID id, AnalysisPreparation preparation) {
        var snapshot = preparation.marketContext().getSnapshot();
        boolean forecastPlanned = forecastPlanned(preparation.locator(), preparation.depth().skip());
        var input = Map.of("snapshotId", id,
                "metricScopes", preparation.marketContext().getTimeframeMetrics().keySet().stream()
                        .map(Enum::name).toList(),
                "horizonCount", preparation.horizons().size());
        journal.run(id, "SAVE_RESULTS", input,
                () -> dao.markPrepared(id, Instant.now(), snapshot.snapshotTimeUtc(),
                        snapshot.passport().exchange(), preparation.horizons(),
                        preparation.marketContext().getMetrics(),
                        preparation.marketContext().getTimeframeMetrics(), forecastPlanned),
                Map.of("analysisId", id, "status", forecastPlanned ? "FORECASTING" : "PREPARED"));
    }

    private boolean forecastPlanned(InstrumentLocator locator, Set<SkippableStep> skip) {
        return locator.type() == AssetType.FUTURE && !skip.contains(SkippableStep.FORECAST);
    }

    private String failureMessage(Exception error) {
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        return message.substring(0, Math.min(message.length(), 1000));
    }

}
