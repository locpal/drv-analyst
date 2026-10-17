package ru.sewaiper.derivative.analyst.domain.analysis;

import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;

import java.util.Set;

public record StartAnalysisRequest(String providerId,
                                   AssetType assetType,
                                   Set<SkippableStep> skip) {
    public StartAnalysisRequest {
        skip = skip == null ? Set.of() : Set.copyOf(skip);
    }

    public StartAnalysisRequest(String providerId, AssetType assetType) {
        this(providerId, assetType, Set.of());
    }
}
