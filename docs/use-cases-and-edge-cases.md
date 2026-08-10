# MarketLens — Use Cases and Edge Cases

The ten use cases, the Clean Architecture chain behind each, and every failure path the code
actually handles. Every user-facing message is quoted verbatim from the source, so it matches
what the running application says.

Companion documents:

- [Architecture overview](architecture.md) — layers, the Dependency Rule, known violations
- [Use case diagram](use-case-diagram.md) — actors and the ten use cases
- [Entity class diagram](entity-class-diagram.md) — the innermost layer in full
- [Sequence diagrams](sequence-diagrams.md) — runtime call order for three of them

---

## The 10 use cases at a glance

| # | Use case | Actor | Touches network? | Touches disk? |
|---|---|---|---|---|
| 1 | Add Ticker | User | ✅ prices + name | ✅ via Save |
| 2 | Remove Ticker | User | ❌ never | ✅ via Save |
| 3 | Refresh Ticker | User | ✅ bypasses cache | ❌ |
| 4 | Show Watchlist | User / start-up | ❌ never | ❌ |
| 5 | Configure Moving Average | User | ❌ | ❌ |
| 6 | Configure Momentum | User | ❌ | ❌ |
| 7 | Run Backtest | User | ❌ | ❌ |
| 8 | Compare Strategies | User | ❌ | ❌ |
| 9 | Save Watchlist | **System** (sub-use-case of 1 & 2) | ❌ | ✅ |
| 10 | Load Watchlist | **System** (start-up only) | ❌ | ✅ |

9 and 10 have no UI button. Save is triggered *by* Add and Remove; Load runs once at start-up. That asymmetry is deliberate.

---

## Shared validation machinery

Three of the four watchlist use cases run the same preamble, so the edge cases are shared rather than duplicated.

**`TickerSymbolValidator`** — max length 10, pattern `[A-Z0-9.\-]+`. Checks in this exact order:

1. blank/null → `BLANK`
2. normalize (strips *internal* whitespace, so `" a a p l "` becomes a valid `AAPL`)
3. illegal characters → `ILLEGAL_CHARACTERS`
4. too long → `TOO_LONG`

> **The order is load-bearing.** A 30-character run of illegal characters reports `ILLEGAL_CHARACTERS`, not `TOO_LONG` — you tell the user the *first* thing wrong, not the last.

> **`Locale.ROOT` is deliberate.** On a Turkish-locale JVM `"titan".toUpperCase()` produces `"TİTAN"` (dotted capital I), which would split one holding across two cache keys.

**All 11 user-facing failure messages** live in one exhaustive `switch` in `WatchlistPresenter.messageFor(...)` — no `default` branch, so a new failure kind won't compile until someone writes its message.

| Kind | Message |
|---|---|
| `BLANK_INPUT` | Enter a ticker symbol before continuing. |
| `BAD_FORMAT` | "AA$PL" is not a valid ticker symbol. Use letters, digits, dots, and hyphens only. |
| `TOO_LONG` | … is too long. Ticker symbols are at most 10 characters. |
| `DUPLICATE` | "AAPL" is already on your watchlist. |
| `NOT_ON_WATCHLIST` | "TSLA" is not on your watchlist. Add it first. |
| `NETWORK` | Could not reach the market data service for … Check your connection and try again. |
| `RATE_LIMIT` | The market data service request limit has been reached. Wait a minute, then try … again. |
| `INVALID_SYMBOL` | The market data service does not recognize … |
| `EMPTY_RESPONSE` | The market data service returned no price history for … |
| `MALFORMED_RESPONSE` | The market data for … could not be read. Try again later. |
| `MISSING_API_KEY` | No market data API key is configured, so … cannot be loaded. Set ALPHA_VANTAGE_API_KEY and restart. |

When the symbol is blank, the placeholder reads **"the symbol you typed"** rather than empty quotes.

> **Failure preserves state.** `prepareFailView` copies the current rows, selection, status and *ticker field text* forward — so a rate-limited refresh never blanks the table, and a typo doesn't have to be retyped.

---

## UC 1 — Add Ticker

**Chain:** `WatchlistView.onAdd` (in a `SwingWorker`) → `WatchlistController.addTicker` → `AddTickerInputBoundary` → `AddTickerInputData{rawSymbol}` → `AddTickerInteractor` → `AddTickerOutputData{addedSymbol, companyName, priceCount, snapshot, companyNameFailureKind}` → `AddTickerOutputBoundary` → `WatchlistPresenter` → `WatchlistViewModel` → `WatchlistView.render`

**Happy path:**
1. View sends the raw text, buttons disabled
2. `resolve(raw, watchlist, MUST_BE_ABSENT)` → normalized symbol
3. **Fetch prices, then name — before any mutation**
4. Build `Stock` (last thing that can reject), then mutate watchlist + repo + save
5. `Added AAPL (Apple Inc.) with 100 days of price history.`

**Edge cases:**

| Condition | Result |
|---|---|
| `inputData == null` | **NPE on purpose** — a wiring bug, not a user error |
| Blank / illegal chars / too long | The three validation messages |
| Already on watchlist | `DUPLICATE` — case-insensitive, `Ticker.equals` uses `equalsIgnoreCase` |
| Price fetch throws | One of 6 provider messages. **Nothing mutated** — no half-added ticker |
| Name lookup returns empty | **Still a success:** `Added AAPL with 100 days of price history. No company name was available.` |
| Name lookup throws | **Still a success:** `… The company name could not be looked up right now.` — deliberately doesn't name the failure kind |
| Provider sends duplicate/out-of-order/null dates | `Stock` constructor rejects it → `MALFORMED_RESPONSE`. Nothing mutated |
| Empty price list, no error | Accepted — row reads `Not loaded` |

> **Fetch before mutate** is the clearest example in the project of ordering as a correctness
property.

---

## UC 2 — Remove Ticker

**Chain:** `WatchlistView.onRemove` → `WatchlistController.removeTicker` → `RemoveTickerInteractor` → `RemoveTickerOutputData{removedSymbol, snapshot}` → presenter → view

**Edge cases:** the three validation failures, plus `NOT_ON_WATCHLIST`.

> **This use case can never fail from network or quota** — it has no market-data dependency at all. Snapshot is built with `selectedSymbol = ""` so the price table clears; nothing is selected after a removal.

`lookupKey` builds `new Ticker(symbol, null)` — a null company name is correct for a *lookup* and never overwrites a stored name.

**Gap:** no confirmation prompt. A mis-click is irreversible from the UI.

---

## UC 3 — Refresh Ticker

**Chain:** `WatchlistView.onRefresh` → `WatchlistController.refreshTicker` → `RefreshTickerInteractor` → `RefreshTickerOutputData{symbol, priceCount, latestDate, snapshot}` → presenter → view

**Two design decisions worth naming:**

> **Membership check runs before the provider is touched.** Refreshing something you never added cannot spend a quota request.

> **Failure degrades to stale-but-usable.** A network error or exhausted quota leaves the previously stored history in place rather than losing what the user already had.

**Rate-limit circuit breaker:** `Load prices` refreshes tickers *sequentially* and returns the moment it sees a rate-limit message. The free tier allows ~25 requests/day, so marching through 8 tickers after the first 429 would burn the whole daily budget.

**Empty response handling:** the presenter guards on *both* the price count and the date —
`if (priceCount > 0 && hasText(latestDate))` — else `Refreshed AAPL, but no price history was returned.` Guarding on both is what stops a dangling `"latest ."` on screen.

**Also deliberate:** price history is *never* hydrated automatically at start-up. A restored 8-ticker watchlist would spend 8 requests on window open.

---

## UC 4 — Show Watchlist

**Chain:** row click → `WatchlistController.showWatchlist` → `ShowWatchlistInteractor` → `ShowWatchlistOutputData{tickerCount, snapshot}` → presenter → view

**Edge cases — note how many are handled by doing *nothing*:**

| Condition | Result |
|---|---|
| `null` selected symbol | Normalized to `""` by `ShowWatchlistInputData` itself |
| Invalid symbol | Returns `""`. **Silent, not an error** |
| Symbol not on watchlist | Returns `""` — a stale table selection is "ordinary and self-correcting" |
| Empty watchlist | `Your watchlist is empty. Add a ticker to begin.` |

> **Design asymmetry enforced by arity.** This interactor takes **3** constructor arguments where Add takes **5**. It performs zero I/O — no gateway, no save. Clicking a row costs nothing against the quota, and you can't accidentally add I/O without changing the signature.

> **This is the one success path that *populates* the ticker field instead of clearing it.** If it cleared, clicking the AAPL row and pressing Refresh would answer "Enter a ticker symbol before continuing." with the row plainly selected.

**Two guards worth mentioning:**
- **Recursion guard** — repopulating the table fires selection events that would otherwise re-drive this use case
- **Concurrency guard** — the table itself is disabled during a background worker, because `prepareFailView` does a non-atomic read-modify-write

> **Best boundary-design talking point in the project:** `ShowWatchlistOutputBoundary.prepareFailView` is declared, implemented and unit-tested — but **no interactor ever calls it.** It exists purely for symmetry. An output boundary method with a provably empty caller set is a great "is this good design or dead code?" discussion.

---

## UC 5 — Configure Moving Average

**Chain:** `MovingAverageConfigurationView` → `MovingAverageController.configure(short, long)` → `ConfigureMovingAverageInteractor` → `ConfigureMovingAverageOutputData{configuration}` → presenter → view

**Validation is split in two — this is the interesting part:**

*Interactor owns parsing:*

| Condition | Message |
|---|---|
| null input data | Configuration input cannot be null |
| short blank | Short window is required |
| long blank | Long window is required |
| short not an integer | Short window must be a whole number |
| long not an integer | Long window must be a whole number |

*Entity owns the invariants* — thrown as `IllegalArgumentException` and forwarded verbatim:

| Condition | Message |
|---|---|
| `shortWindow <= 0` | Short window value must be positive |
| `longWindow <= 0` | Long window value must be positive |
| `shortWindow >= longWindow` | Long window must be greater than short window |

The view writes both raw strings back into the state *before* submitting, so a rejected submission stays visible and editable, and focus returns to the short-window field with the text selected.

---

## UC 6 — Configure Momentum

**Chain:** `MomentumConfigurationView` → `MomentumController.execute(period, oversold, overbought)` → `ConfigureMomentumInteractor` → presenter → view. Defaults: period 14, oversold 30, overbought 70.

*Entity invariants:*

| Condition | Message |
|---|---|
| `period <= 1` | RSI period must be greater than 1 |
| oversold NaN, `< 0` or `> 100` | Oversold threshold must be between 0 and 100 |
| overbought NaN, `< 0` or `> 100` | Overbought threshold must be between 0 and 100 |
| `oversold >= overbought` | Oversold threshold must be smaller than overbought threshold |

> **The best single edge case in the project.** `Double.parseDouble("NaN")` **succeeds**, and `NaN < 0` and `NaN > 100` are **both false** — so without the explicit `Double.isNaN` guard, a NaN threshold would slip silently through every range comparison. `"Infinity"` is caught by `> 100`, `"-Infinity"` by `< 0`, but NaN needs its own check.



---

## UC 7 — Run Backtest

**Chain:** `BacktestView.onRun` → `BacktestController` → `RunBacktestInteractor` → `RunBacktestOutputData{backtestResult}` → **anonymous decorator in `Main` files the result into `CompletedBacktestStore`** → `BacktestPresenter` → `BacktestViewModel` → `BacktestResultsView`

**Trading semantics:**
- Initial capital `$10,000`
- **A day-`i` signal executes at the day-`i+1` OPEN** — this is how look-ahead bias is avoided
- **Consequence: the final day's signal is never executed**
- BUY only when flat, SELL only when long — no pyramiding, no shorting
- Whole shares only
- An open position at the end is liquidated at the final close
- Win rate = trades with return > 0; **0.0 for an empty trade log**

**Engine guards** (all forwarded verbatim to the user): `Ticker cannot be null`, `Strategy cannot be null`, `Prices cannot be null`, `Price history cannot be empty`, `Price entries cannot be null`, `Opening and closing prices must be positive`, `Prices must be ordered oldest to newest`, `Strategy must produce one signal per price`, `Signal entries cannot be null`.

**Insufficient-data guards:**

| Strategy | Condition | Message |
|---|---|---|
| Moving Average | `prices.size() < longWindow + 1` | Not enough price history to calculate a crossover |
| RSI Momentum | `prices.size() < period + 1` | Insufficient price history for RSI calculation |

> The `+ 1` on the MA side is because detecting a *crossover* needs the averages for both the current **and** the previous date.

**RSI division-by-zero — three guards before the ratio:**
- both averages zero (perfectly flat prices) → **50.0**
- no losses → **100.0**
- no gains → **0.0**

**View-level guards:** empty repository disables the Run button with an instruction; defaults `(5, 20)` and `(14, 30, 70)` are chosen specifically to fit the free tier's ~100-trading-day response, so neither strategy throws on a freshly loaded ticker.

---

## UC 8 — Compare Strategies

**Chain:** `ComparisonView` → `ComparisonController.compare()` (**reads `CompletedBacktestStore` itself, so the view never names an entity type**) → `CompareStrategies.Interactor` → `ComparisonOutputData{resultsRankedByReturn, best}` → `ComparisonPresenter` → view

Ranking is `Comparator.comparingDouble(BacktestResult::getTotalReturn).reversed()`, best first.

**Edge cases:**

| Condition | Result |
|---|---|
| Store empty | `Run at least one backtest before comparing strategies.` — **the case every demo hits first** |
| Error state | Table cleared, heading resets, message in the status label |
| Exactly one result | Works — trivially the best |

> **`setError` deliberately clears the previous ranking.** Leaving a stale ranking visible underneath an error message would present old data as though it were current.

The store hands out `Collections.unmodifiableList`, so the interactor can't mutate it.

---

## UC 9 — Save Watchlist (system)

Triggered by Add and Remove. **Refresh deliberately does not trigger it** — refreshing changes prices, not membership, and prices aren't persisted.

**The durability story:**

1. Serialize to `watchlist.dat.tmp`
2. Atomically move into place
3. On `AtomicMoveNotSupportedException`, retry with `REPLACE_EXISTING`
4. On `IOException`, delete the temp and throw `PersistenceException`
5. If the temp cleanup itself fails, swallow it — the caller is already throwing

> **Why write-then-swap:** a crash partway through a save can't truncate the previous good file. A truncated file is exactly the corruption `load()` then has to recover from.

> **`Stock` is deliberately not `Serializable`.** Only `Watchlist → WatchlistEntry → Ticker` is persisted. Price history is excluded on purpose, so the save file format is completely unaffected by market data.

---

## UC 10 — Load Watchlist (system, start-up only)

**Edge cases — this is the richest set in the project:**

| Condition | Handling |
|---|---|
| File doesn't exist (first run) | Return empty watchlist. **Not an error** |
| Corrupted / truncated / garbled | Back the file up, return a fresh watchlist — rather than blocking the user from ever opening the app |
| Deserialized fine but wrong type | Throws `InvalidClassException` internally, caught by the clause above |
| Backup filename collision | Loop to `…corrupted-20260809-141530-2`, `-3`, … |
| Backup itself fails | **Throw** — refuse to silently reset data you can't preserve |
| Any other `IOException` | Throw — don't silently reset the user's data |

> **Two details worth noting.** (a) The wrong-type case is thrown *internally and caught one frame up* rather than handled inline — because the streams must close first; **moving a file you still hold open fails on Windows.** (b) The backup timestamp is only second-precision, so two recoveries in the same second would collide — hence the suffix loop. *A backup that clobbers the previous backup is no backup.*

> **Restored tickers show `Not loaded` until you click Load prices.** The repository is in-memory only. This is deliberate quota protection, not a bug — worth saying before a TA asks.

---

## Entity coverage matrix

| Entity | Add | Rem | Refr | Show | MA | Mom | Back | Cmp | Save | Load |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| `Ticker` | ● | ● | ● | ● | | | ● | ● | ● | ● |
| `Watchlist` | ● | ● | ● | ● | | | | | ● | ● |
| `WatchlistEntry` | ● | ● | ● | ● | | | | | ● | ● |
| `Stock` | ● | ● | ● | ● | | | ● | | | |
| `DailyPrice` | ● | ● | ● | ● | | | ● | | | |
| `MovingAverageConfiguration` | | | | | ● | | ● | | ○ | ○ |
| `MomentumConfiguration` | | | | | | ● | ● | | ○ | ○ |
| `MovingAverageCrossoverStrategy` | | | | | | | ● | | | |
| `RSIMomentumStrategy` | | | | | | | ● | | | |
| `TradingStrategy` / `TradingSignal` / `SignalType` | | | | | | | ● | | | |
| `BacktestEngine` | | | | | | | ● | | | |
| `BacktestResult` | | | | | | | ● | ● | | |
| `Trade` | | | | | | | ● | ● | | |

● = touched  ○ = serializable and reachable in principle, but the setters are never called

**Run Backtest has the widest entity footprint of any use case** — 13 of them.

---

---

## Known gaps

Named here rather than left to be discovered. Each is real; none is hidden behind a comment that
claims otherwise.

### Fixed since this catalogue was first written

- **Repeat backtests were counted twice.** Running the same ticker and strategy twice put two
  identical rows in the ranking and two identical bars in the chart.
  `CompletedBacktestStore` now identifies a run by ticker and strategy name and replaces the
  earlier one.
- **`Watchlist`, `Ticker` and `DailyPrice` declared no `serialVersionUID`.** Any future field
  change would have shifted the JVM's computed UID, turning every existing save file into an
  `InvalidClassException` — which the DAO reads as corruption and recovers from by resetting. All
  three now pin the UID the JVM had already computed, so files written by earlier builds still
  load.

### Open

1. **`BacktestView` constructs entities directly.** It imports eight entity types and builds
   strategy objects itself — a Frameworks & Drivers class reaching past the adapter layer into
   Entities. The project's remaining Dependency Rule violation; see
   [Architecture overview §4](architecture.md).
2. **Save failures never reach the use case that caused them.** `AddTickerInteractor` and
   `RemoveTickerInteractor` call `saveWatchlist.execute(...)` fire-and-forget, so the app can show
   `Added AAPL…` in the watchlist panel and `Could not save watchlist: …` in the status bar at the
   same moment.
3. **A failed load can destroy the save file.** On a recoverable failure such as a permission
   error, `Main` substitutes an empty `Watchlist` and continues — and the next Add or Remove writes
   that empty watchlist over the real file. There is no "do not save until load succeeded" flag.
4. **Corruption recovery is silent.** The file is backed up, but nothing in the UI ever says so:
   the presenter is only reached on the throwing paths, and recovery does not throw.
5. **Strategy configurations are never persisted.** Both setters on `WatchlistEntry` exist and both
   configuration classes are `Serializable`, but nothing calls either setter. They are also global
   rather than per-ticker, despite `WatchlistEntry` being modelled per-ticker.
6. **The rate-limit circuit breaker is a string-prefix match.** `WatchlistView` compares against the
   opening sentence of the presenter's rate-limit message. Reword that message and `Load prices`
   silently starts spending the whole daily quota again. No test enforces the link.
7. **A bad configuration edit un-configures the strategy.** Both configuration presenters clear the
   previously valid configuration on a parse failure, so a typo loses the working settings.
8. **`RunBacktestInteractor`'s catch list is not exhaustive.** It catches
   `IllegalArgumentException`, `NullPointerException` and `IllegalStateException`, so any other
   `RuntimeException` escapes — and `BacktestView` runs the backtest on the event dispatch thread
   with no `SwingWorker`, unlike `WatchlistView`, so it would surface only in the console while the
   UI froze.
9. **`CompletedBacktestStore` is unbounded, never cleared, and not thread-safe.** There is no
   "clear results" control, and it is a plain `ArrayList`. The thread-safety part only becomes
   reachable if the backtest moves off the event thread — which gap 8 would do.
10. **Ties in the ranking resolve by insertion order.** `Stream.sorted` is stable, so two strategies
    with an identical total return leave the earlier-run one as best. Nothing surfaces the tie.
