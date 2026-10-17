package ru.sewaiper.derivative.analyst.service.market;

import org.springframework.stereotype.Component;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.GetFutureRequest;
import ru.sewaiper.derivative.analyst.mapper.TInvestMapper;

import java.util.Objects;

@Component
public class FutureDetailsLoader implements InstrumentDetailsLoader {
    private final TInvestClient client;

    public FutureDetailsLoader(TInvestClient client) {
        this.client = client;
    }

    @Override
    public AssetType assetType() {
        return AssetType.FUTURE;
    }

    @Override
    public InstrumentDetails load(String uid) {
        var response = Objects.requireNonNull(client.getFuture(
                new GetFutureRequest(uid, InstrumentIdType.INSTRUMENT_ID_TYPE_UID)), "getFuture response");
        var future = Objects.requireNonNull(response.instrument(), "getFuture instrument");
        if (!uid.equals(future.uid())) {
            throw new IllegalStateException("T-Invest returned a different future UID");
        }
        return new InstrumentDetails(TInvestMapper.toPassport(future), TInvestMapper.mapFutureMechanics(future));
    }
}
