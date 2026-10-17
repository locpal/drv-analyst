package ru.sewaiper.derivative.analyst.service.analyzer;

import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.tech.RsiMetrics;

@Component
public class RsiAnalyzer implements IndicatorAnalyzer {
    private static final int PERIOD = 14;
    private static final int CHANGE_LOOKBACK = 5;
    private static final int REQUIRED_CANDLES = PERIOD + CHANGE_LOOKBACK + 1;

    @Override
    public void analyze(MarketContext context) {
        BarSeries series = context.getBarSeries();
        if (series.getBarCount() < REQUIRED_CANDLES) {
            return;
        }

        int endIndex = series.getEndIndex();
        var indicator = new RSIIndicator(new ClosePriceIndicator(series), PERIOD);
        if (endIndex - CHANGE_LOOKBACK < series.getBeginIndex() + indicator.getCountOfUnstableBars()) {
            return;
        }

        Num current = indicator.getValue(endIndex);
        Num previous = indicator.getValue(endIndex - CHANGE_LOOKBACK);
        Num hundred = series.numFactory().hundred();
        if (!Num.isValid(current) || !Num.isValid(previous)
                || current.isNegative() || current.isGreaterThan(hundred)
                || previous.isNegative() || previous.isGreaterThan(hundred)) {
            throw new IllegalStateException("RSI is unavailable or outside 0–100");
        }

        context.addMetric(new RsiMetrics(
                current,
                current.minus(previous), PERIOD,
                CHANGE_LOOKBACK, series.getBarCount(),
                series.getBar(endIndex).getEndTime()
        ));
    }
}
