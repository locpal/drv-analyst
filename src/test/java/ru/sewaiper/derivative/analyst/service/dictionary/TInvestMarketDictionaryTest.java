package ru.sewaiper.derivative.analyst.service.dictionary;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.dao.InstrumentPassportDao;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.GetFuturesResponse;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TInvestMarketDictionaryTest {

    @Test
    void searchesByAssetTypeAndPaginatesInStableOrder() {
        var dao = mock(InstrumentPassportDao.class);
        var dictionary = new TInvestMarketDictionary(mock(TInvestClient.class), dao);
        var byName = passport("COPPER1226", "Медь / MED Copper", "uid-1");
        var byTicker = passport("MED", "Copper-12.26", "uid-2");
        var unrelated = passport("SILVER", "Серебро", "uid-3");
        when(dao.findByType(Provider.T_INVEST, AssetType.FUTURE))
                .thenReturn(List.of(unrelated, byName, byTicker));
        var share = new InstrumentPassport("MED", "Share", AssetType.SHARE, Provider.T_INVEST,
                "share-uid", "share-figi", "moex_mrng_evng_eurusd_weekend");
        when(dao.findByType(Provider.T_INVEST, AssetType.SHARE)).thenReturn(List.of(share));

        assertEquals(List.of(byName), dictionary.search(AssetType.FUTURE, "мЕдЬ", 0, 20).items());
        var firstPage = dictionary.search(AssetType.FUTURE, "med", 0, 1);
        assertEquals(List.of(byTicker), firstPage.items());
        assertEquals(2, firstPage.totalElements());
        assertEquals(2, firstPage.totalPages());
        assertEquals(List.of(byName), dictionary.search(AssetType.FUTURE, "med", 1, 1).items());
        assertEquals(List.of(), dictionary.search(AssetType.FUTURE, "med", 2, 1).items());
        assertEquals(List.of(share), dictionary.search(AssetType.SHARE, "", 0, 20).items());
    }

    @Test
    void synchronizesSelectedTypeAndPropagatesProviderFailures() {
        var client = mock(TInvestClient.class);
        var dao = mock(InstrumentPassportDao.class);
        var dictionary = new TInvestMarketDictionary(client, dao);
        assertEquals(Set.of(AssetType.SHARE, AssetType.FUTURE, AssetType.BOND, AssetType.DFA),
                dictionary.supportedAssetTypes());
        when(client.getFutures(any())).thenReturn(new GetFuturesResponse(List.of()));

        dictionary.sync(AssetType.FUTURE);

        verify(dao).replacePassports(Provider.T_INVEST, AssetType.FUTURE, List.of());

        var failure = new IllegalStateException("T-Invest unavailable");
        when(client.getFutures(any())).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> dictionary.sync(AssetType.FUTURE)));
        assertThrows(IllegalArgumentException.class, () -> dictionary.sync(AssetType.CRYPTO));
    }

    private InstrumentPassport passport(String ticker, String name, String uid) {
        return new InstrumentPassport(ticker, name, AssetType.FUTURE, Provider.T_INVEST,
                uid, "figi-" + uid, "forts_futures_weekend");
    }
}
