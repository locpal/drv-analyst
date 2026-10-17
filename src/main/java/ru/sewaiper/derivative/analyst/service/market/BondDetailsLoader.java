package ru.sewaiper.derivative.analyst.service.market;

import org.springframework.stereotype.Component;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondByRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;
import ru.sewaiper.derivative.analyst.mapper.TInvestMapper;

import java.util.Objects;

@Component
public class BondDetailsLoader implements InstrumentDetailsLoader {
    private final TInvestClient client;

    public BondDetailsLoader(TInvestClient client) {
        this.client = client;
    }

    @Override
    public AssetType assetType() {
        return AssetType.BOND;
    }

    @Override
    public InstrumentDetails load(String uid) {
        var response = Objects.requireNonNull(client.getBond(
                new GetBondByRequest(InstrumentIdType.INSTRUMENT_ID_TYPE_UID, null, uid)), "getBond response");
        var bond = Objects.requireNonNull(response.instrument(), "getBond instrument");
        if (!uid.equals(bond.uid())) {
            throw new IllegalStateException("T-Invest returned a different bond UID");
        }
        return new InstrumentDetails(TInvestMapper.toPassport(bond), TInvestMapper.mapBondMechanics(bond));
    }
}
