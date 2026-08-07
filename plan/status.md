# Status

Current phase: 4 — Composition Root
Active agents: none (Phase 4 is orchestrator-only — two append-only edits into Member 4's
`app/Main.java` and `view/MainView.java`, per `agents/orchestrator.md` §6. Then reviewer.)
Branch: `feature/watchlist-use-cases`

## Completed phases

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

## Next

Phase 4 — Composition Root. Orchestrator only, no agents. Two **append-only** edits into
Member 4's files per `agents/orchestrator.md` §6: uncomment the nav-button template at
`view/MainView.java:30-33` and replace the TODO at `app/Main.java:42-44` with the watchlist
wiring block. `git diff` on both must be additive only.

**Do this before the first Phase 4 commit — it is the one irreversible item on the board.**
The `vision.md` §8 "before" screenshot is still uncaptured. `plan/handoffs/screenshots.md`
has the run command and a placeholder path. Once the nav button is uncommented the shot
cannot be retaken without a revert.

Four things Phase 4 must know, all established in Phase 3:

1. Use **one** `WatchlistPresenter` instance across all four interactors — it is what keeps
   an add and a refresh from drifting into two different-sounding applications.
2. Construct `WatchlistController` in the order `(add, remove, refresh, show)`.
3. Constructing `WatchlistView` alone paints `WatchlistState.initial()` — `Ready.` with
   empty tables. Call `watchlistController.showWatchlist("")` inside the existing
   `SwingUtilities.invokeLater` or the restored watchlist never renders.
4. Resolve **D3-c** at this gate. Recommended: correct the `plan/phases.md` sentence to
   newest-first rather than change the frozen contract — it costs one line of prose and
   breaks nothing, whereas the reverse also falsifies two `WatchlistPresenter` javadocs.

W3-5 and W3-8 are both cheap to close while wiring: disabling `tickerTable` alongside the
buttons in `WatchlistView.setButtonsEnabled` fixes the one genuine race in the vertical and
the in-progress-typing loss at the same time.

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
