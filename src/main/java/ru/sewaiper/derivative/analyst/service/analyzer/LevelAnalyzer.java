package ru.sewaiper.derivative.analyst.service.analyzer;

import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RecentFractalSwingHighIndicator;
import org.ta4j.core.indicators.RecentFractalSwingLowIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.num.Num;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.tech.ConfirmedSwing;
import ru.sewaiper.derivative.analyst.domain.market.tech.LevelMetrics;

import java.util.Optional;

@Component
public class LevelAnalyzer implements IndicatorAnalyzer {
    private static final int WINDOW = 50;
    private static final int SWING_LEFT_BARS = 2;
    private static final int SWING_RIGHT_BARS = 2;
    private static final int REQUIRED_CANDLES = WINDOW;

    @Override
    public void analyze(MarketContext context) {
        BarSeries series = context.getBarSeries();
        if (series.getBarCount() < REQUIRED_CANDLES) {
            return;
        }

        int endIndex = series.getEndIndex();
        var highest = new HighestValueIndicator(new HighPriceIndicator(series), WINDOW);
        var lowest = new LowestValueIndicator(new LowPriceIndicator(series), WINDOW);
        if (endIndex < series.getBeginIndex() + Math.max(
                highest.getCountOfUnstableBars(), lowest.getCountOfUnstableBars())) {
            return;
        }

        Num high = highest.getValue(endIndex);
        Num low = lowest.getValue(endIndex);
        Num close = new ClosePriceIndicator(series).getValue(endIndex);
        if (!Num.isValid(high) || !Num.isValid(low) || !Num.isValid(close)
                || !close.isPositive() || high.isLessThan(close) || low.isGreaterThan(close)) {
            throw new IllegalStateException("Rolling levels or the last close price are unavailable");
        }

        Num hundred = series.numFactory().hundred();
        Num highDistance = high.minus(close);
        Num lowDistance = close.minus(low);
        var swingHigh = new RecentFractalSwingHighIndicator(
                new HighPriceIndicator(series), SWING_LEFT_BARS, SWING_RIGHT_BARS, 0);
        var swingLow = new RecentFractalSwingLowIndicator(
                new LowPriceIndicator(series), SWING_LEFT_BARS, SWING_RIGHT_BARS, 0);
        context.addMetric(new LevelMetrics(
                high, low,
                highDistance, highDistance.dividedBy(close).multipliedBy(hundred),
                lowDistance, lowDistance.dividedBy(close).multipliedBy(hundred),
                swing(series, swingHigh.getLatestSwingIndex(endIndex), true),
                swing(series, swingLow.getLatestSwingIndex(endIndex), false),
                SWING_LEFT_BARS, SWING_RIGHT_BARS,
                WINDOW, series.getBarCount(), series.getBar(endIndex).getEndTime()));
    }

    private Optional<ConfirmedSwing> swing(BarSeries series, int index, boolean high) {
        if (index < series.getBeginIndex()) {
            return Optional.empty();
        }
        return Optional.of(new ConfirmedSwing(
                high ? series.getBar(index).getHighPrice() : series.getBar(index).getLowPrice(),
                series.getBar(index).getEndTime(),
                series.getBar(index + SWING_RIGHT_BARS).getEndTime()));
    }
}
