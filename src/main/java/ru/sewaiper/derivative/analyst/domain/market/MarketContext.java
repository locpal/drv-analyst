package ru.sewaiper.derivative.analyst.domain.market;

import org.jspecify.annotations.NonNull;
import org.ta4j.core.BarSeries;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.market.tech.TechMetrics;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class MarketContext {

    /**
     * Неизменяемые сырые данные (то, что пришло от провайдера)
     */
    private final InstrumentSnapshot snapshot;
    private final BarSeries barSeries;

    /**
     * Динамическая "доска" для метрик.
     */
    private final Map<MetricName, TechMetrics> metrics = new EnumMap<>(MetricName.class);
    private final Map<Timeframe, Map<MetricName, TechMetrics>> timeframeMetrics = new EnumMap<>(Timeframe.class);

    public MarketContext(InstrumentSnapshot snapshot, BarSeries barSeries) {
        this.snapshot = snapshot;
        this.barSeries = barSeries;
    }

    public InstrumentSnapshot getSnapshot() {
        return snapshot;
    }

    public BarSeries getBarSeries() {
        return barSeries;
    }

    public Map<MetricName, TechMetrics> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    public Map<Timeframe, Map<MetricName, TechMetrics>> getTimeframeMetrics() {
        return Collections.unmodifiableMap(timeframeMetrics);
    }

    public void addTimeframeMetrics(Timeframe timeframe, Map<MetricName, TechMetrics> values) {
        timeframeMetrics.put(timeframe, Map.copyOf(values));
    }

    public void addMetric(@NonNull TechMetrics metric) {
        Objects.requireNonNull(metric, "Metric must not be null");
        this.metrics.put(metric.type(), metric);
    }
}
