# Team notes from the Member 1 vertical (watchlist + market data)

Written at the close of the watchlist vertical, on `feature/watchlist-use-cases`. Six
items. The first affects Members 2 and 3 directly and is the reason this note exists;
items 3–6 are things this vertical deliberately **raised rather than absorbed**, because
silently taking on other people's scope hides it instead of fixing it.

---

## 1. The ~100-trading-day ceiling — read this before choosing a strategy window

Alpha Vantage's free tier serves `TIME_SERIES_DAILY` with `outputsize=compact`, which
returns **roughly the latest 100 trading days**. Full history is a premium feature we do
not have. Every `Stock` this vertical hands you is bounded by that.

`MovingAverageCrossoverStrategy.generateSignals` requires
`prices.size() >= longWindow + 1`, because detecting a crossover needs the moving averages
for both today and yesterday. Below that it throws:

```
IllegalArgumentException: Not enough price history to calculate a crossover
```

So:

| Long window | Against a ~100-day compact response |
|---|---|
| 20 | fine — ~80 signal days |
| 50 | fine — needs 51 records, leaves 50 signal days |
| 90 | fine, but this is the edge of comfort |
| 100+ | **throws** |

**Keep long windows at or below ~90.** The recommendation is 90 rather than 99 because
"roughly 100" is doing real work in that sentence: market holidays and recently listed
symbols both return fewer rows, and a config that works on `AAPL` can throw on a thinner
symbol.

The arithmetic is pinned by a test rather than left as prose —
`MarketDataHandoffTest.aLongWindowAtTheCompactResponseCeilingBreaksTheStrategy` builds a
100-day series and asserts that a long window of 100 throws while 99 does not, so the cliff
is located exactly at `longWindow == size` rather than approximately.

**Be clear about what that test does and does not do.** It pins the arithmetic; it is
**not** a guard on your configuration. No production code currently configures a long
window, so raising one past the ceiling would not break this or any other test — it would
break at runtime, in front of whoever is demoing. If you add a configurable window, add a
validation check with it.

## 2. Raw prices are unadjusted

The `close` values are as-traded: **not** adjusted for splits or dividends. A comparison
spanning a split will show a discontinuity that is real in the data and meaningless as a
return. Fine for a 100-day backtest; worth one line in the README so nobody debugs it as a
bug in the strategy.

## 3. The hand-off surface, and how to use it

Once a ticker has been added or refreshed, its history is in the `StockRepository`:

```java
stockRepository.findBySymbol("AAPL")            // Optional<Stock>
    .map(Stock::getDailyPrices)                 // List<DailyPrice>, guaranteed:
                                                //   oldest to newest
                                                //   no nulls
                                                //   no duplicate dates
                                                //   unmodifiable
    .map(strategy::generateSignals);
```

You need to know nothing about Alpha Vantage, JSON, HTTP, or API keys. The ordering
guarantee is a constructor invariant on `Stock`, not a convention — a reversed or sparse
list cannot be constructed, so it cannot reach you.

`MarketDataHandoffTest` is the executable proof: it drives
`InMemoryMarketDataGateway.withSampleData()` → `AddTickerInteractor` →
`StockRepository.findBySymbol` and feeds the result straight into a real
`MovingAverageCrossoverStrategy(5, 20)`, asserting at least one **BUY** and one **SELL**
come back. Not just "it didn't throw" — a flat series returns nothing but HOLD, and a
hand-off that only ever produces HOLDs is a hand-off that does not work.

**Offline development works.** With `ALPHA_VANTAGE_API_KEY` unset the app wires
`InMemoryMarketDataGateway.withSampleData()` and shows a status line saying sample data is
in use. `AAPL`, `MSFT` and `TSLA` each carry 120 deterministic trading days that genuinely
oscillate, so you can develop and demo a strategy with no key and no network.

## 4. Two things about the app that changed under you

**The watchlist is now the launch card (Member 4).** `CardLayout` shows whichever card was
added first, and nothing calls `ViewManagerModel.setActiveView` at startup. The watchlist
wiring sits between Persistence and Comparison in `Main`, so `WatchlistView` is added
before `ComparisonView` and is now what you see on launch — where the Comparison view used
to be. This was deliberate (the individual walkthrough and screenshot need the vertical
visible immediately), but it is a change to your screen and you should know about it. If
you want Comparison back as the landing card, one `setActiveView` call at startup does it
— that is the right fix, not reordering the wiring.

Also: `view/MainView.java` was touched **append-only**, which means two of its existing
comments now describe the file inaccurately (they refer to the nav-button block as
commented-out; it is now live). Cosmetic, and yours to reword.

**Save failures are currently invisible (Member 4).** `PersistenceViewModel` is bound to no
view, and the persistence input boundary returns `void`. If a write to `watchlist.dat`
fails, the user still sees "Added AAPL with 120 days of price history." while nothing was
saved, and finds out at the next launch. This is inherited from the existing persistence
design, not introduced by the watchlist work, and fixing it means either a return value on
the boundary or binding that view model to something — both of which are Member 4's call,
which is why it is filed here instead of patched.

## 5. Three gaps nobody owns

Real team gaps. This vertical files them rather than silently taking them on.

**`checkstyle.xml` + `maven-checkstyle-plugin`.** The rubric wants an automated style tool
to score above 3/5 on code quality, and the repo has none. A hand-written style checklist
has been the interim substitute for this vertical's reviews — **that substitute disappears
when this plan closes.** Somebody should add the plugin; it is a small `pom.xml` change and
a config file.

**`accessibility-report.md`.** A required course deliverable that does not exist. Note the
split: the accessibility *behaviour* is implemented (`WatchlistView` has keyboard operation,
an explicit focus order, mnemonics and accessible names). The *report* is not written.

**`serialVersionUID` — seven of the eight `Serializable` entities rely on the implicit,
computed UID.** The eight are `BacktestResult`, `DailyPrice`, `MovingAverageConfiguration`,
`Ticker`, `Trade`, `TradingSignal`, `Watchlist` and `WatchlistEntry`; only
`MovingAverageConfiguration` already declares one. Read this before you touch the other
seven:

> Adding an arbitrary `private static final long serialVersionUID = 1L;` to `Ticker` or
> `Watchlist` **breaks every existing `watchlist.dat`** with `InvalidClassException`. The
> recovery code then renames the file `.corrupted-*` and hands back an empty watchlist. It
> destroys saved data during demo week and it looks like a bug in the persistence feature.

The only safe procedure is to capture the *currently computed* value rather than invent one:

```
serialver -classpath target/classes entity.Ticker entity.Watchlist
```

Paste those exact values, in their own commit, verified against a real saved file before
and after. **Leave `entity/WatchlistEntry.java` alone entirely** — it is the one with the
most saved instances in the wild.

## 6. The backtest engine is still missing

Nothing in the application can currently produce a `BacktestResult`.
`MainAppState.addCompletedResult` has zero callers, so the Compare screen only ever renders
its empty state. The watchlist vertical demos standalone and does **not** unblock this —
it supplies the price data a backtest would consume, but nothing runs the backtest.

If the engine is not merged by Aug 8 EOD, script the team demo honestly around the empty
state rather than discovering it live.
