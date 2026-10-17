package ru.sewaiper.derivative.analyst.domain.tinvest.dfa;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;

public record GetDfaByRequest(@NonNull InstrumentIdType idType,
                              @Nullable String classCode, @NonNull String id) {
}
