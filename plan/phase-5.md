# Phase 5 — Hand-off Proof and Close-out

**Active agents:** orchestrator, with `use-case` (A) and `data-access` (B) on call, then
reviewer.
**Goal:** prove the hand-off to Members 2 and 3 executably rather than verbally, land the
coverage numbers, and file the gaps this vertical deliberately did not absorb.

---

## Dependencies to install

None.

---

## Files to create

| File | Purpose |
|---|---|
| `src/test/java/use_case/watchlist/MarketDataHandoffTest.java` | the executable proof that this vertical's output drives a real strategy |
| `plan/handoffs/team-notes.md` | the ~100-day ceiling and the three unowned gaps |
| `plan/handoffs/coverage.md` | measured coverage, plus what is excluded and why |

---

## What to implement

### 1. The hand-off test — the single most valuable artifact in this phase

`vision.md` §8 names it as a required deliverable: *"a test that feeds my output straight
into `MovingAverageCrossoverStrategy` and gets real signals back — the executable proof of
the hand-off."*

Shape:
1. Build a `Stock` through the real path — `InMemoryMarketDataGateway.withSampleData()` →
   `AddTickerInteractor` → `StockRepository.findBySymbol(...)`. Do not hand-construct the
   price list; the point is to exercise the pipeline.
2. Pass `stock.getDailyPrices()` straight into
   `new MovingAverageCrossoverStrategy(new MovingAverageConfiguration(5, 20))
   .generateSignals(prices)`.
3. Assert **at least one `BUY` and at least one `SELL`** come back — not merely that the
   call doesn't throw. A flat series yields nothing but `HOLD`, which would make both this
   test and the demo vacuous (`vision.md` principle 8). If the assertion fails, the fix is
   Agent B making the sample series genuinely oscillate, not weakening the assertion.
4. Assert the strategy's three preconditions hold on real output: dates strictly
   increasing oldest→newest, no null elements, `size() >= longWindow + 1`.

This test is also the answer to "does the offline fake actually work?" — it exercises the
gateway, the interactor, the repository and the entity in one shot.

### 2. Coverage close-out

Run `mvn clean verify` and read `target/site/jacoco/index.html`.

Targets from the rubric: **>90% line coverage on use-case interactors, >70% overall with
documented exclusions.**

If overall falls short, the lever is Agent A and Agent B adding entity, gateway and
use-case tests. **Never add Swing tests for this purpose** — `WatchlistView` sits on top of
already-uncovered `MainView`, `ViewManager`, `ComparisonView` and `Main`, and chasing that
denominator with UI tests buys brittle tests and no rubric credit (hazard H6).

Write `plan/handoffs/coverage.md` naming what is excluded and why: the Swing views, the
composition root, and Member 4's `MainAppState` integration seam.

### 3. Team notes — raise, do not absorb

`plan/handoffs/team-notes.md`, to be posted to the team:

**The ~100-day ceiling (raise this first — it affects Members 2 and 3 directly).**
Alpha Vantage's free `TIME_SERIES_DAILY` compact response returns roughly the latest 100
trading days; full history is premium. A 50-day long window needs 51 records and leaves
~49 signal days — workable. But **any strategy long-window above ~90 silently violates
`MovingAverageCrossoverStrategy.generateSignals`'s `size() >= longWindow + 1`
precondition** and throws `IllegalArgumentException("Not enough price history to calculate
a crossover")`. Members 2 and 3 do not know this yet.

**Raw prices are unadjusted.** Splits and dividends distort long comparisons. Worth a line
in the README.

**Three unowned gaps, deliberately out of this vertical's scope:**
- `checkstyle.xml` + `maven-checkstyle-plugin`. The rubric needs a tool like it to score
  above 3/5 on code quality. Nobody owns it. `agents/reviewer.md`'s style checklist has
  been the interim substitute; it does not survive this plan ending.
- `accessibility-report.md` — a required course deliverable that does not exist. The
  accessibility *behaviour* is implemented in `WatchlistView`; the report is not written.
- `serialVersionUID` on the seven `Serializable` entities. **Warn explicitly:** adding an
  arbitrary `= 1L` to `Ticker` or `Watchlist` breaks every existing `watchlist.dat` with
  `InvalidClassException`, and Member 4's recovery code then renames it `.corrupted-*` and
  returns an empty watchlist. It is only safe via
  `serialver -classpath target/classes entity.Ticker entity.Watchlist` to capture the
  *currently computed* value, in its own commit, verified against a real saved file.
  `WatchlistEntry` must be left alone entirely.

**The backtest engine.** Nothing in the application can still produce a `BacktestResult`;
`MainAppState.addCompletedResult` has zero callers and the Compare screen shows its empty
state. This vertical demos standalone and does not unblock that. If the engine is not
merged by Aug 8 EOD, the team demo should be scripted honestly around the empty state.

---

## Verification

1. `mvn clean verify` — green.
2. `MarketDataHandoffTest` passes and asserts a real `BUY` and a real `SELL`.
3. JaCoCo: ≥90% on the four interactors, ≥70% overall, exclusions documented in
   `plan/handoffs/coverage.md`.
4. `plan/handoffs/team-notes.md` exists and covers all five items above.
5. No API key anywhere:
   `grep -rniE "apikey=[A-Za-z0-9]{8,}" src/ README.md` returns nothing.
6. `plan/status.md` updated to reflect completion.
7. Reviewer writes `plan/review-phase-5.md`; status is not FAIL.
