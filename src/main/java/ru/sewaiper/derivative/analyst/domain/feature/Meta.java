package ru.sewaiper.derivative.analyst.domain.feature;

import java.time.Instant;

public class Meta {

    private String schemaVersion;
    private Instant generatedAtUtc;
    private String source;

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Instant getGeneratedAtUtc() {
        return generatedAtUtc;
    }

    public void setGeneratedAtUtc(Instant generatedAtUtc) {
        this.generatedAtUtc = generatedAtUtc;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
