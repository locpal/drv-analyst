package ru.sewaiper.derivative.analyst.service.market;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.CandleInterval;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TInvestMarketDataProviderTest {
    @Test
    void requestsBoundedH1AndD1HistoriesAndKeepsOnlyLatestClosedCandles() {
        var client = mock(TInvestClient.class);
        var loader = mock(InstrumentDetailsLoader.class);
        when(loader.assetType()).thenReturn(AssetType.FUTURE);
        when(loader.load("uid")).thenReturn(new InstrumentDetails(
                new InstrumentPassport("TEST", "Test", AssetType.FUTURE, Provider.T_INVEST,
                        "uid", "figi", "forts_futures"), null));
        when(client.getCandles(any(GetCandlesRequest.class))).thenAnswer(invocation -> {
            GetCandlesRequest request = invocation.getArgument(0);
            boolean hourly = request.interval() == CandleInterval.CANDLE_INTERVAL_HOUR;
            int count = hourly ? 130 : 110;
            List<Candle> candles = new ArrayList<>();
            for (int i = count; i >= 1; i--) {
                candles.add(candle(request.to().minus(hourly ? Duration.ofHours(i) : Duration.ofDays(i)), true));
            }
            candles.add(candle(request.to().minusSeconds(30), false));
            candles.add(candle(request.from().minusSeconds(1), true));
            return new GetCandlesResponse(candles);
        });
        var provider = new TInvestMarketDataProvider(client, List.of(loader));
        provider.afterPropertiesSet();
            var snapshot = provider.getSnapshot(
                    new InstrumentLocator("uid", AssetType.FUTURE, Provider.T_INVEST),
                    new SnapshotDepth(10, Set.of(SkippableStep.MARKET_TICK, SkippableStep.ORDER_BOOK)));
            assertEquals(120, snapshot.candleData().get(Timeframe.H1).candles().size());
            assertEquals(100, snapshot.candleData().get(Timeframe.D1).candles().size());
            assertTrue(snapshot.candleData().values().stream().flatMap(data -> data.candles().stream())
                    .allMatch(Candle::isComplete));
            var requests = org.mockito.ArgumentCaptor.forClass(GetCandlesRequest.class);
            verify(client, times(2)).getCandles(requests.capture());
            assertTrue(requests.getAllValues().stream().anyMatch(request ->
                    request.interval() == CandleInterval.CANDLE_INTERVAL_HOUR
                            && Duration.between(request.from(), request.to()).equals(Duration.ofDays(30))));
            assertTrue(requests.getAllValues().stream().anyMatch(request ->
                    request.interval() == CandleInterval.CANDLE_INTERVAL_DAY
                            && Duration.between(request.from(), request.to()).equals(Duration.ofDays(150))));
            assertEquals(1, requests.getAllValues().stream().map(GetCandlesRequest::to).distinct().count());
            verify(client, never()).getMarketValues(any());
            verify(client, never()).getOrderBook(any());
    }

    private Candle candle(Instant at, boolean complete) {
        var price = new Quotation(null, "100", 0);
        return new Candle(price, price, price, price, "1", at, complete, null, null, null);
    }
}
