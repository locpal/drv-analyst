package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Свечи провайдера с точными значениями Quotation до преобразования в ta4j. */
public record TInvestCandleData(int schemaVersion,
                                Timeframe timeframe,
                                List<Candle> candles) implements CandleData {

    private static final int CURRENT_VERSION = 1;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public TInvestCandleData {
        if (schemaVersion != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported T-Invest candle format version: " + schemaVersion);
        }
        Objects.requireNonNull(timeframe, "timeframe");
        candles = List.copyOf(candles);
    }

    public TInvestCandleData(Timeframe timeframe, List<Candle> candles) {
        this(CURRENT_VERSION, timeframe, candles);
    }

    @Override
    public String serialize() {
        try {
            return JSON.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize T-Invest candles", e);
        }
    }

    public static TInvestCandleData restore(String json) {
        try {
            return JSON.readValue(json, TInvestCandleData.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot restore T-Invest candles", e);
        }
    }

    @Override
    public BarSeries toBarSeries() {
        Duration period = switch (timeframe) {
            case M1 -> Duration.ofMinutes(1);
            case M5 -> Duration.ofMinutes(5);
            case M15 -> Duration.ofMinutes(15);
            case H1 -> Duration.ofHours(1);
            case D1 -> Duration.ofDays(1);
        };
        BarSeries series = new BaseBarSeriesBuilder()
                .withNumFactory(DecimalNumFactory.getInstance())
                .build();
        Instant previous = null;
        for (Candle candle : candles) {
            if (!candle.isComplete() || candle.time() == null
                    || previous != null && !candle.time().isAfter(previous)) {
                throw new IllegalArgumentException("Candle data contains incomplete or unordered candles");
            }
            series.barBuilder()
                    .timePeriod(period)
                    .beginTime(candle.time())
                    .endTime(candle.time().plus(period))
                    .openPrice(candle.open().asBigDecimal())
                    .highPrice(candle.high().asBigDecimal())
                    .lowPrice(candle.low().asBigDecimal())
                    .closePrice(candle.close().asBigDecimal())
                    .volume(candle.volume())
                    .add();
            previous = candle.time();
        }
        return series;
    }
}
