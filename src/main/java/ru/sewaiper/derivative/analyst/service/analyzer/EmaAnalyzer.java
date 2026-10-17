package ru.sewaiper.derivative.analyst.service.analyzer;

import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.averages.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.tech.EmaMetrics;

@Component
public class EmaAnalyzer implements IndicatorAnalyzer {
    private static final int FAST_PERIOD = 20;
    private static final int SLOW_PERIOD = 50;
    private static final int CHANGE_LOOKBACK = 5;
    private static final int REQUIRED_CANDLES = SLOW_PERIOD;

    @Override
    public void analyze(MarketContext context) {
        BarSeries series = context.getBarSeries();
        if (series.getBarCount() < REQUIRED_CANDLES) {
            return;
        }

        int endIndex = series.getEndIndex();
        var closeIndicator = new ClosePriceIndicator(series);
        var fast = new EMAIndicator(closeIndicator, FAST_PERIOD);
        var slow = new EMAIndicator(closeIndicator, SLOW_PERIOD);
        if (endIndex - CHANGE_LOOKBACK < series.getBeginIndex() + fast.getCountOfUnstableBars()
                || endIndex < series.getBeginIndex() + slow.getCountOfUnstableBars()) {
            return;
        }

        Num ema20 = fast.getValue(endIndex);
        Num ema50 = slow.getValue(endIndex);
        Num previousEma20 = fast.getValue(endIndex - CHANGE_LOOKBACK);
        Num close = closeIndicator.getValue(endIndex);
        if (!Num.isValid(ema20) || !Num.isValid(ema50) || !Num.isValid(previousEma20)
                || !Num.isValid(close) || !ema20.isPositive() || !ema50.isPositive()
                || !previousEma20.isPositive()) {
            throw new IllegalStateException("EMA or the last close price is unavailable");
        }

        Num hundred = series.numFactory().hundred();
        Num spreadPts = ema20.minus(ema50);
        Num deviationPts = close.minus(ema20);
        Num changePts = ema20.minus(previousEma20);
        context.addMetric(new EmaMetrics(
                ema20,
                ema50,
                spreadPts,
                spreadPts.dividedBy(ema50).multipliedBy(hundred),
                deviationPts,
                deviationPts.dividedBy(ema20).multipliedBy(hundred),
                changePts,
                changePts.dividedBy(previousEma20).multipliedBy(hundred),
                FAST_PERIOD,
                SLOW_PERIOD,
                CHANGE_LOOKBACK,
                series.getBarCount(),
                series.getBar(endIndex).getEndTime()
        ));
    }
}
