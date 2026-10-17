Режим: ENTRY.
Проанализируй данные и скажи, есть ли сейчас разумные условия для входа (ALLOW_ENTRY / WAIT / NO_TRADE).
Если ALLOW_ENTRY — опиши 1–2 сценария входа как условия (триггеры), а не как приказ “покупай/продавай”.

Мой стиль: SWING

Данные:
```json
{
    "meta": {
        "schemaVersion": "futures_v1",
        "generatedAtUtc": "2026-07-13T22:07:15.120279Z",
        "source": "broker_api_sandbox"
    },
    "instrument": {
        "uid": "ac958c01-0d6a-4dc0-a263-3a8f9e87def1",
        "figi": "FCOPPER09260",
        "ticker": "CEU6",
        "classCode": "SPBFUT",
        "name": "COPPER-9.26 Медь",
        "realExchange": "REAL_EXCHANGE_MOEX",
        "currency": "rub",
        "assetType": "TYPE_COMMODITY",
        "basicAsset": "Медь",
        "basicAssetSize": 10.000000000,
        "settlementType": null,
        "lot": 1,
        "tickSize": 1.000000000,
        "tickValue": 0.766210000,
        "firstTradeDate": "2026-03-04T07:00:00Z",
        "lastTradeDate": "2026-09-15T11:00:00Z",
        "expirationDate": "2026-09-16T00:00:00Z",
        "margins": {
            "initialMarginBuy": 1299.840000000,
            "initialMarginSell": 1326.140000000
        },
        "availability": {
            "apiTradeAvailable": true,
            "buyAvailable": true,
            "sellAvailable": true,
            "shortEnabled": true,
            "tradingStatus": "SECURITY_TRADING_STATUS_SESSION_ASSIGNED",
            "requiredTests": [
                "derivative"
            ],
            "forQualInvestorOnly": false
        },
        "market": {
            "asOfUtc": "2026-07-13T20:48:08.895824431Z",
            "lastPrice": 13599.0,
            "closePrice": 13599.0,
            "closeTimeUtc": "2026-07-13T20:50:00Z",
            "eveningSessionPrice": 13601.0,
            "eveningTimeUtc": "2026-07-12T15:44:26.845419Z",
            "openInterest": 133576,
            "openInterestTimeUtc": "2026-07-13T20:48:08.895824431Z"
        },
        "atr": {
            "atrPts": 193.68,
            "atrPct": 1.42,
            "atrSlopePct": -11.44,
            "isHigh": false,
            "isIncreasing": false,
            "avgWindow": 28,
            "slopeLookback": 7
        },
        "ema": {
            "emaFast": 13591.26,
            "emaSlow": 13647.84,
            "spreadPct": -0.41,
            "priceDeviationPct": 0.07,
            "slopePct": -0.52,
            "slopePts": -71.09,
            "slopePtsPerBar": -7.1093,
            "trend": "MIXED",
            "fastPeriod": 20,
            "slowPeriod": 50,
            "slopeLookback": 10,
            "completeCandlesCount": 114,
            "lastCompleteCandleTimeUtc": "2026-07-12T00:00:00Z"
        },
        "liquidity": {
            "asOfUtc": "2026-07-13T20:48:08.895824431Z",
            "marketOpen": true,
            "reason": null,
            "bestBid": 13550.0,
            "bestAsk": 13687.0,
            "bestBidQty": 6.0,
            "bestAskQty": 60.0,
            "spreadPts": 137.0,
            "spreadMoney": 104.97076999999999,
            "topN": 20,
            "topDepthBidQty": 1835.0,
            "topDepthAskQty": 1824.0,
            "imbalance": "0.0030"
        }
    }
}
```