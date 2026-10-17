package ru.sewaiper.derivative.analyst.domain.analysis;

import tools.jackson.databind.JsonNode;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Persistent run state and its requested omissions; forecast details are stored separately. */
public record AnalysisRun(UUID id,
                          InstrumentLocator locator,
                          String methodologyVersion,
                          Set<SkippableStep> skip,
                          AnalysisStatus status,
                          Instant createdAtUtc,
                          Instant startedAtUtc,
                          Instant finishedAtUtc,
                          Instant snapshotAtUtc,
                          String exchange,
                          List<TradingHorizon> horizons,
                          JsonNode metrics,
                          String errorMessage) {
}
