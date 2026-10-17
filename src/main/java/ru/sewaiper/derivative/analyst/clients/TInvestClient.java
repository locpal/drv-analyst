package ru.sewaiper.derivative.analyst.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.sewaiper.derivative.analyst.config.TInvestConfiguration;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondByRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondByResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondsRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondsResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.dfa.Dfa;
import ru.sewaiper.derivative.analyst.domain.tinvest.dfa.GetDfaByRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.dfa.GetDfasRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.dfa.GetDfasResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.GetFutureRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.GetFutureResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.candles.GetCandlesResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.GetFuturesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.GetFuturesResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.instrument.GetInstrumentRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.instrument.GetInstrumentResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.MarketValuesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.MarketValuesResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.OrderBookRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.OrderBookResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.TradingSchedulesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.schedule.TradingSchedulesResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetShareByRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetShareByResponse;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetSharesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetSharesResponse;

@FeignClient(
        name = "t-invest",
        url = "${clients.t-invest.url}",
        configuration = TInvestConfiguration.class
)
public interface TInvestClient {

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/TradingSchedules")
    TradingSchedulesResponse getTradingSchedules(@RequestBody TradingSchedulesRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/GetInstrumentBy")
    GetInstrumentResponse getInstrument(@RequestBody GetInstrumentRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/Bonds")
    GetBondsResponse getBonds(@RequestBody GetBondsRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/Dfas")
    GetDfasResponse getDfas(@RequestBody GetDfasRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/Futures")
    GetFuturesResponse getFutures(@RequestBody GetFuturesRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/Shares")
    GetSharesResponse getShares(@RequestBody GetSharesRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/BondBy")
    GetBondByResponse getBond(@RequestBody GetBondByRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/DfaBy")
    Dfa getDfa(@RequestBody GetDfaByRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/ShareBy")
    GetShareByResponse getShare(@RequestBody GetShareByRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.InstrumentsService/FutureBy")
    GetFutureResponse getFuture(@RequestBody GetFutureRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.MarketDataService/GetMarketValues")
    MarketValuesResponse getMarketValues(@RequestBody MarketValuesRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.MarketDataService/GetOrderBook")
    OrderBookResponse getOrderBook(@RequestBody OrderBookRequest request);

    @PostMapping(path = "tinkoff.public.invest.api.contract.v1.MarketDataService/GetCandles")
    GetCandlesResponse getCandles(@RequestBody GetCandlesRequest request);
}
