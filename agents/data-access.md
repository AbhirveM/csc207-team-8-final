# Agent B — data-access

**Role:** Own the gateway implementations — make the Alpha Vantage DAO's behaviour match
its documentation, make the cache safe to use from the background thread the design
depends on, and make the tests prove things rather than assert tautologies.

---

## Owns

- `src/main/java/data_access/**`
- `src/test/java/data_access/**`
- `src/test/resources/alphavantage/**`

Exceptions — Member 4's, read-only to this agent:
- `src/main/java/data_access/FileWatchlistDataAccessObject.java`
- `src/test/java/data_access/FileWatchlistDataAccessObjectTest.java`

So this agent owns: `AlphaVantageMarketDataAccessObject`, `CachingMarketDataGateway`,
`InMemoryMarketDataGateway`, `InMemoryStockRepository`, `HttpJsonClient`,
`JdkHttpJsonClient`, their tests, `JsonFixtures`, `StubHttpJsonClient`, and all 16 JSON
fixtures.

---

## Never Touch

- `src/main/java/use_case/**` and `src/test/java/use_case/**` — Agent A and teammates.
  **In particular `MarketDataGateway` and `StockRepository`**: you implement those ports,
  you do not edit them. The orchestrator owns the port javadoc.
- `src/main/java/interface_adapter/**` and `src/test/java/interface_adapter/**` — Agent C
- `src/main/java/view/**` — Agent D and Member 4
- `src/main/java/app/Main.java` — orchestrator
- `src/main/java/entity/**` — frozen
- `data_access/FileWatchlistDataAccessObject.java` and its test — Member 4
- `pom.xml` — orchestrator. **You need no new dependency**: `org.json:json:20240303` is
  already a compile-scope dependency and the JDK supplies the HTTP client.
- `plan/**`, `agents/**`

If a port signature genuinely needs to change, write to
`plan/handoffs/agent-b-request.md` and stop.

---

## Reads (never writes)

- `agents/orchestrator.md` §5.1 (the null/blank contract you must implement), §7, §8
- `vision.md` §5 principles 6, 7, 8 and §7 (constraints)
- `use_case/watchlist/MarketDataGateway.java` — the frozen port, including its new
  normative javadoc
- `use_case/watchlist/StockRepository.java` and `MarketDataException.java`
- `entity/Stock.java`, `entity/DailyPrice.java`
- `use_case/persistence/SaveWatchlistTest.java` — the test style reference

---

## Interface Contract

### Inputs — what the orchestrator provides before you start

- `MarketDataGateway` with the normative null/blank contract (orchestrator §5.1). This is
  the one behavioural change you must propagate into **all three** implementations:
  - `Objects.requireNonNull(normalizedSymbol, "Symbol cannot be null")`
  - a blank symbol → `MarketDataException(Kind.INVALID_SYMBOL, ...)`, no network call, no
    cache entry
- `StockRepository`, `MarketDataException` — unchanged.

### Outputs — what other components depend on

- **Agent A's tests** depend on `InMemoryMarketDataGateway`'s fluent seams staying
  available and behaviourally stable: `withSampleData()`, `putPrices`, `putCompanyName`,
  `failPricesWith`, `failCompanyNameWith`, `syntheticSeries`, and the call counters. If
  you change any of them, say so in `plan/handoffs/agent-b-to-a.md`.
- **The orchestrator (Phase 4)** depends on
  `AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()` (currently has no
  production caller — Phase 4 becomes its first), the public
  `AlphaVantageMarketDataAccessObject(String apiKey)` constructor, the
  `CachingMarketDataGateway(MarketDataGateway)` constructor, and
  `InMemoryMarketDataGateway.withSampleData()`.
- **Agent A also depends** on the sample data genuinely oscillating. A flat series yields
  nothing but HOLD signals and makes both the demo and the Phase 5 hand-off test vacuous
  (`vision.md` principle 8). If `withSampleData()`'s series does not produce at least one
  moving-average crossover at the default 5/20 windows, fix it.

---

## Phase Tasks

### Phase 2 (primary)

One commit per defect, with the ID in the message.

**D1 — javadoc contradicts code on the `{}` OVERVIEW body.**
`parseCompanyName`'s javadoc says an unknown symbol yields `{}` and is *"reported as an
absent name rather than an error"*. But the code calls `rejectProviderError` first, whose
empty-root check throws `EMPTY_RESPONSE`. So `fetchCompanyName("ZZZZ")` throws instead of
returning `Optional.empty()`. **Make the code match the javadoc** — the documented
behaviour is the right one, because a missing company name must never block adding a
ticker, and `OVERVIEW` is the first endpoint the quota kills. Add
`overview_unknown_symbol.json` (a bare `{}`) and a test.

**D2 — mutable list aliased into the cache.**
`parseDailyPrices` returns a live `ArrayList` (both at the daily-series site and the
company-name site's sibling path). `CachingMarketDataGateway.storeAndReturn` stores and
returns that same reference, so any caller that sorts or clears the list corrupts the
cache for every subsequent caller. Return `List.copyOf(prices)` from the DAO and store
the immutable list in the cache.

`CachingMarketDataGatewayTest:125` currently asserts `assertSame(refreshed, cached)`,
which **enshrines this bug as intended behaviour**. Replace it with an `assertEquals` on
contents plus an
`assertThrows(UnsupportedOperationException.class, () -> result.add(...))` proving the
returned list is unmodifiable.

**D3 — the cache is not thread-safe.**
Both maps are plain `HashMap` with non-atomic check-then-put, yet `WatchlistSnapshot`'s
own javadoc argues the Swing view refreshes on a background thread — the design
explicitly anticipates concurrency the cache cannot survive. This is the most serious
internal inconsistency in the vertical. Fix all four parts:
- `ConcurrentHashMap` for both the price cache and the company-name cache.
- Reject a non-positive `Duration` in the constructor with `IllegalArgumentException`
  (a zero TTL silently turns the cache into a no-op today). Add a test.
- Bound the company-name cache, which is currently unbounded and never expires. A
  `MAX_NAME_ENTRIES = 64` with clear-on-overflow is sufficient and testable.
- Evict expired price entries on access, so `getCachedSymbolCount()` stops counting dead
  entries — its current name promises valid entries and it does not deliver them.

**D8 — implement the null/blank symbol contract** in all three implementations, per
orchestrator §5.1. Today `AlphaVantageMarketDataAccessObject` NPEs out of
`URLEncoder`, `CachingMarketDataGateway` caches under `""`, and
`InMemoryMarketDataGateway` returns `INVALID_SYMBOL` — three implementations of one port,
three behaviours. Add the same triple of tests to each implementation's test class.

While you are there: the `toUpperCase(Locale.ROOT)` key-normalization idiom is written out
three separate times (`CachingMarketDataGateway.key`, `InMemoryMarketDataGateway.key`,
`InMemoryStockRepository.key`). Route them all through a single
`TickerSymbolValidator`-adjacent helper if one exists after Agent A's D9 work; if not,
leave a note in `plan/handoffs/agent-b-to-a.md` rather than editing `use_case`.

**D12 — dead code.**
- `CachingMarketDataGateway.clear()` and `getCachedSymbolCount()` are called only by
  tests. **Demote to package-private** rather than deleting — the test lives in the same
  package, and they are legitimate test seams.
- `StubHttpJsonClient.getRequestedUrls()` has no caller. D13's rewritten URL test is its
  natural first user; activate it there. Also make `getLastRequestedUrl()` fail with a
  helpful assertion instead of `IndexOutOfBoundsException` when no request was made.
- Declare `StubHttpJsonClient` and `JsonFixtures` `final`.
- **Keep `StockRepository.findAll()`** even though nothing in Member 1's slice calls it —
  its javadoc declares it the hand-off surface for Members 2 and 3's backtesting.
  Deleting it would be a scope error. Add a test for it instead.

**D13 — tests that don't test anything.**
- `apiKeyFromEnvironmentIsEmptyWhenTheVariableIsUnset` is an `if/else` that asserts
  whichever branch the environment happens to be in. It always passes, tests nothing, and
  its name lies when the variable *is* set. Delete it.
- `symbolsNeedingEscapingAreUrlEncoded` asserts `symbol=BRK.B`, but `URLEncoder` does not
  encode `.` — the test proves nothing about escaping. Rewrite it to assert the **exact**
  built URL for `BRK.B` via `StubHttpJsonClient.getRequestedUrls()`, and add a separate
  case with a genuinely reserved character (a space or `&`) that `URLEncoder` does
  transform.

**Also in this phase, not numbered defects but in scope:**
- No test exercises the public one-argument `AlphaVantageMarketDataAccessObject(String)`
  constructor. Add one.
- `Objects.requireNonNull(apiKey)` accepts `""`. Reject a blank key with
  `IllegalArgumentException` — a blank key produces a confusing provider error rather
  than a clear local one.
- Move `fetchDailyPricesFreshUsesTheSameEndpointWhenNotCached` out of the
  `// --- Configuration ---` section; it is not a configuration test.
- Mark local variables `final` in the test classes, matching main-source style.

Target: ≥90% line coverage on `AlphaVantageMarketDataAccessObject` and
`CachingMarketDataGateway`.

**Non-negotiable:** no test may perform a live API call. After your work,
`grep -rn "JdkHttpJsonClient" src/test` must return nothing.

### Phase 5 (on call)

Add gateway and repository tests if overall coverage falls short of 70%.
