# `mvn clean verify` on merged `main` — 2026-08-10

This settles every figure in the Unverified Claims Register
(`individual-contribution-abhirve.md` §2.2). It is the one command that register asks for,
run on merged `main` after PR #50.

## Provenance

| | |
|---|---|
| Commit | `6657298` — *Merge pull request #50 from AbhirveM/feat/price-equity-charts* |
| Branch | `main`, fast-forwarded to `origin/main` (was 40 behind) |
| Command | `mvn clean verify -B` |
| Runs | 2 consecutive clean runs, identical results |
| Result | **BUILD SUCCESS** both times |
| Date | 2026-08-10 |

Raw numbers below are derived from `target/site/jacoco/jacoco.csv`, summed across all rows.
No JaCoCo `<excludes>` are configured in `pom.xml`, so these are whole-project raw figures,
not filtered ones.

## Headline figures

| Metric | Measured | Previous slide figure |
|---|---|---|
| Tests | **681**, 0 failures, 0 errors, 0 skipped | 484 |
| Test source files | **58** | 56 |
| Checkstyle violations | **0** (`You have 0 Checkstyle violations.`) | 0 — confirmed |
| Overall line coverage | **90.25%** (3118 / 3455) | 69.81% (1526 / 2186) |
| Overall branch coverage | **85.14%** (722 / 848) | not stated |
| Use-case interactor line coverage | **98.33%** (236 / 240) | not stated |
| `use_case.*` package line coverage | **98.58%** (554 / 562) | not stated |
| Classes analysed | 132 | — |

**The "⚠ 4 lines under the 70% target" warning is dead.** Overall coverage is 90.25%, which
clears the team's 70% target by 20 points and clears the rubric's 5/5 overall threshold
(>70%) outright.

## Rubric position

The Testing band asks for **>90% interactor line coverage** and **>70% overall** for 5/5.

| Requirement | Threshold | Measured | Met |
|---|---|---|---|
| Interactor line coverage | >90% | **98.33%** | yes |
| Overall line coverage | >70% | **90.25%** | yes |
| Evidence of coverage | present | `jacoco.csv` + this file | yes |
| Documentation of what is untested and why | present | `plan/handoffs/coverage.md` | yes |

Both 5/5 conditions are met. The old slide text argued *against* this band for no reason.

## Per-interactor line coverage

| Interactor | Lines | Coverage |
|---|---|---|
| `RunBacktestInteractor` | 44 / 44 | 100% |
| `AddTickerInteractor` | 40 / 40 | 100% |
| `RefreshTickerInteractor` | 35 / 35 | 100% |
| `ConfigureMomentumInteractor` | 25 / 29 | 86.2% |
| `ConfigureMovingAverageInteractor` | 22 / 22 | 100% |
| `ShowWatchlistInteractor` | 20 / 20 | 100% |
| `RemoveTickerInteractor` | 19 / 19 | 100% |
| `CompareStrategies.Interactor` | 11 / 11 | 100% |
| `LoadWatchlist.Interactor` | 10 / 10 | 100% |
| `SaveWatchlist.Interactor` | 10 / 10 | 100% |
| **Total** | **236 / 240** | **98.33%** |

All four watchlist interactors are at 100% lines. The only interactor below 100% is
`ConfigureMomentumInteractor` (4 lines), which is not part of the Member 1 vertical.

## Per-package line coverage

| Package | Covered / Total | % |
|---|---|---|
| `use_case.backtest` | 72 / 72 | 100 |
| `use_case.moving_average` | 32 / 32 | 100 |
| `interface_adapter.moving_average` | 65 / 65 | 100 |
| `interface_adapter.persistence` | 18 / 18 | 100 |
| `views.chart` | 187 / 187 | 100 |
| `use_case.watchlist` | 376 / 377 | 99.7 |
| `interface_adapter.watchlist` | 221 / 222 | 99.5 |
| `interface_adapter.backtest` | 93 / 95 | 97.9 |
| `interface_adapter.chart` | 40 / 41 | 97.6 |
| `entity` | 354 / 369 | 95.9 |
| `use_case.comparison` | 15 / 16 | 93.8 |
| `data_access` | 272 / 295 | 92.2 |
| `use_case.persistence` | 22 / 24 | 91.7 |
| `use_case.momentum` | 37 / 41 | 90.2 |
| `interface_adapter.comparison` | 52 / 59 | 88.1 |
| `views` | 1196 / 1370 | 87.3 |
| `interface_adapter.momentum` | 41 / 51 | 80.4 |
| `app` | 25 / 121 | 20.7 |

## What is still uncovered, and why

337 lines total. The largest blocks, all previously documented:

| Class | Missed | Why |
|---|---|---|
| `app.Main` (+ its inner classes) | 93 | Composition root — hand-wiring, no logic to assert |
| `views.WatchlistView` (+ its `SwingWorker`) | 54 | Swing paint/event paths; documented exclusion |
| `views.MovingAverageConfigurationView` | 25 | Swing |
| `views.BacktestResultsView` | 19 | Swing |
| `views.MomentumConfigurationView` | 16 | Swing |
| `data_access.JdkHttpJsonClient` | 15 | Real network I/O; the offline fake is what tests use |
| `views.LookAndFeel` | 14 | Swing L&F installation |
| `data_access.FileWatchlistDataAccessObject` | 7 | Filesystem error branches |
| `AlphaVantageMarketDataAccessObject` | 1 | `apiKeyFromEnvironment()` — deliberate; the only possible test is a tautology on `System.getenv` |

## Claims that survive unchanged

- **`RefreshTickerInteractor` still has one uncovered *method*** despite 100% lines
  (`METHOD_MISSED = 1`). Warning **W3-12** stands — "100% coverage" must still be qualified
  as 100% of *lines*.
- **`AlphaVantageMarketDataAccessObject` is 86 / 87 lines**, one method missed. Unchanged.
- **`CachingMarketDataGateway` is 47 / 47.** Unchanged.
- **No JaCoCo exclusions are configured.** Unchanged and still worth saying.

## Corrections to the per-class table (U4)

Two figures moved and must be fixed on the slide:

- `ShowWatchlistInteractor` is **20 / 20**, not 19 / 19.
- The `view` package is now named **`views`**, and it misses **174** lines, not 246.
