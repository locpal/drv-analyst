package ru.sewaiper.derivative.analyst.dao;

import org.junit.jupiter.api.Test;
import org.ta4j.core.num.DecimalNumFactory;
import ru.sewaiper.derivative.analyst.domain.market.tech.AtrMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.ConfirmedSwing;
import ru.sewaiper.derivative.analyst.domain.market.tech.EmaMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.LevelMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.LiquidityMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MarketMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.market.tech.RsiMetrics;
import ru.sewaiper.derivative.analyst.domain.market.tech.TechMetrics;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisMetricJsonTest {
    @Test
    void encodesEveryCurrentMetricIncludingNestedAndMissingValues() {
        var nums = DecimalNumFactory.getInstance();
        var at = Instant.parse("2026-10-16T19:00:00Z");
        var swing = new ConfirmedSwing(nums.numOf("14572.000000001"),
                at.minusSeconds(10_800), at.minusSeconds(3_600));
        Map<MetricName, TechMetrics> metrics = Map.of(
                MetricName.ATR, new AtrMetrics(nums.numOf("39.24084022700896"),
                        nums.numOf("0.2723167260722343"), 28, 200, at),
                MetricName.EMA, new EmaMetrics(nums.numOf("14470"), nums.numOf("14495"),
                        nums.numOf("-25"), nums.numOf("-0.17"), nums.numOf("-60"),
                        nums.numOf("-0.41"), nums.numOf("5"), nums.numOf("0.03"),
                        20, 50, 5, 200, at),
                MetricName.RSI, new RsiMetrics(nums.numOf("40.4989776750219"),
                        nums.numOf("-11.894398728305525"), 14, 5, 200, at),
                MetricName.LEVEL, new LevelMetrics(nums.numOf("14731"), nums.numOf("14324"),
                        nums.numOf("321"), nums.numOf("2.22"), nums.numOf("86"),
                        nums.numOf("0.59"), Optional.of(swing), Optional.empty(),
                        2, 2, 50, 200, at),
                MetricName.LIQUIDITY, new LiquidityMetrics(nums.numOf("100.123456789"),
                        nums.numOf("100.123456790"), 7, 8, nums.numOf("0.000000001"),
                        nums.numOf("0.000000000998"), Optional.empty(),
                        1, 7, 8, nums.numOf("-0.06666666666666667"), at),
                MetricName.MARKET, new MarketMetrics(nums.numOf("100.123456789"),
                        nums.numOf("100"), nums.numOf("0.123456789"),
                        nums.numOf("0.123456789"), at, Optional.empty(),
                        Optional.of(123456L), Optional.of(at)));

        var stored = AnalysisMetricJson.decode(AnalysisMetricJson.encode(metrics));

        assertEquals("39.24084022700896", stored.get("ATR").get("atrPts").asText());
        assertEquals("14572.000000001", stored.get("LEVEL")
                .get("confirmedSwingHigh").get("price").asText());
        assertEquals(at.minusSeconds(3_600).toString(), stored.get("LEVEL")
                .get("confirmedSwingHigh").get("confirmationCandleEndUtc").asText());
        assertTrue(stored.get("LEVEL").get("confirmedSwingLow").isNull());
        assertTrue(stored.get("LIQUIDITY").get("spreadAtrPct").isNull());
        assertEquals("0.000000001", stored.get("LIQUIDITY").get("spreadPts").asText());
        assertEquals(123456L, stored.get("MARKET").get("openInterest").asLong());
        assertEquals("40.4989776750219", stored.get("RSI").get("value").asText());
        assertEquals(20, stored.get("EMA").get("fastPeriod").asInt());
    }

    @Test
    void rejectsMissingMetrics() {
        assertThrows(IllegalArgumentException.class, () -> AnalysisMetricJson.encode(Map.of()));
    }
}
