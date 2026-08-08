# Review — Phase 5: Hand-off Proof and Close-out

Status: **PASS WITH WARNINGS**

Reviewed: the whole diff from `2be20e1` to the current working tree, committed and
uncommitted. Six changed paths, no deletions, no production behaviour change.

Independently re-measured, not taken on report:

- `mvn -o clean verify` — exit 0. **415 tests, 0 failures** (surefire XML aggregate).
- `target/site/jacoco/jacoco.csv` aggregate — **1008 / 1406 lines = 71.6927%**.
- `grep -rniE "apikey=[A-Za-z0-9]{8,}" src/ README.md` → nothing.
- `grep -rn "JdkHttpJsonClient\|HttpClient" src/test` → nothing.
- `grep -rn "javax.swing\|java.awt"` over `use_case`, `interface_adapter`, `entity` → nothing.

**Scope note.** Per the owner's decision this phase covered the Phase 5 exit criteria plus
W2-7 and W2-8 only. W2-1…W2-6, W2-9, W2-11, W2-12, W3-6, W3-10, W3-11, W4-1, W4-3, W4-4
and W4-5 were deliberately not attempted and are **not** raised below as findings. They
need re-logging as knowingly-unclosed at close-out.

---

## Critical (blocking — must fix before next phase)

**None.**

---

## The Phase 5 "Done when" criteria, item by item

| # | `plan/phases.md` § Phase 5 criterion | Verdict |
|---|---|---|
| 1 | A test feeds `Stock.getDailyPrices()` straight into `MovingAverageCrossoverStrategy.generateSignals(...)` and asserts at least one BUY and one SELL — not just HOLDs | **MET** |
| 2 | `mvn clean verify` reports ≥70% overall and ≥90% on the four interactors, exclusions documented | **MET** |
| 3 | `plan/handoffs/` contains the team notes: the ~100-day ceiling, and the three unowned gaps (Checkstyle, `accessibility-report.md`, `serialVersionUID`) | **MET** |
| 4 | `plan/review-phase-5.md` is not FAIL | **MET** (this file) |

### 1 — verified, not taken on trust

`MarketDataHandoffTest.marketDataFromTheRealPipelineProducesBothBuyAndSellSignals` builds
the `Stock` through `InMemoryMarketDataGateway.withSampleData()` → `AddTickerInteractor` →
`InMemoryStockRepository.findBySymbol("AAPL")`, then calls
`.generateSignals(stock.getDailyPrices())` with **no reshaping whatsoever** — the list
handed to the strategy is the identical `List<DailyPrice>` reference `Stock` exposes. That
is exactly what `vision.md` §8 asks for.

The BUY/SELL assertions are `countOf(...) > 0` on both types with distinct messages. They
cannot pass vacuously: `countOf` returns 0 for an empty or all-HOLD list, so a flat series
fails. `InMemoryMarketDataGateway.syntheticSeries` drives close on a sine of period 50 over
120 days, giving several full cycles, so the oscillation is real and deterministic rather
than incidental.

`assertEquals(stock.getPriceCount(), signals.size())` is arithmetically right:
`generateSignals` emits `longWindow` HOLDs plus `size - longWindow` computed signals =
`size`. With 120 sample days and `longWindow` 20 that is 120.

The precondition test walks the real output and asserts non-null elements, non-null dates
and **strictly** increasing dates (`previous.isBefore(current)`), matching `Stock`'s
constructor invariant rather than a weaker reading of it.

### The H8 ceiling arithmetic — checked against the code, not approximated

`MovingAverageCrossoverStrategy:57` guards `if (prices.size() < longWindow + 1) throw`.
The test builds exactly `COMPACT_RESPONSE_DAYS = 100` prices and asserts:

- `longWindow = 100` → `100 < 101` → throws. **Correct — the cliff is at
  `longWindow == size`, not near it.**
- `longWindow = 99` → `100 < 100` is false → returns, and the returned size is asserted to
  be 100. **Correct.**

`MovingAverageConfiguration`'s constructor rejects only non-positive windows and
`shortWindow >= longWindow`; `(5, 100)` and `(5, 99)` are both valid, so neither call is
diverted into a different `IllegalArgumentException`. The exception message is pinned with
`assertEquals` against the literal in `MovingAverageCrossoverStrategy:59`. The cliff is
located exactly. See W5-3 for the one structural nit here.

### 2 — every number in `coverage.md` re-derived from `jacoco.csv`

Confirmed exact: overall 1008/1406 = 71.69%; `AddTickerInteractor` 40/40,
`RemoveTickerInteractor` 19/19, `RefreshTickerInteractor` 35/35, `ShowWatchlistInteractor`
19/19, `AlphaVantageMarketDataAccessObject` 86/87, `CachingMarketDataGateway` 47/47;
`WatchlistSnapshotFactory` 40/40, `TickerSymbolValidator` 14/15,
`interface_adapter.watchlist` 166/166, `use_case.watchlist` 336/337; `view` 246 missed
(`WatchlistView` 148, `ComparisonView` 36, `MainView` 21), `app/Main` 48,
`interface_adapter.comparison` 30, `.persistence` 23 with `PersistenceViewModel` 14,
`JdkHttpJsonClient` 15; `entity` 165/189, `data_access` 272/295, `Trade` 0/13. The W3-12
method-axis qualification is also accurate — `RefreshTickerInteractor` METHOD_MISSED=1 and
`AlphaVantageMarketDataAccessObject` METHOD_MISSED=1.

`pom.xml` configures no JaCoCo `<excludes>`, so the "no exclusions, deliberately" framing
is true and the 71.69% is the raw whole-project number. Numeric defects are W5-1 and W5-2.

### 3 — the team notes, claim by claim

Verified against the code: the `outputsize=compact` request
(`AlphaVantageMarketDataAccessObject:159`); the `size() >= longWindow + 1` precondition and
its exact message; `Stock`'s four guarantees (oldest→newest, no nulls, no duplicate dates,
`Collections.unmodifiableList`) as **constructor invariants**, so "cannot reach you" is
literally true; 120 deterministic sample days for AAPL/MSFT/TSLA; **D4-e** — `Main:107`
adds `WatchlistView` and `Main:116` adds `ComparisonView`, and no `setActiveView` call
exists in `Main`, so the watchlist genuinely is the launch card; **W4-9** —
`SaveWatchlist.InputBoundary.execute` returns `void` and `PersistenceViewModel` is bound to
no view (0/14 lines, no view constructs it), so a failed save really is invisible behind an
"Added AAPL…" message; the `MainView` comment claim (the class javadoc still says "for now
only the Comparison view (yours) is wired in", and `MainView:32` still reads "Add nav
buttons for each screen here, e.g.:" above what is now live code).

Defects: W5-1 (overclaim), W5-4 (off-by-one), W5-5 (miscount).

---

## Warnings (non-blocking but fix soon)

- **W5-1 — `team-notes.md` overclaims the ceiling test as a guard.**
  `plan/handoffs/team-notes.md:41`: *"If anyone raises the window past the ceiling, the
  build breaks instead of the demo."* That is not what the test does.
  `aLongWindowAtTheCompactResponseCeilingBreaksTheStrategy` hard-codes 100 and 99 against a
  synthetic 100-day series it generates itself; no production code configures a long window,
  so raising one anywhere would not break this or any other test. What the test genuinely
  pins is the *arithmetic* of the cliff — that `longWindow == size` throws and
  `longWindow == size - 1` does not — which is valuable and worth saying. This is the one
  place in the hand-off documents where a confidently-worded sentence promises protection
  that does not exist, and it goes to two teammates who will rely on it. Reword.

- **W5-2 — `coverage.md`'s margin figure is the wrong arithmetic for the sentence it
  supports.** `plan/handoffs/coverage.md:76-82` states a **~24-line** margin and then draws
  the conclusion *"anyone adding uncovered production code to this repo from here needs
  tests landing beside it."* 24 is the margin for converting *covered* lines to uncovered
  (1008 − 0.70 × 1406 = 23.8). Adding new uncovered lines grows the denominator too:
  1008 / (1406 + x) ≥ 0.70 solves to **x ≤ 34**, and 1008/1440 = 70.0% exactly. The stated
  figure is conservative so no decision made from it is unsafe, but it is a numeric claim in
  a graded document and it is wrong by ten lines for the use it is put to. Either state 34
  for added lines, or say explicitly that 24 is the covered→uncovered figure.

- **W5-3 — the ceiling test's `assertThrows` lambda contains two throw sites.**
  `MarketDataHandoffTest:172-177` wraps both `new MovingAverageConfiguration(5, 100)` and
  `generateSignals(...)` in one lambda, and both can raise `IllegalArgumentException`. It is
  saved from vacuity only by the `assertEquals` on the message. Hoisting the configuration
  and strategy construction above the lambda would make the intent structural instead of
  message-dependent, and would survive someone loosening the message assertion later.

- **W5-4 — off-by-one in the `team-notes.md` window table.**
  `plan/handoffs/team-notes.md:29` says a long window of 50 "leaves ~49 signal days". The
  row above it computes exactly (100 − 20 = 80). `generateSignals` emits computed signals
  for indices `longWindow .. size-1`, so 100 records and a 50 window leave **50**, not 49.
  Inherited verbatim from `plan/phase-5.md:73`; correct both, or make the whole column
  approximate rather than one row exact and one row not.

- **W5-5 — `team-notes.md` §5 miscounts the `Serializable` type set.**
  Line 117 says "the seven `Serializable` entities". There are **eight**
  `implements Serializable` classes in `entity/` — `BacktestResult`, `DailyPrice`,
  `MovingAverageConfiguration`, `Ticker`, `Trade`, `TradingSignal`, `Watchlist`,
  `WatchlistEntry`. Exactly seven of them lack an explicit `serialVersionUID`;
  `MovingAverageConfiguration:7` already declares `= 1L`. The *actionable* set really is
  seven, so the advice is sound — but the sentence as written miscounts the types, in the
  one paragraph whose whole purpose is to stop somebody being careless here. One clause
  fixes it: "seven of the eight `Serializable` entities rely on the implicit, computed UID".

- **W5-6 — `MarketDataHandoffTest` loop variables are not `final`.**
  Lines 131 and 207: `for (DailyPrice price : prices)` and
  `for (TradingSignal signal : signals)`. §7 asks for `final` locals wherever possible, and
  Agent B's new test *in this same phase* writes `for (final String blank : ...)`
  (`AlphaVantageMarketDataAccessObjectTest:462`). The phase's own two new test files
  disagree with each other. Everything else in both files is clean: 4-space indent, K&R
  braces, `else` on its own line, `exception` not `e`, no wildcard imports, `java.*` block
  first, trailing newlines present, and **no line over 100 characters** in any of the four
  changed source files. (The two 101-character lines in
  `AlphaVantageMarketDataAccessObject.java` at 206 and 266 are pre-existing W2-5 territory,
  untouched this phase.)

- **W5-7 — `WatchlistSnapshotFactoryTest.buildRejectsNullCollaborators` leaves the third
  parameter unpinned.** Lines 130-136 assert `NullPointerException` for a null `watchlist`
  and a null `stocks`, but never for a null `selectedSymbol`. Whatever the factory does with
  a null selection — throw, or treat it as no selection — is currently unspecified by any
  test, and this file exists precisely so that a refactor cannot silently change factory
  semantics. Three lines close it.

---

## Notes

- **I fixed nothing.** No file outside `plan/review-phase-5.md` was touched by this review.

- **Ownership is clean; no file was touched by two agents.** `git diff --name-only 2be20e1`
  plus untracked files gives six paths. Agent B:
  `src/main/java/data_access/AlphaVantageMarketDataAccessObject.java` and
  `src/test/java/data_access/AlphaVantageMarketDataAccessObjectTest.java` — both inside
  `agents/data-access.md` § Owns, and neither is one of the two Member 4 files carved out as
  read-only. Agent A: `src/test/java/use_case/watchlist/WatchlistSnapshotFactoryTest.java` —
  inside `agents/use-case.md` § Owns. Orchestrator: `MarketDataHandoffTest.java`,
  `plan/handoffs/*`, `.gitignore`. **Zero overlap between A's and B's diffs.**
  `entity/WatchlistEntry.java` was not opened, and neither was any other `entity/`,
  `view/`, `interface_adapter/` or `app/` file. No teammate-owned file was touched at all
  this phase.

- **One sanctioned boundary crossing, no collision.** `MarketDataHandoffTest.java` sits in
  `src/test/java/use_case/watchlist/**`, which is Agent A's glob, and the orchestrator wrote
  it. `plan/phase-5.md` § Files to create assigns it there explicitly and Agent A never
  touched the file, so this is the D4-b situation rather than the critical the reviewer brief
  names ("a file touched by two agents in one phase"). Worth a line in `decisions.md` as a
  Phase 5 deviation so the pattern stays visible.

- **Agent B's extraction is behaviour-preserving — checked line by line.** Old:
  `key = getenv(...)`; `if (key == null || key.isBlank()) return empty`; `return
  Optional.of(key.strip())`. New: `apiKeyFromEnvironment()` is
  `return apiKeyFrom(System.getenv(API_KEY_ENV_VARIABLE))`, and `apiKeyFrom` applies the
  identical null/blank test and the identical `strip()`. Same predicate, same order, same
  return for every input including the `"  "`-is-blank and `"\tKEY\n"`-strips cases.
  `app/Main.java:67` calls `apiKeyFromEnvironment()` and is unaffected — its signature,
  visibility, `static`ness and return type are unchanged. The new method is package-private
  and its only production caller is the delegate, which is the intended shape of W2-7, not
  dead code.

- **W2-7 and W2-8 are genuinely closed, and closed the way the brief described.** W2-7 asked
  for "a pure package-private `apiKeyFrom(String rawValue)`" cutting the uncovered surface to
  one line — delivered exactly, and JaCoCo confirms the DAO went from 4 uncovered lines to 1.
  W2-8 asked for "a direct `WatchlistSnapshotFactory.build(...)` assertion" that distinguishes
  A-N3's new behaviour from the old double-`findBySymbol` — delivered, and it is a real
  discriminator: `aSelectedSymbolWithStoredPricesButNoWatchlistEntryYieldsNoPriceRows` first
  proves the prices *are* in the repository (`stored.get().getPriceCount() == 4`) and the
  ticker is *not* on the watchlist, so the empty result cannot be an artifact of a missing
  save, and `theSameSelectionYieldsPriceRowsOnceItsTickerIsOnTheWatchlist` is a genuine
  positive control against a factory that returned nothing unconditionally. Neither closure
  is a workaround.

- **No tautological tests, and none asserting a bug.** Every new test in the phase asserts a
  fixed expected value; there is no `if/else` on ambient state anywhere in the diff. The
  `apiKeyFrom` tests are the direct replacement for the D13 tautology and the DAO test file
  now carries a comment block explaining why `apiKeyFromEnvironment()` stays uncovered — that
  reasoning is correct and should not be "fixed" by a later coverage push.

- **`System.getenv` is still in `data_access`, not `app/Main`.** `agents/reviewer.md`'s
  Clean-Architecture line says it "appears in exactly one place: the composition root"; it
  actually appears exactly once, in
  `AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment():72`, which `Main` calls. The
  spirit of the rule (one read, in one place, invoked from the composition root) holds and
  this is unchanged from Phase 4. Recording it so nobody re-derives the discrepancy: either
  the checklist line or the sentence in `coverage.md`/`team-notes.md` describing it as the
  composition root's read should be made precise.

- **`walkthrough.md` is an undeclared deliverable.** `plan/phase-5.md` § Files to create
  lists three files; four were produced. The content is good and the cross-references check
  out (its "before" screenshot path, date and SHA `fc27b3c` match `screenshots.md` exactly,
  and `fc27b3c` is indeed the pre-Phase-4-wiring commit). Log it as a Phase 5 deviation
  rather than leaving it as an unexplained extra file.

- **The `.gitignore` change is correct and arrived just in time.** `watchlist.dat` was
  untracked-and-visible at the start of this phase and is now ignored; it was never
  committed, so no history rewrite is needed. `watchlist.dat.corrupted-*` matches the pattern
  `FileWatchlistDataAccessObject:92` actually builds. Both entries land in a clearly
  commented block; nothing pre-existing was reordered or removed.

- **Still owed at close-out, not a finding:** `plan/status.md` is `plan/phase-5.md`
  verification item 6 and still reads "Current phase: 5 … Agents A and B on call". Also owed
  are the re-logging of the sixteen knowingly-unclosed carried warnings, and the manual
  `walkthrough.md` script — steps 6 and 7 (restart round trip, resize, Tab order, mid-refresh
  freeze) and the "after" screenshot are still the only part of `vision.md` §8 that no
  machine has checked.
