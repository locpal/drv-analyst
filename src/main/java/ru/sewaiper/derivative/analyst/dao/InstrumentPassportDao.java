package ru.sewaiper.derivative.analyst.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.domain.market.enums.Provider;
import ru.sewaiper.derivative.analyst.domain.market.snapshot.InstrumentPassport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Repository
public class InstrumentPassportDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public InstrumentPassportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new  NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public Optional<InstrumentPassport> findByProviderId(Provider provider,
                                                         AssetType assetType, String providerId) {

        //language=sqlite
        String query = """
                SELECT
                    *
                FROM instrument_passports WHERE provider   = :provider AND
                                                asset_type = :assetType AND
                                                provider_id = :providerId
                """;

        var params = new MapSqlParameterSource()
                .addValue("provider", provider.name())
                .addValue("assetType", assetType.name())
                .addValue("providerId", providerId);

        return jdbcTemplate.query(query, params, this::toPassport).stream().findFirst();
    }

    public List<InstrumentPassport> findByType(Provider provider, AssetType assetType) {
        String query = """
                SELECT * FROM instrument_passports
                WHERE provider = :provider AND asset_type = :assetType
                """;
        var params = new MapSqlParameterSource()
                .addValue("provider", provider.name())
                .addValue("assetType", assetType.name());
        return jdbcTemplate.query(query, params, this::toPassport);
    }

    @Transactional
    public void replacePassports(Provider provider, AssetType assetType, List<InstrumentPassport> passports) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(assetType, "assetType");
        Objects.requireNonNull(passports, "passports");
        if (passports.stream().anyMatch(passport -> passport == null
                || passport.provider() != provider || passport.assetType() != assetType)) {
            throw new IllegalArgumentException("Passports must belong to " + provider + " / " + assetType);
        }

        String deleteQuery = """
                DELETE FROM instrument_passports
                WHERE provider = :provider AND asset_type = :assetType
                """;
        var deleteParams = new MapSqlParameterSource()
                .addValue("provider", provider.name())
                .addValue("assetType", assetType.name());
        jdbcTemplate.update(deleteQuery, deleteParams);

        //language=sqlite
        String query = """
                insert into instrument_passports (ticker, name, asset_type, provider, provider_id, figi, exchange)
                values (:ticker, :name, :assetType, :provider, :providerId, :figi, :exchange)
                """;

        SqlParameterSource[] parameters = passports.stream()
                .map(this::toParameters)
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(query, parameters);
    }

    private SqlParameterSource toParameters(InstrumentPassport passport) {
        return new MapSqlParameterSource()
                .addValue("ticker", passport.ticker())
                .addValue("name", passport.name())
                .addValue("assetType", passport.assetType().name())
                .addValue("provider", passport.provider().name())
                .addValue("providerId", passport.providerId())
                .addValue("figi", passport.figi())
                .addValue("exchange", passport.exchange());
    }

    private InstrumentPassport toPassport(ResultSet rs, int rowNum) throws SQLException {
        String ticker = rs.getString("ticker");
        String name = rs.getString("name");
        String assetType = rs.getString("asset_type");
        String provider = rs.getString("provider");
        String providerId = rs.getString("provider_id");
        String figi = rs.getString("figi");
        String exchange = rs.getString("exchange");
        return new InstrumentPassport(
                ticker,
                name,
                AssetType.valueOf(assetType),
                Provider.valueOf(provider),
                providerId,
                figi,
                exchange
        );
    }
}
