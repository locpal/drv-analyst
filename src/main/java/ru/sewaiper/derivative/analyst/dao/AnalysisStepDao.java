package ru.sewaiper.derivative.analyst.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStep;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AnalysisStepDao {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcTemplate jdbc;

    public AnalysisStepDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long start(UUID analysisId, String name, Object input) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO analysis_steps (analysis_id, step_name, status, started_at_utc, input_json)
                    VALUES (?, ?, 'STARTED', ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, analysisId.toString());
            statement.setString(2, name);
            statement.setString(3, Instant.now().toString());
            statement.setString(4, JSON.writeValueAsString(input));
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public void complete(long stepId, Object output) {
        update("""
                UPDATE analysis_steps SET status = 'COMPLETED', finished_at_utc = ?, output_json = ?
                WHERE id = ? AND status = 'STARTED'
                """, Instant.now().toString(), JSON.writeValueAsString(output), stepId);
    }

    public void fail(long stepId, Exception error) {
        update("""
                UPDATE analysis_steps SET status = 'FAILED', finished_at_utc = ?, error_message = ?
                WHERE id = ? AND status = 'STARTED'
                """, Instant.now().toString(), error.getClass().getSimpleName() + ": " + error.getMessage(), stepId);
    }

    public void failInterruptedSteps(Instant finishedAt) {
        jdbc.update("""
                UPDATE analysis_steps SET status = 'FAILED', finished_at_utc = ?,
                    error_message = 'Analysis interrupted by application restart'
                WHERE status = 'STARTED'
                """, finishedAt.toString());
    }

    public List<AnalysisStep> findByAnalysisId(UUID analysisId) {
        return jdbc.query("""
                SELECT * FROM analysis_steps WHERE analysis_id = ? ORDER BY id
                """, (rs, row) -> new AnalysisStep(rs.getLong("id"), rs.getString("step_name"),
                rs.getString("status"), Instant.parse(rs.getString("started_at_utc")),
                parseInstant(rs.getString("finished_at_utc")), read(rs.getString("input_json")),
                read(rs.getString("output_json")), rs.getString("error_message")), analysisId.toString());
    }

    private JsonNode read(String value) {
        return value == null ? null : JSON.readTree(value);
    }

    private Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private void update(String sql, Object... arguments) {
        if (jdbc.update(sql, arguments) != 1) {
            throw new IllegalStateException("Analysis step is missing or already finished");
        }
    }
}
