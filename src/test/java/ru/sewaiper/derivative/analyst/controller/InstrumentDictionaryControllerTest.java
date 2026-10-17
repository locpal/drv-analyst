package ru.sewaiper.derivative.analyst.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.service.dictionary.MarketDictionary;

import java.util.EnumSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstrumentDictionaryControllerTest {

    @Test
    void servesSupportedTypesWithoutTreatingPathAsAssetType() throws Exception {
        var dictionary = mock(MarketDictionary.class);
        when(dictionary.supportedAssetTypes()).thenReturn(
                EnumSet.of(AssetType.SHARE, AssetType.FUTURE, AssetType.BOND, AssetType.DFA));
        var mvc = MockMvcBuilders.standaloneSetup(new InstrumentDictionaryController(dictionary)).build();

        mvc.perform(get("/api/v1/instruments/asset-types"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"SHARE\",\"FUTURE\",\"BOND\",\"DFA\"]"));
    }
}
