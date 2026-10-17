package ru.sewaiper.derivative.analyst.service.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.clients.TInvestClient;
import ru.sewaiper.derivative.analyst.dao.InstrumentPassportDao;
import ru.sewaiper.derivative.analyst.domain.InstrumentPage;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;
import ru.sewaiper.derivative.analyst.domain.tinvest.bonds.GetBondsRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.dfa.GetDfasRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentExchange;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentStatus;
import ru.sewaiper.derivative.analyst.domain.tinvest.futures.GetFuturesRequest;
import ru.sewaiper.derivative.analyst.domain.tinvest.shares.GetSharesRequest;
import ru.sewaiper.derivative.analyst.mapper.TInvestMapper;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TInvestMarketDictionary implements MarketDictionary {

    private static final Logger log = LoggerFactory.getLogger(TInvestMarketDictionary.class);
    private static final Set<AssetType> SUPPORTED_ASSET_TYPES = Collections.unmodifiableSet(
            EnumSet.of(AssetType.SHARE, AssetType.FUTURE, AssetType.BOND, AssetType.DFA));

    private final TInvestClient investClient;
    private final InstrumentPassportDao passportDao;

    public TInvestMarketDictionary(TInvestClient investClient,
                                   InstrumentPassportDao passportDao) {
        this.investClient = investClient;
        this.passportDao = passportDao;
    }

    @Override
    public Set<AssetType> supportedAssetTypes() {
        return SUPPORTED_ASSET_TYPES;
    }

    @Override
    public void sync(AssetType assetType) {
        if (!SUPPORTED_ASSET_TYPES.contains(assetType)) {
            throw new IllegalArgumentException("Dictionary synchronization is unavailable for " + assetType);
        }
        log.info("Synchronizing {} passports", assetType);
        try {
            switch (assetType) {
                case BOND -> syncBonds();
                case SHARE -> syncShares();
                case FUTURE -> syncFutures();
                case DFA -> syncDfas();
                default -> throw new IllegalArgumentException("Dictionary synchronization is unavailable for " + assetType);
            }
            log.info("Synchronized {} passports", assetType);
        } catch (RuntimeException exception) {
            log.error("Unable to synchronize {} passports", assetType, exception);
            throw exception;
        }
    }

    @Override
    public InstrumentPassport getPassport(Provider provider, AssetType assetType, String providerId) {
        return passportDao.findByProviderId(provider, assetType, providerId).orElseThrow(() ->
                new IllegalArgumentException(
                        "Instrument " + providerId + "[" + assetType + "] from provider " + provider + " not found"));
    }

    @Override
    public InstrumentPage search(AssetType assetType, String query, int page, int size) {
        if (assetType == null || query == null || page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Invalid instrument search parameters");
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        List<InstrumentPassport> matches = passportDao.findByType(Provider.T_INVEST, assetType).stream()
                .filter(passport -> matches(passport, needle))
                .sorted(Comparator.comparingInt((InstrumentPassport passport) -> rank(passport, needle))
                        .thenComparing(InstrumentPassport::ticker)
                        .thenComparing(InstrumentPassport::providerId))
                .toList();
        long totalElements = matches.size();
        long offset = (long) page * size;
        List<InstrumentPassport> items = matches.stream().skip(offset).limit(size).toList();
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new InstrumentPage(items, page, size, totalElements, totalPages);
    }

    private boolean matches(InstrumentPassport passport, String needle) {
        return normalized(passport.ticker()).contains(needle)
                || normalized(passport.name()).contains(needle)
                || normalized(passport.figi()).contains(needle)
                || normalized(passport.providerId()).contains(needle);
    }

    private int rank(InstrumentPassport passport, String needle) {
        String ticker = normalized(passport.ticker());
        String name = normalized(passport.name());
        if (ticker.equals(needle)) return 0;
        if (ticker.startsWith(needle)) return 1;
        if (ticker.contains(needle)) return 2;
        if (name.startsWith(needle)) return 3;
        return 4;
    }

    private String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void syncBonds() {
        var request = new GetBondsRequest(
                InstrumentStatus.INSTRUMENT_STATUS_BASE,
                InstrumentExchange.INSTRUMENT_EXCHANGE_UNSPECIFIED
        );

        List<InstrumentPassport> passports = investClient.getBonds(request)
                .instruments()
                .stream()
                .map(TInvestMapper::toPassport)
                .toList();

        passportDao.replacePassports(Provider.T_INVEST, AssetType.BOND, passports);
    }

    private void syncShares() {
        var request = new GetSharesRequest(
                InstrumentStatus.INSTRUMENT_STATUS_BASE,
                InstrumentExchange.INSTRUMENT_EXCHANGE_UNSPECIFIED
        );

        List<InstrumentPassport> passports = investClient.getShares(request)
                .instruments()
                .stream()
                .map(TInvestMapper::toPassport)
                .toList();

        passportDao.replacePassports(Provider.T_INVEST, AssetType.SHARE, passports);
    }

    private void syncFutures() {
        var request = new GetFuturesRequest(
                InstrumentStatus.INSTRUMENT_STATUS_BASE,
                InstrumentExchange.INSTRUMENT_EXCHANGE_UNSPECIFIED
        );

        List<InstrumentPassport> passports = investClient.getFutures(request)
                .instruments()
                .stream()
                .map(TInvestMapper::toPassport)
                .toList();

        passportDao.replacePassports(Provider.T_INVEST, AssetType.FUTURE, passports);
    }

    private void syncDfas() {
        var request = new GetDfasRequest();

        List<InstrumentPassport> passports = investClient.getDfas(request)
                .instruments()
                .stream()
                .map(TInvestMapper::toPassport)
                .toList();

        passportDao.replacePassports(Provider.T_INVEST, AssetType.DFA, passports);
    }
}
