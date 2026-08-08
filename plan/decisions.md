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

### D3-d — D-N1 accepted, and scoped to Show Watchlist only

Agent D filed the one cross-agent need of the phase. `WatchlistView` reads the ticker field
for Add, Remove and Refresh alike, so the demo path "click the AAPL row, press Refresh"
produced `Enter a ticker symbol before continuing.` with the row plainly selected — every
success clears `tickerFieldText`, including Show Watchlist's.

Agent D was right to file rather than absorb it: fixing it view-side needs a conditional on
whether the field is empty plus a fallback to the table selection, which is a data conditional
in the view (H6) and a second source of truth for "which symbol is the user talking about".

**Accepted.** `prepareSuccessView(ShowWatchlistOutputData)` now sets `tickerFieldText` to the
snapshot's selected symbol. That value is already `""` when nothing is selected, so the
empty-selection behaviour is untouched.

**Declined:** the optional half, extending the same to Add. Add, Remove and Refresh are text
*submissions* and clearing the field after them is a pinned contract; Show Watchlist is a
*selection*, so populating the field from it is coherent rather than contradictory. All
existing clearing tests stand unchanged.

Routed back to Agent C rather than patched by the orchestrator, so the change lands with its
pinning test in the file's owner's hands.

### D3-e — D-N2 accepted as a risk, not fixed

"Load prices" must stop spending the daily quota at the first rate-limit failure (H5). The only
signal reaching the view is the error prose, so `WatchlistView.RATE_LIMIT_PREFIX` matches the
opening sentence of the `RATE_LIMIT` row. This is the single place the view depends on the
*content* of a presenter string rather than merely displaying it: reword that sentence and the
constant must be reworded with it, or "Load prices" keeps hammering the API after the quota is
gone.

The clean fix is a structured `boolean isQuotaExhausted()` on `WatchlistState`. Not taken this
phase: `WatchlistState` is orchestrator-owned and its shape is frozen in `agents/orchestrator.md`
§5.3, and widening a frozen contract after both agents have built against it is exactly the
churn the freeze exists to prevent. `WatchlistPresenterTest` pins the sentence with
`assertEquals`, so a reword breaks a test rather than silently breaking the quota guard — which
is what makes the coupling survivable. Revisit in Phase 5 if the shape is being touched anyway.

### D3-f — both agent worktrees were provisioned from the wrong commit

Both C and D reported their worktree was created from `bff35db` (an old `feature/momentum-strategy`
merge) rather than the `394d3fb` their briefs named — no `agents/`, no `plan/`, no
`interface_adapter/watchlist/`. Both detected it independently, reset to `394d3fb` before
reading or writing anything, and reported it unprompted. Verified after the fact:
`git merge-base --is-ancestor 394d3fb <branch>` is true for both, and both merged with zero
overlapping files.

Recorded because the tooling will do it again. **Any future phase that fans out to worktrees
must have the orchestrator verify each worktree's base commit before the agents start**, rather
than relying on the agents to notice.

---

## Warnings carried forward from `plan/review-phase-3.md`

Status **PASS WITH WARNINGS** — **zero criticals**, eleven warnings. Five were fixed
immediately because they are orchestrator-owned contract drift, and Phase 4 and 5 agents read
those documents to derive their briefs — the same reasoning that fixed W2-10 in Phase 2. The
remaining six are logged here.

**Fixed during Phase 3 close-out:** W3-1 (`agents/orchestrator.md` §5.6 and
`agents/reviewer.md` still said "7 success messages"); W3-2 (three documents contradicted the
shipped presenter on the failure `statusMessage` rule and on Show Watchlist's ticker-field
behaviour — `agents/adapter.md`, `plan/phase-3.md` and orchestrator §5.3 all corrected);
W3-3 (`WatchlistState`'s javadoc claimed value equality exists so the view model can skip a
repaint, which is the exact opposite of what `setState` deliberately does); W3-4
(`WatchlistViewModel.state` is now `volatile`); W3-9 (the one 101-character line, and the
missing `hashCode` javadoc). W3-7's brief self-contradiction was also fixed — both agent
briefs listed `plan/**` under Never Touch while instructing the agent to write into
`plan/handoffs/`, so `plan/handoffs/<agent>-*.md` is now carved out explicitly in both.

| # | Warning | Owner | Due |
|---|---|---|---|
| W3-5 | `prepareFailView` is a non-atomic read-modify-write on the view model, and a concurrent writer exists: `runInBackground` disables the four buttons but **not** the ticker table, so clicking a different row while a refresh is in flight drives `showWatchlist → setState` on the EDT concurrently with the worker's `setState`. Worst case is a lost update and a stale render — no crash, because `WatchlistState` is immutable. The one genuine race in the vertical, and invisible to the test suite. Cheapest fix: have `setButtonsEnabled(false)` also disable `tickerTable`. | D | Phase 4 |
| W3-6 | The D-N2 tripwire fires but does not point anywhere. `WatchlistView.RATE_LIMIT_PREFIX` prefix-matches the presenter's `RATE_LIMIT` sentence, and D3-e argued the pinning `assertEquals` makes that safe. It does not: the test that breaks on a reword is `rateLimitFailureTellsTheUserToWaitAMinute`, whose message says nothing about the view constant, so the natural repair is to update the expected string and move on — leaving "Load prices" spending quota after it is exhausted. Fix: name `WatchlistView.RATE_LIMIT_PREFIX` in the presenter's `RATE_LIMIT` arm, or take the structured `isQuotaExhausted()` in Phase 5. | C, D | Phase 4 or 5 |
| W3-8 | `WatchlistView.render` calls `tickerField.setText(...)` unconditionally on every state event, so a user typing the next symbol while a refresh is in flight loses it and the caret jumps. A consequence of D3-d making the field view-model-owned, not a mistake. Fix alongside W3-5, or guard the `setText` on the value actually differing. | D | Phase 4 |
| W3-10 | `OrderedFocusTraversalPolicy` silently wraps on an unknown component: `order.indexOf(component)` returns `-1` and `Math.floorMod` turns that into a plausible index rather than a signal. Cannot throw, so a robustness nit — but a control added later and forgotten in `installFocusOrder` gives a focus order nobody can debug. | D | Phase 5 |
| W3-11 | `WatchlistView` marks parameters `final` throughout; nothing else in the repo does. Defensible under §7 ("`final` locals wherever possible") but visibly inconsistent with `ComparisonView` and `WatchlistPresenter` next to it. Lowest priority. | D | Phase 5 |
| W3-12 | Not every interactor is at 100% *method* coverage despite 100% lines — `RefreshTickerInteractor` shows one method and three instructions missed, `TickerSymbolValidator` one line. Phase 2 territory. Recorded so "all four interactors 100%" is not carried forward unqualified: it is true of lines, not of every JaCoCo axis. | A | Phase 5 |

### D3-g — the coverage margin is thinner than the headline

73.3% against a 70% target, with `WatchlistView`'s 171 uncovered lines already counted. Phase 4
adds `Main`'s wiring block, also uncovered. The real margin is roughly **45 lines**. If Phase 5
measures short the levers are already on the books and are not Swing tests (H6): W2-7's
`apiKeyFrom(String)` extraction and W2-8's factory assertion.

### D3-h — `plan/handoffs/` was cleared selectively, not emptied

`/execute` step 5 says to delete the contents of `plan/handoffs/`. The four agent files
(`adapter-done.md`, `view-done.md`, `view-needs.md`, and the needs file C never had to create)
were removed — their content is now recorded in `plan/review-phase-3.md` and in D3-d/D3-e above.
**`screenshots.md` was deliberately kept:** it carries an unfinished action item with an
irreversible deadline, and Phase 5 populates this directory with the team hand-off notes anyway.
Deleting a pending obligation to satisfy a cleanup step would be the wrong trade.

---

## Phase 4 — Composition Root

Review: `plan/review-phase-4.md` — **PASS WITH WARNINGS**, **zero criticals**, nine warnings.
Build green: `mvn -o clean verify`, 403 tests, 0 failures. Four warnings were fixed at
close-out (W4-2, W4-6, W4-7's over-length line, W4-8); the rest carry to Phase 5 below.

### D4-a — a third file was touched, and it is orchestrator-owned

`plan/phase-4.md` said "Shared interfaces to write first: none". One was needed after all:
`WatchlistViewModel.SAMPLE_DATA_STATUS`, the offline notice. §6 requires "a visible status line
stating that sample data is in use", but every existing status string is written by
`WatchlistPresenter` from a use-case result, and *which gateway got wired* is not in any result
— it is knowledge only the composition root has. The three candidates were a presenter method
(cleanest by §7, but `WatchlistPresenter.java` is Agent C's and Phase 4 spawns no agents), a
literal in `app/Main.java` (puts user-facing prose in `app/`, the one thing §7 forbids), and a
constant on the orchestrator-owned view model, which already holds user-facing column headers.
The third was taken. §7 now records it as the single documented exception, so a future reader
does not read it as the rule eroding.

### D4-b — `WatchlistView.java` was edited with no agents active

W3-5 was booked to Agent D and due in Phase 4 — a phase that spawns no agents, so nobody owned
the fix. The orchestrator made the one-line change (`setButtonsEnabled` also disables
`tickerTable`) rather than carry the vertical's only genuine race for another phase. The
reviewer confirmed the crossing is a warning and not a FAIL: the reviewer brief's critical is
"two agents touching one file in one phase", and Phase 4 has none.

The reviewer also verified the fix empirically against a realized `JFrame` rather than by
reading: on a disabled `JTable` a dispatched left-click and a `VK_DOWN` both leave
`selectedRow` unchanged with zero `ListSelectionListener` fires, while programmatic
`setRowSelectionInterval` still works, so `restoreSelection` is unaffected. **W3-5 is closed.
W3-8 is not — see W4-1.**

### D4-c — the documented launch recipe did not work offline

`plan/phase-4.md` verification step 4 said `mvn exec:java`; `plan/handoffs/screenshots.md` said
`mvn -o dependency:build-classpath`. Neither plugin is in the local repository and `-o` forbids
fetching one, so both fail. Adding `exec-maven-plugin` would be a shared `pom.xml` edit (§2:
one isolated edit, announced first). Both documents now name the single runtime dependency
directly instead: `java -cp "target/classes;<org.json jar>" app.Main`.

### D4-d — D3-c resolved: newest-first

`plan/phases.md` Phase 4 "Done when" said the price table fills oldest-to-newest; the frozen,
tested `WatchlistSnapshotFactory` contract emits newest-first. The prose was corrected, not the
contract — the reverse would also have falsified two `WatchlistPresenter` javadocs and
`WatchlistSnapshot`'s. Confirmed empirically at this gate: a harness replaying the §8 script
through the exact graph `Main` builds returned 2026-08-05 first and 2026-02-19 last.

### D4-e — the watchlist is now the launch card

Nothing calls `ViewManagerModel.setActiveView` at startup, so `CardLayout` shows whichever card
was added first. The wiring block sits between Persistence and Comparison per §6, so
`WatchlistView` is added before `ComparisonView` and is what the user sees on launch — where
Member 4's Comparison view used to be. Accepted deliberately: the §8 walkthrough and the "after"
screenshot both want the vertical visible immediately. It is a behaviour change to a teammate's
screen and belongs in the ping `vision.md` §9 asks for.

### D4-f — the manual §8 walkthrough is verified by harness, not yet by hand

`plan/phase-4.md` verification items 4–7 are a human script. What has been machine-verified: the
app launches offline with `ALPHA_VANTAGE_API_KEY` unset with no exception, and a throwaway
harness replayed the whole §8 sequence through the real object graph — `aapl` → `AAPL`, company
name resolved, 120 rows newest-first, Refresh updated count and latest date, a row selection
repopulated the price table, Remove dropped the row, `!!junk!!` produced the specific worded
error with the surviving row untouched.

**Still owed by hand, and this is the honest gap in the Phase 4 gate:** the visual items — resize
behaviour, Tab order against visual order, the restart-persistence round trip through a real
`watchlist.dat`, and the **"after" screenshot** for the individual presentation. `screenshots.md`
carries the launch recipe and the "before" path.

### Carried warnings — Phase 4

| # | Warning | Owner | Due |
|---|---|---|---|
| W4-1 | **W3-8 is not closed, contrary to `plan/status.md`'s Phase 3 prediction that one edit would fix it alongside W3-5.** Disabling `tickerTable` stops a *selection* from overwriting the ticker field, but `tickerField` itself stays enabled and `render` still calls `setText` unconditionally, so text typed while a worker is in flight is still lost when the success state arrives wanting `""`. The guard suggested in W3-8 (skip `setText` when the value matches) fixes only the caret jump, not the loss; a real fix needs the field to track whether it is user-dirty, which is more than a close-out edit. Left open deliberately rather than claimed closed. | D | Phase 5 |
| W4-3 | `Main` hand-assembles a `WatchlistState` from six positional arguments, four of them interchangeable `String`s, with no test covering it. A `WatchlistState.withStatusMessage(...)` copy method would remove the hazard. Deferred rather than taken at close-out: `WatchlistState.java` is orchestrator-owned but its test is Agent C's, and adding an untested method would thin the coverage margin W4-10 already flags. | orchestrator, C | Phase 5 |
| W4-4 | Tab dies on the disabled `tickerTable` while a worker is in flight — `OrderedFocusTraversalPolicy` ignores `accept()`. Transient, but pairs with the still-open W3-10 and should be fixed in the same edit. | D | Phase 5 |
| W4-5 | A disabled `JTable` gives no visual cue that it is frozen. The freeze is correct (W3-5) but silent, so a user mid-refresh reads an unresponsive table as a bug. | D | Phase 5 |
| W4-9 | `PersistenceViewModel` is bound to no view, so a save or load failure is invisible: the presenter says "Added AAPL…" while the write to `watchlist.dat` failed silently. Inherited from Member 4's `void` persistence boundary, not introduced here. Belongs in the Phase 5 hand-off notes rather than being absorbed (§9). | Member 4 / hand-off | Phase 5 |
| W4-10 | **The coverage margin is now ~22 lines, not D3-g's ~45.** Project line coverage fell 73.3% → **71.6%** (1004/1403), because `Main`'s wiring block is 47 uncovered lines rather than the ~35 estimated. Still above the 70% target, but Phase 5 cannot add uncovered production code without tests landing beside it. The levers remain non-Swing (H6): W2-7's `apiKeyFrom(String)` extraction and W2-8's factory assertion. | orchestrator, A, B | Phase 5 |

---

## Phase 5 — Hand-off Proof and Close-out

Review: `plan/review-phase-5.md` — **PASS WITH WARNINGS**, **zero criticals**, seven warnings.
Build green: `mvn -o clean verify`, **415 tests**, 0 failures. Project line coverage
**71.69%** (1008/1406). All four Phase 5 "Done when" criteria met, verified item by item by
the reviewer rather than asserted.

### D5-a — scope was deliberately narrowed, and sixteen warnings are knowingly unclosed

Sixteen warnings from Phases 2–4 were booked "due Phase 5". The owner scoped this phase to the
Phase 5 exit criteria plus the two named **coverage levers** — W2-7 and W2-8 — and nothing else.

The reasoning: most of the rest belong to Agents C and D, who are not active in Phase 5 (fixing
them would mean crossing ownership with no agent to route through, the D4-b situation repeated
fourteen times); several are real design work on frozen contracts; and the coverage margin was
too thin to absorb untested production changes. A wider diff would also have made this phase's
review — the last one this plan gets — materially harder.

**The register below is the point of this entry.** These are not fixed, and this plan is
ending, so nothing will come along later to close them. They are recorded here as the standing
list for whoever picks the code up.

| # | Warning | Owner |
|---|---|---|
| W2-1 | `concurrentReadersDoNotCorruptTheCache` drives a fake whose call counters are plain `HashMap`s — the one test proving thread safety races on unsynchronised maps. Latent flake. | B |
| W2-2 | `CachingMarketDataGateway.getCachedNameCount` javadoc promises a bound the non-atomic size-check → clear → put sequence does not hold under concurrency. | B |
| W2-3 | Exception variables still named `e`: `RefreshTickerInteractor:64`, `AlphaVantageMarketDataAccessObject:151,202,300`, `JsonFixtures:27`. | A, B |
| W2-4 | All four `data_access` files order imports project-first, inverting §7, while `use_case` next door was reordered *into* the correct form. Explicitly left alone in Phase 5 to keep W2-7's diff narrow. | B |
| W2-5 | 26 lines over the ~100-char guidance. Two survive in `AlphaVantageMarketDataAccessObject` at 206 and 266. | A, B |
| W2-6 | `InMemoryStockRepository:27-29` hand-rolls `new NullPointerException(...)` instead of `Objects.requireNonNull(x, "...")`. | B |
| W2-9 | The four interactor tests import `data_access` implementations. Note the nuance Phase 5 adds: `MarketDataHandoffTest` does this **deliberately and correctly** — the offline fake is what the app runs on, so the import is the point. W2-9 is about the *other* four. | A |
| W2-11 | D4's `catch (IllegalArgumentException exception)` in Add and Refresh binds and never reads it, discarding which invariant broke. Needs a decision (`WatchlistFailure` has no field for it), not a one-liner. | A |
| W2-12 | The four `data_access` classes are not `final` while every `use_case` counterpart is. | B |
| W3-6 | `WatchlistView.RATE_LIMIT_PREFIX` prefix-matches presenter prose, and the test that breaks on a reword says nothing about the view constant — so the natural repair leaves "Load prices" spending quota after it is exhausted. | C, D |
| W3-10 | `OrderedFocusTraversalPolicy` silently wraps on an unknown component (`indexOf` → −1 → `floorMod`). | D |
| W3-11 | `WatchlistView` marks parameters `final` throughout; nothing else in the repo does. | D |
| W4-1 | **W3-8 is still open.** Text typed into `tickerField` while a worker is in flight is lost when the success state lands. Needs the field to track user-dirty state. Called out in `plan/handoffs/walkthrough.md` step 7 so it is not discovered live. | D |
| W4-3 | `Main` hand-assembles a `WatchlistState` from six positional arguments, four interchangeable `String`s, untested. A `withStatusMessage(...)` copy method would remove the hazard. | orchestrator, C |
| W4-4 | Tab dies on the disabled `tickerTable` while a worker is in flight. Pairs with W3-10. | D |
| W4-5 | A disabled `JTable` gives no visual cue it is frozen; a user mid-refresh reads it as a hang. | D |

### D5-b — `MarketDataHandoffTest` is a second sanctioned ownership crossing

`src/test/java/use_case/watchlist/MarketDataHandoffTest.java` sits inside Agent A's glob and
was written by the orchestrator. This is sanctioned twice over — `agents/orchestrator.md` §4
assigns the hand-off test to the orchestrator in Phase 5, and `plan/phase-5.md` § Files to
create names the exact path — but it is the same shape as D4-b and is recorded so the pattern
stays visible. Agent A was told explicitly not to create the file and did not; the reviewer
confirmed zero overlap. Not the reviewer brief's critical, which is *two agents* touching one
file in one phase.

### D5-c — a fourth deliverable, `plan/handoffs/walkthrough.md`

`plan/phase-5.md` § Files to create lists three files; four were produced. The extra is the
owner's manual checklist for the `vision.md` §8 script that D4-f left owed by hand — the
restart round trip, resize, Tab order, the W3-5 freeze check, and the "after" screenshot. It
was split out of `team-notes.md` because the audience is different: team notes go to Members
2, 3 and 4; the walkthrough is a task list for the owner alone. Its cross-references to
`screenshots.md` were verified by the reviewer.

### D5-d — `plan/handoffs/` was again cleared selectively (as in D3-h)

`/execute` step 5 says to delete the contents of `plan/handoffs/`. The two agent files
(`use-case-done.md`, `data-access-done.md`) were removed — their content is recorded in
`plan/review-phase-5.md` and above. **The four deliverables were kept:** `team-notes.md`,
`coverage.md`, `walkthrough.md` and `screenshots.md`. They are what this phase exists to
produce, and `walkthrough.md` and `screenshots.md` both carry unfinished obligations. Deleting
this phase's own output to satisfy a cleanup step would invert the purpose of the step.

### Warnings from `plan/review-phase-5.md`

**Fixed at close-out** — all four documentation-truth findings, because these documents go to
teammates and to a grader and a confidently-worded wrong claim is worse than an omission (the
same reasoning that fixed W2-10 and the five Phase 3 doc warnings):

- **W5-1** — `team-notes.md` claimed the ceiling test would break the build if someone raised a
  strategy window past ~90. It would not: the test hard-codes 100/99 against its own series and
  no production code configures a window. Reworded to say what the test genuinely pins (the
  arithmetic of the cliff) and to say plainly that it is not a guard on anyone's configuration.
- **W5-2** — `coverage.md` gave a ~24-line margin and then drew a conclusion about *adding*
  uncovered code, where the correct figure is 34 (new code grows the denominator too). Both
  numbers are now stated, each against the case it applies to.
- **W5-4** — off-by-one in the window table: a 50-day window against 100 records leaves 50
  signal days, not 49. Inherited verbatim from `plan/phase-5.md:73`.
- **W5-5** — "the seven `Serializable` entities" miscounted the types. There are eight; seven
  lack an explicit UID because `MovingAverageConfiguration` already declares one. The advice was
  sound but the miscount sat in the one paragraph whose whole purpose is to stop carelessness.

Also fixed, both in the orchestrator's own new file: **W5-3** (the ceiling test's `assertThrows`
lambda had two throw sites — the configuration and strategy are now hoisted above it, so the
intent is structural rather than dependent on the message assertion) and **W5-6** (`final` on
the two loop variables, matching Agent B's new test in the same phase).

**Left open:**

| # | Warning | Owner |
|---|---|---|
| W5-7 | `WatchlistSnapshotFactoryTest.buildRejectsNullCollaborators` pins null `watchlist` and null `stocks` but not null `selectedSymbol`, leaving the factory's behaviour on a null selection unspecified by any test. Three lines. Left because the file is Agent A's and no agent is active — the same reason sixteen other warnings are on the register above. | A |
| W5-8 | `agents/reviewer.md`'s Clean-Architecture checklist says `System.getenv` "appears in exactly one place: the composition root". It appears exactly once, in `AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()`, which `Main` calls. The spirit holds and this is unchanged since Phase 4, but the sentence is imprecise. | orchestrator |
