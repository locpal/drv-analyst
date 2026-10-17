package ru.sewaiper.derivative.analyst.service.market;

import org.springframework.stereotype.Component;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetShareByRequest;
import ru.sewaiper.derivative.analyst.mapper.TInvestMapper;

import java.util.Objects;

@Component
public class ShareDetailsLoader implements InstrumentDetailsLoader {
    private final TInvestClient client;

    public ShareDetailsLoader(TInvestClient client) {
        this.client = client;
    }

    @Override
    public AssetType assetType() {
        return AssetType.SHARE;
    }

    @Override
    public InstrumentDetails load(String uid) {
        var response = Objects.requireNonNull(client.getShare(
                new GetShareByRequest(InstrumentIdType.INSTRUMENT_ID_TYPE_UID, null, uid)), "getShare response");
        var share = Objects.requireNonNull(response.instrument(), "getShare instrument");
        if (!uid.equals(share.uid())) {
            throw new IllegalStateException("T-Invest returned a different share UID");
        }
        return new InstrumentDetails(TInvestMapper.toPassport(share), TInvestMapper.mapShareMechanics(share));
    }
}
