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
