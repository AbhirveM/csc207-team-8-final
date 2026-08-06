# MarketLens — Vision

**Owner of this document:** Abhirve Munipalle (`AbhirveM`), Member 1
**Repo:** `github.com/AbhirveM/csc207-team-8-final` (CSC207 Team 8)
**Written:** 2026-08-06 · **Presentation:** ~2026-08-10

---

## 1. The product

MarketLens is a Java desktop application that lets a user explore how predefined
trading strategies would have performed against real historical stock-market data.

From the team blueprint, verbatim:

> The program allows users to explore how predefined trading strategies would have
> performed using historical stock-market data. Through a desktop interface, users can
> create a watchlist by adding and removing ticker symbols. For each accepted ticker,
> the program displays its symbol, company name when available, and available daily
> opening price, highest price, lowest price, closing price, and trading volume.
> Invalid symbols, unavailable data, and network failures produce clear error messages
> instead of causing the program to crash.

The central workflow — the team story — is: **add a stock → configure a strategy → run
it against its available historical prices → view a performance summary.**

### Non-goals

Stated explicitly so scope creep is easy to refuse:

- No real trades, no brokerage connection, no financial advice.
- No price prediction, and no claim that past performance predicts future results.
- No user-entered code or plug-ins. The two strategies are built in and configurable.
- No portfolio optimization, fees, taxes, shorting, or fractional shares.

Stretch goals only if everything else is done: buy-and-hold benchmark, charts,
additional strategies, CSV export, flexible date ranges.

---

## 2. Where the project actually is (2026-08-06)

This matters more than the plan on paper, because the team's own schedule called today
**code-freeze day** and it has slipped badly.

**On `main` (`4aa066b`):**

| Area | State | Owner |
|---|---|---|
| Shared strategy contract (`TradingStrategy`, `SignalType`, `TradingSignal`) | Done | Member 2 |
| Moving Average config + crossover strategy (+ its config use case) | Done | Member 2 |
| Watchlist persistence (save/load, corrupted-file recovery) | Done | Member 4 |
| Strategy comparison use case | Done | Member 4 |
| Navigation shell (`MainView`, `ViewManager`, `CardLayout`) | Done | Member 4 |
| **RSI Momentum strategy** | Open PR #21, changes requested | Member 3 |
| **Backtest engine** | **Does not exist** | Member 3 |
| **Watchlist + Alpha Vantage market data** | **Does not exist** | **Member 1 (me)** |

**The consequence:** nothing in the application can currently produce a
`BacktestResult`. `MainAppState.addCompletedResult` has zero callers, so the Compare
screen always shows its empty-state message. **The app has no working happy path**, and
the two missing pieces are the backtest engine and my market-data slice.

Supporting gaps nobody owns: no Checkstyle config (the rubric requires a tool like it
to score above 3/5 on code quality), no `accessibility-report.md`, no CI, and no
`serialVersionUID` on any of the seven `Serializable` entities.

---

## 3. My scope: the Member 1 vertical

From the blueprint's ownership table, verbatim:

> **Member 1 — Watchlist and Alpha Vantage market data**
> - Own Watchlist, Stock, and the first proposed version of DailyPrice for team review.
> - Implement Add Ticker, Remove Ticker, and Refresh Ticker use cases.
> - Normalize ticker input, reject blank/duplicate tickers, and validate provider responses.
> - Define MarketDataGateway and implement AlphaVantageMarketDataAccessObject.
> - Convert API responses to immutable DailyPrice objects sorted oldest-to-newest.
> - Retrieve company information when available.
> - Build the watchlist controller, presenter, view model/state, and Swing view.
> - Provide a fake market-data gateway so all development and tests can run offline.
> - Handle network, quota, invalid-symbol, empty-response, and malformed-response failures.
>
> **Handoff:** A Stock containing a documented oldest-to-newest `List<DailyPrice>` that
> Members 2 and 3 can use without knowing anything about Alpha Vantage JSON.
>
> **Not responsible for:** Moving Average, RSI, trade simulation, result comparison, or
> persistence.

My two user stories:

1. As a user, I want to add a valid ticker and automatically receive its company
   information and daily price history, so that I do not enter prices manually.
2. As a user, I want to remove or refresh a ticker and receive a clear explanation when
   a symbol is invalid or market data cannot be retrieved.

Acceptance criteria, verbatim: *"Valid ticker creates one watchlist entry with ordered
daily prices; duplicate/blank ticker fails; remove/refresh works; API/network/quota
errors do not crash; tests use a fake gateway."*

---

## 4. Contracts I must not break

These are already merged and other people's code depends on them. My slice plugs into
them; it does not renegotiate them.

**What I must produce** — the strategies consume exactly this:

```java
public interface TradingStrategy {
    String getName();
    List<TradingSignal> generateSignals(List<DailyPrice> prices);
}
```

`MovingAverageCrossoverStrategy.generateSignals(prices)` requires:
- prices ordered **oldest → newest**
- **no null elements** (it throws `NullPointerException`)
- `size() >= longWindow + 1`, else `IllegalArgumentException("Not enough price history
  to calculate a crossover")`
- it reads only `getDate()` and `getClose()`

**Entities as they exist on `main`** (I own `DailyPrice`; Member 4 owns the rest):

```java
DailyPrice(LocalDate date, double open, double high, double low, double close, long volume)
// no validation, no equals/hashCode, no serialVersionUID

Ticker(String symbol, String companyName)
// no validation; equals/hashCode are case-insensitive on symbol only; NPEs on a null symbol

WatchlistEntry  // holds ONLY a Ticker; strategy-config fields are commented out (issue #7)

Watchlist       // addTicker silently no-ops on duplicates and reports nothing back;
                // getEntries() returns the LIVE internal list
```

**Persistence boundary I call rather than reimplement:**

```java
SaveWatchlist.InputBoundary.execute(Watchlist watchlist)
```

**Hard constraint the rest of the team does not yet know:** Alpha Vantage's free
`TIME_SERIES_DAILY` compact response returns roughly the **latest 100 trading days**
(full history is premium). A 50-day long window needs 51 records and leaves ~49 signal
days — workable, but **any strategy window above ~90 silently violates the
`generateSignals` precondition.** Members 2 and 3 need to hear this.

---

## 5. Design principles for my slice

These are the decisions I want to hold to, and the reason for each — they're the
substance of my individual presentation.

1. **The gateway interface lives in the use-case layer, implemented in `data_access`.**
   Interactors depend on the port, `Main` injects the implementation. This is the
   Dependency Inversion arrow and the best slide in my deck. A grader hunting for
   Dependency Rule violations must find zero.

2. **Price history never enters the persisted entity graph.** `WatchlistEntry` is
   Member 4's file with open issue #7 on it, and nothing in the codebase declares a
   `serialVersionUID`. Adding a field there changes the computed UID, so every existing
   `watchlist.dat` throws `InvalidClassException`, and Member 4's own recovery code
   renames it `.corrupted-*` and returns an empty watchlist. That would destroy
   teammates' data during demo week and look like *their* bug. Prices live in a store I
   own. Framing for the presentation: **watchlist membership is durable; market data is
   cached and re-fetched.**

3. **The blueprint's `Stock` is realized by composition, not by renaming `Ticker`.**
   `Ticker` stays the persisted identity; `Stock` adds a validated price history and is
   deliberately not `Serializable`. Renaming `Ticker` would break `BacktestResult`,
   `ComparisonView`, and the saved file format.

4. **The oldest→newest guarantee is an invariant, not a comment.** Enforce it in a
   constructor so a strategy can never receive reversed, sparse, or null-containing
   data.

5. **No entity crosses the output boundary.** Only strings and numbers. This
   simultaneously contains `Watchlist.getEntries()`'s live-list leak, prevents a
   background UI refresh from tripping a concurrent-modification failure, and keeps the
   view model free of Swing *and* entity imports.

6. **HTTP sits behind an injectable seam.** The single highest-leverage decision
   available: it turns an untestable network class into a fully unit-tested one with no
   network and no mocking library, and it lets a test assert the request URL names both
   `TIME_SERIES_DAILY` and `OVERVIEW` — which is the API rubric's top band, *proven by
   test rather than claimed on a slide*.

7. **A missing company name must never block adding a ticker.** `OVERVIEW` returns `{}`
   for many valid symbols (ETFs especially) and is the first thing cut off when the
   quota runs out. Fall back to the symbol. A demo that dies on a symbol that worked
   yesterday is the worst possible failure.

8. **The offline fake is production code, not a test helper.** The blueprint mandates a
   fake for development *and* tests; the application needs it as a fallback when no API
   key is configured; and the demo must not depend on the network or a credential. Its
   sample data must genuinely oscillate — a flat series yields nothing but HOLD signals
   and makes both the demo and the hand-off test vacuous.

9. **Validation errors are words, never colour alone**, and every input carries a
   visible label. Cheap to do, and it's an entire rubric category.

10. **Fetch before mutate.** A provider failure must never leave a half-added ticker.

---

## 6. Architecture

Package map (the blueprint's, which the repo already follows):

```
entity                      DailyPrice (mine), Stock (mine), Ticker, Watchlist, WatchlistEntry
use_case.watchlist          MY SLICE: gateway port, failure type, validator,
                            add/remove/refresh boundaries + interactors
use_case.moving_average     Member 2 (done)
use_case.backtest           Member 3 (missing)
use_case.persistence        Member 4 (done)
use_case.comparison         Member 4 (done)
interface_adapter.watchlist MY SLICE: controller, presenter, view model/state
data_access                 MY SLICE: Alpha Vantage DAO, HTTP seam, offline fake, caching
view                        MY SLICE: WatchlistView. Shell is Member 4's.
app                         Member 4's composition root; I add wiring for my slice
```

**Boundary convention:** five separate top-level files per use case
(`XInputBoundary`, `XInputData`, `XInteractor`, `XOutputBoundary`, `XOutputData`),
matching `use_case.moving_average`. Member 4 nests his (`SaveWatchlist.InputBoundary`);
both styles exist on `main`, and separate files read better as boxes on a class diagram.

**API usage:** `TIME_SERIES_DAILY` (prices) and `OVERVIEW` (company name). Indicators
are computed locally by the strategy classes, so no indicator endpoint is called.
Naming two real endpoints is what the API rubric's top band requires.

**Threading:** network calls must not run on the Swing event thread — a blocking call
freezes the window for seconds and will be visible on stage. The background worker
belongs in the **view**, the only layer allowed to import Swing; interactors and
presenter stay synchronous and therefore trivially testable. Corollary: the presenter
then updates the view model off-thread, so the view's change handler must marshal back
onto the event thread.

**The view sequence:** ticker field → Add / Remove / Refresh buttons → watchlist table →
daily-price table → status and error text. That order is also the keyboard focus order.

---

## 7. Constraints

| Constraint | Implication |
|---|---|
| Free tier: ~25 requests/day; compact = ~100 trading days | Cache responses; develop and test against the fake; strategy windows stay under ~90 |
| **I have no API key right now** | Build the DAO against canned JSON fixtures; live smoke test is a deferred follow-up (one add + one refresh = 3 requests) |
| API key must stay out source control | Read from `ALPHA_VANTAGE_API_KEY` only, at the composition root. Never a `.env`, never a default in code |
| No live API calls in unit tests | Non-negotiable per the blueprint |
| `pom.xml` has zero runtime dependencies | Needs a JSON library; HTTP can use the JDK client. **Shared file — coordinate before editing** |
| Local JDK is **24**, project targets 17 | Tooling must support 24 (e.g. JaCoCo 0.8.12 cannot instrument it; 0.8.13 can) |
| No CI, no branch checks | "Tests pass" is enforced socially: `mvn clean install` plus one teammate approval |
| Raw prices are unadjusted | Splits and dividends distort long comparisons — document it |

---

## 8. What "done" means

Per-PR gate (blueprint Definition of Done): feature branch, agreed contracts respected,
entity and interactor tests passing, demonstrable through a Swing view or harness, PR
explains the behaviour and links its issue, **one teammate approves**, no keys or build
folders committed, and I can explain the entity, interactor, boundaries, presenter, and
dependency direction out loud.

Team quality targets: >90% line coverage on use-case interactors, >70% overall with
documented exclusions, Checkstyle enforced, every member reviews at least one teammate
PR, no live API calls in tests, accessibility report completed.

My slice is done when, **with no API key and no network**, I can: type `aapl` → it
normalizes to `AAPL` → the company name resolves → the price table fills with sorted
rows → Refresh updates the count and latest date → Remove drops the row → typing a
junk symbol shows a specific, worded error → and restarting the app still shows the
watchlist. Plus: a test that feeds my output straight into
`MovingAverageCrossoverStrategy` and gets real signals back — the executable proof of
the hand-off.

For the individual presentation I additionally need: **a "before" screenshot captured
before my view is wired in** (it cannot be recreated afterwards), an "after"
screenshot, my interactor's code, and a class diagram of the full use case.

---

## 9. Risks

| Risk | Response |
|---|---|
| Four days to the presentation, and my slice is the whole vertical | Front-load the graded content: gateway and fake first (also unblocks Member 3), then Add Ticker and the DAO, then the Swing view. Cut the caching subtlety before cutting a named use case |
| `pom.xml` conflicts — everyone edits the same lines, and PR #21 is open | One isolated edit, announced first, merged before anyone rebases |
| `Main.java` / `MainView.java` are Member 4's | Touch them only in the last PR, in the spot his own comments designate, append-only, and ping him |
| Adding fields to `WatchlistEntry` silently wipes saved watchlists | Never open that file; tell Member 4 why issue #7 should stay deferred until after the demo |
| Adding validation to `DailyPrice`/`Ticker` breaks teammates' tests | Additive changes only (`equals`/`hashCode`/`toString`); all validation goes in my interactor and DAO |
| 25 requests/day evaporates during manual testing | Cache; default to the fake; the live test is 3 requests total |
| **No backtest engine means no team happy path** | Raise it today, not the day before. My slice demos standalone. If the engine isn't merged by Aug 8 EOD, the team demo should be scripted honestly around the empty state |
| Promising a live API demo on stage | Don't. I have no key; the fake is deterministic and that's a feature, not an excuse |

---

## 10. Open decisions

1. **Do I push the ~4 lines of wiring into `Main.java` / `MainView.java` myself, or hand
   Member 4 a snippet?** (Integration is his issue #9.)
2. **JSON library choice** — a small dedicated library, or hand-rolled parsing to avoid
   touching `pom.xml` at all?
3. **Coverage evidence** — add a coverage plugin (report only, never a build gate), or
   screenshot the IDE's coverage run?
4. **Issue #8** is assigned to me and still open: confirm the `ALPHA_VANTAGE_API_KEY`
   environment-variable approach that the README already documents.
5. **No issue exists for the watchlist feature at all.** Every other issue belongs to a
   teammate. One should be filed before the work starts, so the git history shows the
   feature was tracked like everything else.
6. **Unowned team gaps** — Checkstyle, `accessibility-report.md`, `serialVersionUID` on
   the entities. Raise and assign rather than silently absorb.
