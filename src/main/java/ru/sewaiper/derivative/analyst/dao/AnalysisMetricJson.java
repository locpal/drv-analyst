package ru.sewaiper.derivative.analyst.dao;

import org.ta4j.core.num.Num;
import ru.sewaiper.derivative.analyst.domain.market.tech.MetricName;
import ru.sewaiper.derivative.analyst.domain.market.tech.TechMetrics;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Stable JSON facts for persistence and the read API; Num values are exact decimal strings. */
public final class AnalysisMetricJson {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private AnalysisMetricJson() {
    }

    public static String encode(Map<MetricName, TechMetrics> metrics) {
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("Analysis has no calculated metrics");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        metrics.forEach((name, metric) -> {
            if (metric == null || name != metric.type()) {
                throw new IllegalArgumentException("Metric key does not match its type: " + name);
            }
            values.put(name.name(), normalize(metric));
        });
        return JSON.writeValueAsString(values);
    }

    public static JsonNode decode(String json) {
        return JSON.readTree(json);
    }

    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(AnalysisMetricJson::normalize).orElse(null);
        }
        if (value instanceof Num number) {
            if (!Num.isValid(number)) {
                throw new IllegalArgumentException("Cannot persist an invalid indicator number");
            }
            return number.bigDecimalValue().toPlainString();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value.getClass().isRecord()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (RecordComponent component : value.getClass().getRecordComponents()) {
                try {
                    fields.put(component.getName(), normalize(component.getAccessor().invoke(value)));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read metric field " + component.getName(), e);
                }
            }
            return fields;
        }
        throw new IllegalArgumentException("Unsupported metric field type: " + value.getClass());
    }
}
