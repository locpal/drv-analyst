package ru.sewaiper.derivative.analyst.service.analyzer;

import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.num.Num;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;

@Component
@Order(0)
public class AtrAnalyzer implements IndicatorAnalyzer {
    private static final int PERIOD = 28;
    private static final int REQUIRED_CANDLES = PERIOD;

    @Override
    public void analyze(MarketContext context) {
        BarSeries series = context.getBarSeries();
        if (series.getBarCount() < REQUIRED_CANDLES) {
            return;
        }

        int endIndex = series.getEndIndex();
        ATRIndicator indicator = new ATRIndicator(series, PERIOD);
        if (endIndex < series.getBeginIndex() + indicator.getCountOfUnstableBars()) {
            return;
        }
        Num atr = indicator.getValue(endIndex);
        Num close = series.getBar(endIndex).getClosePrice();
        if (!Num.isValid(atr) || !Num.isValid(close) || !close.isPositive()) {
            throw new IllegalStateException("ATR or the last close price is unavailable");
        }

        Num atrPct = atr.dividedBy(close).multipliedBy(series.numFactory().hundred());
        context.addMetric(new AtrMetrics(
                atr, atrPct, PERIOD, series.getBarCount(),
                series.getBar(endIndex).getEndTime()
        ));
    }
}
