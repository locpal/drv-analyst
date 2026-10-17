package ru.sewaiper.derivative.analyst.domain.tinvest.bonds;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import ru.sewaiper.derivative.analyst.domain.tinvest.enums.InstrumentIdType;

public record GetBondByRequest(@NonNull InstrumentIdType idType,
                               @Nullable String classCode, @NonNull String id) {
}
