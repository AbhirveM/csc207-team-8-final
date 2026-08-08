# Review — Phase 1 (Contract Freeze)

Reviewed at working-tree state on `feature/watchlist-use-cases`, base commit `038b77b`.
Build verified independently: `mvn -o clean test` — green.

Status: PASS WITH WARNINGS

Every hard gate in `agents/reviewer.md` passes: dependency rule clean, no credential leak,
no network in tests, contracts match `agents/orchestrator.md` §5 byte for byte, build
green. `FAIL` is not warranted — none of the reserved FAIL conditions (red build,
ownership violation, dependency-rule violation, leaked credential, network call in a test,
unclosed claimed defect) is present.

The two Criticals below are **process/hygiene blockers on spawning Agents A and B**, not
code defects. Phase 2 should not start until they are resolved.

---

## Critical (blocking — must fix before next phase)

- **Phase 1 was never committed; there is no SHA for Agents A and B to branch from.**
  `plan/status.md` still reads `Completed phases: none` / `Last commit: none`, and
  `git status` shows every Phase 1 file still uncommitted (`MarketDataGateway.java`
  modified, the other eight untracked).
  `plan/phase-1.md` step 6 and `agents/orchestrator.md` §4 both require the commit and the
  recorded SHA; §4 Phase 2 further requires *"Give each agent brief the Phase 1 commit
  SHA"* and a **worktree per agent** provisioned from it (hazard H4).
  → Commit Phase 1 on `feature/watchlist-use-cases`, record the SHA in `plan/status.md`,
  and only then provision the worktrees.

- **The working tree is contaminated with Phase-2-scope work, so the Phase 1 commit cannot
  be the isolated contract-freeze commit the plan describes.**
  `plan/phase-1.md` step 5 requires `git diff --stat` to show exactly nine files and
  *"nothing under `data_access/` …"*. It does not. Also uncommitted and un-reviewed:
  - `src/main/java/data_access/AlphaVantageMarketDataAccessObject.java`
  - `src/main/java/data_access/CachingMarketDataGateway.java`
  - `src/test/java/data_access/{AlphaVantageMarketDataAccessObjectTest,CachingMarketDataGatewayTest,JsonFixtures,StubHttpJsonClient}.java`
  - `src/test/resources/`
  - `src/test/java/use_case/watchlist/{RecordingSaveWatchlist,RecordingWatchlistPresenter}.java`
  - the four `Add/Remove/Refresh` interactor slices and `WatchlistSnapshotFactory.java`

  File mtimes place all of this at 13:53–14:02, before the Phase 1 edits at 14:35–14:36, so
  this is **pre-existing uncommitted work, not a Phase 1 ownership violation** — no agent
  overstepped. But it has two consequences that must be decided deliberately:
  1. Whatever is committed now becomes "the Phase 1 base", and Agents A and B will inherit
     files the phase brief assumes they are about to author. Their briefs must be updated
     to say *finish and test what exists*, not *create*.
  2. This code has never been through a review gate. Several Phase 2 findings below already
     come from it.

  → Either commit it as a separate, clearly-labelled pre-Phase-1 commit *before* the
  contract-freeze commit (preferred — keeps the freeze commit clean and reviewable), or
  accept the mixed commit and amend the Phase 2 briefs accordingly.

---

## Warnings (non-blocking but fix soon)

### Files the orchestrator owns

- **Import order is reversed against the canonical slice.**
  `src/main/java/use_case/watchlist/MarketDataGateway.java:3-6` puts `entity.DailyPrice`
  before the `java.*` block. `agents/orchestrator.md` §7 and
  `use_case/moving_average/ConfigureMovingAverageInteractor.java:3-5` both establish
  `java.*` first, blank line, then project packages.

- **Public members without javadoc**, contrary to §7 (*"Javadoc on every public type and
  member"*):
  - `WatchlistFailure.java:67` `getKind()`, `:71` `getSymbol()`
  - `WatchlistSnapshot.java:51` `getTickerRows()`, `:60` `getSelectedPriceRows()`
  - `AddTickerOutputData.java:30` `getAddedSymbol()`, `:48` `getPriceCount()`,
    `:52` `getSnapshot()`
  - `ShowWatchlistOutputData.java:21` `getTickerCount()`, `:25` `getSnapshot()`
  The neighbouring members in the same files *are* documented, so this reads as an
  oversight rather than a house style.

- **`WatchlistFailure.java:39-42` — the javadoc does not state the null-symbol behaviour.**
  The constructor silently normalizes a null `symbol` to `""`, but `@param symbol` only
  describes the raw-vs-normalized distinction. Every other class in the package uses a
  named `requireNonNull`, so a reader will assume this one does too. Same latent surprise
  at `ShowWatchlistInputData.java:14-16` (there it *is* documented — copy that wording).

- **`WatchlistSnapshot` has `equals`/`hashCode` but no `toString`.** Spec-compliant
  (`plan/phase-1.md` only asked for `toString` on `WatchlistFailure`), but a failing
  `assertEquals` on a snapshot in Agent C's presenter tests will print
  `WatchlistSnapshot@1a2b3c` and tell the reader nothing. Adding it now is cheaper than
  debugging without it in Phase 3.

- **`ShowWatchlistOutputData` carries two sources of truth for the same number.**
  `getTickerCount()` and `getSnapshot().getTickerRows().size()` can disagree. The shape is
  exactly as §5.2 specifies so this is not drift — but bind Agent A explicitly: the
  interactor must construct it as
  `new ShowWatchlistOutputData(snapshot.getTickerRows().size(), snapshot)`, and a test
  should pin the invariant.

### Files owned by Agents A and B (raise in their Phase 2 briefs)

- **Exception variables named `e`, not `exception`** (§7):
  `AddTickerInteractor.java:75`, `:94`; `RefreshTickerInteractor.java:66`;
  `AlphaVantageMarketDataAccessObject.java:108`, `:238`; `JdkHttpJsonClient.java:52`;
  `JsonFixtures.java:27`.

- **Lines over ~100 characters** (§7): `AddTickerInteractor.java:48,49,67`;
  `RefreshTickerInteractor.java:35,36`; `RemoveTickerInteractor.java:27`;
  `WatchlistSnapshotFactory.java:33,57,61`.

- **Swallowed diagnostic at `AddTickerInteractor.java:94-96`.** The
  `catch (MarketDataException e) { companyName = null; }` discards `e.getKind()` entirely.
  §7 is explicit: *"threading `exception.getKind()` into the `WatchlistFailure` or the
  status message"* is what "don't swallow the diagnostic" means here. A rate-limited
  company-name lookup and an unrecognised symbol are indistinguishable to the user. The
  add correctly still succeeds — only the diagnostic is lost. (`companyName` is also a
  non-final local, against §7's "locals `final` wherever possible".)

- **Tautological test — `AlphaVantageMarketDataAccessObjectTest.java:278-291.**
  `apiKeyFromEnvironmentIsEmptyWhenTheVariableIsUnset` is an `if/else` on
  `System.getenv(...)` that asserts whichever branch it lands in. This is verbatim the
  pattern `agents/reviewer.md` §Tests prohibits: it passes regardless of the code under
  test and buys only a JaCoCo line. Delete it, or refactor `apiKeyFromEnvironment()` to
  take a `Function<String, String>` lookup so both branches are deterministically testable.

- **Two test doubles with zero callers.** `RecordingWatchlistPresenter` and
  `RecordingSaveWatchlist` (`src/test/java/use_case/watchlist/`) are defined and never
  used — `grep` finds no reference outside their own files. Already the Phase 2 emphasis
  line in `agents/reviewer.md`; flagging so Agent A treats it as a requirement, not a
  nice-to-have. Note `RecordingWatchlistPresenter` implements only three of the four output
  boundaries — it will need `ShowWatchlistOutputBoundary` added in Phase 2.

- **The Phase 1 value semantics are entirely untested.** There is no
  `WatchlistFailureTest`, `WatchlistSnapshotTest`, `AddTickerOutputDataTest`, or
  `ShowWatchlistOutputDataTest`, and no interactor test at all —
  `src/test/java/use_case/watchlist/` contains only `TickerSymbolValidatorTest` plus the two
  unused doubles. The `equals`/`hashCode` contracts Phase 1 just added (reflexive,
  symmetric, null-safe, `hashCode` consistency, the `null → ""` normalization) are exactly
  the kind of hand-written code that is wrong silently. Assign these to Agent A; they also
  count toward the 90% interactor target.

---

## Notes

### Hard gates — all clean

| Check | Result |
|---|---|
| `mvn -o clean test` | green (re-run independently) |
| Swing/AWT in `use_case`, `interface_adapter`, `entity` | none |
| `^import data_access` in `use_case` | none |
| `^import use_case` in `view` | none |
| API key in any source/fixture/test/message | none — `grep -rniE "alphavantage.co/query\?.*apikey=[A-Z0-9]" src/` empty |
| `JdkHttpJsonClient\|HttpClient` in `src/test` | none |
| Wildcard imports in `use_case/watchlist` | none |
| Trailing newline on all nine Phase 1 files | present |

### Contract drift — none

Every signature in `agents/orchestrator.md` §5.2 matches the files exactly:
`ShowWatchlistInputBoundary.execute(ShowWatchlistInputData)`;
`ShowWatchlistInputData` final with `getSelectedSymbol()`;
`ShowWatchlistOutputBoundary.prepareSuccessView(ShowWatchlistOutputData)` +
`prepareFailView(WatchlistFailure)`;
`ShowWatchlistOutputData` final with `getTickerCount()` + `getSnapshot()`.
The §5.1 gateway contract is reproduced faithfully on all three methods, and the diff on
`MarketDataGateway.java` is javadoc-only — no signature moved. Hazard H2 is closed: Agent C
can compile against `ShowWatchlistOutputBoundary` in Phase 3 without waiting on Agent A.

### Phase 1 deliverables — all present

- D11: `WatchlistFailure` and `WatchlistSnapshot` are both `final` with `equals`/`hashCode`;
  `WatchlistFailure` also has `toString`. Both `from(...)` factories and both exhaustive
  `switch` expressions survived intact.
- `WatchlistSnapshot.java:44-45` — named `requireNonNull` now precedes each `List.copyOf`,
  as specified.
- D10: `AddTickerOutputData` carries exactly one company-name field;
  `isCompanyNameAvailable()` is `return !companyName.isEmpty();`. The nonsense state
  (`"Apple", false`) is now unrepresentable. The `AddTickerInteractor` edit is confined to
  the single construction site at line 103-107 — compile-preserving, as promised.
- D8: javadoc only, as `plan/phase-1.md` §2 scopes it.

### For Phase 2

- **D8 is not yet enforced anywhere.** None of the three implementations satisfies the
  contract the javadoc now declares: `AlphaVantageMarketDataAccessObject` has no
  `requireNonNull`/`isBlank` guard on `normalizedSymbol` (NPEs out of `URLEncoder`),
  `CachingMarketDataGateway` computes `key(normalizedSymbol)` before any validation (caches
  under `""`), and `InMemoryMarketDataGateway` does the same. This is by design — the
  javadoc is the Phase 1 deliverable and the enforcement is Agent B's — but it means
  `MarketDataGateway`'s javadoc currently over-promises against all three implementations.
  It should be the first thing Agent B closes, with a test per implementation for both the
  null and the blank case, and an assertion that a blank symbol left no cache entry.
- `System.getenv` appears exactly once in `src/main`, at
  `AlphaVantageMarketDataAccessObject.java:61` inside `apiKeyFromEnvironment()`. That is the
  form §6 sanctions, since `Main` calls it. Phase 4 must call it through that method and add
  no second reader.

### Out of our ownership — raise with the team, do not absorb

`view/ComparisonView.java:3` and `view/MainAppState.java:3` both
`import entity.BacktestResult`, i.e. an entity crossing into the view layer. Member 2/3's
files, so out of scope per `agents/orchestrator.md` §2, but a grader hunting Dependency Rule
violations will find these even though the watchlist vertical is clean. Worth a message to
the team now rather than during demo week — it goes in `plan/handoffs/` alongside the H8
~100-day ceiling note.

### Self-audit

No source file was modified by this review. Only `plan/review-phase-1.md` was written.
