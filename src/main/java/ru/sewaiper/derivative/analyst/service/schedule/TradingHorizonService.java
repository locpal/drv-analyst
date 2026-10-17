package ru.sewaiper.derivative.analyst.service.schedule;

import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.Day;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.Exchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.TradingSchedulesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.TradingSchedulesResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class TradingHorizonService {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");
    private static final List<Integer> HORIZONS = List.of(1, 5, 10);
    private static final int AVAILABLE_SCHEDULE_DAYS = 14;

    private final TInvestClient client;

    public TradingHorizonService(TInvestClient client) {
        this.client = client;
    }

    public List<TradingHorizon> resolve(String exchangeName, Instant snapshotCapturedAtUtc) {
        if (exchangeName == null || exchangeName.isBlank()) {
            throw new IllegalArgumentException("exchangeName must not be blank");
        }
        Objects.requireNonNull(snapshotCapturedAtUtc, "snapshotCapturedAtUtc");
        LocalDate start = snapshotCapturedAtUtc.atZone(MOSCOW).toLocalDate();
        LocalDate endExclusive = start.plusDays(AVAILABLE_SCHEDULE_DAYS);
        Exchange schedule = fetchSchedule(exchangeName, start, endExclusive);
        return resolve(schedule, snapshotCapturedAtUtc);
    }

    private Exchange fetchSchedule(String exchangeName, LocalDate start, LocalDate endExclusive) {
        Instant from = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = endExclusive.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        TradingSchedulesResponse response = client.getTradingSchedules(new TradingSchedulesRequest(exchangeName, from, to));
        if (response == null || response.exchanges() == null) {
            throw new IllegalStateException("Trading schedule is unavailable for " + exchangeName);
        }
        if (response.exchanges().size() != 1) {
            throw new IllegalStateException("Expected exactly one trading schedule for " + exchangeName);
        }
        Exchange exchange = response.exchanges().getFirst();
        if (exchange == null) {
            throw new IllegalStateException("Trading schedule is unavailable for " + exchangeName);
        }
        if (exchange.days() == null) {
            throw new IllegalStateException("Trading days are unavailable for " + exchangeName);
        }
        return exchange;
    }

    public List<TradingHorizon> resolve(Exchange exchange, Instant snapshotCapturedAtUtc) {
        Objects.requireNonNull(exchange, "exchange");
        Objects.requireNonNull(snapshotCapturedAtUtc, "snapshotCapturedAtUtc");
        if (exchange.days() == null) {
            throw new IllegalStateException("Trading days are unavailable for " + exchange.exchange());
        }
        LocalDate snapshotDate = snapshotCapturedAtUtc.atZone(MOSCOW).toLocalDate();
        List<Day> tradingDays = exchange.days()
                .stream()
                .filter(Objects::nonNull)
                .filter(Day::isTradingDay)
                .filter(day -> day.date() != null && LocalDate.ofInstant(day.date(), ZoneOffset.UTC).isAfter(snapshotDate))
                .sorted(Comparator.comparing(Day::date))
                .toList();
        if (tradingDays.size() < 10) {
            throw new IllegalStateException("Trading schedule contains only " + tradingDays.size()
                    + " future trading days for " + exchange.exchange() + "; ten are required");
        }
        for (int i = 0; i < 10; i++) {
            Day day = tradingDays.get(i);
            if (day.startTime() == null || day.endTime() == null || !day.endTime().isAfter(day.startTime())) {
                throw new IllegalStateException("Main trading session is incomplete on " + day.date());
            }
            if (i > 0 && LocalDate.ofInstant(day.date(), ZoneOffset.UTC)
                    .equals(LocalDate.ofInstant(tradingDays.get(i - 1).date(), ZoneOffset.UTC))) {
                throw new IllegalStateException("Trading schedule has a duplicate trading day");
            }
        }
        return HORIZONS.stream()
                .map(horizon -> selectHorizon(horizon, tradingDays))
                .toList();
    }

    private TradingHorizon selectHorizon(Integer horizon, List<Day> tradingDays) {
        Day day = tradingDays.get(horizon - 1);
        return new TradingHorizon(horizon, LocalDate.ofInstant(day.date(), ZoneOffset.UTC), day.endTime());
    }
}
