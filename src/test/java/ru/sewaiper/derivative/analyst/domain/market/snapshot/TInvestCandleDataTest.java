package ru.sewaiper.derivative.analyst.domain.market.snapshot;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.CandleSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TInvestCandleDataTest {
    @Test
    void restoresExactProviderPriceAndBuildsDecimalBar() {
        var price = new Quotation(null, "123", 456_789_123);
        var candle = new Candle(price, price, price, price, "9007199254740993",
                Instant.parse("2026-10-16T10:00:00Z"), true,
                CandleSource.CANDLE_SOURCE_EXCHANGE, null, null);
        var original = new TInvestCandleData(Timeframe.H1, List.of(candle));

        var restored = TInvestCandleData.restore(original.serialize());
        var series = restored.toBarSeries();

        assertEquals(original, restored);
        assertEquals(1, series.getBarCount());
        assertEquals("123.456789123", series.getBar(0).getClosePrice().toString());
        assertEquals("9007199254740993", series.getBar(0).getVolume().toString());
        assertEquals(Instant.parse("2026-10-16T11:00:00Z"), series.getBar(0).getEndTime());
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        assertThrows(IllegalArgumentException.class,
                () -> TInvestCandleData.restore("{\"schemaVersion\":2,\"timeframe\":\"H1\",\"candles\":[]}"));
    }
}
