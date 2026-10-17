package ru.sewaiper.derivative.analyst.service.market;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.SnapshotDepth;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.TInvestCandleData;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.MarketTick;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.OrderBookSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.PriceLevel;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.Candle;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.CandleInterval;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.CandleSource;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.MarketValuePosition;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.MarketValue;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.MarketValueType;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.MarketValuesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.OrderBookRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.OrderBookResponse;
import ru.sewaiper.derivative.analyst.mapper.TInvestMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TInvestMarketDataProvider implements MarketDataProvider, InitializingBean {
    private static final int H1_LOOKBACK_DAYS = 30;
    private static final int H1_MAX_CANDLES = 120;
    private static final int D1_LOOKBACK_DAYS = 150;
    private static final int D1_MAX_CANDLES = 100;

    private final TInvestClient client;
    private final Map<AssetType, InstrumentDetailsLoader> detailsLoaders;

    private final ThreadPoolTaskExecutor executor;
    public TInvestMarketDataProvider(TInvestClient client,
                                     List<InstrumentDetailsLoader> detailsLoaders) {
        this.client = client;
        this.detailsLoaders = detailsLoaders.stream()
                .collect(Collectors.toUnmodifiableMap(InstrumentDetailsLoader::assetType, Function.identity()));

        this.executor = new ThreadPoolTaskExecutorBuilder()
                .threadNamePrefix("TInvestMarketDataProvider")
                .maxPoolSize(100)
                .corePoolSize(20)
                .keepAlive(Duration.ofSeconds(120))
                .build();

    }

    @Override
    public void afterPropertiesSet() {
        executor.afterPropertiesSet();
    }

    @Override
    public @NonNull InstrumentSnapshot getSnapshot(@NonNull InstrumentLocator locator, @NonNull SnapshotDepth depth) {
        Objects.requireNonNull(locator, "locator");
        Objects.requireNonNull(depth, "depth");
        if (locator.provider() != Provider.T_INVEST) {
            throw new IllegalArgumentException("Unsupported market data provider: " + locator.provider());
        }
        InstrumentDetailsLoader loader = detailsLoaders.get(locator.type());
        if (loader == null) {
            throw new IllegalArgumentException("Unsupported asset type for analysis: " + locator.type());
        }
        InstrumentDetails details = loader.load(locator.providerId());
        return buildSnapshot(details, depth);
    }

    private @NonNull InstrumentSnapshot buildSnapshot(InstrumentDetails details, SnapshotDepth depth) {
        var passport = details.passport();
        String uid = passport.providerId();
        Instant candleCutoff = Instant.now();

        // 1. Базовые показатели рынка
        var marketFuture = depth.skip().contains(SkippableStep.MARKET_TICK)
                ? CompletableFuture.<MarketTick>completedFuture(null)
                : CompletableFuture.supplyAsync(() -> fetchMarketTick(uid), executor);
        // 2. Достаём свечи
        var h1Future = CompletableFuture.supplyAsync(
                () -> fetchCandles(uid, Timeframe.H1, H1_LOOKBACK_DAYS, H1_MAX_CANDLES, candleCutoff), executor);
        var d1Future = CompletableFuture.supplyAsync(
                () -> fetchCandles(uid, Timeframe.D1, D1_LOOKBACK_DAYS, D1_MAX_CANDLES, candleCutoff), executor);
        // 3. Собираем стакан
        var orderBookFuture = depth.skip().contains(SkippableStep.ORDER_BOOK)
                ? CompletableFuture.<OrderBookSnapshot>completedFuture(null)
                : CompletableFuture.supplyAsync(() -> fetchOrderBook(uid, depth), executor);
        CompletableFuture.allOf(
                marketFuture, h1Future, d1Future, orderBookFuture).join();

        Map<Timeframe, TInvestCandleData> candleData = new EnumMap<>(Timeframe.class);
        candleData.put(Timeframe.H1, new TInvestCandleData(Timeframe.H1, h1Future.join()));
        candleData.put(Timeframe.D1, new TInvestCandleData(Timeframe.D1, d1Future.join()));

        return new InstrumentSnapshot(
                passport.ticker(),
                Instant.now(),
                passport,
                candleData,
                orderBookFuture.join(),
                marketFuture.join(),
                details.mechanics()
        );
    }

    private MarketTick fetchMarketTick(String uid) {
        var request = new MarketValuesRequest(List.of(uid), List.of(
                MarketValueType.INSTRUMENT_VALUE_LAST_PRICE,
                MarketValueType.INSTRUMENT_VALUE_CLOSE_PRICE,
                MarketValueType.INSTRUMENT_VALUE_OPEN_INTEREST
        ));

        MarketValuePosition position = client.getMarketValues(request)
                .instruments().stream()
                .filter(mvp -> uid.equals(mvp.instrumentUid()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("T-Invest returned no market values for " + uid));

        Map<MarketValueType, MarketValue> marketValues = position.values()
                .stream()
                .collect(Collectors.toMap(MarketValue::type, Function.identity()));

        MarketValue lastPriceVal = marketValues.get(MarketValueType.INSTRUMENT_VALUE_LAST_PRICE);
        MarketValue closePriceVal = marketValues.get(MarketValueType.INSTRUMENT_VALUE_CLOSE_PRICE);
        MarketValue openInterestVal = marketValues.get(MarketValueType.INSTRUMENT_VALUE_OPEN_INTEREST);
        if (lastPriceVal == null || lastPriceVal.value() == null || lastPriceVal.time() == null
                || closePriceVal == null || closePriceVal.value() == null) {
            throw new IllegalStateException("T-Invest returned incomplete market prices for " + uid);
        }
        Long openInterest = openInterestVal == null || openInterestVal.value() == null
                ? null : openInterestVal.value().asBigDecimal().longValueExact();
        return new MarketTick(lastPriceVal.time(), lastPriceVal.value().asBigDecimal(),
                closePriceVal.value().asBigDecimal(), openInterest,
                closePriceVal.time(), openInterest == null ? null : openInterestVal.time());
    }

    private List<Candle> fetchCandles(String uid, Timeframe timeframe, int lookbackDays,
                                      int maxCandles, Instant to) {
        Duration backward = Duration.ofDays(lookbackDays);
        Instant from = to.minus(backward);

        CandleInterval interval = asTInvestInterval(timeframe);
        Duration maxChunk = getMaxChunkDuration(interval);

        Map<Instant, Candle> candles = new TreeMap<>();
        Instant currentFrom = from;
        while (currentFrom.isBefore(to)) {
            Instant currentTo = currentFrom.plus(maxChunk);
            if (currentTo.isAfter(to)) {
                currentTo = to;
            }
            var request = new GetCandlesRequest(
                    currentFrom, currentTo,
                    interval, uid,
                    CandleSource.CANDLE_SOURCE_EXCHANGE, // Для бектестера и реальной торговли почти всегда нужны чистые биржевые данные
                    null
            );

            client.getCandles(request)
                    .candles()
                    .forEach(c -> candles.put(c.time(), c));

            currentFrom = currentTo;
        }
        List<Candle> selected = new ArrayList<>(candles.values().stream()
                .filter(Candle::isComplete)
                .filter(candle -> !candle.time().isBefore(from))
                .filter(candle -> candle.time().isBefore(to))
                .toList());
        return List.copyOf(selected.subList(Math.max(0, selected.size() - maxCandles), selected.size()));
    }

    private CandleInterval asTInvestInterval(Timeframe timeframe) {
        return switch (timeframe) {
            case M1 -> CandleInterval.CANDLE_INTERVAL_1_MIN;
            case M5 -> CandleInterval.CANDLE_INTERVAL_5_MIN;
            case M15 -> CandleInterval.CANDLE_INTERVAL_15_MIN;
            case H1 -> CandleInterval.CANDLE_INTERVAL_HOUR;
            case D1 -> CandleInterval.CANDLE_INTERVAL_DAY;
        };
    }

    private Duration getMaxChunkDuration(CandleInterval interval) {
        return switch (interval) {
            case CANDLE_INTERVAL_1_MIN,
                 CANDLE_INTERVAL_2_MIN,
                 CANDLE_INTERVAL_3_MIN  -> Duration.ofDays(1);

            case CANDLE_INTERVAL_5_MIN,
                 CANDLE_INTERVAL_10_MIN -> Duration.ofDays(7);   // 1 неделя

            case CANDLE_INTERVAL_15_MIN,
                 CANDLE_INTERVAL_30_MIN -> Duration.ofDays(21);  // 3 недели

            case CANDLE_INTERVAL_HOUR,
                 CANDLE_INTERVAL_2_HOUR,
                 CANDLE_INTERVAL_4_HOUR -> Duration.ofDays(90);  // 3 месяца (~90 дней)

            case CANDLE_INTERVAL_DAY    -> Duration.ofDays(365 * 6); // 6 лет

            case CANDLE_INTERVAL_WEEK   -> Duration.ofDays(365 * 5); // 5 лет
            case CANDLE_INTERVAL_MONTH  -> Duration.ofDays(365 * 10); // 10 лет

            case CANDLE_INTERVAL_5_SEC,
                 CANDLE_INTERVAL_10_SEC -> Duration.ofMinutes(200);
            case CANDLE_INTERVAL_30_SEC -> Duration.ofHours(20);

            default -> Duration.ofDays(1); // Безопасный фоллбек
        };
    }

    private OrderBookSnapshot fetchOrderBook(String uid, SnapshotDepth depth) {
        var request = new OrderBookRequest(depth.orderBookDepth(), uid);

        OrderBookResponse response = client.getOrderBook(request);
        if (!uid.equals(response.instrumentUid())) {
            throw new IllegalStateException("T-Invest returned a different order book UID");
        }
        List<PriceLevel> bids = response.bids().stream().map(TInvestMapper::mapPriceLevel).toList();
        List<PriceLevel> asks = response.asks().stream().map(TInvestMapper::mapPriceLevel).toList();
        return new OrderBookSnapshot(Instant.now(), bids, asks);
    }

}
