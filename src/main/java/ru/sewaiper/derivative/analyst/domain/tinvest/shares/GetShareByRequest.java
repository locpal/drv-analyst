package ru.sewaiper.derivative.analyst.domain.tinvest.shares;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;

public record GetShareByRequest(@NonNull InstrumentIdType idType,
                                @Nullable String classCode, @NonNull String id) {
}
