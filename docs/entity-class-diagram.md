# MarketLens — Entity Class Diagram

The innermost Clean Architecture layer: the 15 classes in `entity`, their fields, and their
relationships. Nothing here depends on any other package in the project.

```mermaid
classDiagram
    direction TB

    class Ticker {
        -String symbol
        -String companyName
        +getSymbol() String
        +getCompanyName() String
        +equals(Object) boolean
    }

    class Watchlist {
        -List~WatchlistEntry~ entries
        +addTicker(Ticker) void
        +removeTicker(Ticker) void
        +contains(Ticker) boolean
        +findEntry(Ticker) Optional~WatchlistEntry~
        +getEntries() List~WatchlistEntry~
    }

    class WatchlistEntry {
        -Ticker ticker
        -MovingAverageConfiguration movingAverageConfiguration
        -MomentumConfiguration momentumConfiguration
        +getTicker() Ticker
        +setMovingAverageConfiguration(MovingAverageConfiguration) void
        +setMomentumConfiguration(MomentumConfiguration) void
    }

    class Stock {
        -Ticker ticker
        -List~DailyPrice~ dailyPrices
        +getSymbol() String
        +getCompanyName() String
        +getDailyPrices() List~DailyPrice~
        +getPriceCount() int
        +getLatestPrice() Optional~DailyPrice~
        +getEarliestPrice() Optional~DailyPrice~
        +withDailyPrices(List~DailyPrice~) Stock
        +withCompanyName(String) Stock
    }

    class DailyPrice {
        -LocalDate date
        -double open
        -double high
        -double low
        -double close
        -long volume
    }

    class TradingStrategy {
        <<interface>>
        +getName() String
        +generateSignals(List~DailyPrice~) List~TradingSignal~
    }

    class MovingAverageCrossoverStrategy {
        -MovingAverageConfiguration configuration
        +getName() String
        +generateSignals(List~DailyPrice~) List~TradingSignal~
        -calculateAverage() double
    }

    class RSIMomentumStrategy {
        -MomentumConfiguration configuration
        +getName() String
        +generateSignals(List~DailyPrice~) List~TradingSignal~
        -determineSignalType(double) SignalType
    }

    class MovingAverageConfiguration {
        -int shortWindow
        -int longWindow
        +getShortWindow() int
        +getLongWindow() int
    }

    class MomentumConfiguration {
        -int period
        -double oversoldThreshold
        -double overboughtThreshold
        +getPeriod() int
        +getOversoldThreshold() double
        +getOverboughtThreshold() double
    }

    class TradingSignal {
        -LocalDate date
        -SignalType signalType
        +getDate() LocalDate
        +getSignalType() SignalType
    }

    class SignalType {
        <<enumeration>>
        BUY
        SELL
        HOLD
    }

    class BacktestEngine {
        +double INITIAL_CAPITAL$
        +run(Ticker, TradingStrategy, List~DailyPrice~) BacktestResult
        -calculateWinRate() double
        -validatePrices() void
    }

    class BacktestResult {
        -Ticker ticker
        -String strategyName
        -List~Trade~ tradeLog
        -double finalCapital
        -double totalReturn
        -int numberOfTrades
        -double winRate
    }

    class Trade {
        -Ticker ticker
        -LocalDate entryDate
        -double entryPrice
        -LocalDate exitDate
        -double exitPrice
        -int quantity
        +getReturnPercent() double
    }

    Watchlist "1" *-- "0..*" WatchlistEntry : owns
    WatchlistEntry "1" --> "1" Ticker
    WatchlistEntry "1" o-- "0..1" MovingAverageConfiguration
    WatchlistEntry "1" o-- "0..1" MomentumConfiguration

    Stock "1" --> "1" Ticker
    Stock "1" *-- "0..*" DailyPrice : oldest to newest

    TradingStrategy <|.. MovingAverageCrossoverStrategy
    TradingStrategy <|.. RSIMomentumStrategy
    MovingAverageCrossoverStrategy "1" --> "1" MovingAverageConfiguration
    RSIMomentumStrategy "1" --> "1" MomentumConfiguration
    TradingStrategy ..> TradingSignal : produces
    TradingSignal "1" --> "1" SignalType

    BacktestEngine ..> TradingStrategy : uses
    BacktestEngine ..> DailyPrice : reads
    BacktestEngine ..> BacktestResult : produces
    BacktestResult "1" *-- "0..*" Trade : trade log
    BacktestResult "1" --> "1" Ticker
    Trade "1" --> "1" Ticker
```

## What the diagram shows

**The Strategy pattern is the centrepiece.** `BacktestEngine` depends only on the `TradingStrategy`
interface, never on either implementation. A third strategy is added without touching the engine —
this is the clearest Open/Closed example in the project.

**Configuration is separated from behaviour.** `MovingAverageConfiguration` and
`MomentumConfiguration` hold validated parameters and nothing else; the strategies hold the
algorithm. Each configuration is supplied through its strategy's constructor, per the blueprint's
shared `TradingStrategy` contract.

**Two aggregates, one shared identity.** `Watchlist → WatchlistEntry` models *membership*;
`Stock → DailyPrice` models *market data*. They meet only at `Ticker`. That separation is what lets
the save file contain membership without ever containing price history.

**Composition vs aggregation is deliberate.** Filled diamonds (`*--`) mark ownership with a shared
lifetime — a `WatchlistEntry` has no meaning outside its `Watchlist`, and a `Trade` has none outside
its `BacktestResult`. Hollow diamonds (`o--`) mark the optional configurations, which outlive any
single entry.

## Serialization

`Serializable`: `Ticker`, `Watchlist`, `WatchlistEntry`, `DailyPrice`, `Trade`, `TradingSignal`,
`BacktestResult`, `MovingAverageConfiguration`, `MomentumConfiguration`.

**`Stock` is deliberately not `Serializable`** — price history is never persisted, so the save file
format is unaffected by market data. A restored ticker shows "Not loaded" until refreshed, which is
also quota protection.

> **Known gap.** `Watchlist`, `Ticker` and `DailyPrice` declare no `serialVersionUID`, so any future
> field change alters the JVM's computed UID and turns every existing save file into an
> `InvalidClassException` — which `FileWatchlistDataAccessObject` treats as corruption and silently
> resets. `WatchlistEntry`, `Trade` and both configuration classes do declare one.

## Deviations from the blueprint's shared contracts

Appendix A of the project blueprint fixed these shapes across the team. Two have drifted:

- **`BacktestResult` has no `initialCapital` field.** The contract specifies "initial/final capital".
  In practice the initial capital is the constant `BacktestEngine.INITIAL_CAPITAL` ($10,000), so
  nothing is *wrong*, but a consumer cannot read it off the result — which is why reconstructing an
  equity curve requires reaching for the engine constant.
- **`Trade` has no `profitOrLoss` accessor.** The contract lists "profit or loss, and percentage
  return"; only `getReturnPercent()` exists. Absolute profit is derivable from entry/exit price and
  quantity, but every caller has to derive it.

`Trade` also names its fields `entryDate`/`entryPrice`/`exitDate`/`exitPrice` where the blueprint
says `purchaseDate`/`purchasePrice`/`saleDate`/`salePrice`. Same meaning, different vocabulary.
