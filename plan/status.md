# Status

Current phase: **none — all five phases complete.**
Active agents: none.
Branch: `main` at `6657298`. Every branch this plan used is merged; no PRs or issues are open.

## Verified build on merged `main`, 2026-08-10

`mvn clean verify -B` at `6657298` (the PR #50 charts merge), run twice, **BUILD SUCCESS** both
times with identical results:

| Metric | Value |
|---|---|
| Tests | **681**, 0 failures, 0 errors, 0 skipped |
| Checkstyle | **0 violations** |
| Overall line coverage | **90.25%** (3118 / 3455) |
| Overall branch coverage | **85.14%** (722 / 848) |
| Interactor line coverage | **98.33%** (236 / 240) |

**The coverage-target risk this file used to carry is closed with room to spare.** The 69.81%
slip described in the 2026-08-09 section below is history: coverage is now 20 points above the
70% team target, and the project clears the rubric's 5/5 Testing band on both conditions
(>90% interactor, >70% overall).

Per-package, the two large uncovered areas are unchanged in kind but renamed and much improved:
`app` 20.7% (composition root, uncovered by design) and `views` 87.3% — the old `view` package
was 15.73%; PR #44 and PR #50 brought Swing code under test. `views.chart` is 100%.
`interface_adapter.momentum` 80.4% and `interface_adapter.comparison` 88.1% are now the weakest
adapter packages.

Full per-package and per-class tables, plus the untested-code accounting:
`plan/handoffs/verify-on-merged-main-2026-08-10.md`.

---

## Historical record — the team story wired end to end (2026-08-09)

**The largest risk this file used to carry is closed.** PRs #29–#34 landed after the section below
was written. `app/Main.java` now constructs `BacktestEngine`, `BacktestViewModel`,
`BacktestPresenter`, `CompletedBacktestStore`, `RunBacktestInteractor`, `BacktestController` and
`BacktestView`, and an anonymous `RunBacktestOutputBoundary` decorator files each successful run
into the store the Compare screen reads. `MainView` carries five nav buttons. So **add a stock →
configure a strategy → run a backtest → compare results** is reachable by a user, and the Compare
screen is no longer permanently empty.

Also closed since: the minimum window size (`MainView` sets 820×500), the invisible save failures
(`Main` binds `PersistenceViewModel` to a status line in `MainView`), and the last 19 Checkstyle
violations (PR #33).

**Verified build on `main`, 2026-08-09:** `mvn clean verify` — BUILD SUCCESS, 0 Checkstyle
violations, **490 tests**, 0 failures, **70.86% overall line coverage** (1549/2186).

**Coverage had slipped below the 70% team target** — it measured 69.81% (1526/2186) once PRs
#29–#32 landed wired-but-untested classes, down from 73.1%. Closed by covering
`interface_adapter.persistence`, which was **0%** (0/23 lines) despite `PersistencePresenter` and
`PersistenceViewModel` both being live in `Main`; it is now 100% and the project clears the target
with ~19 lines of margin. `PersistencePresenterTest` pins the case that motivated the binding in the
first place: a failed save must replace "Watchlist saved." rather than leave it on screen.

Remaining per-package: `view` 15.73% and `app` 0% (composition root, uncovered by design) are the
two large uncovered areas; `interface_adapter.momentum` 76.32%, `interface_adapter.comparison`
79.17%. Everything in `use_case` and `interface_adapter.watchlist` is at or near 100%.

⚠️ **Before the demo, every member must delete their local `watchlist.dat`.** PR #23 added both new
configuration fields *and* `serialVersionUID = 1L` to `WatchlistEntry`, which is exactly the change
`vision.md` §5.2 warned would make older saved files unreadable. The recovery path then opens on an
empty watchlist, which on stage reads as a persistence bug rather than a format change.

---

## Historical record — the state as of 2026-08-08

**The plan is finished, and both items it used to list as owed are now done:**

1. ~~The `vision.md` §8 manual walkthrough~~ — **complete.** All eight steps recorded in
   `plan/handoffs/walkthrough.md`. It found a real defect: `JTable` installs its own focus
   traversal keys, so Tab could never leave either table and never reached the status line
   where errors are announced — a WCAG 2.1.2 keyboard trap. Fixed in **PR #27**. It also
   found the shrink-resize clipping, raised for Member 4 rather than patched.
2. ~~The "after" screenshot~~ — **captured** 2026-08-08, `docs/after-watchlist-view.png`.

## Cross-cutting work closed since the plan ended (2026-08-08)

Done on `chore/presentation-readiness`, after nobody claimed the gaps raised in
`plan/handoffs/team-raise-2026-08-08.md`:

- **Checkstyle enforced** — `checkstyle.xml` + plugin bound to `validate`, failing the
  build on any violation. Found 172, all resolved. Closes warning **W2-3** (exception
  variables named `e`) and the `MomentumConfiguration` indentation defect.
- **`accessibility-report.md` written** — all seven Universal Design principles, target
  market, and the excluded-demographics discussion. Was a 5-point category scoring zero.
- **`LICENSE` added**; README no longer claims a license it does not have.
- **README corrected** — the Features section no longer presents unreachable features as
  delivered, and Usage no longer instructs the reader to launch with `mvn exec:java`, which
  the same file said elsewhere cannot work.
- **`docs/architecture.md`** — whole-project layer diagram, patterns/SOLID evidence, and
  both Alpha Vantage endpoints named for the slides.
- **`view` no longer imports `entity`** — `ComparisonView`, `BacktestResultsView` and the
  deleted `MainAppState` all did. View models now carry display-ready records and the
  presenters format. `MainAppState` became `interface_adapter.comparison.CompletedBacktestStore`.

**Current build:** `mvn clean verify` — 0 Checkstyle violations, **461 tests**, 0 failures,
**73.1%** overall line coverage (1247/1706).

~~**Still open, and not mine to close**~~ — the run-backtest wiring gap raised in
`plan/handoffs/team-raise-2026-08-08.md` §1 was **picked up and closed by PR #29**. See the
2026-08-09 section at the top of this file.

## Completed phases

### Phase 5 — Hand-off Proof and Close-out ✅ 2026-08-08

- **Build:** `mvn -o clean verify` green — **415 tests**, 0 failures (up from 403).
- **Coverage:** project **71.69%** (1008/1406), *up* from 71.5% — the phase widened the
  margin rather than spending it. All six ≥90% gates hold: the four interactors at **100%**
  lines, `AlphaVantageMarketDataAccessObject` 98.9%, `CachingMarketDataGateway` 100%. The
  whole `interface_adapter.watchlist` package is 100%. **No JaCoCo exclusions are
  configured** — that is the raw whole-project number, which is the stronger claim.
- **Review:** `plan/review-phase-5.md` — **PASS WITH WARNINGS**, **zero criticals**, seven
  warnings. The reviewer re-measured every number independently and checked the Phase 5
  "Done when" criteria item by item; all four are MET. Six warnings were fixed at close-out
  (W5-1…W5-6), two left open (W5-7, W5-8).
- **Delivered — the graded artifact:** `MarketDataHandoffTest` drives
  `InMemoryMarketDataGateway.withSampleData()` → `AddTickerInteractor` →
  `StockRepository.findBySymbol` and feeds the result *straight* into a real
  `MovingAverageCrossoverStrategy(5, 20)` — the reviewer confirmed the list handed to the
  strategy is the identical reference `Stock` exposes, with no reshaping. It asserts a real
  **BUY and a real SELL**, not merely that the call did not throw, plus the three
  preconditions and the H8 compact-response ceiling (100 records: a 100 window throws, a 99
  window does not — the cliff located exactly, not approximately).
- **Also delivered:** `plan/handoffs/team-notes.md` (the ~100-day ceiling, unadjusted
  prices, the hand-off surface, D4-e and W4-9 for Member 4, the three unowned gaps, the
  missing backtest engine), `coverage.md`, `walkthrough.md`.
- **Coverage levers closed:** W2-7 by Agent B (pure `apiKeyFrom(String)` extracted; the DAO
  went from 4 uncovered lines to 1, and `apiKeyFromEnvironment()` stays uncovered *by
  design* — covering it needs the `System.getenv` tautology D13 already removed once) and
  W2-8 by Agent A (a `WatchlistSnapshotFactory.build(...)` assertion that genuinely
  discriminates A-N3's behaviour; Agent A verified this by temporarily reverting the factory
  and confirming exactly one test failed).
- **Parallelism:** A and B merged with **zero overlapping files** for the third time — the
  layer split held in every phase that used it. Worktree base commits were verified before
  spawning this time, per D3-f.
- **Deviations:** D5-a…D5-d. **Read D5-a** — it carries the register of sixteen warnings
  that are knowingly unclosed now that the plan is ending.

### Phase 4 — Composition Root ✅ 2026-08-08

- **Commit:** `575e3ad`.
- **Build:** `mvn -o clean verify` green — **403 tests**, 0 failures.
- **Coverage:** project **71.5%**, down from 73.3% because `Main`'s wiring block is 47
  uncovered lines. Still over the 70% target, but the margin is now **~20 lines** — see
  **W4-10**, which supersedes D3-g's ~45-line estimate.
- **Review:** `plan/review-phase-4.md` — **PASS WITH WARNINGS**, **zero criticals**, nine
  warnings. Four fixed at close-out (W4-2, W4-6, W4-7's over-length line, W4-8); five carried
  to Phase 5 as W4-1, W4-3, W4-4, W4-5, W4-9, W4-10 in `plan/decisions.md`.
- **Delivered:** the feature runs offline with no API key. Gateway selection is the only
  environment read in the codebase; one `WatchlistPresenter` serves all four interactors;
  `showWatchlist("")` at launch renders a restored watchlist. `git diff` on Member 4's two
  files touches only the integration points his own comments designate — the reviewer checked
  each of the seven changed lines against §6.
- **Also closed:** W3-5, the vertical's only genuine race, verified against a realized
  `JFrame` rather than by reading (D4-b). **W3-8 was not closed** despite the Phase 3
  prediction that one edit would fix both — see W4-1.
- **Deviations:** D4-a…D4-f. Read **D4-f** before starting Phase 5.

### Phase 3 — Adapter and View ✅ 2026-08-07

- **Build:** `mvn -o clean verify` green — **403 tests**, 0 failures.
- **Coverage:** project **73.3%**, above the Phase 5 70% target. The entire
  `interface_adapter.watchlist` package is **100% line and 100% branch** — presenter
  103/103 and 37/37, state 35/35 and 20/20, controller 14/14, view model 12/12. All six
  Phase 2 ≥90% gates still hold. `WatchlistView` is 0% by design (hazard H6).
- **Review:** `plan/review-phase-3.md` — **PASS WITH WARNINGS**, **zero criticals**, eleven
  warnings. Five were fixed at close-out because they were orchestrator-owned contract
  drift that Phase 4 and 5 agents would read as truth (W3-1, W3-2, W3-3, W3-4, W3-9), as
  was W3-7's brief self-contradiction. The other six are logged as W3-5, W3-6, W3-8, W3-10,
  W3-11, W3-12 in `plan/decisions.md`, mostly against Phase 4.
- **Delivered:** `WatchlistPresenter` serving all four output boundaries, with an
  exhaustive `switch` over `WatchlistFailure.Kind` and no `default`; `WatchlistController`
  as four pass-throughs; `WatchlistView` with both halves of hazard H1; and the
  orchestrator's `WatchlistViewModel`/`WatchlistState` seam. All 11 failure strings and all
  8 success strings pinned byte-for-byte with `assertEquals`.
- **Parallelism:** C and D again merged with **zero overlapping files**. One cross-agent
  need (D-N1) was raised rather than absorbed and resolved in the presenter.

### Phase 2 — Core Remediation and Interactor Tests ✅ 2026-08-06

- **Build:** `mvn -o clean verify` green — 319 tests, 0 failures.
- **Coverage:** all four interactors **100%**; `CachingMarketDataGateway` 100%;
  `AlphaVantageMarketDataAccessObject` 95.3%. All six ≥90% gates hold. Project-wide
  **80.6%**, already past the Phase 5 70% target before the view layer lands.
- **Review:** `plan/review-phase-2.md` — **PASS WITH WARNINGS**, **zero criticals**.
  Twelve warnings; W10 fixed immediately (orchestrator-owned doc drift), the other eleven
  logged in `plan/decisions.md` as W2-1…W2-12 and assigned mostly to Phase 5.
- **Delivered:** defects D1–D9 and D12–D13 all closed and named in commits; carried-forward
  warnings W1–W5 all closed; `ShowWatchlistInteractor` created; the `WatchlistInputSupport`
  preamble extracted; the two `Recording*` doubles activated by real tests. No test can
  reach the network.
- **Parallelism:** Agents A and B ran in separate git worktrees and merged with **zero
  overlapping files** — the layer split held exactly as designed. Worktrees removed.

### Phase 1 — Contract Freeze ✅ 2026-08-06

- **Commit SHA (branch point for Agents A and B):** `09ff73537e8d1436e8d2dd69589a24c0d2096a74`
- Preceding commit `2f4e611` landed pre-existing uncommitted `data_access` and test
  scaffolding separately — see `plan/decisions.md` D1-a.
- **Build:** `mvn -o clean test` green — 164 tests, 0 failures.
- **Review:** `plan/review-phase-1.md` — **PASS WITH WARNINGS**. Both criticals were
  process (uncommitted tree, Phase-2-scope contamination); both resolved by the two-commit
  split. Five warnings logged in `plan/decisions.md` W1–W5 and assigned to Phase 2.
- **Delivered:** `WatchlistFailure` and `WatchlistSnapshot` final with value semantics;
  `MarketDataGateway`'s normative null/blank contract; `AddTickerOutputData` collapsed to
  one company-name field; the four `ShowWatchlist*` contract files. Hazard H2 closed.

## Deviations

- **D1-a** — one commit became two; the nine-file `git diff --stat` check in
  `plan/phase-1.md` was unsatisfiable because most watchlist files were untracked from
  before the plan began.
- **D1-b** — **read before spawning Phase 2.** Agents A and B branch from a tree that
  already contains files their briefs read as not-yet-written:
  `AddTickerInteractor`, `RemoveTickerInteractor`, `RefreshTickerInteractor`,
  `AlphaVantageMarketDataAccessObject`, `CachingMarketDataGateway` and their tests. Their
  Phase 2 briefs must say *remediate against D1–D9, D12–D13*, not *create*.

- **D2-a** — the two follow-up round-trips (A-N1's tail, B-N1) ran serially in the main
  tree rather than in worktrees; by then each depended on the other agent's merged output.
- **D2-b** — three cross-agent needs (A-N1, A-N2, B-N1) were resolved mid-phase rather
  than deferred. See `plan/decisions.md`.
- **D2-c** — `WatchlistSnapshotFactory` selected price rows now follow watchlist
  membership (A-N3). Reviewer verified the changed branch is unreachable through all four
  public boundaries.

- **D3-a** — the add-success prose table has **eight** rows, not seven; the Phase 3 gate is
  11 failure + 8 success strings. See `plan/decisions.md`.
- **D3-b** — the `vision.md` §8 "before" screenshot moved from Agent D to the owner.
  **Still uncaptured — see Next.**
- **D3-c** — `plan/phases.md` Phase 4 "Done when" says the price table fills
  oldest-to-newest; the frozen, tested contract emits newest-first. Deferred to the Phase 4
  gate. Nothing in Phase 3 depends on the answer (verified by the reviewer: neither the
  presenter nor the view sorts).
- **D3-d** — D-N1 accepted and scoped to Show Watchlist only; the optional half (extending
  it to Add) was declined.
- **D3-e** — D-N2 accepted as a risk, not fixed. The reviewer then found the mitigation
  weaker than claimed — see W3-6.
- **D3-f** — **read before spawning any future parallel phase.** Both agent worktrees were
  provisioned from the wrong commit (`bff35db`, not `394d3fb`). Both agents caught it
  themselves and reset before writing, which is luck, not process. **The orchestrator must
  verify each worktree's base commit before the agents start.**
- **D3-g** — the coverage margin over 70% is ~45 lines once Phase 4's uncovered `Main`
  wiring lands. Thinner than 73.3% suggests.
- **D3-h** — `plan/handoffs/` was cleared *selectively*: the four agent files were removed,
  `screenshots.md` was deliberately kept because it carries an unfinished obligation with an
  irreversible deadline.

## Next — the plan is complete; these are the human items it cannot do

**1. Run `plan/handoffs/walkthrough.md` by hand, and capture the "after" screenshot.**
The full checklist and the launch recipe are in that file. Short version (D4-c —
`mvn exec:java` and `dependency:build-classpath` both fail offline, neither plugin is in the
local repository):

```
mvn -o clean compile
java -cp "target/classes;C:\Users\abhir\.m2\repository\org\json\json\20240303\json-20240303.jar" app.Main
```

Do not set `ALPHA_VANTAGE_API_KEY` — running offline is the point.

**2. Post `plan/handoffs/team-notes.md` to the team.** This is the ping `vision.md` §9 asks
for, and it carries the three things teammates most need: the ~100-trading-day ceiling
(Members 2 and 3), the fact that the app now opens on the Watchlist card and that save
failures are currently invisible (Member 4, D4-e and W4-9), and the three unowned gaps —
Checkstyle, `accessibility-report.md`, `serialVersionUID`.

**3. Still needed for the individual presentation, beyond the screenshots:** the interactor's
code and a class diagram of the full use case (`vision.md` §8). Neither is in this plan's
scope.

**Before touching this code again, read `plan/decisions.md` § Phase 5 D5-a.** It is the
register of sixteen warnings that are knowingly unclosed. The plan ending is what makes that
register load-bearing: nothing will come along later to close them.

---

## Superseded — Phase 3 pre-flight notes (kept for the record)

Phase 3 — Adapter and View. Per `agents/orchestrator.md` §4 and phases.md, **write
`interface_adapter/watchlist/WatchlistViewModel.java` and `WatchlistState.java` first,
per §5.3 and §5.4, and commit them before spawning either agent** — they are the only
files C and D share, and authoring them centrally is what breaks the C↔D cycle (H3).

Three things Agent C must be told, all new since its brief was written:

1. `AddTickerOutputData.getCompanyNameFailureKind()` is new and nullable. A successful add
   now has **three** distinguishable states — name present, no company record, and lookup
   failed — so the success-prose table gains a *success-with-caveat* string, not a failure
   row.
2. `ShowWatchlistOutputBoundary.prepareFailView` must still be implemented but **no
   interactor will ever drive it**. Cover it by calling it directly from a presenter test;
   do not assert that Show Watchlist produces a `WatchlistFailure`. (§5.2 corrected.)
3. Price lists returned by any gateway are now **unmodifiable** — the presenter must copy
   before sorting.

Restate to both C and D: hazard H1 (the view's `propertyChange` must re-dispatch via
`SwingUtilities.invokeLater` when off the EDT) is the single most likely runtime bug in
this vertical.
