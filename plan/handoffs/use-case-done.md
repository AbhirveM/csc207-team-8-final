# Agent A — Phase 5 hand-off

**Branch:** `phase5/agent-a` (worktree `C:\Users\abhir\CSC207\wt-agent-a`), based on `2be20e1`.
**Scope this phase:** close warning **W2-8** only. Tests added, no production code touched.

---

## What was built

`src/test/java/use_case/watchlist/WatchlistSnapshotFactoryTest.java` — a new
package-private JUnit 5 test class calling `WatchlistSnapshotFactory.build(...)` directly.
Four tests:

1. `aSelectedSymbolWithStoredPricesButNoWatchlistEntryYieldsNoPriceRows` — **the W2-8
   regression guard.** `MSFT` is saved into `InMemoryStockRepository` but never added to
   the `Watchlist`; `AAPL` is on the watchlist. The test first asserts the stock *is*
   present in the repository with four prices and that the watchlist does *not* contain it,
   so an empty result cannot pass vacuously because the save never happened. It then
   asserts `getSelectedPriceRows()` is empty while `getSelectedSymbol()` is still `MSFT`
   and the single ticker row is `AAPL`.
2. `theSameSelectionYieldsPriceRowsOnceItsTickerIsOnTheWatchlist` — **the positive
   control.** Identical repository contents and identical selection, with the ticker added
   to the watchlist: four price rows come back, newest-first. A factory that returned empty
   rows unconditionally would fail here.
3. `aSelectedTickerOnTheWatchlistWithNoStoredPricesYieldsNoPriceRows` — the other half of
   the same branch: membership without prices (a ticker restored from `watchlist.dat`
   before any refresh) renders with `priceCount() == 0` and no price rows.
4. `buildRejectsNullCollaborators` — pins the two `Objects.requireNonNull` guards added in
   Phase 2.

### Why a new class rather than adding to `ShowWatchlistInteractorTest`

The behaviour W2-8 names is **unreachable through any of the four public input
boundaries** — `ShowWatchlistInteractor` normalizes an unknown selection to `""` before
the factory ever sees it, which is precisely why the existing unknown-selection test
passes under both the old and new code. Filing the assertion inside an interactor test
would misrepresent it as behaviour some boundary exercises, and would put a
non-interactor-shaped setup (no interactor, no presenter, no `SaveWatchlist`) inside a
class whose `setUp` builds all three. A dedicated class names its subject, keeps the
`WatchlistSnapshotFactory` javadoc contract and its tests one hop apart, and is where a
future reader looking for "what pins D2-c" would actually look.

### Revert-detection verified, not assumed

The factory was temporarily patched back to the old shape — `priceRowsFor(stocks
.findBySymbol(selectedSymbol))` instead of `priceRowsFor(selectedStock)` — and the suite
re-run. Result: **exactly one failure**, test 1 at the `getSelectedPriceRows().isEmpty()`
assertion; tests 2–4 stayed green. The patch was then reverted with `git checkout --`, so
no production file is modified on this branch (`git status` shows only the new test file).
This is the evidence that W2-8's "stop a future refactor silently reverting it" is actually
satisfied rather than nominally addressed.

---

## Files

| File | Change |
|---|---|
| `src/test/java/use_case/watchlist/WatchlistSnapshotFactoryTest.java` | **created** |

Nothing else created or modified. No production code, no `pom.xml`, no orchestrator
carve-out file, no other agent's tree. `MarketDataHandoffTest.java` was **not** created —
it remains the orchestrator's carve-out per `agents/orchestrator.md` §4.

---

## Verification

- `mvn -o clean verify` — **BUILD SUCCESS**.
- **407 tests**, 0 failures, 0 errors, 0 skipped (baseline was 403; +4).
- No new dependency. No Mockito, no AssertJ, no network access — the test builds its
  series from the existing `WatchlistTestData` helper and the existing
  `InMemoryStockRepository`.
- Coverage moves the right way: four new tests against already-covered production lines,
  and **zero new production lines**, so the ~20-line margin over the 70% target (W4-10) is
  not spent — it widens slightly.

---

## Open needs

None. No request was written to `plan/handoffs/use-case-needs.md`; nothing outside the
Owns section was required.

---

## For the next phase

- **W2-8 can be marked closed** in `plan/decisions.md`.
- The `WatchlistSnapshotFactory` javadoc still does not *state* the membership rule in
  prose — the behaviour is now pinned by test and explained in the class comment on the
  build loop, but a reader of `build(...)`'s `@param selectedSymbol` ("the symbol whose
  prices should be shown") would not learn that a non-member symbol shows nothing. That is
  a one-line javadoc clarification, not a defect, and it touches a file Agent A owns —
  left undone this phase only because the task was scoped to W2-8's test. Worth folding
  into any later doc pass.
- Whoever writes `plan/handoffs/coverage.md` should note that
  `WatchlistSnapshotFactory` is now covered directly as well as transitively, including the
  two null-guard branches.
