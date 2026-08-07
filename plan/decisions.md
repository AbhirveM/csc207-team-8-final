# Decisions and deviations

## Phase 1 — Contract Freeze

### D1-a. Pre-existing uncommitted work split into its own commit
`plan/phase-1.md` §Verification expected `git diff --stat` to show exactly nine files.
It could not: most of `use_case/watchlist/**`, all of the new `data_access/**` gateway
work, and four test files were already present in the working tree **untracked** from
before this plan started (mtimes 13:53–14:02, ahead of the Phase 1 edits at 14:35).

Resolution: two commits rather than one. The pre-existing `data_access` and test
scaffolding lands first, on its own, so the Phase 1 contract-freeze commit reads as the
contract freeze it is. No pre-existing file was rewritten to make this work.

### D1-b. Phase 2 agent briefs must be amended before spawning
Consequence of D1-a: Agents A (use-case) and B (data-access) will branch from a tree
that already contains files their briefs assume they are about to author —
`AddTickerInteractor`, `RemoveTickerInteractor`, `RefreshTickerInteractor`,
`AlphaVantageMarketDataAccessObject`, `CachingMarketDataGateway`, and their tests.

**Action at the head of Phase 2:** restate to both agents that these files exist and
their task is to *remediate* them against the D1–D9, D12–D13 defect list, not to create
them from nothing.

### D1-c. `AddTickerInteractor` edited by the orchestrator
Sanctioned by `agents/orchestrator.md` §4 — the single compile-preserving call-site fix
for the `AddTickerOutputData` reshape. One line. Agent A branches from the resulting
commit.

---

## Warnings carried forward from `plan/review-phase-1.md`

Logged here rather than fixed, because each falls inside an agent's ownership and lands
naturally in Phase 2.

| # | Warning | Owner | Due |
|---|---|---|---|
| W1 | `MarketDataGateway`'s new javadoc over-promises: none of the three implementations guards null or blank, and `CachingMarketDataGateway` still keys the cache on `""` before validating. This is the gap the contract was written to close. | B | Phase 2 (D8) |
| W2 | Tautological test at `AlphaVantageMarketDataAccessObjectTest.java:278-291` — an `if/else` on `System.getenv` that asserts whichever branch it lands in. Verbatim the pattern `agents/reviewer.md` bans. | B | Phase 2 |
| W3 | `RecordingWatchlistPresenter` and `RecordingSaveWatchlist` have zero callers — dead code until real interactor tests use them. Already a Phase 2 exit criterion. | A | Phase 2 |
| W4 | Swallowed diagnostic at `AddTickerInteractor.java:94-96`: the company-name `catch` discards `exception.getKind()`. Per §7 the kind should be threaded into the status, not dropped. | A | Phase 2 |
| W5 | The Phase 1 `equals`/`hashCode`/`toString` work is untested — no test exists for any watchlist type except `TickerSymbolValidator`. | A | Phase 2 |

W1 is the first thing Phase 2 should close: every other gateway behaviour is specified
relative to it.

**Outcome:** all five closed in Phase 2. W1 by Agent B in `dc6e972` (D8), W2 in `f288aa1`
(D13), W3 and W5 by Agent A in `9ca12c0` (D12), W4 in `fe8a25d` plus `8825910` once the
orchestrator resolved A-N1.

---

## Phase 2 — Core Remediation and Interactor Tests

### D2-a. Two agents ran in parallel git worktrees
`C:\Users\abhir\CSC207\wt-agent-a` (branch `phase2/agent-a`) and `wt-agent-b`
(`phase2/agent-b`), both from `e3cd012`, per hazard H4. Both merged with `--no-ff` and
**zero overlapping files** — the layer split in `agents/orchestrator.md` §3 held exactly
as designed. Worktrees removed at the end of the phase.

The two follow-up round-trips (A-N1's tail and B-N1) ran **serially in the main tree**
instead: by then each depended on the other agent's merged output, so a worktree would
have been stale, and serialising removed the `target/` contention H4 warns about.

### D2-b. Three cross-agent needs resolved mid-phase
- **A-N1** (orchestrator, `fa3c665`): `AddTickerOutputData` gained an additive, nullable
  fifth constructor argument carrying the `MarketDataException.Kind` that blocked the
  company-name lookup, plus `getCompanyNameFailureKind()`. The four-argument constructor
  delegates with `null`, so nothing broke. This unblocked the half of D5/W4 that Agent A
  could not reach from inside its ownership. **Consequence for Agent C:** a successful add
  now has *three* distinguishable states, not two — name present (`null` kind), no company
  record (`""` + `null` kind), and lookup failed (`""` + a kind). The third is a
  success-with-caveat status string, not a new failure row.
- **A-N2** (orchestrator, `fa3c665` and this file's §5.2 correction): D6's
  `requireNonNull` makes `ShowWatchlistOutputBoundary.prepareFailView` permanently
  unreachable. Resolved as documentation, not code — routing a controller wiring bug to
  the presenter would put a fabricated failure in front of a user who cannot act on it.
  The reviewer endorsed fail-fast as the right call.
- **B-N1** (`1144daa` then `b7bb19c`): Agent A added
  `TickerSymbolValidator.normalizeKey(String)` (`Locale.ROOT`, pinned by a Turkish-locale
  test); Agent B collapsed **four** copies of the key idiom onto it — one more than N1
  named, in `InMemoryMarketDataGateway.syntheticSeries`. The resulting
  `data_access → use_case` edge runs in the sanctioned direction.

### D2-c. `WatchlistSnapshotFactory` behaviour change (A-N3)
Tidying the double `findBySymbol` means selected price rows now follow watchlist
membership: a `selectedSymbol` with stored prices but no watchlist entry yields no price
rows. The reviewer verified the changed branch is unreachable through all four public
boundaries and that the new semantics are the more defensible ones. Accepted.

---

## Warnings carried forward from `plan/review-phase-2.md`

Status **PASS WITH WARNINGS** — **zero criticals**. W10 was fixed immediately (it is an
orchestrator-owned file and would have misled Agent C in Phase 3). The rest are logged
here rather than fixed, per `/execute` step 5.

| # | Warning | Owner | Due |
|---|---|---|---|
| W2-1 | `concurrentReadersDoNotCorruptTheCache` drives a delegate whose call counters are plain `HashMap`s — the one test that exists to prove thread safety races on unsynchronised maps. Latent flake. Fix: `ConcurrentHashMap`/`AtomicInteger` counters in the fake, or pre-warm the cache before the threads start. | B | Phase 5 |
| W2-2 | `CachingMarketDataGateway.getCachedNameCount` javadoc promises "never above `MAX_NAME_ENTRIES`", but the size-check → clear → put sequence is not atomic, so the bound fails under the concurrency the class advertises. Soften the wording or make the reset atomic. | B | Phase 5 |
| W2-3 | Exception variables still named `e`: `RefreshTickerInteractor:64` (worst — the same method has `catch (IllegalArgumentException exception)` twenty lines below), `AlphaVantageMarketDataAccessObject:151,202,300`, `JsonFixtures:27`. | A, B | Phase 5 |
| W2-4 | All four `data_access` files order imports project-first, inverting §7 — while the `use_case` files next door were reordered *into* the correct form this phase. The two packages now visibly disagree. | B | Phase 5 |
| W2-5 | 26 lines over the ~100-char guidance (101–115). Four of them are the same `Objects.requireNonNull(this.stockRepository, ...)` line copied across the interactors; wrapping it once fixes four. | A, B | Phase 5 |
| W2-6 | `InMemoryStockRepository:27-29` hand-rolls `new NullPointerException(...)` instead of `Objects.requireNonNull(x, "...")` — the only departure from the §7 form in the phase's output. | B | Phase 5 |
| W2-7 | `apiKeyFromEnvironment()` is the DAO's only uncovered code (4 lines). Leaving it uncovered beats faking it (that way lies the W2/D13 tautology), but extracting a pure package-private `apiKeyFrom(String rawValue)` would cut the uncovered surface to one line and pin the `strip()` behaviour Phase 4 depends on. | B | Phase 4 or 5 |
| W2-8 | The A-N3 change is safe but no test *distinguishes* old from new behaviour — the existing unknown-selection test would pass under the old code too. A direct three-line `WatchlistSnapshotFactory.build(...)` assertion would stop a future refactor silently reverting it. | A | Phase 5 |
| W2-9 | The four use-case interactor tests import `data_access` implementations. Not a Dependency Rule violation (that rule is about `src/main`, which is clean), but it couples Agent A's suite to Agent B's classes and shows up in a whole-tree grep. A nested `Map`-backed fake in `WatchlistTestData` would remove both problems. Judgement call. | A | Phase 5 |
| W2-10 | `agents/orchestrator.md` §5.2 carried the superseded "for the null-`inputData` case only" claim that A-N2 corrected in the javadoc. | orchestrator | **Fixed in Phase 2** |
| W2-11 | D4's `catch (IllegalArgumentException exception)` in both Add and Refresh binds the exception and never reads it, discarding *which* invariant the series broke — the same swallowed-diagnostic shape as W4, one catch block away. `WatchlistFailure` has no field for it, so this needs a decision, not a one-liner. | A | Phase 5 |
| W2-12 | The four `data_access` classes are not `final` while every `use_case` counterpart is. §7 mandates `final` on data and interactor classes and does not name adapters, so this is defensible; the asymmetry is just unexplained. Lowest priority. | B | Phase 5 |

Two items for the **team**, not this vertical (raise, do not absorb — §9):
`view/ComparisonView.java:3` and `view/MainAppState.java:3` import `entity.BacktestResult`,
which a grader running a `view → entity` check will hit. Both are teammates' files.

---

## Phase 3 decisions

### D3-a — the add-success prose table has **eight** rows, not seven

`plan/status.md` §Next flagged that `AddTickerOutputData.getCompanyNameFailureKind()` is new
and nullable, so a successful add has three distinguishable outcomes: name resolved, no company
record, and lookup failed. Decided this phase: the third becomes a **success-with-caveat**
string, never a failure row — the ticker and its prices were stored either way.

| Add outcome | Message |
|---|---|
| name present | `Added %s (%s) with %d days of price history.` |
| no name, kind `null` | `Added %s with %d days of price history. No company name was available.` |
| no name, kind non-null | `Added %s with %d days of price history. The company name could not be looked up right now.` |

The caveat wording deliberately does **not** name the underlying kind. Threading
`(rate limit)` into the sentence would make the string vary by kind and stop
`WatchlistPresenterTest` pinning it with a single `assertEquals` — and the user cannot act on
the distinction anyway, since the add already succeeded.

The Phase 3 gate is therefore **11 failure strings + 8 success strings**. `plan/phases.md`,
`plan/phase-3.md` and `agents/adapter.md` § Interface Contract were corrected before Agent C
was spawned; all three are orchestrator-owned.

### D3-b — the "before" screenshot is the owner's to capture

`agents/view.md` § Presentation artifact asks Agent D to launch the app and screenshot it.
A subagent cannot reliably drive a desktop window, and burning agent time discovering that
helps nobody. Agent D was told to skip it; the orchestrator wrote
`plan/handoffs/screenshots.md` with the command and a placeholder path for the owner to fill
in **before Phase 4 wiring lands** — after that the artifact cannot be recreated.

### D3-c — price-row ordering: `plan/phases.md` Phase 4 contradicts the frozen contract

`plan/phases.md` § Phase 4 "Done when" says the price table fills **oldest-to-newest**.
`WatchlistSnapshotFactory.priceRowsFor` (line 79) deliberately emits **newest-first** — "the
order a user expects to read prices in" — and `WatchlistSnapshot`'s class javadoc documents
that explicitly, noting it is purely presentational and does not weaken the oldest-to-newest
guarantee `Stock` makes to the strategies. The two cannot both be right.

Not resolved in Phase 3: the presenter maps snapshot rows straight through in the order they
arrive, so this changes nothing about Agent C's or Agent D's work. It is a Phase 4 acceptance
question — decide it at that gate rather than churning a frozen, tested contract mid-phase.
