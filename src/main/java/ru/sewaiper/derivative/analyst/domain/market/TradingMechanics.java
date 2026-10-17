package ru.sewaiper.derivative.analyst.domain.market;

/**
 * Механика исполнения (Универсальная для расчета рисков).
 *
 */
public record TradingMechanics(
        // Базовые параметры (обязательны для всех инструментов)
        int lot,
        double minPriceIncrement,
        String currency,

        // Торговые флаги и ограничения
        boolean shortEnabledFlag,
        boolean apiTradeAvailableFlag,
        boolean forQualInvestorFlag,

        // Маржинальные показатели (Актуально для акций и облигаций)
        Double kLong,
        Double kShort,

        // Специфичные параметры производных инструментов (Фьючерсы)
        Double minPriceIncrementAmount, // Стоимость шага цены
        Double initialMarginOnBuy,      // ГО покупателя
        Double initialMarginOnSell      // ГО продавца
) {

    /**
     * Вспомогательный метод для проверки, является ли инструмент фьючерсом,
     * опираясь на наличие стоимости шага цены.
     */
    public boolean isDerivative() {
        return minPriceIncrementAmount != null;
    }
}
