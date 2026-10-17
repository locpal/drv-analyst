package ru.sewaiper.derivative.analyst.service.analyzer;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class IndicatorAnalysisService {

    private final List<IndicatorAnalyzer> analyzers;

    public IndicatorAnalysisService(List<IndicatorAnalyzer> analyzers) {
        this.analyzers = List.copyOf(analyzers);
    }

    public @NonNull MarketContext analyze(@NonNull InstrumentSnapshot snapshot) {
        return analyze(snapshot, Set.of());
    }

    public @NonNull MarketContext analyze(@NonNull InstrumentSnapshot snapshot, Set<SkippableStep> skip) {
        Objects.requireNonNull(snapshot, "snapshot");
        TInvestCandleData h1 = snapshot.candleData().get(Timeframe.H1);
        TInvestCandleData d1 = snapshot.candleData().get(Timeframe.D1);
        if ((h1 == null || h1.candles().isEmpty()) && (d1 == null || d1.candles().isEmpty())) {
            throw new IllegalStateException("No closed H1 or D1 candles for analysis");
        }
        MarketContext context = new MarketContext(snapshot,
                h1 != null && !h1.candles().isEmpty() ? h1.toBarSeries() : d1.toBarSeries());
        for (Timeframe timeframe : List.of(Timeframe.H1, Timeframe.D1)) {
            TInvestCandleData data = snapshot.candleData().get(timeframe);
            if (data == null || data.candles().isEmpty()) {
                continue;
            }
            MarketContext seriesContext = new MarketContext(snapshot, data.toBarSeries());
            for (IndicatorAnalyzer analyzer : analyzers) {
                if (!(analyzer instanceof MarketAnalyzer) && !(analyzer instanceof LiquidityAnalyzer)) {
                    analyzer.analyze(seriesContext);
                }
            }
            context.addTimeframeMetrics(timeframe, seriesContext.getMetrics());
        }
        for (IndicatorAnalyzer analyzer : analyzers) {
            if (!(analyzer instanceof MarketAnalyzer) && !(analyzer instanceof LiquidityAnalyzer)) {
                continue;
            }
            if ((analyzer instanceof MarketAnalyzer && skip.contains(SkippableStep.MARKET_TICK))
                    || (analyzer instanceof LiquidityAnalyzer && skip.contains(SkippableStep.ORDER_BOOK))) {
                continue;
            }
            analyzer.analyze(context);
        }
        return context;
    }
}
