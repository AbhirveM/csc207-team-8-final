# MarketLens — Member 1 Vertical: Phases

Five sequential phases. Odd phases are orchestrator-only (contracts and integration);
even phases fan out to parallel component agents.

Source of truth for scope: `vision.md`. Source of truth for the defect list:
`plan/phase-2.md`.

**Standing rule:** a phase is not done until `agents/reviewer.md` has written
`plan/review-phase-N.md` with status `PASS` or `PASS WITH WARNINGS`. A `FAIL` sends the
work back to the owning agent; the next phase does not start.

---

## Phase 1: Contract Freeze

- **Goal:** every byte that crosses an agent boundary exists and compiles, so Phases 2
  and 3 can run agents in parallel without either one waiting on the other's file.
- **Active agents:** orchestrator only. Then reviewer.
- **Shared interfaces to write first:** this phase *is* the writing of them —
  `WatchlistFailure` (final + value semantics), `WatchlistSnapshot` (final + value
  semantics), the `MarketDataGateway` null/blank symbol contract, the reshaped
  `AddTickerOutputData`, and the four new `ShowWatchlist*` files.
- **Done when:**
  - `mvn -o clean test` is green.
  - `WatchlistFailure` and `WatchlistSnapshot` are `final` and have
    `equals`/`hashCode`/`toString`.
  - `MarketDataGateway`'s javadoc states the null and blank behaviour normatively.
  - `AddTickerOutputData` has exactly one company-name field.
  - `ShowWatchlistInputBoundary`, `ShowWatchlistInputData`,
    `ShowWatchlistOutputBoundary`, `ShowWatchlistOutputData` exist and compile.
  - All frozen files are committed on `feature/watchlist-use-cases`, and the commit SHA
    is recorded in `plan/status.md`.
  - `plan/review-phase-1.md` is not FAIL.

---

## Phase 2: Core Remediation and Interactor Tests

- **Goal:** every audited defect below the adapter layer is closed, and the four
  interactors — the graded heart of the vertical — are test-covered.
- **Active agents:** `use-case` (A) ∥ `data-access` (B). Then reviewer.
- **Shared interfaces to write first:** none new. Everything A and B need was frozen in
  Phase 1. The orchestrator writes the agent briefs referencing the Phase 1 commit SHA
  and provisions a git worktree per agent (hazard H4).
- **Done when:**
  - `mvn -o clean verify` is green.
  - JaCoCo line coverage ≥90% on `AddTickerInteractor`, `RemoveTickerInteractor`,
    `RefreshTickerInteractor`, `ShowWatchlistInteractor`,
    `AlphaVantageMarketDataAccessObject`, `CachingMarketDataGateway`.
  - Defects D1–D9 and D12–D13 are each closed and named in a commit message.
  - `RecordingWatchlistPresenter` and `RecordingSaveWatchlist` are used by real tests
    (they are currently dead code).
  - `grep -rn "JdkHttpJsonClient" src/test` returns nothing — no test can reach the
    network.
  - `plan/review-phase-2.md` is not FAIL.

---

## Phase 3: Adapter and View

- **Goal:** the vertical becomes visible and operable — one presenter serving four
  output boundaries, and a Swing panel that never blocks the event thread.
- **Active agents:** `adapter` (C) ∥ `view` (D). Then reviewer.
- **Shared interfaces to write first:** the orchestrator writes
  `interface_adapter/watchlist/WatchlistViewModel.java` and
  `interface_adapter/watchlist/WatchlistState.java` **before spawning either agent**.
  These are the only files C and D share; authoring them centrally is what breaks the
  C↔D cycle (hazard H3).
- **Done when:**
  - `mvn -o clean verify` is green.
  - `WatchlistPresenterTest` pins the exact user-facing string for all 11
    `WatchlistFailure.Kind` values and all 7 success messages.
  - `grep -rn "javax.swing" src/main/java/interface_adapter` returns nothing.
  - `grep -n "use_case" src/main/java/view/WatchlistView.java` returns nothing.
  - Every button handler in `WatchlistView` runs its controller call inside a
    `SwingWorker`, and the view's `propertyChange` re-dispatches via
    `SwingUtilities.invokeLater` when off the EDT.
  - `plan/review-phase-3.md` is not FAIL.

---

## Phase 4: Composition Root

- **Goal:** the feature actually runs, offline, with no API key.
- **Active agents:** orchestrator only. Then reviewer.
- **Shared interfaces to write first:** none. This phase only consumes what Phases 1–3
  produced. The two files edited (`app/Main.java`, `view/MainView.java`) belong to
  Member 4 and are touched append-only, in the spots his own comments designate.
- **Done when:**
  - `mvn -o clean verify` is green.
  - With `ALPHA_VANTAGE_API_KEY` unset, launching the app shows a Watchlist nav button,
    and the `vision.md` §8 walkthrough passes end to end: type `aapl` → normalizes to
    `AAPL` → company name resolves → price table fills with oldest-to-newest rows →
    Refresh updates the count and latest date → Remove drops the row → a junk symbol
    shows a specific worded error → restart still shows the watchlist.
  - Clicking a different row in the watchlist table repopulates the price table
    (this is what `ShowWatchlist` exists for).
  - `git diff` on `Main.java` and `MainView.java` is additive only — no existing line
    is deleted or reordered.
  - `plan/review-phase-4.md` is not FAIL.

---

## Phase 5: Hand-off Proof and Close-out

- **Goal:** prove the hand-off to Members 2 and 3 executably rather than verbally, and
  land the coverage numbers the rubric asks for.
- **Active agents:** orchestrator, with `use-case` (A) and `data-access` (B) on call if
  coverage falls short. Then reviewer.
- **Shared interfaces to write first:** none.
- **Done when:**
  - A test feeds this vertical's `Stock.getDailyPrices()` straight into
    `MovingAverageCrossoverStrategy.generateSignals(...)` and asserts at least one BUY
    and one SELL come back — not just HOLDs.
  - `mvn clean verify` reports ≥70% overall line coverage and ≥90% on the four
    interactors, with any exclusions documented.
  - `plan/handoffs/` contains the team notes: the ~100-trading-day compact-response
    ceiling (any strategy window above ~90 silently violates `generateSignals`'s
    precondition), and the three unowned gaps this vertical deliberately did **not**
    absorb — Checkstyle config, `accessibility-report.md`, and `serialVersionUID`.
  - `plan/review-phase-5.md` is not FAIL.

---

## Agent roster

| Agent | File | Active in |
|---|---|---|
| orchestrator | `agents/orchestrator.md` | 1, 2, 3, 4, 5 |
| use-case (A) | `agents/use-case.md` | 2, 5 (on call) |
| data-access (B) | `agents/data-access.md` | 2, 5 (on call) |
| adapter (C) | `agents/adapter.md` | 3 |
| view (D) | `agents/view.md` | 3 |
| reviewer | `agents/reviewer.md` | after every phase |
