# Orchestrator

## Role

Own every file that crosses an agent boundary, spawn component agents only after their
inputs exist and compile, run the single authoritative build at each phase gate, and
perform the two integration edits into Member 4's files.

---

## 1. System architecture

MarketLens is a Java 17 Swing desktop app built on Clean Architecture. This plan
completes the **Member 1 vertical**: watchlist management backed by Alpha Vantage market
data.

```
                       ┌──────────────────────────────────────────┐
  view/                │ WatchlistView (Swing, SwingWorker)       │  Agent D
                       └───────────────┬──────────────────────────┘
                                       │ reads WatchlistState
                                       │ calls WatchlistController
                       ┌───────────────▼──────────────────────────┐
  interface_adapter/   │ WatchlistController   WatchlistPresenter │  Agent C
     watchlist/        │ WatchlistViewModel    WatchlistState     │  ORCHESTRATOR
                       └───────────────┬──────────────────────────┘
                                       │ *InputBoundary / *OutputBoundary
                       ┌───────────────▼──────────────────────────┐
  use_case/watchlist/  │ Add / Remove / Refresh / Show interactors│  Agent A
                       │ WatchlistFailure  WatchlistSnapshot      │  ORCHESTRATOR
                       │ MarketDataGateway  StockRepository (ports)│ ORCHESTRATOR
                       └───────────────┬──────────────────────────┘
                                       │ implemented by
                       ┌───────────────▼──────────────────────────┐
  data_access/         │ AlphaVantageMarketDataAccessObject       │  Agent B
                       │ CachingMarketDataGateway                 │
                       │ InMemoryMarketDataGateway (offline fake) │
                       │ InMemoryStockRepository                  │
                       │ HttpJsonClient / JdkHttpJsonClient       │
                       └──────────────────────────────────────────┘

  entity/              Stock, DailyPrice (mine) · Ticker, Watchlist,
                       WatchlistEntry (Member 4's — read-only)
  app/Main.java        composition root (Member 4's — orchestrator, append-only)
```

**The dependency arrow that must never be violated:** `MarketDataGateway` and
`StockRepository` are declared in `use_case.watchlist` and implemented in `data_access`.
Interactors depend on the port; `Main` injects the implementation. A grader hunting for
Dependency Rule violations must find zero.

**Threading model.** Interactors and the presenter are synchronous and therefore
trivially testable. The background worker lives in the **view** — the only layer allowed
to import Swing. The consequence, and the single most likely runtime bug in this
vertical, is hazard H1 below.

**Boundary convention.** Five separate top-level files per use case
(`XInputBoundary`, `XInputData`, `XInteractor`, `XOutputBoundary`, `XOutputData`),
matching `use_case.moving_average`. Do **not** adopt Member 4's nested style
(`SaveWatchlist.InputBoundary`), even though it also exists on `main`.

---

## 2. Files the orchestrator owns

No component agent may create or modify any of these.

### Cross-boundary contracts
| File | Why centrally owned |
|---|---|
| `src/main/java/use_case/watchlist/WatchlistFailure.java` | A produces it, C consumes it |
| `src/main/java/use_case/watchlist/WatchlistSnapshot.java` | A produces it, C consumes it |
| `src/main/java/use_case/watchlist/MarketDataGateway.java` | A depends on it, B implements it |
| `src/main/java/use_case/watchlist/StockRepository.java` | A depends on it, B implements it |
| `src/main/java/use_case/watchlist/AddTickerOutputData.java` | shape change breaks A and C at once |
| `src/main/java/use_case/watchlist/ShowWatchlistInputBoundary.java` | A implements, C calls |
| `src/main/java/use_case/watchlist/ShowWatchlistInputData.java` | same |
| `src/main/java/use_case/watchlist/ShowWatchlistOutputBoundary.java` | A calls, C implements |
| `src/main/java/use_case/watchlist/ShowWatchlistOutputData.java` | same |
| `src/main/java/interface_adapter/watchlist/WatchlistViewModel.java` | C writes it, D reads it |
| `src/main/java/interface_adapter/watchlist/WatchlistState.java` | same |

### Shared build and integration files
| File | Rule |
|---|---|
| `pom.xml` | Shared with the whole team. **One isolated edit, announced first.** Currently needs none — `org.json` and JaCoCo 0.8.13 are already present. |
| `src/main/java/app/Main.java` | Member 4's. Phase 4 only. Append-only, at the TODO on lines 42–44. |
| `src/main/java/view/MainView.java` | Member 4's. Phase 4 only. Uncomment the nav-button template at lines 30–33. |
| `plan/**` (except `plan/review-phase-*.md`) | Planning docs |
| `agents/**` | Agent briefs |

### Read-only for everyone, including the orchestrator
`entity/WatchlistEntry.java` — **never open this file.** It is `Serializable` with an
implicit `serialVersionUID`. Adding a field changes the computed UID, every existing
`watchlist.dat` throws `InvalidClassException`, and Member 4's recovery code renames it
`.corrupted-*` and returns an empty watchlist. That destroys teammates' data during demo
week and looks like *their* bug.

`entity/Ticker.java`, `entity/Watchlist.java`, `entity/DailyPrice.java`,
`entity/BacktestResult.java`, `entity/Trade.java`, the strategy classes,
`use_case/persistence/**`, `use_case/comparison/**`, `interface_adapter/comparison/**`,
`interface_adapter/persistence/**`, `view/ComparisonView.java`, `view/ViewManager*.java`,
`view/MainAppState.java`, `data_access/FileWatchlistDataAccessObject.java` — all
teammates' files. Read them; do not write them.

---

## 3. Component agents

| Agent | Brief | Owned directories |
|---|---|---|
| **A — use-case** | `agents/use-case.md` | `src/main/java/use_case/watchlist/**`, `src/test/java/use_case/watchlist/**` (minus the orchestrator carve-outs in §2) |
| **B — data-access** | `agents/data-access.md` | `src/main/java/data_access/**`, `src/test/java/data_access/**`, `src/test/resources/alphavantage/**` (minus `FileWatchlistDataAccessObject.java` and its test) |
| **C — adapter** | `agents/adapter.md` | `src/main/java/interface_adapter/watchlist/WatchlistController.java`, `.../WatchlistPresenter.java`, `src/test/java/interface_adapter/watchlist/**` |
| **D — view** | `agents/view.md` | `src/main/java/view/WatchlistView.java` |
| **reviewer** | `agents/reviewer.md` | `plan/review-phase-*.md` |

**Why the split is by layer.** Every seam between these agents is already a compile-time
Java interface rather than a convention, so ownership is provably disjoint and conflicts
are caught by `javac` rather than by review. The obvious alternative — one agent per use
case (add / remove / refresh) — is strictly worse: all three would touch
`WatchlistSnapshotFactory`, `WatchlistFailure`, the same presenter and the same view.

---

## 4. Per-phase: what to write before spawning

### Phase 1 — Contract Freeze (no agents spawned)

Write, in this order, then commit and record the SHA in `plan/status.md`:

1. **`WatchlistFailure`** → `final class`; add `equals`, `hashCode`, `toString`. Keep
   both `from(...)` factories and both exhaustive `switch` expressions.
2. **`WatchlistSnapshot`** → `final class`; add `equals`, `hashCode`. Replace the bare
   `List.copyOf(...)` calls with `Objects.requireNonNull(x, "... cannot be null")`
   followed by `List.copyOf(...)`, so a null argument fails with a named message like
   everything else in the package.
3. **`MarketDataGateway`** → add the normative contract to the javadoc of both fetch
   methods (see §5.1). No signature change.
4. **`AddTickerOutputData`** → collapse to a single non-null `String companyName` that
   is `""` when unavailable; `isCompanyNameAvailable()` becomes
   `return !companyName.isEmpty();`. Then make the one-line call-site fix in
   `AddTickerInteractor` so the build stays green. **This is the only time the
   orchestrator edits an agent-owned file** — it is a compile-preserving call-site edit,
   and Agent A branches from the resulting commit.
5. **The four `ShowWatchlist*` files** per §5.2.

Verify: `mvn -o clean test` green. Then spawn the reviewer for
`plan/review-phase-1.md`.

### Phase 2 — spawn A ∥ B

Nothing new to write. Before spawning:
- Provision a **separate git worktree per agent** (hazard H4). If worktrees are
  unavailable, serialise: A runs to completion, then B.
- Give each agent brief the Phase 1 commit SHA.
- Restate to both: **no live API calls in tests, ever.**

### Phase 3 — write the ViewModel first, then spawn C ∥ D

Write `WatchlistViewModel.java` and `WatchlistState.java` per §5.3 and §5.4, commit, and
only then spawn C and D. This is what breaks the C↔D cycle.

### Phase 4 — no agents

Two append-only edits (§6).

### Phase 5 — orchestrator, A and B on call

Write the hand-off test and the `plan/handoffs/` notes.

---

## 5. Data shapes and API contracts

### 5.1 `MarketDataGateway` — null and blank symbol contract

Add to the javadoc of `fetchDailyPrices`, `fetchDailyPricesFresh`, and
`fetchCompanyName`, and enforce it in all three implementations
(`AlphaVantageMarketDataAccessObject`, `CachingMarketDataGateway`,
`InMemoryMarketDataGateway`):

> `normalizedSymbol` must be non-null. Implementations reject `null` with
> `NullPointerException` via `Objects.requireNonNull(normalizedSymbol,
> "Symbol cannot be null")` — a null symbol is a programming error, not a user error,
> and callers run `TickerSymbolValidator` first. A **blank** symbol must be rejected
> with `MarketDataException` of kind `INVALID_SYMBOL`, must not reach the network, and
> must never be cached.

This exists because the three implementations currently differ: one throws NPE from
`URLEncoder`, one caches under `""`, one returns `INVALID_SYMBOL`.

### 5.2 `ShowWatchlist*` — the fourth use case

```
ShowWatchlistInputBoundary   void execute(ShowWatchlistInputData inputData)

ShowWatchlistInputData       final
                             String getSelectedSymbol()   // "" means none selected

ShowWatchlistOutputBoundary  void prepareSuccessView(ShowWatchlistOutputData outputData)
                             void prepareFailView(WatchlistFailure failure)

ShowWatchlistOutputData      final
                             int getTickerCount()
                             WatchlistSnapshot getSnapshot()
```

Semantics, binding on Agent A:
- `ShowWatchlistInteractor` performs **no I/O**. It never calls `MarketDataGateway` and
  never calls `SaveWatchlist`. It only reads `Watchlist` + `StockRepository` and re-emits
  `WatchlistSnapshotFactory.build(...)`.
- A `selectedSymbol` that is not on the watchlist is **not** a failure — it degrades
  silently to `""`.
- `prepareFailView` is on the boundary for symmetry and for the null-`inputData` case
  only.

**Why this use case exists.** Without it, (a) clicking a different row in the watchlist
table cannot repopulate the price table — no existing use case can do that without a
network call, and (b) after `LoadWatchlist` restores tickers from disk, nothing renders
them. The alternative — making `WatchlistSnapshotFactory` public and calling it from the
controller — is an adapter reaching into the use-case layer, i.e. exactly the Dependency
Rule violation this design is meant to have zero of.

### 5.3 `WatchlistState` — the adapter-layer DTO

`final class`, immutable, with `equals`/`hashCode`/`toString`, one all-args constructor,
and `static WatchlistState initial()`.

```java
public record TickerRow(String symbol, String companyName, String priceCount,
                        String latestDate, String latestClose) { }
public record PriceRow(String date, String open, String high, String low,
                       String close, String volume) { }

List<TickerRow> getTickerRows()     // never null, may be empty
List<PriceRow>  getPriceRows()      // never null, may be empty
String  getSelectedSymbol()         // "" when none
String  getStatusMessage()          // never "" — "Ready." at minimum
String  getErrorMessage()           // "" when no error
boolean isErrorPresent()            // == !getErrorMessage().isEmpty()
String  getTickerFieldText()        // "" after success; preserved after failure
```

Two rules that make this contract load-bearing:

1. **Every field is a `String`, including `priceCount`.** The presenter renders `0` as
   `"Not loaded"` and empty dates/closes as `"—"`. The view performs no formatting
   whatsoever — no `String.format`, no `NumberFormat`, no branching on emptiness. This
   is what keeps the view dumb enough that its uncovered lines don't sink the 70%
   coverage target (hazard H6).
2. **`WatchlistState` declares its own `TickerRow`/`PriceRow` records** rather than
   re-exporting `WatchlistSnapshot`'s. The ~10 lines of duplication buy a view with zero
   `use_case` imports, and give the presenter the place it needs to substitute
   human-readable placeholders.

### 5.4 `WatchlistViewModel`

Matches `interface_adapter/comparison/ComparisonViewModel.java` exactly in shape: plain
class, private `PropertyChangeSupport support = new PropertyChangeSupport(this)`, no
Swing imports, no generic `ViewModel<State>` base class (this repo does not use one).

```java
public static final String VIEW_NAME      = "watchlist";
public static final String STATE_PROPERTY = "state";
public static final String[] TICKER_COLUMNS =
    {"Symbol", "Company", "Days of prices", "Latest date", "Latest close"};
public static final String[] PRICE_COLUMNS =
    {"Date", "Open", "High", "Low", "Close", "Volume"};

WatchlistState getState()
void setState(WatchlistState state)   // fires STATE_PROPERTY
void addPropertyChangeListener(PropertyChangeListener listener)
```

Column headers live here, not in the view, so the presenter and the view cannot drift.
`TICKER_COLUMNS.length` must equal `TickerRow`'s component count, and
`PRICE_COLUMNS.length` must equal `PriceRow`'s.

### 5.5 `WatchlistController` (Agent C writes; Agent D depends on)

```java
void addTicker(String rawSymbol)
void removeTicker(String rawSymbol)
void refreshTicker(String rawSymbol)
void showWatchlist(String selectedSymbol)
```

All four are `void` and synchronous, and pass the raw string straight through —
normalization stays in the interactor, where it is tested. Constructor takes the four
input boundaries in that order.

### 5.6 Failure and success prose

The full 11-row failure table and 7 success messages are specified verbatim in
`agents/adapter.md` §Interface Contract. Agent C must pin every one with a test. That
table is the only place user-facing strings are allowed to exist.

---

## 6. Phase 4 integration edits

**`view/MainView.java`, lines 30–33.** A commented-out watchlist nav-button template is
already there. Uncomment it and change the hardcoded string to
`WatchlistViewModel.VIEW_NAME`. Do not restructure the constructor; there is no
`addNavButton` API and adding one is out of scope.

**`app/Main.java`, lines 42–44.** Replace the TODO with a `// --- Watchlist (Member 1) ---`
block placed between the Persistence block and the Comparison block, following the
existing `ViewModel → Presenter → Interactor → Controller → View` declaration order.

Gateway selection — **the only place in the codebase that may read the environment**:

```
AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()
  present → new CachingMarketDataGateway(new AlphaVantageMarketDataAccessObject(key))
  absent  → InMemoryMarketDataGateway.withSampleData()
            + a visible status line saying sample data is in use
```

Never a `.env`, never a default key in code, never a key in a log or an error message.

Then: seed a `Watchlist` from `loadWatchlistInteractor.execute()` reading
`persistenceViewModel`, falling back to `new Watchlist()`; construct
`InMemoryStockRepository`, `WatchlistViewModel`, `WatchlistPresenter`, the four
interactors, `WatchlistController`, `WatchlistView`; call
`mainView.addView(WatchlistViewModel.VIEW_NAME, watchlistView)`; and inside the existing
`SwingUtilities.invokeLater`, call `watchlistController.showWatchlist("")` so the
restored watchlist renders immediately.

The `saveWatchlistInteractor` and `loadWatchlistInteractor` locals at lines 38–41 are
currently constructed and never consumed. This wiring is what consumes them.

---

## 7. Cross-cutting standards

Binding on every agent. With no Checkstyle config in this repo (deliberately out of
scope), the reviewer enforces these by reading.

**Style** — derived from `use_case.moving_average`, the repo's canonical slice:
- 4-space indent, K&R braces, but `catch` and `else` on their **own line** after the
  closing brace.
- Exception variables are named `exception`, never `e`.
- Fields are `private final`. Locals are `final` wherever possible. Data and interactor
  classes are `final`.
- `Objects.requireNonNull(x, "X cannot be null")` — always with a message.
- Javadoc on every public type and member: one-sentence summary, `@param`/`@return`/
  `@throws` where they add information.
- Imports: `java.*` block, blank line, then project packages. No wildcards in new files.
- Lines stay under ~100 characters.
- Every file ends with a newline. (Several existing files do not — fix the ones you own.)

**Error format:**
- Inside the use-case layer, failures are `WatchlistFailure` values — never strings,
  never exceptions crossing the output boundary.
- Inside `data_access`, failures are `MarketDataException` with an explicit `Kind`.
- Unchecked exceptions must not escape `XInteractor.execute`. Anything a collaborator can
  throw — including `Stock`'s `IllegalArgumentException` — is caught and mapped.
- User-facing prose exists only in `WatchlistPresenter`.

**Environment config:** `ALPHA_VANTAGE_API_KEY`, read only at the composition root, only
via `System.getenv`. No `.env` file, no default value, no key in any message.

**Logging:** this project has no logging framework and is not adding one. "Don't swallow
the diagnostic" means threading `exception.getKind()` into the `WatchlistFailure` or the
status message — not adding a logger.

**Testing:** JUnit 5, package-private `XTest` classes, package-private `void` test
methods with full-sentence lowerCamelCase names, static-imported `Assertions`,
hand-written nested fakes. **No Mockito, no AssertJ, no new test dependency.** No test
may touch the network.

**No entity crosses an output boundary.** Only `String`, primitives, and the
`WatchlistSnapshot` / `WatchlistState` records built from them.

---

## 8. Hazards

- **H1 — Cross-thread view-model mutation.** The presenter is synchronous and is called
  from the `SwingWorker` background thread, so `setState` and the resulting
  `PropertyChangeSupport` fire off the EDT. The view's `propertyChange` handler is the
  sole re-entry point and **must** re-dispatch via `SwingUtilities.invokeLater` when
  `!SwingUtilities.isEventDispatchThread()`. This is the most likely runtime bug in the
  whole vertical; it is stated again in `agents/view.md`.
- **H2 — A↔C cycle on `ShowWatchlist`.** Resolved by the orchestrator authoring the four
  contract files in Phase 1.
- **H3 — C↔D cycle on the ViewModel.** Resolved by the orchestrator authoring
  `WatchlistViewModel` and `WatchlistState` at the head of Phase 3.
- **H4 — Concurrent `mvn` in one working tree.** Parallel agents share `target/` and
  clobber each other's JaCoCo `.exec` files, producing spurious failures. Give each
  parallel agent its own git worktree, or serialise. The orchestrator runs the single
  authoritative `mvn clean verify` at each gate.
- **H5 — Quota burn on hydration.** A restored 8-ticker watchlist must not fire 8 API
  calls at launch against a ~25/day free tier. Hydration is lazy and user-driven: a
  "Load prices" button drives one `SwingWorker` that refreshes tickers **sequentially**
  and aborts on the first `RATE_LIMIT`.
- **H6 — Coverage arithmetic.** `WatchlistView` adds ~250–350 uncovered lines on top of
  already-uncovered `MainView`, `ViewManager`, `ComparisonView` and `Main`. The 70%
  overall target is load-bearing on A and B hitting their 90%s. If Phase 5 measures
  short, the lever is more entity and gateway tests — never Swing tests.
- **H7 — `WatchlistEntry` is untouchable.** See §2.
- **H8 — The ~100-day ceiling.** Alpha Vantage's free `TIME_SERIES_DAILY` compact
  response returns roughly the latest 100 trading days. Any strategy long-window above
  ~90 silently violates `MovingAverageCrossoverStrategy.generateSignals`'s
  `size() >= longWindow + 1` precondition. Members 2 and 3 need to hear this; it goes in
  `plan/handoffs/` in Phase 5.

---

## 9. Out of scope — raise, do not absorb

Confirmed with the owner. These are real team gaps; this vertical files them rather than
silently taking them on.

- `checkstyle.xml` + `maven-checkstyle-plugin` — the rubric needs a tool like it to score
  above 3/5 on code quality. The style checklist in `agents/reviewer.md` is the interim
  substitute.
- `accessibility-report.md` — a required course deliverable. Note that the accessibility
  *behaviour* is still in scope (`agents/view.md`); only the report file is not.
- `serialVersionUID` on the seven `Serializable` entities. **Warn the team explicitly:**
  adding an arbitrary `= 1L` to `Ticker` or `Watchlist` breaks every existing
  `watchlist.dat` with `InvalidClassException`. It is only safe via
  `serialver -classpath target/classes entity.Ticker ...` to capture the *currently
  computed* value, in its own commit, verified against a real saved file.
