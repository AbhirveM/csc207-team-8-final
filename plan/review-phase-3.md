# Review — Phase 3 (Adapter and View)

**Range reviewed:** `c35dd52..HEAD` on `feature/watchlist-use-cases` (phase base `394d3fb`).
**Reviewer changed no application code.** Nothing was fixed; everything below is a Warning
or a Note for its owning agent.

```
Status: PASS WITH WARNINGS
Criticals: 0
Warnings: 11
```

---

## Critical (blocking — must fix before next phase)

None.

---

## What was verified, not trusted

| Claim | Result |
|---|---|
| `mvn -o clean verify` green, 403 tests, 0 failures | **Confirmed.** `BUILD SUCCESS`, `Tests run: 403, Failures: 0, Errors: 0, Skipped: 0` |
| Project line coverage 73.3% | **Confirmed** from `target/site/jacoco/jacoco.csv`: 1004 covered / 365 missed = 73.3% line, 82.5% branch |
| `interface_adapter.watchlist` 100% line and branch | **Confirmed.** Presenter 103/103 line, 37/37 branch; State 35/35, 20/20; Controller 14/14; ViewModel 12/12; both records 1/1 |
| `WatchlistView` 0% (hazard H6) | **Confirmed.** 147 lines + 24 in the three nested classes, all uncovered. Expected and correct — Swing tests are explicitly not the lever (H6) |
| `grep -rn "javax.swing\|java.awt" src/main/java/{use_case,interface_adapter,entity}` | empty |
| `grep -rn "^import data_access" src/main/java/use_case` | empty |
| `grep -rn "^import use_case" src/main/java/view` | empty |
| `grep -n "use_case\|entity\|String.format\|NumberFormat\|DateTimeFormatter" WatchlistView.java` | empty |
| `grep -rn "JdkHttpJsonClient\|HttpClient" src/test` | empty |
| `grep -rniE "alphavantage.co/query\?.*apikey=[A-Z0-9]" src/` | empty |
| `System.getenv` count | exactly 1, `AlphaVantageMarketDataAccessObject:61` (the composition-root helper). Not yet called from `Main` — that is Phase 4 |
| Lines >100 chars in the eight phase files | 1 (see W3-9) |
| Trailing newline on all eight files | all present |
| `catch (X e)` in new code | none |

Pre-existing `view → entity` imports at `ComparisonView.java:3` and `MainAppState.java:3`
remain. Teammates' files, already logged in `plan/review-phase-2.md`. Not this phase's.

---

## 1. The prose table — the phase's primary deliverable

**All 11 failure strings and all 8 success strings match `agents/adapter.md`
§ Interface Contract byte for byte,** and every one is pinned with `assertEquals` on the
whole string. No `contains` anywhere in `WatchlistPresenterTest`. I compared each row
character by character, including the two-part concatenations
(`BAD_FORMAT`, `NETWORK`, `RATE_LIMIT`, `MISSING_API_KEY`, the add caveat).

- `WatchlistFailure.Kind` has exactly 11 constants; `messageFor` is a `switch` expression
  over all 11 with **no `default`**, as specified. A twelfth kind is a compile error.
- The eight-row success table is right, and D3-a's third add outcome
  (`getCompanyNameFailureKind() != null`) is a *success* — `aFailedCompanyNameLookupIsStillASuccessAndNotAnError`
  asserts `isErrorPresent()` is false, which is the assertion that actually matters.
- `quoted(...)` is pinned in both directions:
  `aFailureCarryingABlankSymbolStillReadsAsASentence` covers the
  `the symbol you typed` branch.
- The three behaviours `plan/phase-3.md` calls out as "worse than the underlying error"
  are each pinned: rows preserved on failure, `tickerFieldText` preserved on failure,
  `"Not loaded"` / `"—"` / symbol-for-blank-name substitution.
- `everyFailureKindProducesANonEmptyErrorThatTheViewWillShow` loops over
  `Kind.values()` — a real invariant, not a tautology. No test in this phase asserts
  whichever branch it lands in, and none asserts a bug as intended behaviour.

This deliverable is clean.

## 2. `prepareFailView` carrying the previous `statusMessage` forward

**Agent C's call is right, and the adapter brief — not `WatchlistState` — is the thing
that should have given way.** Reasoning:

- The brief's literal instruction (`statusMessage = ""`) is not merely suboptimal, it is
  *unreachable*: `WatchlistState`'s constructor throws `IllegalArgumentException` on a
  blank status, so a compliant `prepareFailView` would have thrown on **every failure**.
  One of the two documents had to be wrong, and the one that would have produced a
  crashing app is the one that was wrong.
- Relaxing `WatchlistState` instead would have pushed a blank-check branch into
  `WatchlistView` — a conditional about data in the view, which is exactly what H6 and
  `agents/view.md` forbid. The constraint is load-bearing where it is.
- Carrying the previous status forward is also the better product behaviour: the footer
  reads "what last succeeded" above "what just failed", which is strictly more
  information than "Ready." or an invented sentence.
- `WatchlistState.initial()` guarantees the carried value is never blank, so the path can
  never throw. `prepareFailViewKeepsThePreviousStatusMessageBecauseABlankStatusIsRejected`
  pins it, and the javadoc on `prepareFailView` explains why in the code rather than only
  in a handoff.

The unresolved half is documentary: `agents/adapter.md` still asserts the false rule. See
W3-2.

Agent C also preserves `selectedSymbol` on failure, which the brief did not ask for. That
is correct — blanking it would empty the price table by a side door, the same class of bug
the row-preservation rule exists to prevent — and it is pinned by
`prepareFailViewKeepsTheSelectedSymbol`.

## 3. Hazard H1

**Both halves are present and correct.**

- `WatchlistView.onViewModelChanged` (line 326) opens with exactly the specified guard:
  `if (!SwingUtilities.isEventDispatchThread()) { SwingUtilities.invokeLater(...); return; }`.
  It then ignores the event payload and re-reads the whole state from the view model,
  matching `ComparisonView`.
- Every button handler routes through `runInBackground`, which is the *only* method in the
  class that constructs a `SwingWorker` and the only path to `addTicker`, `removeTicker`
  and `refreshTicker`. Buttons are disabled before `execute()` and re-enabled in `done()`,
  which Swing runs on the EDT whether `doInBackground` returned or threw — so a network
  failure cannot lock the UI permanently.

**`onTickerSelected` calling `controller.showWatchlist(...)` directly on the EDT is
sanctioned, and the claim still holds.** I read `ShowWatchlistInteractor`: its constructor
takes only `Watchlist`, `StockRepository` and the presenter — it has no
`MarketDataGateway` and no `SaveWatchlist` field, so the *arity itself* enforces "no I/O",
not just a comment. `execute` calls `WatchlistSnapshotFactory.build(...)` over in-memory
structures and nothing else. Safe on the EDT.

Re-entrancy is handled: `render` sets `repopulating` in a `try`/`finally` around the ticker
model rebuild, and `onTickerSelected` returns early on it, so the synchronous
`showWatchlist → setState → render` cycle inside a `ListSelectionListener` cannot recurse.
`restoreSelection` re-applies `state.getSelectedSymbol()` afterwards.

H5 is also satisfied: nothing hydrates at construction (`render(viewModel.getState())`
only), and `onLoadPrices` drives **one** worker refreshing sequentially with an early
return.

## 4. Thread-safety of the seam — the plain assessment

**It is sound today, but it is sound by accident of two properties rather than by design.**
Stated plainly:

- `WatchlistViewModel.state` is a non-volatile field written from the worker thread and
  read from the EDT. The happens-before edge that saves it is `SwingUtilities.invokeLater`:
  posting to the `EventQueue` and dequeuing on the EDT synchronize on the same lock, so
  the worker's write to `state` is visible to the EDT read inside `render`.
- Independently, `WatchlistState` is deeply immutable — every field `final`, both nested
  types are records, both lists are `List.copyOf`. JMM final-field semantics therefore
  guarantee the EDT can never observe a partially-constructed state even without that
  edge. A *stale* read would still be possible in principle; the `invokeLater` edge is
  what rules it out.
- `WatchlistView.onLoadPrices` reading `getErrorMessage()` between iterations is **not** a
  cross-thread read at all. `controller.refreshTicker` is synchronous and the presenter's
  `setState` runs on that same worker thread, so the read is ordered by program order.
  This one is trivially safe.
- The constructor's `render(viewModel.getState())` runs on the constructing thread, which
  Phase 4 will make the EDT.

So: no bug today. Two residual risks are recorded as W3-4 and W3-5 — the visibility
guarantee is undocumented and one `volatile` keyword away from being explicit, and
`prepareFailView`'s `getState()`-then-`setState()` is a non-atomic read-modify-write that
a concurrent EDT `showWatchlist` can interleave with.

## 5. D-N2 — the rate-limit prefix match

**"Accepted risk with an `assertEquals` pinning the sentence" is the right call for
Phase 3, but the pin is weaker than the decision record claims, so it is logged as W3-6
for Phase 4/5.**

The reasoning for not fixing it now is sound: `WatchlistState` is orchestrator-owned and
frozen, both agents had already built against it, and widening a frozen contract
mid-phase is the exact churn the freeze exists to prevent. Agreed.

The gap is in the *safety argument*, not the decision. `plan/decisions.md` D3-e and
`plan/handoffs/view-needs.md` both say the `assertEquals` means "a reword breaks a test
rather than silently breaking the quota guard". That is true but not directional: the
failing test is `rateLimitFailureTellsTheUserToWaitAMinute` in
`WatchlistPresenterTest`, whose message says nothing about
`WatchlistView.RATE_LIMIT_PREFIX`. The natural repair is to update the expected string and
move on — at which point `RATE_LIMIT_PREFIX` no longer matches and "Load prices" keeps
spending quota after it is exhausted, silently, exactly as feared. The tripwire fires; it
just does not point anywhere.

Cheapest fix short of the structured signal: one sentence in `WatchlistPresenter`'s
`RATE_LIMIT` arm (or in the pinning test's failure message) naming
`WatchlistView.RATE_LIMIT_PREFIX` as a co-dependent. That is inside Agent C's file and
costs nothing. The structured `isQuotaExhausted()` remains the right Phase 5 answer if
`WatchlistState` is being touched anyway.

## 6. Ownership

`git diff --name-only 394d3fb HEAD` yields nine files. Checked one by one against the
`Owns` globs, and per-commit to confirm no file was touched by two agents:

| File | Commit(s) | Owner glob | Verdict |
|---|---|---|---|
| `interface_adapter/watchlist/WatchlistController.java` | `19d15cc` (C) | `agents/adapter.md` Owns | in bounds |
| `interface_adapter/watchlist/WatchlistPresenter.java` | `19d15cc`, `721204b` (C) | same | in bounds |
| `src/test/java/interface_adapter/watchlist/WatchlistControllerTest.java` | `19d15cc` (C) | `src/test/java/interface_adapter/watchlist/**` | in bounds |
| `src/test/java/interface_adapter/watchlist/WatchlistPresenterTest.java` | `19d15cc`, `721204b`, `10d7749` (C) | same | in bounds |
| `src/test/java/interface_adapter/watchlist/WatchlistStateTest.java` | `10d7749` (C) | same | in bounds — see below |
| `view/WatchlistView.java` | `654abd7` (D) | `agents/view.md` Owns | in bounds |
| `plan/handoffs/adapter-done.md` | `19d15cc`, `721204b`, `10d7749` (C) | — | W3-7 |
| `plan/handoffs/view-done.md`, `view-needs.md` | `654abd7` (D) | — | W3-7 |

**No file was touched by two agents.** The layer split held again, as in Phase 2.

**`WatchlistStateTest` was the right resolution.** The file sits squarely inside Agent C's
`src/test/java/interface_adapter/watchlist/**` glob — C did not step outside its
ownership by one character. The relevant question is whether writing a *test for* an
orchestrator-owned class creates the coupling that ownership exists to prevent, and it
does not: `git diff-tree` confirms `WatchlistState.java` appears in **no** Agent C commit,
so the class under test was never modified. A test is a read-only consumer of a contract,
and the contract's whole purpose is to be consumed. The alternative — the orchestrator
writing a test into a directory it does not own, in a file the owning agent would then
have to work around — is strictly worse. The 57.1% → 100% result is the evidence that
leaving it unowned was not an option either.

Two secondary observations on that commit: `10d7749` also touched
`WatchlistPresenterTest`, which is C's, so no boundary was crossed there; and the trap
recorded in `adapter-done.md` about `assertNotEquals` argument order is real and correctly
handled (comment at `WatchlistStateTest:115-117`).

## 7. Contract drift

Every signature in `agents/orchestrator.md` §5.3, §5.4 and §5.5 was checked against the
code:

- **§5.3 `WatchlistState`** — both records match component-for-component; all seven
  accessors present with the specified return types; `initial()` present; `final`,
  immutable, `equals`/`hashCode`/`toString` present. `getStatusMessage()`'s "never `""` —
  `Ready.` at minimum" is enforced by the constructor, not merely documented. **No
  signature drift.** One *semantic* drift, see W3-2.
- **§5.4 `WatchlistViewModel`** — `VIEW_NAME` = `"watchlist"`, `STATE_PROPERTY` =
  `"state"`, both column arrays match verbatim, three methods match. `TICKER_COLUMNS.length`
  == 5 == `TickerRow`'s component count; `PRICE_COLUMNS.length` == 6 == `PriceRow`'s.
  Shape matches `ComparisonViewModel` (plain class, private `PropertyChangeSupport`, no
  Swing, no generic base). **No drift.**
- **§5.5 `WatchlistController`** — four `void` methods, constructor argument order
  matches, no trimming or upper-casing (pinned by `WatchlistControllerTest`'s `"  aapl  "`
  fixture). **No drift.**
- **§5.6** — **drift.** Still says "7 success messages". See W3-1.
- `agents/adapter.md` § Outputs — the `WatchlistPresenter` and `WatchlistController`
  declarations quoted there match the real files exactly.

## 8. The deferred item (D3-c, price-row ordering)

**Deferring was right, and nothing in Phase 3 depends on the answer.** Verified rather
than assumed:

- `WatchlistPresenter.toPriceRows` is a straight field-for-field copy in iteration order —
  no sort, no reverse, no comparator.
- `WatchlistView.render` iterates `state.getPriceRows()` in order into the table model —
  also no sort.

So whichever way the Phase 4 gate decides, no Phase 3 code changes; only
`WatchlistSnapshotFactory` (Agent A's, frozen) or the sentence in `plan/phases.md` moves.
That is the definition of a well-deferred question.

One thing for the Phase 4 gate to notice: `WatchlistPresenter`'s `toTickerRows` and
`toPriceRows` javadoc both *assert* "already newest-first". If Phase 4 resolves D3-c in
favour of the `plan/phases.md` wording, those two comments become false as a side effect.
Resolving it in favour of the tested contract — i.e. correcting `plan/phases.md` — costs
one line of prose and breaks nothing, which is the outcome I would expect and the one I
would recommend.

---

## Warnings (non-blocking but fix soon)

**W3-1 — `agents/orchestrator.md` §5.6 still says "7 success messages".**
`agents/orchestrator.md:300`. D3-a corrected `plan/phases.md`, `plan/phase-3.md` and
`agents/adapter.md` but missed the orchestrator's own §5. The reviewer's own per-phase
emphasis table (`agents/reviewer.md:131`) repeats the stale "7" as well. Owner:
orchestrator. Cosmetic now, misleading in Phase 5 when someone re-derives the gate.

**W3-2 — three frozen documents now contradict the shipped presenter.** All
orchestrator-owned; all read by Phase 4/5:
- `agents/adapter.md` § Phase Tasks: "Every failure path sets `statusMessage` to `""`" —
  false, and impossible (see §2 above).
- `agents/adapter.md` § Phase Tasks and `plan/phase-3.md:68`: "success clears
  [`tickerFieldText`]" — false for Show Watchlist since D3-d.
- `agents/orchestrator.md` §5.3: `getTickerFieldText() // "" after success; preserved
  after failure` — same staleness.
`plan/decisions.md` D3-d records the change correctly, but the contract documents an
agent would actually read were not updated. This is precisely the "quoted contracts going
stale is how parallel agents silently diverge" failure mode in `agents/reviewer.md:34-36`.

**W3-3 — a comment contradicts its code, in the seam file.**
`WatchlistState.java:20-21` ("so the view model can compare an incoming state with the
outgoing one") and `WatchlistState.java:128-129` ("so the view model can skip a repaint
when the presenter re-emits an identical state"). The view model does the exact opposite
on purpose: `WatchlistViewModel.setState` passes `null` as the old value specifically so
`firePropertyChange` cannot suppress the event, its javadoc says so, and
`anIdenticalRepeatedResultStillFiresTheEvent` pins it. `WatchlistStateTest:19-20` repeats
the same false claim. This repo has shipped two comment/code contradictions before; this
is a third. Owner: orchestrator (`WatchlistState`), Agent C (the test javadoc).

**W3-4 — `WatchlistViewModel.state` is non-volatile.**
`WatchlistViewModel.java:45`. Correct today only because `SwingUtilities.invokeLater`
supplies the happens-before edge and `WatchlistState` is deeply final (§4 above). Neither
fact is written down at the field. One `volatile` keyword makes the guarantee explicit and
independent of the view's marshalling strategy, at zero cost — a non-volatile field here
survives only as long as every future reader goes through `invokeLater`.
(`ComparisonViewModel` has the same shape, so this is a repo-wide idiom, not a Phase 3
regression.)

**W3-5 — `prepareFailView` is a non-atomic read-modify-write, and a concurrent writer
exists.** `WatchlistPresenter.java:213-220` reads `viewModel.getState()` and writes a
derived state. `runInBackground` disables the four *buttons* for the duration, but it does
not disable the ticker table — so a user clicking a different row while a refresh is in
flight drives `onTickerSelected → controller.showWatchlist → setState` on the **EDT**
concurrently with the worker's `setState`. Worst case is a lost update and a stale render
(no crash, no corruption — `WatchlistState` is immutable). Low likelihood, but it is the
one genuine race in the vertical and it is invisible in tests. Cheapest fix in Phase 4:
have `setButtonsEnabled(false)` also call `tickerTable.setEnabled(false)`.

**W3-6 — the D-N2 tripwire fires but does not point anywhere.** See §5 above.
`WatchlistView.java:62-63` / `WatchlistPresenter.java:249-251`. Add a cross-reference
comment in the presenter's `RATE_LIMIT` arm naming `WatchlistView.RATE_LIMIT_PREFIX`, or
carry D-N2 to Phase 5 as the structured `isQuotaExhausted()`. Accepting the risk was
right; the mitigation is one comment short of actually working.

**W3-7 — both agent briefs forbid the handoff files their own instructions require.**
`agents/adapter.md` Never Touch lists `plan/**`; `agents/view.md` Never Touch lists
`plan/**` — while `agents/view.md:155` instructs Agent D to "file it in
`plan/handoffs/agent-d-to-c.md`". Both agents wrote into `plan/handoffs/` and both were
right to. No harm done (disjoint files, no two-agent collision), but the briefs are
self-contradictory and the next agent may resolve it the other way and silently absorb a
cross-agent need instead of filing it. Carve `plan/handoffs/<agent>-*.md` out of the
`plan/**` prohibition in both briefs.

**W3-8 — `render` overwrites in-progress typing.** `WatchlistView.java:359`,
`tickerField.setText(state.getTickerFieldText())` runs unconditionally on every state
event. Because buttons are disabled during a worker but the field is not, a user who
starts typing the next symbol while a refresh is in flight loses it when the result
arrives, and the caret jumps. Consequence of the D3-d design (the field is view-model
owned), not a mistake — but worth either disabling the field alongside the buttons in W3-5's
fix, or guarding the `setText` on the value actually differing.

**W3-9 — style nits in the seam files.** `WatchlistViewModel.java:47` is 101 characters
(the only over-length line in the phase). `WatchlistState.hashCode` has no javadoc while
its `equals` and `toString` neighbours do, against §7's "javadoc on every public member".
Both orchestrator-owned.

**W3-10 — `OrderedFocusTraversalPolicy` silently wraps on an unknown component.**
`WatchlistView.java:424,430`: `order.indexOf(component)` returns `-1` for anything not in
the fixed list, and `Math.floorMod` turns that into a plausible-looking index rather than
a signal. It cannot throw, so this is a robustness nit, not a bug — but a component added
to the panel later and forgotten in `installFocusOrder` will produce a focus order that is
wrong in a way nobody can debug.

**W3-11 — `WatchlistView` uses `final` on parameters throughout; nothing else in the repo
does.** Not wrong (§7 asks for `final` locals "wherever possible"), but it makes the file
visibly inconsistent with `ComparisonView`, `WatchlistPresenter` and every `use_case`
class next to it. Lowest priority; listed only because this reviewer is the linter.

---

## Notes

- **Nothing was fixed by the reviewer.** No application file was modified. The only file
  written this review is this one.
- **The "before" screenshot is still uncaptured and the window is closing.**
  `plan/handoffs/screenshots.md` has `Path: _(to be filled in by the owner)_`. `vision.md`
  §8 requires it, and Phase 4's two integration edits make it unrecreatable without a
  revert. D3-b correctly reassigned it from Agent D to the owner — but nothing has been
  captured yet. **This must happen before the first Phase 4 commit**, not before the Phase 4
  gate. It is the one genuinely irreversible item in the plan right now.
- **`plan/phases.md` Phase 3 "Done when" is fully satisfied**, including the corrected
  "all 8 success messages" wording and the two threading clauses.
- Coverage arithmetic for Phase 5 (H6): the vertical currently sits at 73.3% with
  `WatchlistView` already counted in as 171 uncovered lines (147 + 24 in the nested
  classes). Phase 4 adds `Main`'s wiring block, also uncovered. The margin over 70% is
  therefore about 45 lines — thinner than it looks. If Phase 5 measures short, W2-7's
  `apiKeyFrom(String)` extraction and W2-8's factory assertion are the two cheapest levers
  already on the books.
- Not every interactor is at 100% *method* coverage despite 100% lines:
  `RefreshTickerInteractor` shows 1 method and 3 instructions missed, and
  `TickerSymbolValidator` 1 line. Phase 2 territory, immaterial to this phase, recorded so
  the "all four interactors 100%" claim is not carried forward unqualified — it is true of
  lines, not of every JaCoCo axis.
- Dead code check on the phase's output: clean. `WatchlistViewModel.VIEW_NAME` and
  `WatchlistView`'s constructor have no caller yet — both are the documented Phase 4
  wiring surface (`agents/orchestrator.md` §6), not dead code. No test double is defined
  and unused; all four `Recording*` fakes in `WatchlistControllerTest` are driven.
- D3-f's finding stands and should be treated as a standing rule: **verify each worktree's
  base commit before the agents start.** Both agents caught it themselves this time, which
  is luck, not process.
- For Phase 4, the two handoffs are accurate and worth reading in full: use **one**
  `WatchlistPresenter` instance across all four interactors, construct the controller in
  the `(add, remove, refresh, show)` order, and call `showWatchlist("")` inside the
  existing `SwingUtilities.invokeLater`. Constructing `WatchlistView` alone paints
  `initial()` — `Ready.` with empty tables — not the restored watchlist.
