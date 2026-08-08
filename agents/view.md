# Agent D — view

**Role:** Own `WatchlistView` — the Swing panel that renders `WatchlistState`, keeps every
network call off the event thread, and satisfies the accessibility requirements in words
rather than in colour.

---

## Owns

- `src/main/java/view/WatchlistView.java`

That is the entire ownership. This agent creates exactly one file.

---

## Never Touch

- `src/main/java/view/MainView.java` — Member 4's; the orchestrator uncomments the nav
  button in Phase 4
- `src/main/java/view/ViewManager.java`, `ViewManagerModel.java`, `ComparisonView.java`,
  `MainAppState.java` — Member 4's
- `src/main/java/app/Main.java` — orchestrator
- `src/main/java/interface_adapter/**` — Agent C and the orchestrator. You **read**
  `WatchlistViewModel`, `WatchlistState` and `WatchlistController`; you never edit them.
- `src/main/java/use_case/**` — Agent A. You must not even *import* from it.
- `src/main/java/data_access/**` — Agent B
- `src/main/java/entity/**` — frozen
- `pom.xml`, `agents/**`, `src/test/**`, and `plan/**` — **except** `plan/handoffs/view-*.md`,
  which you are required to write. Filing a cross-agent need is never an ownership violation.

**`accessibility-report.md` is explicitly out of scope** for this plan (see
`agents/orchestrator.md` §9). The accessibility *behaviour* below is still binding.

---

## Reads (never writes)

- `agents/orchestrator.md` §5.3, §5.4, §5.5, §7, and **§8 hazard H1 — read it twice**
- `interface_adapter/watchlist/WatchlistViewModel.java` and `WatchlistState.java`
- `interface_adapter/watchlist/WatchlistController.java`
- `view/ComparisonView.java` — the panel pattern to match: `extends JPanel`,
  `DefaultTableModel`, a private `onViewModelChanged(PropertyChangeEvent)` registered via
  `viewModel.addPropertyChangeListener(this::onViewModelChanged)`, re-reading *all* state
  from getters on any event
- `vision.md` §5 principle 9, §6 (the view sequence), §8 (what "done" means)

---

## Interface Contract

### Inputs — what you get before you start

- **`WatchlistState`** — every field is already a formatted `String`. `priceCount` may
  read `"Not loaded"`. Dates and closes may read `"—"`. **Perform no formatting of any
  kind**: no `String.format`, no `NumberFormat`, no `DateTimeFormatter`, no branching on
  emptiness, no null checks. If a value looks wrong, that is Agent C's bug, not yours.
- **`WatchlistViewModel`** — `VIEW_NAME`, `STATE_PROPERTY`, `TICKER_COLUMNS`,
  `PRICE_COLUMNS`, `getState()`, `addPropertyChangeListener(...)`. Use the column-header
  arrays from here; never re-declare headers locally, or the two will drift.
- **`WatchlistController`** — `addTicker`, `removeTicker`, `refreshTicker`,
  `showWatchlist`, all `void` and all **blocking**.

### Outputs — what other components depend on

The orchestrator's Phase 4 wiring depends on exactly:

```java
public class WatchlistView extends JPanel {
    public WatchlistView(WatchlistViewModel viewModel, WatchlistController controller);
}
```

Nothing else. No static factory, no setters, no `initialize()` call the composition root
has to remember.

---

## Phase Tasks

### Phase 3 (only phase this agent is active)

**Layout — the order is also the keyboard focus order** (`vision.md` §6):

```
NORTH   [Ticker symbol:] [____] [Add] [Remove] [Refresh] [Load prices]
CENTER  JSplitPane
          left  : watchlist JTable   (TICKER_COLUMNS)
          right : daily price JTable (PRICE_COLUMNS)
SOUTH   status label
        error label
```

**Threading — hazard H1, the most likely runtime bug in this vertical.**

Two halves, and both are required:

1. **Outbound.** Every button handler wraps its controller call in a
   `SwingWorker<Void, Void>`, because `addTicker` and `refreshTicker` block on the
   network and a blocking call freezes the window for seconds — visible on stage.
   `doInBackground` makes the call; `done` is a no-op, because the presenter has already
   pushed the new state. Disable all buttons for the duration and re-enable in `done`
   (including on exception, or the UI locks permanently).
2. **Inbound.** Because the presenter is synchronous, `viewModel.setState(...)` and its
   `PropertyChangeSupport` fire **on the worker thread**. Your `propertyChange` handler is
   the sole re-entry point into Swing and must therefore be:

   ```
   if (!SwingUtilities.isEventDispatchThread()) {
       SwingUtilities.invokeLater(() -> onViewModelChanged(event));
       return;
   }
   ```

   Omitting this produces intermittent, hard-to-reproduce repaint corruption that will
   only show up under a real network delay — i.e. during the demo.

`showWatchlist` does no I/O and may be called directly on the EDT.

**Table row selection.** A selection listener on the watchlist table calls
`controller.showWatchlist(selectedSymbol)`. This is the whole reason the `ShowWatchlist`
use case exists — without it the price table can never change when the user clicks a
different ticker. Guard against re-entrancy: `setState` repopulates the table model,
which fires a selection event, which would call `showWatchlist` again. Suppress the
listener while repopulating, and restore the selection to `state.getSelectedSymbol()`
afterwards.

**"Load prices" and the quota (hazard H5).** A restored watchlist shows every ticker with
`"Not loaded"` and no prices. Hydration is **lazy and user-driven** — never automatic at
startup, because a restored 8-ticker watchlist would burn 8 of a ~25/day free-tier quota
the moment the app opens. The button drives **one** `SwingWorker` that calls
`controller.refreshTicker(symbol)` for each ticker **sequentially**, and stops at the
first failure whose error text indicates the rate limit.

**Accessibility — each item is a rubric line, and each is cheap:**
- Every control has a visible `JLabel`, wired with `label.setLabelFor(component)`. The
  ticker field's label is visible text, not placeholder text and not a tooltip.
- The error label shows prose prefixed `Error: ` and is **never** colour-only. Colour may
  be added on top; it may never be the only signal. Mirror the text into
  `setAccessibleDescription`.
- `setFocusTraversalPolicy` giving: ticker field → Add → Remove → Refresh → Load prices →
  watchlist table → price table → status label.
- Mnemonics on all buttons (`setMnemonic`), and `setToolTipText` on all buttons.
- Table column headings come from `WatchlistViewModel` and are full words
  (`"Days of prices"`, not `"n"`).
- No font smaller than the platform default.

**Empty and error states.** When `getTickerRows()` is empty, the status label already
carries `"Your watchlist is empty. Add a ticker to begin."` from the presenter — render
it, don't invent one. When `isErrorPresent()` is false, the error label is empty, not
hidden, so the layout does not jump.

**Keep this class dumb.** Hazard H6: `WatchlistView` will add ~250–350 uncovered lines to
a project that must hit 70% overall. The less logic it contains, the less that costs. If
you find yourself writing a conditional about *data*, it belongs in the presenter — file
it in `plan/handoffs/view-needs.md`.

**Before finishing, verify:** `grep -n "use_case" src/main/java/view/WatchlistView.java`
returns nothing.

### Presentation artifact — do this before Phase 4 wiring lands

`vision.md` §8 requires a **"before" screenshot captured before the view is wired in** —
it cannot be recreated afterwards. Take it now: launch the app on the current `main`
behaviour and capture the window with only the Compare Strategies tab. Save it outside
the repo and note the path in `plan/handoffs/screenshots.md`.
