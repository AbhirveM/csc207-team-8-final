# Agent A — use-case

**Role:** Own the business logic of the watchlist vertical — close the audited defects in
the four interactors and bring them to >90% line coverage with tests that use only
hand-written fakes.

---

## Owns

- `src/main/java/use_case/watchlist/**`
- `src/test/java/use_case/watchlist/**`

Exceptions — orchestrator-owned, read-only to this agent:
- `src/main/java/use_case/watchlist/WatchlistFailure.java`
- `src/main/java/use_case/watchlist/WatchlistSnapshot.java`
- `src/main/java/use_case/watchlist/MarketDataGateway.java`
- `src/main/java/use_case/watchlist/StockRepository.java`
- `src/main/java/use_case/watchlist/AddTickerOutputData.java`
- `src/main/java/use_case/watchlist/ShowWatchlistInputBoundary.java`
- `src/main/java/use_case/watchlist/ShowWatchlistInputData.java`
- `src/main/java/use_case/watchlist/ShowWatchlistOutputBoundary.java`
- `src/main/java/use_case/watchlist/ShowWatchlistOutputData.java`

So this agent **does** own, and may freely modify: `AddTickerInteractor`,
`RemoveTickerInteractor`, `RefreshTickerInteractor`, the three `*InputBoundary`, the
three `*InputData`, the three `*OutputBoundary`, `RemoveTickerOutputData`,
`RefreshTickerOutputData`, `TickerSymbolValidator`, `MarketDataException`,
`WatchlistSnapshotFactory`, and everything under `src/test/java/use_case/watchlist/`.
It creates `ShowWatchlistInteractor`.

---

## Never Touch

- `src/main/java/data_access/**` and `src/test/java/data_access/**` — Agent B
- `src/test/resources/**` — Agent B
- `src/main/java/interface_adapter/**` and `src/test/java/interface_adapter/**` — Agent C
- `src/main/java/view/**` — Agent D and Member 4
- `src/main/java/app/Main.java` — orchestrator (Member 4's file)
- `src/main/java/entity/**` — Member 4's, plus the frozen `Stock` / `DailyPrice`
- `src/main/java/use_case/persistence/**`, `use_case/comparison/**`,
  `use_case/moving_average/**` — teammates
- `pom.xml` — orchestrator
- The nine orchestrator carve-out files listed above
- `plan/**`, `agents/**`

If you believe one of these needs to change, write the request to
`plan/handoffs/agent-a-request.md` and stop. Do not edit it.

---

## Reads (never writes)

Before starting, read in full:
- `agents/orchestrator.md` §5 (all contracts), §7 (standards), §8 (hazards)
- `vision.md` §4 (contracts I must not break), §5 (design principles)
- `use_case/moving_average/ConfigureMovingAverageInteractor.java` — **the style
  reference.** Match its `catch`-on-its-own-line, `final` locals,
  `Objects.requireNonNull(x, "...")`, and javadoc depth exactly.
- `use_case/persistence/SaveWatchlist.java` — for the `SaveWatchlist.InputBoundary`
  signature you call
- `use_case/persistence/SaveWatchlistTest.java` — **the test style reference**
- `entity/Stock.java`, `entity/Watchlist.java`, `entity/Ticker.java` — the invariants you
  must respect
- `data_access/InMemoryMarketDataGateway.java` — the fake you test against, including its
  `failPricesWith`, `failCompanyNameWith`, `putPrices`, `syntheticSeries` seams
- `data_access/InMemoryStockRepository.java`

---

## Interface Contract

### Inputs — what the orchestrator provides before you start

Frozen in Phase 1, at the commit SHA in `plan/status.md`:

- `WatchlistFailure` — `final`, with `equals`/`hashCode`/`toString`, 11-value `Kind`
  enum, and the two `from(...)` factories.
- `WatchlistSnapshot` — `final`, with value semantics, containing
  `TickerRow(symbol, companyName, priceCount, latestDate, latestClose)` and
  `PriceRow(date, open, high, low, close, volume)`.
- `MarketDataGateway` — with the normative null/blank contract in its javadoc
  (orchestrator §5.1): non-null required, blank rejected as `INVALID_SYMBOL`, never
  cached.
- `AddTickerOutputData` — reshaped to a single non-null `String companyName` (`""` when
  absent) with `isCompanyNameAvailable()` derived. The orchestrator has already made the
  one-line call-site fix in `AddTickerInteractor`; build on top of it.
- The four `ShowWatchlist*` contract files (orchestrator §5.2).

### Outputs — what other components depend on

- **Agent C** depends on: the three existing `*OutputBoundary` interfaces plus
  `ShowWatchlistOutputBoundary` — all four unchanged in signature — and on
  `RemoveTickerOutputData` / `RefreshTickerOutputData` keeping their current getters. If
  you need to change either output-data shape, that is a contract change: request it
  through the orchestrator, do not do it unilaterally.
- **The orchestrator (Phase 4)** depends on all four `*InputBoundary` interfaces and on
  each interactor's constructor arity staying stable. Note the deliberate asymmetry:
  `RefreshTickerInteractor` does **not** take `SaveWatchlist.InputBoundary` (refresh
  changes prices, not membership), and `ShowWatchlistInteractor` takes neither the
  gateway nor `SaveWatchlist`.
- **Agent B** depends on nothing you produce. Your only shared surface with B is the two
  ports, and the orchestrator owns those.

---

## Phase Tasks

### Phase 2 (primary)

Close these defects. Each gets its own commit, with the defect ID in the message.

**D4 — `Stock`'s `IllegalArgumentException` escapes `execute`.**
`new Stock(...)` and `stock.withDailyPrices(...)` throw `IllegalArgumentException` when
the provider returns unsorted or duplicate dates. Today that unchecked exception escapes
`AddTickerInteractor.execute` and `RefreshTickerInteractor.execute` and crashes the
`SwingWorker`. Wrap both construction sites in
`try { ... } catch (IllegalArgumentException exception) { ... }` and report
`new WatchlistFailure(Kind.MALFORMED_RESPONSE, symbol)` through `prepareFailView`.
`MALFORMED_RESPONSE` exists for exactly this.

**D5 — swallowed diagnostic in `AddTickerInteractor`.**
The `catch (MarketDataException e) { companyName = null; }` around `fetchCompanyName`
discards the exception entirely and the assignment is redundant. Keep the *decision* — a
missing company name must never block adding a ticker (`vision.md` principle 7) — but
stop discarding the diagnostic: set `companyName = ""` (matching the reshaped
`AddTickerOutputData`) and thread `exception.getKind()` into the outcome so the user can
tell "no name available" from "we were rate-limited". Rename the variable to `exception`
per the style standard.

**D6 — missing `inputData` null-check.**
First statement of all four `execute` methods:
`Objects.requireNonNull(inputData, "Input data cannot be null");`. An input boundary that
reports every other problem through `prepareFailView` should not NPE from a field access
three lines later.

**D7 — inverted comment in `TickerSymbolValidator`.**
The comment claims length is checked before the character set; the code checks the
character set first. **The code is correct — fix the comment, not the code.** Changing
the order would change which `Reason` a long-and-illegal input reports.

**D9 — copy-pasted preamble.**
~12 lines of validate → normalize → membership-check are duplicated verbatim across all
three interactors. Extract a package-private helper — suggested shape: a
`WatchlistInputSupport` with
`static Resolution resolve(String rawSymbol, Watchlist watchlist, Membership required)`
returning either a normalized symbol or a `WatchlistFailure`. It also removes the
repeated `new Ticker(symbol, null)` lookup-key idiom that appears twice in
`AddTickerInteractor` alone.

**Do not** collapse the three `*InputData` classes into one shared class. The five-file
flat boundary convention is a deliberate, rubric-visible decision (`vision.md` §6) and
that particular duplication is intentional — five files per use case read better as boxes
on a class diagram.

**New — `ShowWatchlistInteractor`.**
Implements `ShowWatchlistInputBoundary` against the frozen contract. Constructor takes
`(Watchlist, StockRepository, ShowWatchlistOutputBoundary)` — no gateway, no
`SaveWatchlist`. Performs no I/O. An unknown `selectedSymbol` degrades silently to `""`
rather than failing. Reuses `WatchlistSnapshotFactory`, which stays package-private.

While you are in `WatchlistSnapshotFactory`: it resolves `stocks.findBySymbol(...)` a
second time in `priceRowsFor` after the build loop already resolved it, and evaluates
`stock.flatMap(Stock::getLatestPrice)` twice per row. Tidy both. Add the missing
`Objects.requireNonNull` on `watchlist` and `stocks`. Add the missing trailing newline.

**Tests — the largest single piece of work in this phase.**

Create `AddTickerInteractorTest`, `RemoveTickerInteractorTest`,
`RefreshTickerInteractorTest`, `ShowWatchlistInteractorTest`. Activate the two existing
dead test doubles (`RecordingWatchlistPresenter`, `RecordingSaveWatchlist`) rather than
writing new ones, and extend `RecordingWatchlistPresenter` to also implement
`ShowWatchlistOutputBoundary` — it currently implements three of the four.

`RecordingWatchlistPresenter` aggregates `successCount` across all four use cases. Split
it into per-use-case counters so a multi-step test can tell which boundary fired.

Required cases:
- null `inputData` on each of the four interactors
- blank input, too-long input, illegal-character input
- duplicate ticker on add; not-on-watchlist on remove and on refresh
- all six `MarketDataException.Kind` values on add, and on refresh, driven through
  `InMemoryMarketDataGateway.failPricesWith`
- company-name failure is non-fatal on add (the ticker is still added)
- `Stock`'s `IllegalArgumentException` → `MALFORMED_RESPONSE`, on both add and refresh
  (feed unsorted `DailyPrice`s through the fake gateway to trigger it)
- fetch-before-mutate: a provider failure on add leaves the watchlist **unchanged** —
  assert `watchlist.getEntries()` is still empty
- refresh preserves the prior company name, and preserves the prior price history on
  failure
- remove clears the selected symbol and the price rows
- `saveWatchlist` is called exactly once on add and on remove, and **zero** times on
  refresh and on show
- lowercase input normalizes to uppercase in the resulting snapshot

Target: ≥90% line coverage on all four `*Interactor` classes.

### Phase 5 (on call)

If `mvn clean verify` reports overall line coverage below 70%, add use-case and entity
tests to close the gap. Never add Swing tests for this purpose (hazard H6).
