# Review — Phase 2 (Core Remediation and Interactor Tests)

Range reviewed: `e3cd012..5e79e22` (25 commits). Reviewer owns only this file; nothing
was modified.

Status: **PASS WITH WARNINGS**

---

## Critical (blocking — must fix before next phase)

None. No red build, no ownership violation, no dependency-rule violation, no leaked
credential, no network call in a test, and every defect the phase claimed to close is
genuinely closed rather than worked around.

---

## Warnings (non-blocking but fix soon)

Ranked most-severe first.

1. **The concurrency test drives a fake whose maps are not thread-safe** —
   `src/test/java/data_access/CachingMarketDataGatewayTest.java:291`
   (`concurrentReadersDoNotCorruptTheCache`) against
   `src/main/java/data_access/InMemoryMarketDataGateway.java:40-45`.
   The cache's check-then-put is deliberately non-atomic, so the first read from eight
   threads produces up to eight simultaneous entries into
   `InMemoryMarketDataGateway.fetchDailyPrices`, each doing
   `priceCallCounts.merge(...)` and `companyNameCallCounts.merge(...)` on plain
   `HashMap`s. The test's own javadoc says "with plain HashMaps this loops forever or
   throws" — which is exactly what its delegate uses. In practice the risk is small (two
   keys, no resize), but the writes are unsynchronised, so this is a latent flake in the
   one test that exists to prove thread safety. Either make the fake's counters
   `ConcurrentHashMap`/`AtomicInteger`, or pre-warm the cache before the threads start
   so the delegate is never entered concurrently. Owner: B.

2. **Javadoc promises a bound the code cannot guarantee under the concurrency the class
   advertises** — `src/main/java/data_access/CachingMarketDataGateway.java:167-168`
   ("never above `MAX_NAME_ENTRIES`") against the non-atomic
   `size() >= MAX` → `clear()` → `put()` sequence at `:133-136`. Single-threaded the
   bound holds; concurrently two threads can both pass the size check and both put after
   the clear. This is the "no comment contradicts its code" rule, in the same class whose
   class-level javadoc explicitly claims thread safety. Softening the wording to
   "approximately bounded" or making the overflow reset atomic would both resolve it.
   Owner: B.

3. **§7 style: exception variables named `e`.**
   - `src/main/java/use_case/watchlist/RefreshTickerInteractor.java:64` —
     `catch (MarketDataException e)`. This one is the worst of the set: twenty lines
     below, in the *same method*, Agent A wrote `catch (IllegalArgumentException
     exception)`. The file is internally inconsistent. Owner: A.
   - `src/main/java/data_access/AlphaVantageMarketDataAccessObject.java:151, 202, 300`
     and `src/test/java/data_access/JsonFixtures.java:27`. Owner: B.

   All are pre-existing lines, but both agents remediated these files this phase and both
   fixed `e` → `exception` elsewhere in them, so the misses read as oversights rather
   than as untouched legacy.

4. **§7 import-order violation across all four `data_access` files** —
   `AlphaVantageMarketDataAccessObject.java:3`, `CachingMarketDataGateway.java:3`,
   `InMemoryMarketDataGateway.java:3`, `InMemoryStockRepository.java:3`. §7 requires a
   `java.*` block, a blank line, then project packages. These four do the reverse
   (`entity`/`use_case`/`org.json` first, then `java.*`). Every `use_case/watchlist` file
   next door gets it right — including `WatchlistSnapshotFactory`, which Agent A
   *reordered into* the correct form this phase. The two packages now disagree with each
   other in a way a grader will see. Owner: B.

5. **Lines over the ~100-character guidance** in files changed this phase. Worst
   offenders: `AlphaVantageMarketDataAccessObjectTest.java:147` (115),
   `InMemoryMarketDataGateway.java:142` (108),
   `InMemoryMarketDataGateway.java:54,56,58` (104),
   `CachingMarketDataGateway.java:90,114,118` (103),
   `WatchlistSnapshotFactory.java:36` (104),
   `AddTickerInteractor.java:57` (106), `RefreshTickerInteractor.java:37` (106),
   `RemoveTickerInteractor.java:26` (106), `ShowWatchlistInteractor.java:44` (106),
   `WatchlistFailureTest.java:83,85` (103/104). 26 lines total, all 101–115. The
   interactor cluster is the same `Objects.requireNonNull(this.stockRepository, ...)`
   line copied four times; wrapping it once fixes four of them. Owners: A and B.

6. **`Objects.requireNonNull` bypassed for a hand-rolled throw** —
   `src/main/java/data_access/InMemoryStockRepository.java:27-29` throws
   `new NullPointerException("Stock cannot be null")` directly. Behaviourally identical,
   but §7 names `Objects.requireNonNull(x, "...")` as the form, and this is the only
   place in the phase's output that departs from it. Owner: B.

7. **`apiKeyFromEnvironment()` is the DAO's only uncovered code (4 lines) and the gap is
   avoidable** — `src/main/java/data_access/AlphaVantageMarketDataAccessObject.java:60-66`.
   Agent B's reasoning is *sound as far as it goes*: an in-process test cannot set an
   environment variable, and a test that branches on whether the variable happens to be
   set is verbatim the W2/D13 tautology this phase deleted. Leaving it uncovered is the
   right call over faking it. But there is a third option neither considered: extract a
   package-private pure `static Optional<String> apiKeyFrom(String rawValue)` holding the
   null/blank/strip logic, test it exhaustively, and leave `apiKeyFromEnvironment()` as a
   one-line `return apiKeyFrom(System.getenv(API_KEY_ENV_VARIABLE));`. That drops the
   uncovered surface from four lines to one and pins the `strip()` behaviour Phase 4
   depends on. Not blocking — the ≥90% gate holds comfortably at 95.3%. Owner: B.

8. **The A-N3 behaviour change is safe but is not pinned by any test** —
   `src/main/java/use_case/watchlist/WatchlistSnapshotFactory.java:57-59, 69`.
   Verdict: the change is safe. Selected price rows now come from the stock found while
   walking `watchlist.getEntries()`, so a `selectedSymbol` with stored prices that is
   *not* on the watchlist yields no price rows where it previously did. I checked all
   four call sites: Add selects a symbol it just added, Refresh selects one it confirmed
   present, Remove selects `""`, and Show normalises an off-watchlist selection to `""`
   at `ShowWatchlistInteractor:78` before building. The changed branch is therefore
   unreachable through the public boundaries, and the new semantics ("the snapshot
   describes the watchlist") are the more defensible ones. What is missing is a test that
   *distinguishes* old from new: `ShowWatchlistInteractorTest.anUnknownSelectedSymbol
   DegradesSilentlyRatherThanFailing` selects `MSFT` with neither a watchlist entry nor a
   repository entry, so it would pass under the old code too. The factory is
   package-private and at 100% line coverage, so a direct
   `WatchlistSnapshotFactory.build(watchlist, stocksContainingAnOffListSymbol, "MSFT")`
   assertion would cost three lines and would stop a future refactor from silently
   reverting it. Owner: A.

9. **The use-case test package imports `data_access` implementations** —
   `AddTickerInteractorTest.java:11-12`, `RefreshTickerInteractorTest.java:11-12`,
   `RemoveTickerInteractorTest.java:8`, `ShowWatchlistInteractorTest.java:8` all import
   `data_access.InMemoryMarketDataGateway` and/or `data_access.InMemoryStockRepository`.
   The Dependency Rule is about `src/main` and is clean there — this is not a violation
   and does not affect the rubric line. But it does two unwanted things: it points Agent
   A's tests at classes Agent B owns (a change to `InMemoryStockRepository` can now
   redden A's suite), and it puts a `use_case → data_access` import in front of any
   grader who greps the whole tree rather than `src/main`. §7 calls for "hand-written
   nested fakes"; a five-line nested `Map`-backed `StockRepository` in
   `WatchlistTestData` would remove both problems. Owner: A. Judgement call — flagging,
   not demanding.

10. **Contract drift in an orchestrator-owned doc** — `agents/orchestrator.md` §5.2 still
    reads "`prepareFailView` is on the boundary for symmetry and for the null-`inputData`
    case only." A-N2 corrected the javadoc on
    `ShowWatchlistOutputBoundary.java:20-24`, but the brief Agent C will read in Phase 3
    still carries the superseded claim. Owner: orchestrator. See Notes for the design
    judgement.

11. **D4 drops the diagnostic that D5 was fixed for dropping** —
    `AddTickerInteractor.java:132` and `RefreshTickerInteractor.java:84` both bind
    `IllegalArgumentException exception` and then never read it, discarding the message
    that says *which* invariant the price series broke (unsorted vs. duplicate dates).
    `WatchlistFailure` has no field for it, so this is not a one-line fix and the brief
    did not ask for one — but it is the same swallowed-diagnostic shape as W4, one
    catch block away from the code that was just fixed for it. Worth a decision rather
    than silence. Owner: A.

12. **Gateway and repository classes are not `final`** —
    `AlphaVantageMarketDataAccessObject:46`, `CachingMarketDataGateway:40`,
    `InMemoryMarketDataGateway:32`, `InMemoryStockRepository:21` are all `public class`.
    §7 mandates `final` on "data and interactor classes" and does not name adapters, so
    this is defensible; noting it only because every `use_case` counterpart is `final`
    and the asymmetry is unexplained. Owner: B. Lowest priority in this list.

---

## Notes

### Every Phase 2 exit criterion re-verified independently

| # | Criterion | Result |
|---|---|---|
| 1 | `mvn -o clean verify` green | **PASS** — re-run from scratch: 319 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS |
| 2 | JaCoCo ≥90% on the four interactors + DAO + cache | **PASS** — see table below |
| 3 | Every defect ID in a commit message | **PASS** — D1 `b9e419e`, D2 `810de65`, D3 `3cc75bd`, D4 `b7f034f`, D5 `fe8a25d` + `8825910`, D6 `2c014a8`, D7 `b994389`, D8 `dc6e972`, D9 `f0a01a1`, D12 `dad338b` (data_access) + `9ca12c0` (doubles), D13 `f288aa1` |
| 4 | `grep -rn "JdkHttpJsonClient" src/test` empty | **PASS** — and so is a wider sweep for `HttpClient`, `java.net.*`, `URLConnection`, `Socket`, `openStream`, `InetAddress`. No test can reach the network |
| 5 | `Recording*` doubles have real callers | **PASS** — `RecordingWatchlistPresenter` and `RecordingSaveWatchlist` are each used by all four interactor tests |
| 6 | No file touched by both agents | **PASS** — verified commit by commit. Agent A stayed in `use_case/watchlist` (main + test), Agent B in `data_access` + `src/test/resources/alphavantage`. The two cross-agent commits are clean: `1144daa` (A adds `normalizeKey`, A's file) then `b7bb19c` (B consumes it, B's files). The orchestrator's `fa3c665` touched only §2 carve-outs |
| 7 | Fetch-before-mutate | **PASS** — `AddTickerInteractorTest.aProviderFailureLeavesTheWatchlistCompletelyUnchanged:210-221` asserts empty `getEntries()`, empty repository, zero saves, *and* zero company-name calls; `aMalformedResponseAlsoLeavesTheWatchlistUnchanged:343-352` covers the `Stock` rejection path |
| 8 | No Mockito, no AssertJ, no new test dependency, `pom.xml` unchanged | **PASS** — grep for mockito/assertj/hamcrest/easymock across `src` and `pom.xml` returns nothing; `git diff e3cd012..HEAD -- pom.xml` is empty |
| 9 | Dependency Rule | **PASS** — no `import data_access` in `src/main/java/use_case`; no `javax.swing`/`java.awt` in `use_case`, `interface_adapter`, or `entity`; no entity crosses a watchlist output boundary (`WatchlistSnapshot` carries only `String`/`int` records) |
| 10 | No API key in source, fixture, log, test, or message | **PASS** — the checklist grep returns nothing. The only key-bearing string is the URL built at `AlphaVantageMarketDataAccessObject:126-142`, and `request()` deliberately omits the URL from `MarketDataException`. The test constant is `"test-key-1234"` |

### Coverage measured from `target/site/jacoco/jacoco.csv`

| Class | Line coverage | Gate |
|---|---|---|
| `AddTickerInteractor` | 100% (40/40) | ≥90% ✓ |
| `RemoveTickerInteractor` | 100% (19/19) | ≥90% ✓ |
| `RefreshTickerInteractor` | 100% (35/35) | ≥90% ✓ |
| `ShowWatchlistInteractor` | 100% (19/19) | ≥90% ✓ |
| `AlphaVantageMarketDataAccessObject` | 95.3% (82/86) | ≥90% ✓ |
| `CachingMarketDataGateway` | 100% (47/47) | ≥90% ✓ |

Agent A's 100%-on-all-four claim is accurate. Agent B's 95.3%/100% claim is accurate, and
the four missed lines are exactly `apiKeyFromEnvironment()` as reported. Also 100%:
`WatchlistSnapshotFactory`, `WatchlistInputSupport`, `WatchlistFailure`,
`WatchlistSnapshot`, `AddTickerOutputData`, `InMemoryStockRepository`,
`InMemoryMarketDataGateway`, and every `*InputData`/`*OutputData`.
`TickerSymbolValidator` is 93.3% (the private constructor). Project-wide line coverage is
**80.6% (838/1040)** — comfortable headroom before `WatchlistView` lands and hazard H6
bites in Phase 5.

### Self-reported items, judged

- **Agent B's non-atomic check-then-put (D3).** The reasoning **holds**. Both maps are
  `ConcurrentHashMap`, so the failure mode of an interleaved miss is one redundant
  delegate call, not a corrupted map; holding a lock across a network call would trade a
  wasted quota unit for a stalled EDT-adjacent worker. It is documented at the class
  level (`CachingMarketDataGateway:33-38`) rather than left implicit. Accepted. The
  residual issues are Warnings 1 and 2, which are about the *test* and the *name-cache
  bound*, not about this decision.
- **Agent B's `InMemoryStockRepository` null tolerance.** **Consistent and correct.**
  `findBySymbol:35-37` returns `Optional.empty()` for null and `remove:43-45` no-ops,
  both before reaching `key(...)`; `save:26-29` throws. `StockRepository`'s own contract
  is what makes the asymmetry right — a null lookup is a legitimate no-op, a null save is
  a bug. The `key(...)` javadoc at `:61-64` states this explicitly and matches the code.
- **Agent A's D6 / `prepareFailView` resolution.** The javadoc-only fix is **the right
  call**, but it does not fully discharge the problem. Making `execute` call
  `prepareFailView` for a null `inputData` would be worse: null `inputData` is a wiring
  bug in the controller, not a user error, and routing it to the presenter would put a
  fabricated `WatchlistFailure.Kind` in front of the user for a fault they cannot act on.
  Fail-fast is correct and is pinned by
  `ShowWatchlistInteractorTest.nullInputDataFailsFastRatherThanReachingTheOutputBoundary`.
  The residue Agent C will trip over is real but small: `WatchlistPresenter` must still
  implement `ShowWatchlistOutputBoundary.prepareFailView`, and no interactor will ever
  drive it, so it can only be covered by calling it directly from a presenter test.
  Agent C's brief should say so plainly, and §5.2 of `agents/orchestrator.md` should be
  corrected (Warning 10). This is a documentation debt, not a design problem — the
  alternative (dropping `prepareFailView` from the interface) would break the symmetry
  the four boundaries are built on for no gain.

### Test quality

No tautological tests found. The two W2/D13 offenders are genuinely gone: `getenv` no
longer appears anywhere in `src/test`, and the `BRK.B` test at
`AlphaVantageMarketDataAccessObjectTest:185-197` now pins the entire URL rather than a
substring `URLEncoder` never touches — with a companion at `:205-214` using `A&B C`,
which actually exercises escaping. The `assertSame` that pinned the D2 aliasing bug at
`CachingMarketDataGatewayTest:125` is gone, replaced by
`aMutableDelegateResultIsCopiedBeforeItIsCached:157-182`, which clears the delegate's
live list and asserts the cache survives. The only surviving `assertSame` in the
watchlist slice is `RemoveTickerInteractorTest:131`, asserting that the live `Watchlist`
instance is what reached `SaveWatchlist` — correct, not a pinned bug. The 5/20-window
oscillation check that Phase 5 depends on exists and is real
(`InMemoryMarketDataGatewayTest:184-200` asserts at least one BUY *and* one SELL for all
three sample symbols).

### Dead code

Everything D12 named now has a caller: `getRequestedUrls` (3 call sites),
`clear()`/`getCachedSymbolCount()`/`getCachedNameCount()` (all exercised), both
`Recording*` doubles, and `StockRepository.findAll` — which is deliberately retained as
Members 2/3's hand-off surface and is now covered by five dedicated tests, with the
rationale recorded at `InMemoryStockRepositoryTest:99-103`.

### For Phase 3

- `System.getenv` appears exactly once, at
  `AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()` — inside `data_access`,
  not `app/Main`. This is sanctioned by `agents/orchestrator.md` §6, which names that
  method as the composition root's gateway-selection mechanism. Flagging only so the
  Phase 4 review does not read the checklist line ("`System.getenv` appears in exactly
  one place: the composition root") as a violation. The single-call-site property holds.
- `view/ComparisonView.java:3` and `view/MainAppState.java:3` import
  `entity.BacktestResult`. Both are teammates' files and out of this vertical's scope, so
  no action — but a grader running the `view → entity` grep will hit them. Worth raising
  with the team rather than absorbing.
- The four boundary contracts frozen in Phase 1 are unchanged apart from the two
  orchestrator-sanctioned edits in `fa3c665`: the additive fifth `AddTickerOutputData`
  constructor argument (the four-argument form still delegates, so no call site broke)
  and the `ShowWatchlistOutputBoundary` javadoc. Agent C can rely on §5.2's shapes as
  written, with the one correction in Warning 10.
- `AddTickerOutputData.getCompanyNameFailureKind()` is new and nullable. Agent C's
  success-prose table gains a *success-with-caveat* variant, not a failure row — the add
  succeeds whether or not the name lookup did. `AddTickerInteractorTest:231-299` pins all
  three states (name present → null kind; no company record → null kind; lookup failed →
  the kind).
