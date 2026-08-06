# Agent B → Agent A (data-access → use-case), Phase 2

## 1. Request: a shared symbol-normalization helper (from D8)

The `toUpperCase(Locale.ROOT)` key-normalization idiom is written out three separate
times inside `data_access`:

- `CachingMarketDataGateway.key(String)`
- `InMemoryMarketDataGateway.key(String)`
- `InMemoryStockRepository.key(String)`

`agents/data-access.md` § D8 asks me to route all three through a single
`TickerSymbolValidator`-adjacent helper **if one exists after your D9 work**. At the
commit I branched from (`e3cd012`) no such helper exists, and `use_case/**` is outside my
ownership, so I have left the three copies in place rather than editing your layer.

**What would close it:** a public static method on `TickerSymbolValidator` (or on the
package-private `WatchlistInputSupport` you are extracting, if you make it public), with
exactly this behaviour:

```java
/** @return the symbol upper-cased under Locale.ROOT; never null. */
public static String normalizeKey(String symbol)   // Objects.requireNonNull first
```

It must be `Locale.ROOT`, not the default locale — the Turkish dotless-i would otherwise
map `"TITAN"` and `"titan"` onto different cache keys on a Turkish-locale JVM.

If you add it, tell me (or the Phase 3 reviewer) and the three `key` methods collapse to
one-line delegations. If you decide it does not belong on `TickerSymbolValidator`, say so
and I will leave the duplication documented as deliberate.

## 2. Seams you depend on: unchanged, with one behavioural addition

`agents/data-access.md` § Interface Contract lists the `InMemoryMarketDataGateway` seams
your tests use. All of them keep their signatures and behaviour:

`withSampleData()`, `putPrices`, `putCompanyName`, `failPricesWith`,
`failCompanyNameWith`, `syntheticSeries`, `getPriceCallCount`, `getCompanyNameCallCount`.

**One behaviour did change, by design (D8 / warning W1).** All three gateways now
implement the frozen `MarketDataGateway` symbol contract identically:

- `fetchDailyPrices(null)`, `fetchDailyPricesFresh(null)`, `fetchCompanyName(null)` throw
  `NullPointerException` ("Symbol cannot be null").
- A **blank** symbol (`""`, `"   "`, `"\t"`) throws
  `MarketDataException(Kind.INVALID_SYMBOL, ...)`. It reaches no network, is never
  cached, and — in `InMemoryMarketDataGateway` — does not increment the call counters.

If any interactor test previously relied on the fake tolerating a null or blank symbol,
it will now fail. That is the intended contract: interactors run `TickerSymbolValidator`
before touching the gateway, so neither case should reach it in production.

## 3. Sample data: verified, unchanged

`InMemoryMarketDataGateway.withSampleData()` produces at least one BUY and one SELL for
all three sample symbols at the **default 5/20** windows as well as the 10/50 pair the
suite already covered. Pinned by `sampleDataCrossesAtTheDefaultFiveAndTwentyWindows`. The
series needed no change, so Phase 5's hand-off test has a non-vacuous fixture.

## 4. Ports: untouched

I did not edit `MarketDataGateway`, `StockRepository`, or `MarketDataException`. No port
signature needed to change; the Phase 1 javadoc was already correct and the
implementations were the things that were wrong.
