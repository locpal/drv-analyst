package ru.sewaiper.derivative.analyst.domain.analysis;

import java.util.UUID;

public record StartAnalysisResponse(UUID id, AnalysisStatus status) {
}
