# Agent B (data-access) — Phase 2 complete

Branch `phase2/agent-b`, branched from `e3cd012`. `mvn -o clean verify` is green:
**189 tests, 0 failures, 0 errors, BUILD SUCCESS.**

Per deviation D1-b, every file below already existed; this phase remediated them against
the defect list rather than creating them. The single genuinely new file is the
`overview_unknown_symbol.json` fixture.

---

## What was built

Seven commits, one per defect, each with the defect ID in the subject line.

| Commit | ID | What changed |
|---|---|---|
| `dc6e972` | **D8** (closes **W1**) | The frozen `MarketDataGateway` null/blank symbol contract is now implemented identically by all three gateways. |
| `b9e419e` | **D1** | A `{}` OVERVIEW body yields `Optional.empty()` instead of throwing `EMPTY_RESPONSE`. |
| `810de65` | **D2** | The DAO returns `List.copyOf`; the cache no longer aliases a mutable list. |
| `3cc75bd` | **D3** | The cache is thread-safe, bounded, evicting, and rejects a non-positive TTL. |
| `dad338b` | **D12** | Test seams demoted, the stub's dead accessor activated and hardened, `findAll()` kept and tested. |
| `f288aa1` | **D13** (closes **W2**) | The two tests that asserted nothing are replaced. |
| `8df2089` | — | The unnumbered in-scope items at the end of `agents/data-access.md` § Phase Tasks. |

### D8 / W1 — the null/blank contract (done first, as instructed)

All three implementations previously differed: the Alpha Vantage DAO threw
`NullPointerException` out of `URLEncoder`, `CachingMarketDataGateway` keyed the cache on
`""` before validating anything, and `InMemoryMarketDataGateway` returned
`INVALID_SYMBOL` but only after incrementing its call counters. Each now validates before
doing anything else, via a local `requireUsableSymbol`:

- `null` → `NullPointerException("Symbol cannot be null")`
- blank → `MarketDataException(Kind.INVALID_SYMBOL, ...)`, no network call, no cache
  entry, no recorded call

`CachingMarketDataGateway.key` no longer folds `null` onto `""`. The same triple of tests
is in all three test classes.

### D1 — `{}` OVERVIEW is an absent name

The javadoc was right and the code was wrong. `parseCompanyName` now checks the empty root
**before** `rejectProviderError`, whose own empty-root check was converting it to
`EMPTY_RESPONSE`. The price path keeps `EMPTY_RESPONSE` — there is no history to degrade
to. A missing company name may never block adding a ticker, and `OVERVIEW` is the first
endpoint the free-tier quota kills, so this path is common rather than exotic.

### D2 — no mutable list in the cache

`parseDailyPrices` returns `List.copyOf(prices)`. `CachingMarketDataGateway.storeAndReturn`
also copies on the way in, because the decorator wraps arbitrary gateways, not only this
one. The `assertSame(refreshed, cached)` at `CachingMarketDataGatewayTest:125` — which
enshrined the aliasing as intended behaviour — is gone, replaced by an `assertEquals` on
contents plus two new tests: one asserting `UnsupportedOperationException` on `add` and
`clear` for every return path, one using a delegate that hands back a live `ArrayList` and
proving it cannot reach into the cache.

### D3 — the cache under concurrency

All four parts:

- Both maps are `ConcurrentHashMap`. The check-then-put around a miss is left non-atomic
  deliberately — worst case is one duplicate request, and holding a lock across a network
  call would be far worse. Documented in the class javadoc.
- Non-positive `Duration` → `IllegalArgumentException` (a zero TTL silently made the cache
  a no-op).
- The never-expiring company-name cache is bounded at `MAX_NAME_ENTRIES = 64` with
  clear-on-overflow.
- Expired price entries are evicted when read, and `getCachedSymbolCount()` purges before
  counting, so it means live entries the way its name promises.

A multi-threaded reader test (8 threads × 200 reads) exercises the maps concurrently.

### D12 — dead code

- `clear()` and `getCachedSymbolCount()` demoted to package-private (legitimate test seams,
  test lives in the same package). This landed in the D3 commit, which rewrote both.
- `StubHttpJsonClient.getRequestedUrls()` now has real callers (the D8 and D13 URL tests)
  and returns an unmodifiable view; `getLastRequestedUrl()` raises an explanatory
  `AssertionError` instead of `IndexOutOfBoundsException`.
- `StubHttpJsonClient` declared `final` (`JsonFixtures` already was).
- `StockRepository.findAll()` **kept** — its javadoc declares it Members 2/3's backtesting
  hand-off surface, so deleting it would be a scope error. Three new tests cover the empty
  case, removals, and that the result is a snapshot rather than a live view.

### D13 / W2 — tests that tested nothing

- `apiKeyFromEnvironmentIsEmptyWhenTheVariableIsUnset` deleted. It was an `if/else` on
  `System.getenv` asserting whichever branch it landed in; it always passed and its name
  lied when the variable was set.
- `symbolsNeedingEscapingAreUrlEncoded` rewritten. It asserted `symbol=BRK.B`, which
  `URLEncoder` produces without transforming anything. It is replaced by an exact
  assertion on the whole built URL via `getRequestedUrls()`, plus two cases with genuinely
  reserved characters (a space and an ampersand, in the symbol and in the API key) that
  `URLEncoder` really does transform. An unescaped `&` would silently truncate the query.
- `fetchDailyPricesFreshUsesTheSameEndpointWhenNotCached` moved out of the
  `// --- Configuration ---` section.

### Unnumbered in-scope items

- A blank API key is now `IllegalArgumentException` rather than being sent and coming back
  as a confusing `MISSING_API_KEY`.
- First test of the public one-argument `AlphaVantageMarketDataAccessObject(String)`
  constructor — the one Phase 4 wires up. Constructing performs no I/O, so it stays
  offline. Same for `CachingMarketDataGateway(MarketDataGateway)` and its default TTL.
- Local variables in the `data_access` test classes marked `final`.
- **Sample-data oscillation verified.** `withSampleData()` produces at least one BUY and
  one SELL for all three symbols at the **default 5/20** windows, not only the 10/50 pair
  already covered. The series needed no change; the check is now pinned by
  `sampleDataCrossesAtTheDefaultFiveAndTwentyWindows`, so Phase 5's hand-off test has a
  non-vacuous fixture.

---

## Files created / modified

**Created (1):**
- `src/test/resources/alphavantage/overview_unknown_symbol.json`

**Modified (8):**
- `src/main/java/data_access/AlphaVantageMarketDataAccessObject.java`
- `src/main/java/data_access/CachingMarketDataGateway.java`
- `src/main/java/data_access/InMemoryMarketDataGateway.java`
- `src/test/java/data_access/AlphaVantageMarketDataAccessObjectTest.java`
- `src/test/java/data_access/CachingMarketDataGatewayTest.java`
- `src/test/java/data_access/InMemoryMarketDataGatewayTest.java`
- `src/test/java/data_access/InMemoryStockRepositoryTest.java`
- `src/test/java/data_access/StubHttpJsonClient.java`

**Not touched, as required:** `use_case/**` (including `MarketDataGateway` and
`StockRepository`), `interface_adapter/**`, `view/**`, `app/Main.java`, `entity/**`,
`pom.xml`, `data_access/FileWatchlistDataAccessObject.java` and its test,
`data_access/HttpJsonClient.java`, `data_access/JdkHttpJsonClient.java`,
`data_access/InMemoryStockRepository.java` (main source), `JsonFixtures.java`,
`agents/**`. `git diff --name-only e3cd012 HEAD` lists only the nine files above.

---

## Verification

| Check | Result |
|---|---|
| `mvn -o clean verify` | BUILD SUCCESS, 189 tests, 0 failures, 0 errors |
| JaCoCo line coverage, `AlphaVantageMarketDataAccessObject` | **95.3%** (82/86) — target ≥90% |
| JaCoCo line coverage, `CachingMarketDataGateway` | **100%** (47/47) — target ≥90% |
| `grep -rn "JdkHttpJsonClient" src/test` | empty |
| Mockito / AssertJ / new test dependency | none; `pom.xml` untouched |
| Live API call or network access in any test | none; every response comes from a fixture |
| Defect ID in each commit message | D1, D2, D3, D8, D12, D13 all present |

The four uncovered lines in the DAO are `apiKeyFromEnvironment()`. That is deliberate:
the JVM cannot modify its own environment, and the test that pretended to cover it was
D13/W2. Faking coverage there would reintroduce exactly the defect this phase removed.

---

## Open needs

`plan/handoffs/data-access-needs.md` — one item, non-blocking:

**N1.** The `toUpperCase(Locale.ROOT)` key idiom is still written out three times
(`CachingMarketDataGateway.key`, `InMemoryMarketDataGateway.key`,
`InMemoryStockRepository.key`). `agents/data-access.md` § D8 says to route them through a
`TickerSymbolValidator`-adjacent helper **if one exists** after Agent A's D9 work, and to
write a note rather than edit `use_case` if it does not. It did not exist at `e3cd012`.
Requested from Agent A in `plan/handoffs/agent-b-to-a.md` § 1, with the exact signature
that would close it.

No port signature needed to change, so `plan/handoffs/agent-b-request.md` was not written.

---

## What the next phase should know

1. **The gateway contract is now uniform and enforced.** Any interactor or adapter test
   that passes a null or blank symbol to a gateway will get `NullPointerException` or
   `MarketDataException(INVALID_SYMBOL)` respectively, from all three implementations.
   Interactors are expected to run `TickerSymbolValidator` first, so neither should reach
   the gateway in production. Detail in `plan/handoffs/agent-b-to-a.md` § 2.

2. **Price lists returned by any gateway are unmodifiable.** Presenters and view models
   must copy before sorting. A `sort` or `clear` on a returned list now throws
   `UnsupportedOperationException` instead of silently corrupting the cache — that is the
   point of D2, not a regression.

3. **`clear()` and `getCachedSymbolCount()` on `CachingMarketDataGateway` are
   package-private.** Nothing outside `data_access` may call them. `Main.java` has no
   reason to; if Phase 4 wants a "clear cache" affordance it needs a use case, not a
   direct call from the composition root.

4. **Phase 4 composition root, unchanged and confirmed available:**
   `AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()`, the public
   `AlphaVantageMarketDataAccessObject(String apiKey)` constructor, the
   `CachingMarketDataGateway(MarketDataGateway)` constructor (default TTL 15 minutes,
   system clock), and `InMemoryMarketDataGateway.withSampleData()`. Note the new
   constructor precondition: a **blank** API key now throws `IllegalArgumentException`, so
   the composition root must branch on `apiKeyFromEnvironment()` being present rather than
   passing whatever it read straight through. `apiKeyFromEnvironment()` already filters
   blanks to `Optional.empty()`, so following orchestrator § 6 exactly is safe.

5. **`InMemoryMarketDataGateway.withSampleData()` is a real fixture.** Crossovers occur at
   both 5/20 and 10/50 for AAPL, MSFT and TSLA. Phase 5's hand-off test can rely on it.

6. **The company-name cache clears wholesale at 64 entries.** Fine for a watchlist, worth
   revisiting only if some later feature caches names for hundreds of symbols.
