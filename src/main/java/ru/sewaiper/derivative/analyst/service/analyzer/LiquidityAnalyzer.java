package ru.sewaiper.derivative.analyst.service.analyzer;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.ta4j.core.num.Num;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.OrderBookSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.PriceLevel;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.LiquidityMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@Order(100)
public class LiquidityAnalyzer implements IndicatorAnalyzer {
    private static final int MAX_DEPTH_LEVELS = 10;

    @Override
    public void analyze(MarketContext context) {
        OrderBookSnapshot book = context.getSnapshot().orderBook();
        if (book == null || book.timeUtc() == null || book.bids() == null || book.asks() == null
                || book.bids().isEmpty() || book.asks().isEmpty()) {
            throw new IllegalStateException("Order book has no valid bid/ask snapshot");
        }
        validate(book.bids());
        validate(book.asks());

        List<PriceLevel> bids = book.bids().stream()
                .sorted(Comparator.comparing(PriceLevel::price).reversed()).toList();
        List<PriceLevel> asks = book.asks().stream()
                .sorted(Comparator.comparing(PriceLevel::price)).toList();
        PriceLevel bestBid = bids.getFirst();
        PriceLevel bestAsk = asks.getFirst();
        if (bestBid.price().compareTo(bestAsk.price()) >= 0) {
            throw new IllegalStateException("Order book is crossed or locked");
        }

        AtrMetrics atr = (AtrMetrics) context.getTimeframeMetrics()
                .getOrDefault(Timeframe.H1, java.util.Map.of()).get(MetricName.ATR);
        if (atr == null) {
            atr = (AtrMetrics) context.getMetrics().get(MetricName.ATR);
        }

        var factory = context.getBarSeries().numFactory();
        Num bid = factory.numOf(bestBid.price());
        Num ask = factory.numOf(bestAsk.price());
        Num spread = ask.minus(bid);
        Num mid = bid.plus(ask).dividedBy(factory.two());
        Num spreadMidPct = spread.dividedBy(mid).multipliedBy(factory.hundred());
        Optional<Num> spreadAtrPct = atr == null || !Num.isValid(atr.atrPts())
                || !atr.atrPts().isPositive()
                ? Optional.empty()
                : Optional.of(spread.dividedBy(atr.atrPts()).multipliedBy(factory.hundred()));

        int levels = Math.min(MAX_DEPTH_LEVELS, Math.min(bids.size(), asks.size()));
        long bidDepth = sumQuantity(bids, levels);
        long askDepth = sumQuantity(asks, levels);
        Num imbalance = factory.numOf(bidDepth).minus(factory.numOf(askDepth))
                .dividedBy(factory.numOf(Math.addExact(bidDepth, askDepth)));

        context.addMetric(new LiquidityMetrics(
                bid, ask, bestBid.quantity(), bestAsk.quantity(), spread, spreadMidPct,
                spreadAtrPct, levels, bidDepth, askDepth, imbalance, book.timeUtc()));
    }

    private static void validate(List<PriceLevel> levels) {
        for (PriceLevel level : levels) {
            if (level == null || level.price() == null || level.price().signum() <= 0
                    || level.quantity() <= 0) {
                throw new IllegalStateException("Order book contains invalid price or quantity");
            }
        }
    }

    private static long sumQuantity(List<PriceLevel> levels, int count) {
        long total = 0;
        for (int i = 0; i < count; i++) {
            total = Math.addExact(total, levels.get(i).quantity());
        }
        return total;
    }
}
