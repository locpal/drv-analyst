package ru.sewaiper.derivative.analyst.service.market;

import org.junit.jupiter.api.Test;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.Bond;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondByRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondByResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetShareByRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetShareByResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.Share;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstrumentDetailsLoaderTest {
    private final TInvestClient client = mock(TInvestClient.class);
    private final Quotation priceStep = new Quotation(null, "0", 100_000_000);

    @Test
    void loadsSharePassportAndMechanicsFromSameApiCard() {
        Share share = mock(Share.class);
        when(share.uid()).thenReturn("share-uid");
        when(share.ticker()).thenReturn("SBER");
        when(share.exchange()).thenReturn("moex_mrng_evng");
        when(share.lot()).thenReturn(10);
        when(share.minPriceIncrement()).thenReturn(priceStep);
        when(share.shortEnabledFlag()).thenReturn(true);
        when(client.getShare(new GetShareByRequest(InstrumentIdType.INSTRUMENT_ID_TYPE_UID, null, "share-uid")))
                .thenReturn(new GetShareByResponse(share));

        InstrumentDetails details = new ShareDetailsLoader(client).load("share-uid");

        assertEquals(AssetType.SHARE, details.passport().assetType());
        assertEquals("moex_mrng_evng", details.passport().exchange());
        assertEquals(10, details.mechanics().lot());
        assertEquals(0.1, details.mechanics().minPriceIncrement());
        assertTrue(details.mechanics().shortEnabledFlag());
        verify(client, never()).getInstrument(any());
    }

    @Test
    void loadsBondByUidAndRejectsMismatchedCard() {
        Bond bond = mock(Bond.class);
        when(bond.uid()).thenReturn("bond-uid");
        when(bond.lot()).thenReturn(1);
        when(bond.minPriceIncrement()).thenReturn(priceStep);
        when(client.getBond(new GetBondByRequest(InstrumentIdType.INSTRUMENT_ID_TYPE_UID, null, "bond-uid")))
                .thenReturn(new GetBondByResponse(bond));

        InstrumentDetails details = new BondDetailsLoader(client).load("bond-uid");
        assertEquals(AssetType.BOND, details.passport().assetType());
        assertEquals("bond-uid", details.passport().providerId());
        assertEquals(1, details.mechanics().lot());

        when(client.getBond(new GetBondByRequest(InstrumentIdType.INSTRUMENT_ID_TYPE_UID, null, "other-uid")))
                .thenReturn(new GetBondByResponse(bond));
        assertThrows(IllegalStateException.class, () -> new BondDetailsLoader(client).load("other-uid"));
    }
}
