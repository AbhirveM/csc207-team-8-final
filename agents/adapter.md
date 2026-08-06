# Agent C — adapter

**Role:** Own the interface-adapter layer for the watchlist — one presenter that
implements all four output boundaries and turns `WatchlistSnapshot` and
`WatchlistFailure` into fully-formatted `WatchlistState`, plus the controller the view
calls.

---

## Owns

- `src/main/java/interface_adapter/watchlist/WatchlistController.java`
- `src/main/java/interface_adapter/watchlist/WatchlistPresenter.java`
- `src/test/java/interface_adapter/watchlist/**`

Exceptions — orchestrator-owned, read-only to this agent (they are the C↔D seam):
- `src/main/java/interface_adapter/watchlist/WatchlistViewModel.java`
- `src/main/java/interface_adapter/watchlist/WatchlistState.java`

---

## Never Touch

- `src/main/java/use_case/**` and `src/test/java/use_case/**` — Agent A. You implement
  the output boundaries; you do not edit them.
- `src/main/java/data_access/**` and `src/test/java/data_access/**` — Agent B
- `src/main/java/view/**` — Agent D and Member 4
- `src/main/java/app/Main.java` — orchestrator
- `src/main/java/entity/**` — frozen
- `interface_adapter/comparison/**`, `interface_adapter/persistence/**` — Member 4
- `WatchlistViewModel.java`, `WatchlistState.java` — orchestrator
- `pom.xml`, `plan/**`, `agents/**`

**Do not attempt to reuse `RecordingWatchlistPresenter`.** It is package-private in
`use_case.watchlist` and Agent A owns it. Your test constructs the real
`WatchlistViewModel` and the `*OutputData` objects directly; it needs no doubles. Asking
for that class to be widened would be a wasteful cross-package refactor.

---

## Reads (never writes)

- `agents/orchestrator.md` §5.3–§5.6, §7, §8 (especially H1)
- `interface_adapter/comparison/ComparisonPresenter.java` and `ComparisonViewModel.java`
  — **the pattern you must match.** Plain class, `PropertyChangeSupport`, no Swing
  imports, no generic `ViewModel<State>` base. This repo does not use the CSC207 lab-5
  generic base class; do not introduce it.
- `interface_adapter/comparison/ComparisonController.java` — controller shape
- `use_case/watchlist/WatchlistFailure.java` — the 11-value `Kind` enum you exhaustively
  map
- `use_case/watchlist/WatchlistSnapshot.java` — the input to every success path
- The four `*OutputBoundary` and `*OutputData` files
- `use_case/moving_average/ConfigureMovingAverageInteractor.java` — style reference
- `vision.md` §5 principle 9 (errors are words, never colour alone)

---

## Interface Contract

### Inputs — what the orchestrator provides before you start

Written at the head of Phase 3, before you are spawned:

- **`WatchlistState`** — final, immutable, value semantics, `static initial()`, with its
  own `TickerRow(symbol, companyName, priceCount, latestDate, latestClose)` and
  `PriceRow(date, open, high, low, close, volume)` records, and the getters listed in
  orchestrator §5.3. **Every field is a `String`.**
- **`WatchlistViewModel`** — `VIEW_NAME`, `STATE_PROPERTY`, `TICKER_COLUMNS`,
  `PRICE_COLUMNS`, `getState()`, `setState(WatchlistState)`,
  `addPropertyChangeListener(...)`.

Also already frozen from Phase 1 and delivered by Agent A in Phase 2: `WatchlistFailure`,
`WatchlistSnapshot`, and the four boundary pairs.

### Outputs — what other components depend on

**Agent D depends on exactly this, and nothing else from you:**

```java
public final class WatchlistController {
    public WatchlistController(AddTickerInputBoundary addTicker,
                               RemoveTickerInputBoundary removeTicker,
                               RefreshTickerInputBoundary refreshTicker,
                               ShowWatchlistInputBoundary showWatchlist);
    public void addTicker(String rawSymbol);
    public void removeTicker(String rawSymbol);
    public void refreshTicker(String rawSymbol);
    public void showWatchlist(String selectedSymbol);
}
```

All four are `void` and synchronous, and pass the raw string straight through —
normalization stays in the interactor where Agent A tests it. The controller does no
validation, no trimming, no formatting.

**The orchestrator (Phase 4) depends on:**

```java
public final class WatchlistPresenter
        implements AddTickerOutputBoundary,
                   RemoveTickerOutputBoundary,
                   RefreshTickerOutputBoundary,
                   ShowWatchlistOutputBoundary {
    public WatchlistPresenter(WatchlistViewModel viewModel);
}
```

One presenter, four `prepareSuccessView` overloads, one `prepareFailView`.

### The prose table — pin every row with a test

This is the **only** place in the codebase where user-facing strings may exist. The
view must never build a message.

`quoted(symbol)` renders `"AAPL"` as `"AAPL"` (with the quote marks), or the literal
`the symbol you typed` when the symbol is blank — so a blank-input failure reads as a
sentence rather than as `"" is not valid`.

| `WatchlistFailure.Kind` | Message |
|---|---|
| `BLANK_INPUT` | `Enter a ticker symbol before continuing.` |
| `BAD_FORMAT` | `%s is not a valid ticker symbol. Use letters, digits, dots, and hyphens only.` |
| `TOO_LONG` | `%s is too long. Ticker symbols are at most 10 characters.` |
| `DUPLICATE` | `%s is already on your watchlist.` |
| `NOT_ON_WATCHLIST` | `%s is not on your watchlist. Add it first.` |
| `NETWORK` | `Could not reach the market data service for %s. Check your connection and try again.` |
| `RATE_LIMIT` | `The market data service request limit has been reached. Wait a minute, then try %s again.` |
| `INVALID_SYMBOL` | `The market data service does not recognize %s.` |
| `EMPTY_RESPONSE` | `The market data service returned no price history for %s.` |
| `MALFORMED_RESPONSE` | `The market data for %s could not be read. Try again later.` |
| `MISSING_API_KEY` | `No market data API key is configured, so %s cannot be loaded. Set ALPHA_VANTAGE_API_KEY and restart.` |

Success messages:

| Situation | Message |
|---|---|
| Add, name known | `Added %s (%s) with %d days of price history.` |
| Add, no name | `Added %s with %d days of price history. No company name was available.` |
| Remove | `Removed %s from your watchlist.` |
| Refresh, history present | `Refreshed %s: %d days of price history, latest %s.` |
| Refresh, no history | `Refreshed %s, but no price history was returned.` |
| Show, non-empty | `Showing %d tickers.` |
| Show, empty | `Your watchlist is empty. Add a ticker to begin.` |

---

## Phase Tasks

### Phase 3 (only phase this agent is active)

**`WatchlistPresenter`.**

Snapshot → state mapping, with the substitutions that make the tables readable:
- `priceCount == 0` → the string `"Not loaded"`, not `"0"`. This is what a
  `LoadWatchlist`-restored ticker looks like before its prices are fetched, and `0` reads
  as an error.
- an empty `latestDate` or `latestClose` → `"—"` (em dash).
- an empty `companyName` → the symbol itself (`vision.md` principle 7: a missing company
  name must never be visible as a blank cell).

Two behavioural rules on `prepareFailView` that a test must pin:
1. It **copies `tickerRows` and `priceRows` from the current state**. A failure must never
   blank the tables — the user's watchlist disappearing because a refresh was rate-limited
   is a worse bug than the rate limit.
2. It **preserves `tickerFieldText`** so the user does not have to retype after a typo.
   Success clears it.

Every success path sets `errorMessage` to `""` and `statusMessage` to the row from the
table above. Every failure path sets `statusMessage` to `""` and `errorMessage` to the
failure row. `isErrorPresent()` is what the view uses to decide whether to show the error
label — **errors are conveyed in words, never colour alone**.

Use an exhaustive `switch` expression over `WatchlistFailure.Kind` with no `default`
branch, so adding a twelfth kind becomes a compile error rather than a silent fallthrough.

**`WatchlistController`.** Four one-line delegations. Constructor null-checks all four
boundaries with `Objects.requireNonNull(x, "... cannot be null")`.

**`WatchlistPresenterTest`.**
- One test per `WatchlistFailure.Kind` — all 11 — asserting the **exact** string via
  `assertEquals`, not `contains`. These strings are the deliverable; a substring check
  lets a typo through.
- One test per success message — all 7.
- A test that `prepareFailView` leaves `getTickerRows()` and `getPriceRows()` unchanged.
- A test that `prepareFailView` preserves and success clears `tickerFieldText`.
- A test that `priceCount == 0` renders as `"Not loaded"`.
- A test that an absent company name renders as the symbol.
- A test that `setState` fires `STATE_PROPERTY` exactly once per boundary call.

**`WatchlistControllerTest`.** Four tests asserting the raw string reaches the input
boundary untouched — including that lowercase and surrounding whitespace are **not**
stripped by the controller (that is the interactor's job, and testing it here would
duplicate Agent A's coverage while hiding a real regression if the controller ever starts
trimming).

**Done when:** `grep -rn "javax.swing" src/main/java/interface_adapter` returns nothing,
and 11/11 kinds plus 7/7 success messages are pinned.
