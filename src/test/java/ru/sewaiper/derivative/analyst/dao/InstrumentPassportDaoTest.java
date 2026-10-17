package ru.sewaiper.derivative.analyst.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.sqlite.SQLiteDataSource;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrumentPassportDaoTest {

    @TempDir
    Path tempDir;

    @Test
    void replacesOnlySelectedTypeAndRollsBackFailedReplacement() {
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("dictionary.sqlite"));

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(TransactionConfiguration.class);
            context.registerBean(DataSource.class, () -> dataSource);
            context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource));
            context.registerBean(PlatformTransactionManager.class,
                    () -> new DataSourceTransactionManager(dataSource));
            context.registerBean(InstrumentPassportDao.class);
            context.refresh();

            var jdbc = context.getBean(JdbcTemplate.class);
            jdbc.execute("""
                    CREATE TABLE instrument_passports (
                        id INTEGER PRIMARY KEY,
                        ticker TEXT NOT NULL,
                        name TEXT,
                        asset_type TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        provider_id TEXT NOT NULL,
                        figi TEXT,
                        exchange TEXT,
                        UNIQUE(provider, provider_id)
                    )
                    """);
            var dao = context.getBean(InstrumentPassportDao.class);
            var oldFuture = passport(AssetType.FUTURE, "OLD");
            var newFuture = passport(AssetType.FUTURE, "NEW");
            var sameTicker = new InstrumentPassport("NEW", "Another NEW", AssetType.FUTURE,
                    Provider.T_INVEST, "uid-OTHER", "figi-OTHER", "MOEX");
            var share = passport(AssetType.SHARE, "SHARE");
            dao.replacePassports(Provider.T_INVEST, AssetType.FUTURE, List.of(oldFuture));
            dao.replacePassports(Provider.T_INVEST, AssetType.SHARE, List.of(share));

            dao.replacePassports(Provider.T_INVEST, AssetType.FUTURE, List.of(newFuture, sameTicker));
            assertEquals(List.of(newFuture, sameTicker), dao.findByType(Provider.T_INVEST, AssetType.FUTURE));
            assertEquals(newFuture, dao.findByProviderId(Provider.T_INVEST, AssetType.FUTURE,
                    newFuture.providerId()).orElseThrow());
            assertEquals(sameTicker, dao.findByProviderId(Provider.T_INVEST, AssetType.FUTURE,
                    sameTicker.providerId()).orElseThrow());
            assertEquals(List.of(share), dao.findByType(Provider.T_INVEST, AssetType.SHARE));

            var duplicateUid = new InstrumentPassport("OTHER", "Other", AssetType.FUTURE,
                    Provider.T_INVEST, newFuture.providerId(), "figi-OTHER", "MOEX");
            assertThrows(DataAccessException.class, () -> dao.replacePassports(
                    Provider.T_INVEST, AssetType.FUTURE, List.of(newFuture, duplicateUid)));
            assertEquals(List.of(newFuture, sameTicker), dao.findByType(Provider.T_INVEST, AssetType.FUTURE));
            assertEquals(List.of(share), dao.findByType(Provider.T_INVEST, AssetType.SHARE));

            dao.replacePassports(Provider.T_INVEST, AssetType.FUTURE, List.of());
            assertEquals(List.of(), dao.findByType(Provider.T_INVEST, AssetType.FUTURE));
            assertEquals(List.of(share), dao.findByType(Provider.T_INVEST, AssetType.SHARE));
        }
    }

    private InstrumentPassport passport(AssetType assetType, String ticker) {
        return new InstrumentPassport(ticker, ticker, assetType, Provider.T_INVEST,
                "uid-" + ticker, "figi-" + ticker, "MOEX");
    }

    @Configuration
    @EnableTransactionManagement
    static class TransactionConfiguration {
    }
}
