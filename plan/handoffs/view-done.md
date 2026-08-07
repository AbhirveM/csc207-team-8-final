# Agent D — done

**Phase 3, view.** One file created, no other file in the tree touched.

| File | Status |
|---|---|
| `src/main/java/view/WatchlistView.java` | new, ~400 lines |
| `plan/handoffs/view-needs.md` | new — two open needs, D-N1 and D-N2 |
| `plan/handoffs/view-done.md` | this file |

---

## What was built

`public class WatchlistView extends JPanel` with the single constructor
`WatchlistView(WatchlistViewModel, WatchlistController)`. No static factory, no setters,
no `initialize()` — constructing it subscribes it to the view model and paints
`viewModel.getState()` once, so a panel that is built and added is a panel that is live.

**Layout**, which is also the tab order:

```
NORTH   [Ticker symbol:] [____] [Add] [Remove] [Refresh] [Load prices]
CENTER  JSplitPane   left: watchlist JTable (TICKER_COLUMNS)
                     right: daily price JTable (PRICE_COLUMNS)
SOUTH   status label
        error label
```

Column headers are read from `WatchlistViewModel.TICKER_COLUMNS` / `PRICE_COLUMNS`; none
are declared locally, so the presenter and the view cannot drift.

**Threading (H1), both halves.**

- Outbound: `runInBackground(Runnable)` is the only path to the controller. It disables all
  four buttons, runs the call in `SwingWorker.doInBackground`, and re-enables them in
  `done` — which the framework runs on the EDT whether the call returned or threw, so a
  network failure cannot lock the UI. `done` does nothing else, because the presenter has
  already pushed the new state.
- Inbound: `onViewModelChanged` opens with the `isEventDispatchThread` guard and
  re-dispatches itself through `invokeLater`. It ignores the event payload and re-reads the
  whole state from `viewModel.getState()`, matching `ComparisonView`.

`showWatchlist` is the one exception — it does no I/O, so the selection listener calls it
directly on the EDT.

**Selection.** A `ListSelectionListener` on the watchlist table calls
`controller.showWatchlist(symbol)`. Re-entrancy is guarded by a `repopulating` flag set
around the model rebuild in `render`, in a `try`/`finally` so an exception cannot leave the
listener permanently muted. After rebuilding, the selection is restored by matching
`state.getSelectedSymbol()` against column 0.

**Load prices (H5).** Nothing is hydrated at start-up. The button snapshots the symbols on
the EDT, then runs **one** worker that calls `refreshTicker` for each in turn, checking
`viewModel.getState().getErrorMessage()` between iterations and returning early when it
starts with `The market data service request limit has been reached.`

**No formatting anywhere.** No `String.format`, no `NumberFormat`, no `DateTimeFormatter`,
no null checks, no branching on emptiness. The single branch on data is
`state.isErrorPresent()`, which `WatchlistState`'s own javadoc designates as the one the
view is meant to make.

**Accessibility.**

- Visible `JLabel` + `setLabelFor` on the ticker field ("Ticker symbol:", with mnemonic T)
  and on both tables ("Watchlist", "Daily prices"). No placeholder text, no tooltip-only
  labelling.
- Error line reads `Error: <prose>` and mirrors the same text into
  `setAccessibleDescription`. The red foreground is set once at construction and is
  decoration layered on words — never the only signal.
- When `isErrorPresent()` is false the line holds a single space rather than being hidden,
  so nothing below it moves. (A truly empty `JLabel` collapses to zero height in
  `BorderLayout`, which is the jump the requirement is guarding against; `ComparisonView`
  uses the same `" "` idiom.)
- `setFocusTraversalPolicy` + `setFocusTraversalPolicyProvider(true)` with the required
  order: ticker field → Add → Remove → Refresh → Load prices → watchlist table → price
  table → status label. The status label is `setFocusable(true)` so it is reachable.
- `setMnemonic` and `setToolTipText` on all four buttons: Add = A, Remove = M, Refresh = R,
  Load prices = L (M for Remove because R is taken by Refresh).
- No font is set anywhere, so nothing is smaller than the platform default.
- The empty-watchlist status line is the presenter's
  `Your watchlist is empty. Add a ticker to begin.`, rendered as-is; the view invents no
  prose beyond the `Error: ` prefix and the static control labels.

**Two private nested helpers**, both `private static final`: `ReadOnlyTableModel` (a
`DefaultTableModel` with `isCellEditable` false) and `OrderedFocusTraversalPolicy` (a
`FocusTraversalPolicy` over a fixed component list — Swing ships no ordered-list policy).

---

## Verification

| Check | Result |
|---|---|
| `grep -n "use_case\|entity" .../WatchlistView.java` | empty |
| `grep -n "String.format\|NumberFormat\|DateTimeFormatter" .../WatchlistView.java` | empty |
| Every button handler routed through a `SwingWorker` | yes — `runInBackground` is the only controller path off the selection listener |
| `propertyChange` has the `isEventDispatchThread` guard | yes, first statement |
| Wildcard imports | none (`ComparisonView`'s `javax.swing.*` was deliberately not copied) |
| Lines over ~100 chars | none |
| File ends with a newline | yes |
| `git status` | only the three files in the table above |

`javac` type-check passed against the real `WatchlistViewModel` / `WatchlistState` plus a
throwaway `WatchlistController` stub compiled **outside the repo**, in the scratch
directory. No stub, fake or placeholder controller was written into this worktree — the
merged tree is where it must compile for real.

`mvn compile` in this worktree alone fails on the single missing symbol
`interface_adapter.watchlist.WatchlistController`. That is expected and is Agent C's file.

---

## For Phase 4 (composition root)

- Construct it last in the watchlist block:
  `new WatchlistView(watchlistViewModel, watchlistController)`. Nothing else to call.
- Register under `WatchlistViewModel.VIEW_NAME` via
  `mainView.addView(WatchlistViewModel.VIEW_NAME, watchlistView)`.
- The constructor paints `WatchlistState.initial()` immediately, so the panel is never
  blank-and-broken before the first use case runs; it reads `Ready.`
- `watchlistController.showWatchlist("")` inside the existing `SwingUtilities.invokeLater`
  is still needed to render a restored watchlist — construction alone paints the *initial*
  state, not the loaded one.
- The view fires **no** network call at start-up by design (H5). If a restored watchlist
  shows every ticker as `Not loaded`, that is correct behaviour, not a bug; the user
  presses "Load prices".
- The constructor throws `NullPointerException` with a named message if either argument is
  null, so a mis-ordered wiring fails loudly at start-up rather than on first click.

## Not done, deliberately

The "before" screenshot in `agents/view.md` § Presentation artifact was reassigned to the
project owner; see `plan/handoffs/screenshots.md`. The app was not launched from this
worktree.
