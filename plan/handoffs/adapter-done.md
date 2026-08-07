# Agent C (adapter) — Phase 3 done

## What was built

`WatchlistPresenter` — one `final` class implementing all four output boundaries
(`AddTicker`, `RemoveTicker`, `RefreshTicker`, `ShowWatchlist`), plus the single
`prepareFailView(WatchlistFailure)` they share. It is the only place in the codebase
where user-facing prose lives; every `WatchlistState` field leaves it display-ready.

`WatchlistController` — four one-line delegations, constructor `Objects.requireNonNull`s
all four input boundaries with a message. No validation, no trimming, no upper-casing.

## Files created

| File | Notes |
|---|---|
| `src/main/java/interface_adapter/watchlist/WatchlistPresenter.java` | 4 boundaries, 1 fail view |
| `src/main/java/interface_adapter/watchlist/WatchlistController.java` | pass-through only |
| `src/test/java/interface_adapter/watchlist/WatchlistPresenterTest.java` | 38 tests |
| `src/test/java/interface_adapter/watchlist/WatchlistControllerTest.java` | 11 tests |

Nothing else was created or modified. `WatchlistViewModel` and `WatchlistState` were
read only.

## Decisions worth knowing

**Failure statusMessage keeps the previous status.** `WatchlistState`'s constructor
rejects a blank `statusMessage`, so `prepareFailView` cannot emit `""`. It carries the
current state's status forward rather than inventing one, so the last thing that actually
happened stays on screen next to the error explaining what just did not.
`WatchlistState.initial()` guarantees the value is never blank, so this can never throw.
Pinned by `prepareFailViewKeepsThePreviousStatusMessageBecauseABlankStatusIsRejected`.

**`prepareFailView` also preserves `selectedSymbol`,** alongside the required
`tickerRows`, `priceRows` and `tickerFieldText`. Blanking the selection would clear the
price table by a side door, which is the same bug the row-preservation rule exists to
prevent.

**Refresh counts history as present only when `priceCount > 0` *and* `latestDate` is
non-empty.** A mismatched pair falls to the "no price history was returned" sentence
rather than producing a dangling `latest .`.

**The add caveat message does not name the underlying `MarketDataException.Kind`.** The
user can do nothing with "the name lookup was rate-limited", and one stable sentence
stays pinnable by a single `assertEquals`. All three add outcomes are successes and none
sets `errorMessage`.

**`messageFor` is an exhaustive `switch` expression over `WatchlistFailure.Kind` with no
`default`.** A twelfth kind is a compile error here, not a silent fallthrough.

**The em dash placeholder is written as a unicode escape rather than as a literal
character,** in both the presenter and the test, so the constant cannot be corrupted by a
mis-set source encoding.

## Verification

- `mvn -o clean verify` — green in this worktree. 49 new tests, 0 failures, 0 errors.
- `grep -rn "javax.swing\|java.awt" src/main/java/interface_adapter` — empty.
- All 11 `WatchlistFailure.Kind` values and all 8 success messages pinned with
  `assertEquals` on the exact string. No `contains`.
- JUnit 5 only. No Mockito, no AssertJ, no new dependency, no test doubles in the
  presenter test (a real `WatchlistViewModel` and real `*OutputData` values).
- `git status` shows only the four files above plus this handoff.
- No line exceeds 100 characters; every file ends with a newline.

## Open needs

None. Nothing was required from another agent's domain, so
`plan/handoffs/adapter-needs.md` was not created.

## For Phase 4 (composition root)

- Construct one `WatchlistPresenter(watchlistViewModel)` and pass **the same instance** to
  all four interactors. It implements all four output boundaries; constructing four
  presenters would still work but wastes the shared-wording guarantee.
- `new WatchlistController(addTickerInteractor, removeTickerInteractor,
  refreshTickerInteractor, showWatchlistInteractor)` — that argument order.
- Calling `watchlistController.showWatchlist("")` inside the existing
  `SwingUtilities.invokeLater` renders the restored watchlist immediately. On an empty
  watchlist the status reads `Your watchlist is empty. Add a ticker to begin.`; on a
  restored one it reads `Showing N tickers.` with every price count rendered
  `Not loaded` until the user presses "Load prices".
- The presenter never reads the environment and never mentions an API key value. The
  `MISSING_API_KEY` message names only the variable name, `ALPHA_VANTAGE_API_KEY`.
- Presenter calls are synchronous and fire `PropertyChangeSupport` on whatever thread
  invokes them — hazard H1 is handled entirely on the view side.

## Note on the worktree

The worktree was initially created at `bff35db`, not at the `394d3fb` the brief names.
It was reset to `394d3fb` (clean tree, nothing lost) before any work began, so this
branch is a true child of the orchestrator's Phase 3 step-0 commit.

---

## D-N1 resolved — Show Watchlist arms Remove and Refresh

Accepted from Agent D by the orchestrator after the first commit merged.

**Problem.** `WatchlistView` reads one ticker field for Add, Remove and Refresh alike.
Because every success cleared `tickerFieldText`, the demo path "click the AAPL row, press
Refresh" answered `Enter a ticker symbol before continuing.` with the row plainly
selected. Fixing it view-side would have needed a second source of truth for "which symbol
is the user talking about", so it belongs here.

**Change.** In `prepareSuccessView(ShowWatchlistOutputData)` only, `tickerFieldText` is
now the snapshot's `getSelectedSymbol()` rather than `""`. That value is already `""` when
nothing is selected, so the empty-selection behaviour is unchanged. `publishSuccess`
gained a three-argument overload taking an explicit ticker field; the two-argument form
still passes `""` and is what Add, Remove and Refresh call.

**Scope, deliberately narrow.** Add, Remove and Refresh still clear the field. They are
text *submissions* — the input was consumed, and leaving it behind invites a
double-submit. Show Watchlist is a *selection*, so populating the field from it is
coherent rather than contradictory. The method javadoc records this reasoning explicitly,
because the next reader will otherwise "fix" it back.

**Tests added** (three, all new):
- `showWatchlistPutsTheSelectedSymbolIntoTheTickerFieldRatherThanClearingIt`
- `showWatchlistWithNoSelectionStillLeavesTheTickerFieldEmpty`
- `addRemoveAndRefreshClearTheTickerFieldEvenWhenTheSnapshotHasASelection` — the guard
  against someone widening this to the other three paths

Every pre-existing test passes unaltered, including `everySuccessClearsTheTickerFieldText`
(its Show Watchlist fixture has no selection).

**No prose changed.** All 11 failure strings and all 8 success strings are byte-for-byte
identical; a `git diff` filtered to the message literals is empty.

**Verify.** `mvn -o clean verify` green in this worktree. `WatchlistPresenterTest` 41
tests, `WatchlistControllerTest` 11 tests, 0 failures, 0 errors.

## D-N2 — noted, no action

The "Load prices" stop condition prefix-matching the `RATE_LIMIT` sentence is an accepted
risk, logged by the orchestrator in `plan/decisions.md`. A structured `isQuotaExhausted()`
on `WatchlistState` would be cleaner, but that class is orchestrator-owned and frozen this
phase. The `assertEquals` on the exact `RATE_LIMIT` string in `WatchlistPresenterTest` is
what makes the coupling safe: changing that sentence breaks this test loudly rather than
breaking the view silently.

---

## `WatchlistStateTest` — closing the 57.1% hole

`WatchlistState` is orchestrator-owned, but it lives in this package and
`src/test/java/interface_adapter/watchlist/**` is Agent C's, so the test was written here.
The source file was **not** modified.

**Created** `src/test/java/interface_adapter/watchlist/WatchlistStateTest.java` — 30 tests
covering `initial()`, the full `equals`/`hashCode` contract (reflexive, two independently
built equal states sharing a hash, one case per each of the six fields differing, null and
unrelated type), defensive copying, list immutability, all seven constructor rejections
asserted by message, `isErrorPresent()` in both directions, and `toString`.

**One trap worth recording.** `assertNotEquals(expected, actual)` compares with
`Objects.equals(expected, actual)`, which invokes `expected.equals(actual)`. Written as
`assertNotEquals("WatchlistState", state)` the receiver is `String`, so
`WatchlistState.equals` never runs and the `instanceof` branch stays uncovered — the test
passes while proving nothing. The state must be the **first** argument. A comment above
those two tests says so.

**`toString` is asserted with `contains`, deliberately** — it is a debugging aid, not a
deliverable, and pinning it byte-for-byte would be a brittle test that buys nothing. A
comment marks this as the one place in the suite where `contains` is correct, precisely
because it is the opposite of the rule governing the message table.

## Two presenter branches closed while in the area

JaCoCo showed `WatchlistPresenter` at 100% line but 35/37 branch. Both gaps were real
defensive paths, now pinned:

- `refreshWithACountButNoLatestDateFallsBackToTheNoHistorySentence` — a positive count with
  an empty date is a contradiction the provider should never emit, but reporting it would
  render as a dangling `latest .`, so it degrades to the emptier sentence.
- `nullSnapshotCellsRenderAsPlaceholdersRatherThanAsTheWordNull` —
  `WatchlistSnapshot.TickerRow` is a record with no null checking, so a null cell is
  reachable; a null company name falls back to the symbol and a null date or close renders
  as the em dash, never as the four letters `null`.

## Final coverage for `interface_adapter/watchlist`

| Class | Line | Branch |
|---|---|---|
| `WatchlistPresenter` | 103/103 (100%) | 37/37 (100%) |
| `WatchlistController` | 14/14 (100%) | — |
| `WatchlistState` | 35/35 (100%) | 20/20 (100%) |
| `WatchlistViewModel` | 12/12 (100%) | — |

`WatchlistState` went from 20/35 (57.1%) to 35/35 (100%), branches 20/20.

**Verify.** `mvn -o clean verify` green. `WatchlistPresenterTest` 43,
`WatchlistStateTest` 30, `WatchlistControllerTest` 11 — 84 tests in this package, 0
failures, 0 errors.
