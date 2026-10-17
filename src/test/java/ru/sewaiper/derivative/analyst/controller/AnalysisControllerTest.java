package ru.sewaiper.derivative.analyst.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisRun;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStatus;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStep;
import ru.sewaiper.derivative.analyst.domain.analysis.LlmExchange;
import ru.sewaiper.derivative.analyst.domain.analysis.MarketHypothesis;
import ru.sewaiper.derivative.analyst.domain.analysis.StartAnalysisRequest;
import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import ru.sewaiper.derivative.analyst.domain.market.InstrumentLocator;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;
import ru.sewaiper.derivative.analyst.service.AnalysisRunService;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalysisControllerTest {
    @Test
    void startsAnalysisWithIdAndExposesItsPersistedStatus() throws Exception {
        var service = mock(AnalysisRunService.class);
        var id = UUID.randomUUID();
        var locator = new InstrumentLocator("uid-1", AssetType.FUTURE, Provider.T_INVEST);
        var run = new AnalysisRun(id, locator, "h1-d1-v1", Set.of(), AnalysisStatus.CREATED,
                Instant.parse("2026-10-16T10:00:00Z"), null, null, null, null, List.of(), null, null);
        when(service.start(any(StartAnalysisRequest.class))).thenReturn(id);
        when(service.find(id)).thenReturn(Optional.of(run));
        var mvc = MockMvcBuilders.standaloneSetup(new AnalysisController(service)).build();

        mvc.perform(post("/api/v1/analyses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId":"uid-1","assetType":"FUTURE"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/v1/analyses/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
        verify(service).start(argThat(request -> request.skip().isEmpty()));

        mvc.perform(post("/api/v1/analyses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId":"uid-1","assetType":"FUTURE","timeframe":"H1",
                                 "skip":["MARKET_TICK","ORDER_BOOK","FORECAST"]}
                                """))
                .andExpect(status().isAccepted());
        verify(service).start(argThat(request -> request.skip().equals(Set.of(
                SkippableStep.MARKET_TICK, SkippableStep.ORDER_BOOK, SkippableStep.FORECAST))));

        mvc.perform(post("/api/v1/analyses").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerId":"uid-1","assetType":"FUTURE","timeframe":"H1","skip":null}
                                """))
                .andExpect(status().isAccepted());
        verify(service, times(2)).start(argThat(request -> request.skip().isEmpty()));

        mvc.perform(get("/api/v1/analyses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locator.providerId").value("uid-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
        when(service.steps(id)).thenReturn(List.of(new AnalysisStep(1, "FETCH_SNAPSHOT", "COMPLETED",
                Instant.parse("2026-10-16T10:00:01Z"), Instant.parse("2026-10-16T10:00:02Z"),
                JsonMapper.builder().build().readTree("{\"providerId\":\"uid-1\"}"),
                JsonMapper.builder().build().readTree("{\"ticker\":\"TEST\"}"), null)));
        mvc.perform(get("/api/v1/analyses/{id}/steps", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("FETCH_SNAPSHOT"))
                .andExpect(jsonPath("$[0].output.ticker").value("TEST"));
    }

    @Test
    void exposesStoredMetricsOnTheExistingGetEndpoint() throws Exception {
        var service = mock(AnalysisRunService.class);
        var id = UUID.randomUUID();
        var at = Instant.parse("2026-10-16T19:00:00Z");
        var metrics = JsonMapper.builder().build().readTree("""
                {"ATR":{"atrPts":"39.24084022700896","period":28}}
                """);
        var run = new AnalysisRun(id,
                new InstrumentLocator("uid-1", AssetType.FUTURE, Provider.T_INVEST),
                "h1-d1-v1", Set.of(SkippableStep.ORDER_BOOK), AnalysisStatus.PREPARED, at, at, at, at,
                "forts_futures", List.of(), metrics, null);
        when(service.find(id)).thenReturn(Optional.of(run));
        var mvc = MockMvcBuilders.standaloneSetup(new AnalysisController(service)).build();

        mvc.perform(get("/api/v1/analyses/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.ATR.atrPts").value("39.24084022700896"))
                .andExpect(jsonPath("$.skip[0]").value("ORDER_BOOK"))
                .andExpect(jsonPath("$.metrics.ATR.period").value(28));
    }

    @Test
    void exposesForecastAndOriginalLlmContent() throws Exception {
        var service = mock(AnalysisRunService.class);
        var id = UUID.randomUUID();
        var at = Instant.parse("2026-10-17T12:00:00Z");
        when(service.find(id)).thenReturn(Optional.of(new AnalysisRun(id,
                new InstrumentLocator("uid-1", AssetType.FUTURE, Provider.T_INVEST),
                "h1-d1-v1", Set.of(), AnalysisStatus.FORECASTED, at, at, at, at,
                "forts_futures", List.of(), null, null)));
        when(service.forecast(id)).thenReturn(Optional.of(new LlmExchange(at,
                JsonMapper.builder().build().readTree("{\"model\":\"tested-model\"}"),
                at, null, "returned-model", "stop", null,
                "{\"hypotheses\":[]}", null, null,
                List.of(new MarketHypothesis(1, "TERMINAL_PRICE_RANGE", new BigDecimal("100"),
                        new BigDecimal("110"), "reason", "alternative", List.of("risk"))))));
        var mvc = MockMvcBuilders.standaloneSetup(new AnalysisController(service)).build();

        mvc.perform(get("/api/v1/analyses/{id}/forecast", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("returned-model"))
                .andExpect(jsonPath("$.finishReason").value("stop"))
                .andExpect(jsonPath("$.content").value("{\"hypotheses\":[]}"))
                .andExpect(jsonPath("$.hypotheses[0].lowerBound").value(100));
    }
}
