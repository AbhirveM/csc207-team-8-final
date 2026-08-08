# Phase 4 — Composition Root

**Active agents:** orchestrator only, then reviewer.
**Goal:** the feature actually runs — offline, with no API key.

---

## Dependencies to install

None.

---

## Files to modify

The first two belong to Member 4. Touch them **append-only**, in the spots his own comments
designate, and ping him (`vision.md` §9).

| File | Owner | Change |
|---|---|---|
| `view/MainView.java` lines 30–33 | Member 4 | uncomment the watchlist nav-button template; use `WatchlistViewModel.VIEW_NAME` instead of a string literal |
| `app/Main.java` lines 42–44 | Member 4 | replace the TODO with the `// --- Watchlist (Member 1) ---` wiring block |
| `interface_adapter/watchlist/WatchlistViewModel.java` | orchestrator (§2) | add `SAMPLE_DATA_STATUS` — the offline notice. See §7's one documented prose exception |
| `view/WatchlistView.java` | **Agent D** | one line in `setButtonsEnabled` to also disable `tickerTable`, closing W3-5. An ownership crossing in an agent-less phase; recorded as a deviation |
| `plan/phases.md`, `plan/phase-4.md` | orchestrator | D3-c prose fix, and this table |

Do not restructure `MainView`'s constructor. There is no `addNavButton` API and adding one
is out of scope.

---

## What to implement

Place the wiring block between the Persistence block and the Comparison block, following
the existing `ViewModel → Presenter → Interactor → Controller → View` declaration order.

**Gateway selection — the only place in the codebase that may read the environment:**

```
AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()
  present → new CachingMarketDataGateway(new AlphaVantageMarketDataAccessObject(key))
  absent  → InMemoryMarketDataGateway.withSampleData()
            + a visible status line stating that sample data is in use
```

Never a `.env`, never a default key in code, never a key in a message. This call is
`apiKeyFromEnvironment()`'s first production caller — it has had none until now.

**Then, in order:**

1. Seed a `Watchlist` — call `loadWatchlistInteractor.execute()` and read the result from
   `persistenceViewModel`, falling back to `new Watchlist()`. The
   `saveWatchlistInteractor` and `loadWatchlistInteractor` locals at lines 38–41 are
   currently constructed and never consumed; this is what consumes them.
2. `new InMemoryStockRepository()`
3. `new WatchlistViewModel()`
4. `new WatchlistPresenter(watchlistViewModel)`
5. The four interactors. Note the deliberate constructor asymmetry:
   - `AddTickerInteractor(watchlist, gateway, stockRepository, saveWatchlistInteractor, presenter)`
   - `RemoveTickerInteractor(watchlist, stockRepository, saveWatchlistInteractor, presenter)`
   - `RefreshTickerInteractor(watchlist, gateway, stockRepository, presenter)` — **no
     save**; refresh changes prices, not membership
   - `ShowWatchlistInteractor(watchlist, stockRepository, presenter)` — no gateway, no save
6. `new WatchlistController(add, remove, refresh, show)`
7. `new WatchlistView(watchlistViewModel, watchlistController)`
8. `mainView.addView(WatchlistViewModel.VIEW_NAME, watchlistView)`
9. Inside the existing `SwingUtilities.invokeLater`, call
   `watchlistController.showWatchlist("")` so a restored watchlist renders immediately
   rather than after the first user action.

---

## Verification

1. `mvn -o clean verify` — green.
2. `git diff app/Main.java view/MainView.java` is **additive only** — no existing line
   deleted or reordered.
3. `grep -rn "System.getenv" src/main/java` returns exactly one hit, in
   `AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()`, called only from `Main`.
4. **Walk the `vision.md` §8 script by hand, with `ALPHA_VANTAGE_API_KEY` unset:**
   - Launch it. There is no `exec-maven-plugin` in `pom.xml` and adding one is a shared
     `pom.xml` edit (`agents/orchestrator.md` §2), so use the recipe already recorded in
     `plan/handoffs/screenshots.md` — `mvn -o clean compile`, then
     `mvn -o dependency:build-classpath -Dmdep.outputFile=target/cp.txt -q`, then
     `java -cp "target/classes;$(cat target/cp.txt)" app.Main` — or run `app.Main` from
     IntelliJ. A Watchlist nav button is present; the status line says sample data is in use.
   - Type `aapl` → Add → it normalizes to `AAPL`, the company name resolves, the price
     table fills with newest-first rows (D3-c, resolved at this gate).
   - Refresh → the day count and latest date update.
   - Click a different row → the price table repopulates. *(This is the whole reason
     `ShowWatchlist` exists.)*
   - Remove → the row disappears.
   - Type a junk symbol → a specific worded error appears in the error label, in prose,
     not colour alone.
   - Close and relaunch → the watchlist is still there, showing `"Not loaded"` until
     "Load prices" is pressed.
5. Resize the window and confirm the split pane and tables behave.
6. Tab through the whole panel and confirm the focus order matches the visual order.
7. Capture the **"after" screenshot** for the individual presentation. The "before" one
   should already exist from Phase 3 (`plan/handoffs/screenshots.md`).
8. Reviewer writes `plan/review-phase-4.md`; status is not FAIL.
