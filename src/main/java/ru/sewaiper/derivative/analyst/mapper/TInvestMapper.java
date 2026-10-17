package ru.sewaiper.derivative.analyst.mapper;

import ru.sewaiper.derivative.analyst.domain.market.TradingMechanics;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.PriceLevel;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.Bond;
import ru.sewaiper.derivative.analyst.domain.tinvest.Quotation;
import ru.sewaiper.derivative.analyst.domain.tinvest.dfa.Dfa;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.Future;
import ru.sewaiper.derivative.analyst.domain.tinvest.market.OrderBookValue;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.Share;

public final class TInvestMapper {

    private TInvestMapper() {}

    public static PriceLevel mapPriceLevel(OrderBookValue value) {
        return new PriceLevel(value.price().asBigDecimal(), Long.parseLong(value.quantity()));
    }

    public static TradingMechanics mapShareMechanics(Share share) {
        return new TradingMechanics(
                share.lot(), share.minPriceIncrement().asDouble(), share.currency(),
                Boolean.TRUE.equals(share.shortEnabledFlag()),
                Boolean.TRUE.equals(share.apiTradeAvailableFlag()),
                Boolean.TRUE.equals(share.forQualInvestorFlag()),
                nullableDouble(share.klong()), nullableDouble(share.kshort()),
                null, null, null
        );
    }

    public static TradingMechanics mapBondMechanics(Bond bond) {
        return new TradingMechanics(
                bond.lot(), bond.minPriceIncrement().asDouble(), bond.currency(),
                Boolean.TRUE.equals(bond.shortEnabledFlag()),
                Boolean.TRUE.equals(bond.apiTradeAvailableFlag()),
                Boolean.TRUE.equals(bond.forQualInvestorFlag()),
                nullableDouble(bond.klong()), nullableDouble(bond.kshort()),
                null, null, null
        );
    }

    private static Double nullableDouble(Quotation quotation) {
        return quotation == null ? null : quotation.asDouble();
    }

    public static TradingMechanics mapFutureMechanics(Future future) {
        return new TradingMechanics(
                future.lot(),
                future.minPriceIncrement().asDouble(),
                future.currency(),

                future.shortEnabledFlag(),
                future.apiTradeAvailableFlag(),
                future.forQualInvestorFlag(),

                // Ставки риска (kLong/kShort) для фьючерсов не применимы,
                // так как "плечо" заложено в само Гарантийное Обеспечение (ГО).
                null,
                null,

                // А вот это самое важное для расчета позиций по фьючерсам:
                future.minPriceIncrementAmount().asDouble(),
                future.initialMarginOnBuy().asDouble(),
                future.initialMarginOnSell().asDouble()
        );
    }

    public static InstrumentPassport toPassport(Share share) {
        return new InstrumentPassport(
                share.ticker(), share.name(), AssetType.SHARE, Provider.T_INVEST,
                share.uid(), share.figi(), share.exchange());
    }

    public static InstrumentPassport toPassport(Bond bond) {
        return new InstrumentPassport(
                bond.ticker(), bond.name(), AssetType.BOND, Provider.T_INVEST,
                bond.uid(), bond.figi(), bond.exchange());
    }

    public static InstrumentPassport toPassport(Future future) {
        return new InstrumentPassport(
                future.ticker(), future.name(), AssetType.FUTURE, Provider.T_INVEST,
                future.uid(), future.figi(), future.exchange());
    }

    public static InstrumentPassport toPassport(Dfa dfa) {
        return new InstrumentPassport(
                dfa.ticker(), dfa.name(), AssetType.DFA, Provider.T_INVEST,
                dfa.uid(), null, null);
    }
}
