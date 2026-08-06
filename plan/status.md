# Status

Current phase: 3 — Adapter and View
Active agents: none (Phase 3 spawns `adapter` C ∥ `view` D, but **only after** the
orchestrator writes `WatchlistViewModel` and `WatchlistState` — that is what breaks the
C↔D cycle, hazard H3)
Branch: `feature/watchlist-use-cases`

## Completed phases

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

## Next

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
