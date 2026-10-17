package ru.sewaiper.derivative.analyst.domain.market;

import ru.sewaiper.derivative.analyst.domain.analysis.SkippableStep;
import java.util.Set;

public record SnapshotDepth(int orderBookDepth,
                            Set<SkippableStep> skip) {
    public SnapshotDepth {
        skip = Set.copyOf(skip);
    }

    public SnapshotDepth(int orderBookDepth) {
        this(orderBookDepth, Set.of());
    }
}
