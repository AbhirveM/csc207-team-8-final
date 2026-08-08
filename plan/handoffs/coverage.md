# Coverage close-out — Member 1 watchlist vertical

Measured at the Phase 5 gate with `mvn -o clean verify`, **415 tests, 0 failures**.
Source of truth: `target/site/jacoco/index.html` and `target/site/jacoco/jacoco.csv`.

## Headline

| Metric | Rubric target | Measured |
|---|---|---|
| Overall line coverage | ≥70% | **71.69%** (1008 / 1406 lines) |
| Use-case interactors | ≥90% | **100%** (all four) |

**There are no JaCoCo exclusions configured, and that is deliberate.** `pom.xml` runs the
plugin with no `<excludes>`, so 71.69% is the *whole* project — every Swing view, the
composition root, and all four teammates' packages included. Excluding the untestable
surface would report a larger number that means less. What follows is therefore a list of
what drags the number down and why we chose to live with it, not a list of things filtered
out of the measurement.

## The six ≥90% gates, all holding

| Class | Lines | % |
|---|---|---|
| `AddTickerInteractor` | 40/40 | 100% |
| `RemoveTickerInteractor` | 19/19 | 100% |
| `RefreshTickerInteractor` | 35/35 | 100% |
| `ShowWatchlistInteractor` | 19/19 | 100% |
| `AlphaVantageMarketDataAccessObject` | 86/87 | 98.9% |
| `CachingMarketDataGateway` | 47/47 | 100% |

Supporting classes in the same vertical: `WatchlistSnapshotFactory` 40/40,
`TickerSymbolValidator` 14/15, the whole `interface_adapter.watchlist` package **166/166 =
100%**, and `use_case.watchlist` overall **336/337 = 99.7%**.

**One honest qualification (warning W3-12).** "100% on the four interactors" is true of
**lines**, not of every JaCoCo axis. `RefreshTickerInteractor` still reports one method
missed, and `AlphaVantageMarketDataAccessObject` reports one — that second one is
`apiKeyFromEnvironment()`, uncovered on purpose (below). Do not carry "100%" forward
unqualified.

## What is uncovered, and why we are not chasing it

| Area | Missed lines | Why |
|---|---|---|
| `view` package | 246 | Swing. `WatchlistView` alone is 148, on top of teammates' `ComparisonView` (36) and `MainView` (21). |
| `app/Main.java` | 48 | The composition root. It is `new`-ing objects in an order; a test of it would assert the wiring diagram back at itself. |
| `interface_adapter.comparison` / `.persistence` | 30 + 23 | Teammates' view models, not this vertical's to test. `PersistenceViewModel` (14) is bound to no view at all — see W4-9 in `team-notes.md`. |
| `JdkHttpJsonClient` | 15 | The only class in the codebase that opens a socket. Covering it would mean a test that reaches the network, which is banned outright. |
| `AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()` | 1 | See below. |

**Why no Swing tests (hazard H6).** `WatchlistView` sits on top of already-uncovered
`MainView`, `ViewManager` and `ComparisonView`. Chasing that denominator with UI tests buys
brittle, timing-dependent tests and no rubric credit. The design decision that makes this
affordable is upstream: every field on `WatchlistState` is a pre-formatted `String`, so the
view does no formatting and contains no branching on data. The logic that *would* be worth
testing was deliberately pushed out of the view and into `WatchlistPresenter`, which is at
100%.

**The one uncovered line in the DAO.** Phase 5 closed W2-7 by splitting the pure policy out
of the environment read:

```java
static Optional<String> apiKeyFrom(String rawValue)     // covered: null, five blank
                                                        // forms, plain, and padded/strip
public static Optional<String> apiKeyFromEnvironment()  // uncovered by design
```

`apiKeyFromEnvironment()` is now a single delegating line. It stays uncovered because the
only way to cover it is a test that reads `System.getenv` and branches on whatever it finds
— a test that asserts whichever branch it lands in and can never fail. That exact pattern
was already removed from this file once as defect D13, and `agents/reviewer.md` bans it.
One permanently-uncovered line is the correct floor here.

## Margin

Two different margins, because two different things move the ratio:

- **Losing coverage on existing lines:** 24. Turning 24 currently-covered lines uncovered
  lands exactly on 70% (1008 − 0.70 × 1406 = 23.8).
- **Adding new uncovered production lines:** 34. New code grows the denominator too, so
  1008 / (1406 + x) ≥ 0.70 solves to x ≤ 34, and 1008/1440 = 70.0% exactly.

Either way it is tens of lines, not hundreds. Warning W4-10 measured ~20 at the Phase 4
gate; Phase 5's additions widened the margin slightly rather than spending it — W2-7's
extraction converted 3 uncovered lines to covered, and W2-8 and the hand-off test added no
production code at all.

**Anyone adding uncovered production code to this repo from here needs tests landing beside
it**, and 34 lines is roughly one medium method. The remaining non-Swing levers, if the number is ever pressed: more `entity` tests
(`entity` is 165/189 = 87.3%, with `Trade` at 0/13 the largest single gap) and more
`data_access` tests (272/295 = 92.2%).
