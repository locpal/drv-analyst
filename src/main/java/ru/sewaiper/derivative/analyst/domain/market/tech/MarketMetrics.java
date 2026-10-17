package ru.sewaiper.derivative.analyst.domain.market.tech;

import org.ta4j.core.num.Num;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Provider market values, without claims about exchange session or price freshness. */
public record MarketMetrics(Num lastPrice,
                            Num providerClosePrice,
                            Num lastVsClosePts,
                            Num lastVsClosePct,
                            Instant lastPriceTimeUtc,
                            Optional<Instant> providerCloseTimeUtc,
                            Optional<Long> openInterest,
                            Optional<Instant> openInterestTimeUtc) implements TechMetrics {
    public MarketMetrics {
        Objects.requireNonNull(lastPrice, "lastPrice");
        Objects.requireNonNull(providerClosePrice, "providerClosePrice");
        Objects.requireNonNull(lastVsClosePts, "lastVsClosePts");
        Objects.requireNonNull(lastVsClosePct, "lastVsClosePct");
        Objects.requireNonNull(lastPriceTimeUtc, "lastPriceTimeUtc");
        Objects.requireNonNull(providerCloseTimeUtc, "providerCloseTimeUtc");
        Objects.requireNonNull(openInterest, "openInterest");
        Objects.requireNonNull(openInterestTimeUtc, "openInterestTimeUtc");
    }

    @Override
    public MetricName type() {
        return MetricName.MARKET;
    }
}
