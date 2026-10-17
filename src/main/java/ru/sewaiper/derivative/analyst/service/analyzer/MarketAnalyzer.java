package ru.sewaiper.derivative.analyst.service.analyzer;

import org.springframework.stereotype.Component;
import org.ta4j.core.num.Num;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.MarketTick;
import ru.sewaiper.derivative.analyst.domain.market.tech.MarketMetrics;

import java.util.Optional;

@Component
public class MarketAnalyzer implements IndicatorAnalyzer {
    @Override
    public void analyze(MarketContext context) {
        MarketTick tick = context.getSnapshot().currentTick();
        if (tick == null || tick.timeUtc() == null || tick.lastPrice() == null
                || tick.closePrice() == null || tick.lastPrice().signum() <= 0
                || tick.closePrice().signum() <= 0
                || tick.openInterest() != null && tick.openInterest() < 0) {
            throw new IllegalStateException("Market snapshot has no valid provider prices");
        }

        var factory = context.getBarSeries().numFactory();
        Num last = factory.numOf(tick.lastPrice());
        Num close = factory.numOf(tick.closePrice());
        Num difference = last.minus(close);
        context.addMetric(new MarketMetrics(
                last, close, difference,
                difference.dividedBy(close).multipliedBy(factory.hundred()),
                tick.timeUtc(), Optional.ofNullable(tick.closePriceTimeUtc()),
                Optional.ofNullable(tick.openInterest()),
                Optional.ofNullable(tick.openInterestTimeUtc())));
    }
}
