package ru.sewaiper.derivative.analyst.service;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.clients.LlmClient;
import ru.sewaiper.derivative.analyst.dao.AnalysisStepDao;
import ru.sewaiper.derivative.analyst.dao.ForecastDao;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisPreparation;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionRequest;
import ru.sewaiper.derivative.analyst.domain.llm.ChatCompletionResponse;
import ru.sewaiper.derivative.analyst.domain.market.MarketContext;
import ru.sewaiper.derivative.analyst.domain.market.TradingMechanics;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentSnapshot;
import ru.sewaiper.derivative.analyst.domain.market.enums.Timeframe;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ForecastServiceTest {
    @Test
    void preservesResponseAndValidationErrorWhenProviderContentIsInvalid() {
        var client = mock(LlmClient.class);
        var contract = mock(ForecastContract.class);
        var dao = mock(ForecastDao.class);
        var journal = new AnalysisStepJournal(mock(AnalysisStepDao.class));
        var service = new ForecastService(client, contract, dao, journal);
        var preparation = mock(AnalysisPreparation.class);
        var context = mock(MarketContext.class);
        var snapshot = mock(InstrumentSnapshot.class);
        when(preparation.marketContext()).thenReturn(context);
        when(context.getSnapshot()).thenReturn(snapshot);
        when(snapshot.tradingMechanics()).thenReturn(new TradingMechanics(1, 0.25, "RUB",
                true, true, false, null, null, 1.0, null, null));
        var request = new ChatCompletionRequest("tested-model", List.of(), false,
                null, null, null, null, null, null);
        var response = new ChatCompletionResponse("response", "chat.completion", 1L, "tested-model", null,
                List.of(new ChatCompletionResponse.Choice(0,
                        new ChatCompletionResponse.Message("assistant", "{}", null, null, null),
                        "stop", null, null)), null);
        when(contract.request(preparation)).thenReturn(request);
        when(client.completion(request)).thenReturn(response);
        var reference = new ForecastContract.ReferencePrice(new java.math.BigDecimal("100"), Timeframe.H1,
                "2026-10-17T10:00:00Z");
        when(contract.referencePrice(preparation)).thenReturn(reference);
        when(contract.validate(eq(response), eq(reference), any())).thenThrow(new IllegalArgumentException("missing ranges"));

        var id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.forecast(id, preparation));

        var order = inOrder(dao, client);
        order.verify(dao).saveRequest(eq(id), eq(request), any(Instant.class));
        order.verify(client).completion(request);
        order.verify(dao).saveResponse(eq(id), eq(response), any(Instant.class));
        order.verify(dao).saveValidationError(id, "missing ranges");
        verify(dao, never()).saveHypotheses(any(), any(), any());
    }
}
