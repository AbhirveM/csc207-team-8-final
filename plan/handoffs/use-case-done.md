# Agent A (use-case) — Phase 2 done

**Branch:** `phase2/agent-a` (worktree `C:\Users\abhir\CSC207\wt-agent-a`), branched from
`e3cd012`.
**Verification:** `mvn -o clean test` green — 277 tests, 0 failures, 0 errors.
`mvn -o verify` green.
**Coverage:** JaCoCo line coverage **100%** on `AddTickerInteractor`,
`RemoveTickerInteractor`, `RefreshTickerInteractor` and `ShowWatchlistInteractor`, and
100% on every other class in `use_case.watchlist` except `TickerSymbolValidator`
(92.9%, one unreachable defensive branch). Overall project line coverage **78.7%**,
already past the Phase 5 70% target before the adapter and view layers land.

---

## What was built

### Defects closed

| ID | What changed | Commit |
|---|---|---|
| D7 | Corrected the inverted check-ordering comment in `TickerSymbolValidator`. The **code** was right and is untouched — it checks the character set first, so a long run of illegal characters reports `ILLEGAL_CHARACTERS` rather than `TOO_LONG`. | `b994389` |
| D9 | Extracted the ~12-line validate → normalize → membership preamble into package-private `WatchlistInputSupport.resolve(rawSymbol, watchlist, Membership)`, and folded the repeated `new Ticker(symbol, null)` lookup-key idiom into `WatchlistInputSupport.lookupKey`. | `f0a01a1` |
| D6 | `Objects.requireNonNull(inputData, "Input data cannot be null")` is the first statement of all four `execute` methods, and is documented on the three input-boundary interfaces this agent owns. | `2c014a8` |
| D4 | Both `Stock` construction sites are wrapped and report `WatchlistFailure.Kind.MALFORMED_RESPONSE`. Add now builds the `Stock` *before* mutating the watchlist, so this failure honours fetch-before-mutate too. Refresh keeps the prior history. | `b7f034f` |
| D5 | Partially closed — see **Open needs** below. The redundant assignment is gone, `companyName` starts as `""`, the variable is named `exception`, and the null-coalescing at the constructor call site is gone. | `fe8a25d` |
| D12 (doubles half) / W3 | `RecordingWatchlistPresenter` and `RecordingSaveWatchlist` now have real callers across four test classes. | `9ca12c0` |
| W4 | Same as D5 — half closed, half filed as A-N1. | `fe8a25d` |
| W5 | The Phase 1 `equals`/`hashCode`/`toString` work is now tested. | `9ca12c0` |

`Do not collapse the three *InputData classes` was honoured — all three remain separate,
per the five-file flat boundary convention in `vision.md` §6.

### New use case

`ShowWatchlistInteractor` implements the frozen `ShowWatchlist*` contract exactly:
constructor `(Watchlist, StockRepository, ShowWatchlistOutputBoundary)` — **no gateway,
no `SaveWatchlist`, no I/O**. An unknown or unusable `selectedSymbol` degrades silently
to `""`; it is never a failure. It reuses the still-package-private
`WatchlistSnapshotFactory`, which is the whole reason the use case exists rather than a
controller calling the factory directly.

### `WatchlistSnapshotFactory`

Null-checks `watchlist` and `stocks`; resolves the selected symbol's `Stock` during the
ticker loop instead of a second `findBySymbol` in `priceRowsFor`; evaluates
`getLatestPrice` once per row instead of twice; trailing newline added.

---

## Files created / modified

**Created (main):**
- `src/main/java/use_case/watchlist/ShowWatchlistInteractor.java`
- `src/main/java/use_case/watchlist/WatchlistInputSupport.java` (package-private)

**Modified (main):**
- `AddTickerInteractor.java`, `RemoveTickerInteractor.java`, `RefreshTickerInteractor.java`
- `TickerSymbolValidator.java` (comment only)
- `WatchlistSnapshotFactory.java`
- `AddTickerInputBoundary.java`, `RemoveTickerInputBoundary.java`,
  `RefreshTickerInputBoundary.java` (javadoc only — no signature change)

**Created (test):**
- `AddTickerInteractorTest.java` (33 tests)
- `RefreshTickerInteractorTest.java` (22)
- `RemoveTickerInteractorTest.java` (13)
- `ShowWatchlistInteractorTest.java` (14)
- `WatchlistFailureTest.java` (13), `WatchlistSnapshotTest.java` (6),
  `WatchlistBoundaryTypesTest.java` (12)
- `WatchlistTestData.java` — deterministic offline price series

**Modified (test):**
- `RecordingWatchlistPresenter.java` — now implements all four output boundaries; the
  single aggregated `successCount` is split into `getAddSuccessCount`,
  `getRemoveSuccessCount`, `getRefreshSuccessCount`, `getShowSuccessCount`.

**Not touched:** every orchestrator carve-out file, all of `data_access/**`,
`src/test/resources/**`, `interface_adapter/**`, `view/**`, `app/Main.java`,
`entity/**`, `pom.xml`. `git diff --name-only e3cd012..HEAD` shows nothing that overlaps
Agent B.

No Mockito, no AssertJ, no new dependency, no network access anywhere in the new tests.

---

## Open needs

Filed in `plan/handoffs/use-case-needs.md`:

- **A-N1 (open, blocks half of D5/W4).** `AddTickerOutputData` — an orchestrator
  carve-out file — needs one additive nullable field,
  `MarketDataException.Kind companyNameFailureKind`, so the interactor can tell the
  presenter *why* a company name is missing. Without it, "this ETF has no company
  record" and "the `OVERVIEW` call was refused for quota" are indistinguishable at the
  presenter: both arrive as `companyName == ""`. The exact spot for the two-line
  follow-up is marked in `AddTickerInteractor`'s `catch` block.
- **A-N2 (open, documentation).** `ShowWatchlistOutputBoundary`'s javadoc says
  `prepareFailView` exists "for the null `inputData` case", which D6 makes unreachable.
  As designed, that method has **no caller at all**.
- **A-N3 (informational).** Snapshot price rows now follow watchlist membership — see
  below.

---

## What the next phase should know

### For Agent C (adapter) — read this before writing the presenter

1. **All four output boundaries are unchanged in signature.** `WatchlistPresenter` can
   implement `AddTickerOutputBoundary`, `RemoveTickerOutputBoundary`,
   `RefreshTickerOutputBoundary` and `ShowWatchlistOutputBoundary` on one class, exactly
   as `RecordingWatchlistPresenter` does. `RemoveTickerOutputData` and
   `RefreshTickerOutputData` keep every getter they had.

2. **`ShowWatchlist` never fails.** Implement `prepareFailView` on
   `ShowWatchlistOutputBoundary` because the interface requires it, but do **not** write
   a test asserting Show Watchlist produces a `WatchlistFailure` — no code path reaches
   it. An off-watchlist or malformed selection comes back as a *success* with
   `snapshot.getSelectedSymbol() == ""` and empty price rows. (A-N2.)

3. **A null `inputData` throws `NullPointerException`; it does not call
   `prepareFailView`.** This is D6 followed literally. The controller must therefore
   never hand a null `*InputData` to a boundary — it constructs the input data itself
   from the raw string, so this is a wiring error, not a user error. The controller may
   pass a null or blank *raw symbol* freely: that is reported as `BLANK_INPUT`.

4. **`AddTickerOutputData.getCompanyName()` is never null — it is `""` when absent**, and
   `isCompanyNameAvailable()` is derived from it. Until A-N1 lands, the presenter
   **cannot** distinguish "no name exists" from "the name lookup was rate-limited", so
   the success-with-no-name status string must not promise a reason. If the orchestrator
   declines A-N1, say so explicitly in the adapter brief.

5. **Failure kinds actually emitted by the interactors**, so the 11-row failure table is
   exercised by real paths rather than hand-built values:

   | Kind | Emitted by |
   |---|---|
   | `BLANK_INPUT`, `BAD_FORMAT`, `TOO_LONG` | add, remove, refresh |
   | `DUPLICATE` | add only |
   | `NOT_ON_WATCHLIST` | remove, refresh |
   | `NETWORK`, `RATE_LIMIT`, `INVALID_SYMBOL`, `EMPTY_RESPONSE`, `MISSING_API_KEY` | add, refresh (gateway) |
   | `MALFORMED_RESPONSE` | add, refresh — from the gateway **and** from `Stock`'s rejected price series |

   `WatchlistFailure.getSymbol()` carries the **raw text the user typed** for the three
   validation kinds and the **normalized symbol** for everything else. The presenter can
   quote it back verbatim in both cases.

6. **`WatchlistFailure` and `WatchlistSnapshot` have real value equality**, now tested.
   The presenter can safely skip a repaint when a use case re-emits an identical
   snapshot.

7. **Snapshot price rows follow watchlist membership** (A-N3). A `selectedSymbol` with
   stored prices but no watchlist entry now yields **no** price rows, while
   `getSelectedSymbol()` still echoes the requested symbol. No current caller does this;
   it is noted so a presenter test does not assume the old behaviour.

### For the orchestrator (Phase 4 wiring)

- Constructor arities are stable and the deliberate asymmetry holds:
  - `AddTickerInteractor(Watchlist, MarketDataGateway, StockRepository, SaveWatchlist.InputBoundary, AddTickerOutputBoundary)`
  - `RemoveTickerInteractor(Watchlist, StockRepository, SaveWatchlist.InputBoundary, RemoveTickerOutputBoundary)`
  - `RefreshTickerInteractor(Watchlist, MarketDataGateway, StockRepository, RefreshTickerOutputBoundary)` — no `SaveWatchlist`
  - `ShowWatchlistInteractor(Watchlist, StockRepository, ShowWatchlistOutputBoundary)` — no gateway, no `SaveWatchlist`
- Every constructor rejects a null argument with a named `NullPointerException`, so a
  mis-wired composition root fails at startup rather than on the first click.
- `watchlistController.showWatchlist("")` at launch is safe on an empty watchlist and on
  a watchlist restored from disk whose tickers have no prices yet: both render, with
  `priceCount == 0` and empty date/close cells for the presenter to substitute
  placeholders into.

### For Agent B (data-access)

Nothing here depends on Agent B's work, and nothing of Agent B's was touched. Two
observations from testing against the fakes, both already on B's Phase 2 list:

- `InMemoryMarketDataGateway` still accepts a null symbol (it keys it as `""`) rather
  than throwing `NullPointerException` per the §5.1 contract — that is D8.
- `InMemoryMarketDataGateway.putPrices` accepts an unsorted series, which is what these
  tests deliberately use to reach the `MALFORMED_RESPONSE` path. **Please keep that
  seam.** If D8 or any later hardening adds ordering validation to `putPrices`, the D4
  tests in `AddTickerInteractorTest` and `RefreshTickerInteractorTest` lose their only
  offline way to trigger `Stock`'s `IllegalArgumentException`.
