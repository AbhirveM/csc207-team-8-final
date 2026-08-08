# Add Ticker — Full Use Case

**Owner:** Abhirve Munipalle (Member 1) · **Feature:** Watchlist and Alpha Vantage market data

This is the class diagram for the complete Add Ticker use case, from the Swing view down to the
Alpha Vantage HTTP call, together with the "before" and "after" views of the screen it drives.

---

## Class diagram

```mermaid
classDiagram
    direction LR

    class WatchlistView {
        -JTextField tickerField
        -JTable watchlistTable
        -JTable priceTable
        +actionPerformed(ActionEvent)
        +propertyChange(PropertyChangeEvent)
    }

    class WatchlistController {
        -AddTickerInputBoundary addTicker
        +addTicker(String rawSymbol)
    }

    class AddTickerInputBoundary {
        <<interface>>
        +execute(AddTickerInputData)
    }

    class AddTickerInputData {
        -String rawSymbol
        +getRawSymbol() String
    }

    class AddTickerInteractor {
        -Watchlist watchlist
        -MarketDataGateway marketDataGateway
        -StockRepository stockRepository
        -SaveWatchlist.InputBoundary saveWatchlist
        -AddTickerOutputBoundary presenter
        +execute(AddTickerInputData)
    }

    class TickerSymbolValidator {
        <<utility>>
        +normalize(String) String
        +normalizeKey(String) String
    }

    class WatchlistInputSupport {
        <<utility>>
        +resolve(String, Watchlist, Membership) Resolution
    }

    class MarketDataGateway {
        <<interface>>
        +fetchDailyPrices(String) List~DailyPrice~
        +fetchCompanyName(String) Optional~String~
    }

    class StockRepository {
        <<interface>>
        +save(Stock)
        +findBySymbol(String) Optional~Stock~
    }

    class AddTickerOutputBoundary {
        <<interface>>
        +prepareSuccessView(AddTickerOutputData)
        +prepareFailView(WatchlistFailure)
    }

    class AddTickerOutputData {
        -String symbol
        -String companyName
        -int priceCount
        -WatchlistSnapshot snapshot
    }

    class WatchlistFailure {
        -Kind kind
        -String symbol
        +from(MarketDataException) WatchlistFailure
    }

    class WatchlistPresenter {
        -WatchlistViewModel viewModel
        +prepareSuccessView(AddTickerOutputData)
        +prepareFailView(WatchlistFailure)
    }

    class WatchlistViewModel {
        -WatchlistState state
        +firePropertyChanged()
    }

    class Stock {
        -Ticker ticker
        -List~DailyPrice~ dailyPrices
        +getDailyPrices() List~DailyPrice~
    }

    class Ticker {
        -String symbol
        -String companyName
    }

    class Watchlist {
        -List~WatchlistEntry~ entries
        +addTicker(Ticker)
    }

    class DailyPrice {
        -LocalDate date
        -double open
        -double high
        -double low
        -double close
        -long volume
    }

    class AlphaVantageMarketDataAccessObject {
        -HttpJsonClient httpClient
        -String apiKey
        +fetchDailyPrices(String) List~DailyPrice~
        +fetchCompanyName(String) Optional~String~
    }

    class CachingMarketDataGateway {
        -MarketDataGateway delegate
    }

    class InMemoryMarketDataGateway {
        +withSampleData() InMemoryMarketDataGateway
    }

    class InMemoryStockRepository

    class HttpJsonClient {
        <<interface>>
        +get(String url) JSONObject
    }

    class JdkHttpJsonClient

    WatchlistView --> WatchlistController : user action
    WatchlistController ..> AddTickerInputData : creates
    WatchlistController --> AddTickerInputBoundary : calls
    AddTickerInputBoundary <|.. AddTickerInteractor : implements
    AddTickerInteractor ..> WatchlistInputSupport : validates via
    WatchlistInputSupport ..> TickerSymbolValidator : uses
    AddTickerInteractor --> MarketDataGateway : port
    AddTickerInteractor --> StockRepository : port
    AddTickerInteractor --> AddTickerOutputBoundary : port
    AddTickerInteractor ..> Stock : creates
    AddTickerInteractor ..> Ticker : creates
    AddTickerInteractor --> Watchlist : mutates
    AddTickerInteractor ..> AddTickerOutputData : creates
    AddTickerInteractor ..> WatchlistFailure : creates on failure
    AddTickerOutputBoundary <|.. WatchlistPresenter : implements
    WatchlistPresenter --> WatchlistViewModel : updates
    WatchlistViewModel ..> WatchlistView : property change
    Stock *-- Ticker
    Stock *-- DailyPrice
    Watchlist o-- Ticker
    MarketDataGateway <|.. AlphaVantageMarketDataAccessObject : implements
    MarketDataGateway <|.. CachingMarketDataGateway : implements
    MarketDataGateway <|.. InMemoryMarketDataGateway : implements
    CachingMarketDataGateway --> MarketDataGateway : decorates
    StockRepository <|.. InMemoryStockRepository : implements
    AlphaVantageMarketDataAccessObject --> HttpJsonClient : seam
    HttpJsonClient <|.. JdkHttpJsonClient : implements
```

---

## The Dependency Rule

The same use case viewed as layers. **Every arrow points inward.** No class in an inner ring names a
class in an outer ring.

```mermaid
flowchart RL
    subgraph FRAMEWORKS["Frameworks & Drivers"]
        V["WatchlistView<br/><i>Swing</i>"]
        DAO["AlphaVantageMarketDataAccessObject<br/>CachingMarketDataGateway<br/>InMemoryMarketDataGateway<br/>InMemoryStockRepository<br/>JdkHttpJsonClient"]
    end

    subgraph ADAPTERS["Interface Adapters"]
        C["WatchlistController"]
        P["WatchlistPresenter"]
        VM["WatchlistViewModel"]
    end

    subgraph USECASE["Use Cases"]
        I["AddTickerInteractor"]
        PORTS["MarketDataGateway<br/>StockRepository<br/>AddTickerInputBoundary<br/>AddTickerOutputBoundary<br/><i>interfaces declared here</i>"]
    end

    subgraph ENTITIES["Entities"]
        E["Stock · Ticker<br/>DailyPrice · Watchlist"]
    end

    V --> C
    C --> PORTS
    I --> PORTS
    I --> E
    P --> PORTS
    DAO -.implements.-> PORTS
    P --> VM
    VM -.notifies.-> V
```

**The key move is Dependency Inversion.** `MarketDataGateway` and `StockRepository` are declared in
the **use-case** layer and implemented in `data_access`. The interactor depends only on the port;
`Main` injects the implementation. The compile-time arrow from `data_access` to `use_case` therefore
runs *against* the direction of control flow — which is exactly what the Dependency Rule requires,
and it is why `AddTickerInteractor` knows nothing about Alpha Vantage, HTTP, JSON, API keys or Swing
and can be unit-tested fully offline.

Two consequences worth naming:

- **Substitutability (Liskov / Open-Closed).** Three gateways implement one port. Swapping the live
  Alpha Vantage DAO for the offline fake is a one-line change in `Main` and no other file moves.
  That is how the application runs with no API key and no network.
- **No entity crosses an output boundary.** `AddTickerOutputData` carries only strings and numbers,
  so the view model imports neither Swing nor any entity, and the live internal list returned by
  `Watchlist.getEntries()` can never leak to the view.

---

## Before

The Watchlist feature did not exist. The application launched into the Compare Strategies screen
with an empty results table and no way to add a ticker.

![Before — the app with no watchlist feature](before-watchlist-view.png)

## After

<!-- Capture during the manual walkthrough (plan/handoffs/walkthrough.md step 8) and save as
     docs/after-watchlist-view.png. Show: a ticker added, company name resolved, and the daily
     price table populated. -->

![After — the Watchlist screen with a ticker added and its price history loaded](after-watchlist-view.png)

---

## Interactor source

The implementation shown in the presentation is
[`AddTickerInteractor`](../src/main/java/use_case/watchlist/AddTickerInteractor.java). The ordering
inside `execute` is deliberate: validate, reject duplicates, **fetch before mutating**, treat a
missing company name as cosmetic, build the `Stock` (which is where a malformed price series is
rejected — still before any mutation), and only then update the watchlist and save. A provider
failure can never leave a half-added ticker behind.
