package ru.sewaiper.derivative.analyst.service.schedule;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.schedule.TradingHorizon;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.Day;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.Exchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.TradingSchedulesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.TradingSchedulesResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TradingHorizonServiceTest {

    @Test
    void countsFutureTradingDaysAcrossWeekend() {
        Instant snapshot = Instant.parse("2026-10-13T12:00:00Z");
        var targets = new TradingHorizonService(mock(TInvestClient.class)).resolve(schedule(14), snapshot);

        assertEquals(List.of(1, 5, 10), targets.stream().map(TradingHorizon::tradingDays).toList());
        assertEquals(List.of(LocalDate.parse("2026-10-14"), LocalDate.parse("2026-10-20"),
                LocalDate.parse("2026-10-27")), targets.stream().map(TradingHorizon::sessionDate).toList());
        assertEquals(Instant.parse("2026-10-20T15:54:59Z"), targets.get(1).targetAtUtc());
    }

    @Test
    void rejectsIncompleteScheduleBeforeForecast() {
        Instant snapshot = Instant.parse("2026-10-13T12:00:00Z");
        var service = new TradingHorizonService(mock(TInvestClient.class));
        assertThrows(IllegalStateException.class, () -> service.resolve(schedule(10), snapshot));
    }

    @Test
    void acceptsProviderScheduleNameDifferentFromInstrumentExchange() {
        TInvestClient client = mock(TInvestClient.class);
        Instant snapshot = Instant.parse("2026-10-17T12:00:00Z");
        Exchange schedule = scheduleFrom(LocalDate.parse("2026-10-17"), 14);
        when(client.getTradingSchedules(any())).thenReturn(new TradingSchedulesResponse(List.of(schedule)));

        var targets = new TradingHorizonService(client).resolve("forts_futures_weekend", snapshot);

        assertEquals(3, targets.size());
        ArgumentCaptor<TradingSchedulesRequest> request = ArgumentCaptor.forClass(TradingSchedulesRequest.class);
        verify(client).getTradingSchedules(request.capture());
        assertEquals("forts_futures_weekend", request.getValue().exchange());
        assertEquals(Instant.parse("2026-10-17T00:00:00Z"), request.getValue().from());
        assertEquals(Instant.parse("2026-10-30T00:00:00Z"), request.getValue().to());
    }

    @Test
    void rejectsAmbiguousScheduleResponse() {
        TInvestClient client = mock(TInvestClient.class);
        Exchange schedule = scheduleFrom(LocalDate.parse("2026-10-17"), 14);
        when(client.getTradingSchedules(any())).thenReturn(new TradingSchedulesResponse(List.of(
                schedule, new Exchange("OTHER", schedule.days()))));
        assertThrows(IllegalStateException.class,
                () -> new TradingHorizonService(client).resolve(
                        "forts_futures_weekend", Instant.parse("2026-10-17T12:00:00Z")));
    }

    @Test
    void stopsWithoutForecastWhenPublishedScheduleHasFewerThanTenFutureTradingDays() {
        TInvestClient client = mock(TInvestClient.class);
        Instant snapshot = Instant.parse("2026-10-13T12:00:00Z");
        when(client.getTradingSchedules(any())).thenReturn(new TradingSchedulesResponse(List.of(
                scheduleFrom(LocalDate.parse("2026-10-13"), 14))));

        var error = assertThrows(IllegalStateException.class,
                () -> new TradingHorizonService(client).resolve("MOEX", snapshot));

        assertEquals("Trading schedule contains only 9 future trading days for MOEX; ten are required",
                error.getMessage());
        ArgumentCaptor<TradingSchedulesRequest> request = ArgumentCaptor.forClass(TradingSchedulesRequest.class);
        verify(client).getTradingSchedules(request.capture());
        assertEquals(Instant.parse("2026-10-13T00:00:00Z"), request.getValue().from());
        assertEquals(Instant.parse("2026-10-26T00:00:00Z"), request.getValue().to());
    }

    private static Exchange schedule(int calendarDays) {
        return scheduleFrom(LocalDate.parse("2026-10-14"), calendarDays);
    }

    private static Exchange scheduleFrom(LocalDate start, int calendarDays) {
        List<Day> days = IntStream.range(0, calendarDays)
                .mapToObj(index -> {
                    LocalDate date = start.plusDays(index);
                    boolean trading = date.getDayOfWeek().getValue() <= 5;
                    Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                    return new Day(dayStart, trading,
                            trading ? dayStart.plusSeconds(6 * 3600 + 10 * 60) : null,
                            trading ? dayStart.plusSeconds(15 * 3600 + 54 * 60 + 59) : null,
                            null, null, null, null, null, null, null, null, null, null, null, null);
                })
                .toList();
        return new Exchange("MOEX", days);
    }
}
