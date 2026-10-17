package ru.sewaiper.derivative.analyst.dao;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrumentPassportMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void migratesExistingPassportsToUidKey() throws Exception {
        var dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("existing.sqlite"));
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE instrument_passports (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    ticker TEXT NOT NULL,
                    asset_type TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    provider_id TEXT NOT NULL,
                    UNIQUE(provider, asset_type, ticker)
                )
                """);
        jdbc.update("""
                INSERT INTO instrument_passports (name, ticker, asset_type, provider, provider_id)
                VALUES ('Old future', 'COPPER', 'FUTURE', 'T_INVEST', 'uid-old')
                """);

        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setResourceLoader(new DefaultResourceLoader());
        liquibase.setChangeLog("classpath:migration/changelog.xml");
        liquibase.afterPropertiesSet();

        assertEquals("uid-old", jdbc.queryForObject(
                "SELECT provider_id FROM instrument_passports WHERE ticker = 'COPPER'", String.class));
        jdbc.update("""
                INSERT INTO instrument_passports (name, ticker, asset_type, provider, provider_id)
                VALUES ('New future', 'COPPER', 'FUTURE', 'T_INVEST', 'uid-new')
                """);
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM instrument_passports WHERE ticker = 'COPPER'", Integer.class));
        assertThrows(DataAccessException.class, () -> jdbc.update("""
                INSERT INTO instrument_passports (name, ticker, asset_type, provider, provider_id)
                VALUES ('Duplicate UID', 'OTHER', 'FUTURE', 'T_INVEST', 'uid-new')
                """));
    }
}
