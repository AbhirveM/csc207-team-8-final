# Phase 3 — Adapter and View

**Active agents:** orchestrator writes two files first, then `adapter` (C) ∥ `view` (D),
then reviewer.
**Goal:** the vertical becomes visible and operable — one presenter serving four output
boundaries, and a Swing panel that never blocks the event thread.

---

## Dependencies to install

None.

---

## Step 0 — orchestrator, before spawning anyone

Create and commit:

| File | Purpose |
|---|---|
| `interface_adapter/watchlist/WatchlistState.java` | immutable, all-`String` DTO with its own `TickerRow` / `PriceRow` records; `static initial()` |
| `interface_adapter/watchlist/WatchlistViewModel.java` | `VIEW_NAME`, `STATE_PROPERTY`, `TICKER_COLUMNS`, `PRICE_COLUMNS`, `getState`/`setState`, `addPropertyChangeListener` |

Exact shapes: `agents/orchestrator.md` §5.3 and §5.4. `WatchlistViewModel` must match
`interface_adapter/comparison/ComparisonViewModel.java` in pattern — plain class,
`PropertyChangeSupport`, no Swing imports, no generic `ViewModel<State>` base (this repo
does not use one).

These two files are the **only** surface C and D share. Authoring them centrally is what
lets C and D run in parallel (hazard H3).

Verify `mvn -o clean test` is green, commit, then spawn C and D.

---

## Agent C — files to create

| File | Purpose |
|---|---|
| `interface_adapter/watchlist/WatchlistPresenter.java` | one class implementing all four output boundaries |
| `interface_adapter/watchlist/WatchlistController.java` | four `void` pass-through methods |
| `src/test/java/interface_adapter/watchlist/WatchlistPresenterTest.java` | pins all 11 failure strings and all 8 success strings |
| `src/test/java/interface_adapter/watchlist/WatchlistControllerTest.java` | asserts the raw string reaches the boundary untouched |

Methods to implement:

```
WatchlistPresenter(WatchlistViewModel viewModel)
  prepareSuccessView(AddTickerOutputData)
  prepareSuccessView(RemoveTickerOutputData)
  prepareSuccessView(RefreshTickerOutputData)
  prepareSuccessView(ShowWatchlistOutputData)
  prepareFailView(WatchlistFailure)

WatchlistController(AddTickerInputBoundary, RemoveTickerInputBoundary,
                    RefreshTickerInputBoundary, ShowWatchlistInputBoundary)
  addTicker(String) · removeTicker(String) · refreshTicker(String) · showWatchlist(String)
```

The complete 11-row failure table and 8 success messages are in `agents/adapter.md`
§ Interface Contract. That table is the **only** place user-facing strings may live.

Three behaviours a test must pin, because getting them wrong is worse than the underlying
error:
- `prepareFailView` copies `tickerRows` / `priceRows` from the current state — a
  rate-limited refresh must never blank the user's watchlist.
- `prepareFailView` preserves `tickerFieldText`; Add/Remove/Refresh success clear it, and
  Show Watchlist success sets it to the selected symbol (`plan/decisions.md` D3-d).
- `priceCount == 0` renders as `"Not loaded"`, an empty date or close as `"—"`, and an
  absent company name as the symbol itself.

Use an exhaustive `switch` expression over `WatchlistFailure.Kind` with **no `default`**,
so a twelfth kind becomes a compile error.

## Agent D — file to create

| File | Purpose |
|---|---|
| `view/WatchlistView.java` | the Swing panel; `WatchlistView(WatchlistViewModel, WatchlistController)` |

Layout, which is also the keyboard focus order:
`[Ticker symbol:] field → Add → Remove → Refresh → Load prices → watchlist table → price
table → status label → error label`.

Two threading requirements, both mandatory (hazard H1):
1. Every button handler wraps its controller call in a `SwingWorker`; buttons disable for
   the duration and re-enable in `done`, including on exception.
2. `propertyChange` re-dispatches via `SwingUtilities.invokeLater` when
   `!SwingUtilities.isEventDispatchThread()` — because the presenter fires
   `PropertyChangeSupport` from the worker thread.

Also: a selection listener on the watchlist table calling `controller.showWatchlist(...)`
(guard re-entrancy while repopulating the model), and a "Load prices" button that
refreshes tickers **sequentially** and aborts on the first rate-limit error — never
automatically at startup (hazard H5).

The view performs **no formatting whatsoever**. Every `WatchlistState` field is already a
display-ready `String`.

Full detail: `agents/view.md` § Phase Tasks — including the accessibility list and the
"before" screenshot that must be captured now, since it cannot be recreated after Phase 4.

---

## Verification

1. `mvn -o clean verify` — green.
2. `WatchlistPresenterTest` asserts the **exact** string (`assertEquals`, not `contains`)
   for all 11 `WatchlistFailure.Kind` values and all 8 success messages.
3. `grep -rn "javax.swing\|java.awt" src/main/java/interface_adapter` — empty.
4. `grep -n "use_case\|entity" src/main/java/view/WatchlistView.java` — empty.
5. `grep -n "String.format\|NumberFormat\|DateTimeFormatter" src/main/java/view/WatchlistView.java`
   — empty. If it isn't, formatting leaked out of the presenter.
6. Every button handler in `WatchlistView` contains a `SwingWorker`, and
   `propertyChange` contains the `isEventDispatchThread` guard.
7. `git diff --name-only` shows C and D touched no shared file.
8. Reviewer writes `plan/review-phase-3.md`; status is not FAIL.
