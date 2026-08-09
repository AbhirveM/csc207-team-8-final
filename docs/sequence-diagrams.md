# MarketLens — Sequence Diagrams

Runtime call order through the Clean Architecture layers for three representative use cases: one
from each member's vertical, chosen because each shows something the class diagrams cannot.

- [Add Ticker](#add-ticker) — why ordering is a correctness property
- [Run Backtest](#run-backtest) — how two features meet without referencing each other
- [Load Watchlist](#load-watchlist-start-up) — corruption recovery

---

## Add Ticker

The order of the first four interactor steps is load-bearing: **every failure path returns before
anything mutates.**

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant View as WatchlistView
    participant Ctrl as WatchlistController
    participant Int as AddTickerInteractor
    participant Support as WatchlistInputSupport
    participant Gateway as MarketDataGateway
    participant WL as Watchlist
    participant Repo as StockRepository
    participant Save as SaveWatchlist
    participant Pres as WatchlistPresenter
    participant VM as WatchlistViewModel

    User->>View: type "aapl", click Add
    View->>View: disable buttons, start SwingWorker
    View->>Ctrl: addTicker("aapl")
    Ctrl->>Int: execute(AddTickerInputData)

    Int->>Support: resolve(raw, watchlist, MUST_BE_ABSENT)
    Support-->>Int: Resolution(symbol="AAPL")

    alt blank / bad format / too long / duplicate
        Int->>Pres: prepareFailView(WatchlistFailure)
        Note over Int: returns — nothing mutated
    end

    Int->>Gateway: fetchDailyPrices("AAPL")
    Gateway-->>Int: List of DailyPrice

    alt MarketDataException
        Int->>Pres: prepareFailView(from(exception))
        Note over Int: returns — nothing mutated
    end

    Int->>Gateway: fetchCompanyName("AAPL")
    Gateway-->>Int: Optional of String
    Note over Int,Gateway: A failure here is recorded, not fatal —<br/>the add still succeeds

    Int->>Int: new Stock(ticker, prices)
    Note over Int: Stock validates oldest-to-newest,<br/>no duplicate dates. Last thing<br/>that can reject the data.

    Int->>WL: addTicker(ticker)
    Int->>Repo: save(stock)
    Int->>Save: execute(watchlist)

    Int->>Pres: prepareSuccessView(AddTickerOutputData)
    Pres->>VM: setState(WatchlistState)
    VM-->>View: propertyChange(STATE_PROPERTY)
    View->>User: "Added AAPL (Apple Inc.) with 100 days of price history."
```

**What to point at.** Both gateway calls and the `Stock` construction happen *above*
`watchlist.addTicker` — everything that can fail runs before the first mutation. So a network error,
a rate limit or a malformed price series can never leave a half-added ticker behind. Building the
`Stock` before touching the watchlist extends that guarantee to the validation failure too, since
`Stock`'s constructor is the last thing that can reject the provider's data.

**Where it is imperfect.** `saveWatchlist.execute(...)` returns `void` and the interactor never
checks it, so a failed save still reaches `prepareSuccessView`. The user sees "Added AAPL…" in the
watchlist panel and "Could not save watchlist: …" in the window's status bar at the same moment.

---

## Run Backtest

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant View as BacktestView
    participant Repo as StockRepository
    participant CfgVM as Momentum / MovingAverage<br/>ViewModel
    participant Ctrl as BacktestController
    participant Int as RunBacktestInteractor
    participant Engine as BacktestEngine
    participant Strat as TradingStrategy
    participant Deco as RunBacktestOutputBoundary<br/>(decorator in Main)
    participant Store as CompletedBacktestStore
    participant Pres as BacktestPresenter
    participant VM as BacktestViewModel
    participant Results as BacktestResultsView

    User->>View: select ticker + strategy, click Run
    View->>Repo: findBySymbol("AAPL")
    Repo-->>View: Stock
    View->>CfgVM: getState().getConfiguration()
    CfgVM-->>View: MomentumConfiguration
    Note over View,CfgVM: Falls back to (5, 20) / (14, 30, 70)<br/>until the user saves their own
    View->>View: buildStrategy()

    View->>Ctrl: runBacktest(ticker, strategy, prices)
    Ctrl->>Int: execute(RunBacktestInputData)
    Int->>Engine: run(ticker, strategy, prices)

    Engine->>Engine: validatePrices(...)
    Engine->>Strat: generateSignals(prices)
    Strat-->>Engine: List of TradingSignal

    loop each day i
        Engine->>Engine: execute day-i signal at day i+1 OPEN
    end
    Note over Engine: Final-day signal is never executed —<br/>this is how look-ahead bias is avoided
    Engine->>Engine: liquidate open position at final CLOSE
    Engine-->>Int: BacktestResult

    Int->>Deco: prepareSuccessView(RunBacktestOutputData)
    Deco->>Store: add(result)
    Deco->>Pres: prepareSuccessView(outputData)
    Pres->>VM: setResult(Summary, List of TradeRow)
    VM-->>Results: propertyChange(RESULT_PROPERTY)
    Results->>User: metrics + trade log

    alt IllegalArgumentException / NPE / IllegalStateException
        Engine--)Int: throw
        Int->>Pres: prepareFailView(exception.getMessage())
        Note over Pres: e.g. "Not enough price history<br/>to calculate a crossover"
    end
```

**What to point at.** The decorator hop is the whole integration story. `RunBacktestInteractor` calls one
`RunBacktestOutputBoundary`; the object behind it is an anonymous decorator built in `Main` that
files the result into `CompletedBacktestStore` before delegating to the presenter. **The backtest
feature does not know a comparison feature exists, and vice versa** — they meet only at that one
instance. Decorating the boundary rather than putting the `add` call inside `BacktestPresenter` is
what keeps that true.

Second point: engine and strategy exception messages are forwarded to the user *verbatim*. That is
why the guard messages read as sentences rather than developer shorthand.

**Where it is imperfect.** Everything above `runBacktest(...)` happens inside `BacktestView`, which
imports eight entity types and constructs strategy objects itself — the project's remaining
Dependency Rule violation. The whole sequence also runs on the event dispatch thread, with no
`SwingWorker`, unlike Add Ticker.

---

## Load Watchlist (start-up)

The richest failure handling in the project. Note there is no user actor — this runs once before
the window is visible.

```mermaid
sequenceDiagram
    autonumber
    participant Main
    participant Int as LoadWatchlist.Interactor
    participant DAO as FileWatchlistDataAccessObject
    participant FS as watchlist.dat
    participant Pres as PersistencePresenter
    participant VM as PersistenceViewModel
    participant MainView
    participant WLCtrl as WatchlistController

    Main->>Int: execute()
    Int->>DAO: load()
    DAO->>FS: exists?

    alt file absent (first run)
        FS-->>DAO: no
        DAO-->>Int: new Watchlist()
        Note over DAO: Not an error
    else file present and readable
        DAO->>FS: readObject()
        FS-->>DAO: Watchlist
    else corrupted / truncated / wrong type
        FS--)DAO: ObjectStreamException / EOFException / ClassNotFoundException
        DAO->>DAO: close streams first
        Note over DAO,FS: Moving a file you still hold open<br/>fails on Windows
        DAO->>FS: rename to watchlist.dat.corrupted-TIMESTAMP
        Note over DAO: Suffix -2, -3 and so on if that name exists<br/>(the timestamp is only second-precision)
        DAO-->>Int: new Watchlist()
    else IOException (permissions, disk)
        FS--)DAO: IOException
        DAO--)Int: PersistenceException
        Int->>Pres: prepareFailView("Failed to read watchlist from ...")
    end

    Int->>Pres: presentWatchlist(watchlist)
    Pres->>VM: setLoadedWatchlist(watchlist)
    VM->>VM: statusMessage = "Watchlist loaded."
    VM-->>MainView: propertyChange(STATUS_PROPERTY)

    Main->>VM: getWatchlist()
    VM-->>Main: Watchlist (or null)
    Note over Main: null degrades to new Watchlist()

    Main->>WLCtrl: showWatchlist("")
    Note over Main,WLCtrl: Show Watchlist is what actually renders<br/>the restored watchlist — constructing<br/>WatchlistView alone paints only "Ready."
```

**What to point at.** Corruption never blocks the user from opening the app: the bad file is backed
up and a fresh watchlist is returned. But a backup that *fails* throws rather than proceeding —
the DAO refuses to silently reset data it could not preserve. The collision suffix exists because
the backup timestamp is only second-precision, and a backup that clobbers the previous backup is
no backup.

**Where it is imperfect.** Three things worth owning:

1. On the `IOException` path, `Main` substitutes an empty watchlist and continues — and the next Add
   or Remove calls Save, which writes that empty watchlist over the real file. There is no "don't
   save until load succeeded" flag.
2. Corruption recovery is **silent**. The presenter is only reached on the throwing paths, so
   nothing in the UI ever says a backup was made.
3. `Main` reaches into a view model (`persistenceViewModel.getWatchlist()`) to retrieve an entity,
   rather than receiving it through the output boundary.
