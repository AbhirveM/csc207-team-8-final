# Review — Phase 4 (Composition Root)

Reviewed: the uncommitted working tree on `feature/watchlist-use-cases` at base commit
`fc27b3c`. Seven files modified, none added, none deleted:

```
plan/handoffs/screenshots.md
plan/phase-4.md
plan/phases.md
src/main/java/app/Main.java
src/main/java/interface_adapter/watchlist/WatchlistViewModel.java
src/main/java/view/MainView.java
src/main/java/view/WatchlistView.java
```

```
Status: PASS WITH WARNINGS
```

Zero criticals. The vertical is wired, runs offline, and the Dependency Rule is intact.
Nine warnings, one of which (W4-1) corrects a claim about a closed defect and must not be
carried forward as "closed".

---

## Verified independently

- **Build.** `mvn -o clean verify` re-run here: **BUILD SUCCESS, 403 tests, 0 failures**,
  79 classes analysed by JaCoCo.
- **Coverage.** Recomputed from `target/site/jacoco/jacoco.csv`: **1004/1403 lines = 71.56%**,
  399 missed. 70% needs 983 covered, so the margin is **~22 lines**. Largest holes:
  `WatchlistView` 148, `Main` 47, `ComparisonView` 36, `MainView` 20.
- **Dependency Rule — zero violations.** `grep` over `use_case`, `interface_adapter`,
  `entity` for `javax.swing|java.awt` → empty. `^import data_access` in `use_case` → empty.
  `^import use_case` in `view` → empty. `^import entity` in `view` → only Member 4's
  pre-existing `ComparisonView` and `MainAppState`, untouched by this phase.
- **Secrets.** `System.getenv` appears exactly once, `AlphaVantageMarketDataAccessObject.java:61`,
  called only from `Main.java:67`. `grep -rniE "alphavantage.co/query\?.*apikey=[A-Z0-9]" src/`
  → nothing. The key is passed to a constructor and never read again; `Main` only calls
  `isPresent()`/`isEmpty()` on the `Optional`. `SAMPLE_DATA_STATUS` names no key. No `.env`,
  no default key. Clean.
- **No network in tests.** `grep -rn "JdkHttpJsonClient\|HttpClient" src/test` → nothing.
- **Interactor wiring matches the frozen signatures byte for byte** (checked against the
  four constructors and `agents/orchestrator.md` §5.5):
  - `Main.java:94` Add — `(watchlist, gateway, stockRepository, saveWatchlistInteractor, presenter)` ✓
  - `Main.java:97` Remove — `(watchlist, stockRepository, saveWatchlistInteractor, presenter)` ✓
  - `Main.java:99` Refresh — `(watchlist, gateway, stockRepository, presenter)`, **no save** ✓
  - `Main.java:101` Show — `(watchlist, stockRepository, presenter)`, no gateway, no save ✓
  - `Main.java:105` `WatchlistController(add, remove, refresh, show)` — order correct ✓
  - Exactly **one** `WatchlistPresenter` (`Main.java:89`), shared by all four ✓
- **Null watchlist on load failure is handled correctly.** `FileWatchlistDataAccessObject.load()`
  returns a fresh `Watchlist` for a missing or corrupt file and throws `PersistenceException`
  only on a real I/O error; `LoadWatchlist.Interactor` then calls `prepareFailView`, so
  `PersistenceViewModel.watchlist` is left `null`. `Main.java:80-83` degrades to
  `new Watchlist()`. Correct, and `getWatchlist()` hands back the live object, so the
  interactors mutate and `SaveWatchlist` persists the same instance.
- **W3-5's race is genuinely closed.** See §"W3-5" below — verified empirically, not by
  reading the javadoc.
- **The "before" screenshot is real.** Opened
  `C:\Users\abhir\Pictures\Screenshots\Screenshot 2026-08-08 113636.png`: one nav button
  ("Compare Strategies"), no Watchlist button, the comparison table empty. Captured at
  `fc27b3c` with the wiring still uncommitted, so the artifact is valid and no longer
  recreatable — the irreversible item on the Phase 3 board is discharged.
- **Launch state, offline, empty watchlist** — reproduced through the exact object graph
  `Main` builds (harness, no Swing): `showWatchlist("")` yields
  `"Your watchlist is empty. Add a ticker to begin."`, which `Main.java:126-135` then
  replaces with the 92-character sample-data notice. See W4-2.

---

## Judgement calls the phase asked for

### The seven deleted lines — all authorised, none a violation

`plan/phase-4.md` §Verification 2 says the diff must be "additive only", but
`agents/orchestrator.md` §6 simultaneously instructs the orchestrator to *replace* a TODO
and to *uncomment* a template — neither is literally achievable without deleting lines. Each
deletion maps to a §6 instruction:

1. `Main.java` — Member 4's 3-line `// TODO: pass saveWatchlistInteractor / loadWatchlistInteractor…`
   removed. §6: "Replace the TODO with a `// --- Watchlist (Member 1) ---` block." The TODO's
   own text asks for exactly this wiring; leaving it would be a comment contradicting its
   code. **Authorised.**
2. `MainView.java:31-33` — the three commented-out nav-button lines became three live lines.
   §6: "Uncomment it and change the hardcoded string to `WatchlistViewModel.VIEW_NAME`."
   The new lines are the old ones minus `// ` plus the constant. **Authorised.**
3. `Main.java` — `SwingUtilities.invokeLater(() -> mainView.setVisible(true));` became a block
   lambda. §6: "inside the existing `SwingUtilities.invokeLater`, call
   `watchlistController.showWatchlist("")`." An expression lambda cannot hold a second
   statement; `mainView.setVisible(true)` survives verbatim as the first statement and its
   semantics are unchanged. **Authorised**, and it is the minimal edit.

Nothing was reordered: the wiring block sits between Persistence and Comparison as specified,
and every pre-existing Member 4 statement retains its relative position.

### The offline notice mechanism — sound, but see W4-2

The mechanism is correct. Both writes happen in the same EDT task, `WatchlistState` is
immutable and its 6-arg constructor is the only way to derive one, and no `SwingWorker` can
start before that task completes (workers are only launched from user actions on the EDT), so
the notice is deterministically the last write at launch. Placing it on `WatchlistViewModel`
rather than in `app/Main.java` is the right call under §7: it keeps the string in the
interface-adapter layer next to `TICKER_COLUMNS`, where a reviewer hunting user-facing prose
looks. The javadoc documents the exception honestly. Accepted as an exception to §7 — with
the two consequences recorded as W4-2 (what it clobbers) and W4-3 (positional construction).

### W3-5 — the fix works; I verified it rather than trusting the reasoning

`WatchlistView.java:326` adds `tickerTable.setEnabled(enabled)`. I ran a probe on a realized
`JFrame` to settle whether disabling a `JTable` actually suppresses the selection event W3-5
describes:

| probe | result |
|---|---|
| `setEnabled(false)` itself | selection unchanged, **zero** `ListSelectionListener` fires |
| left-click dispatched over row 1 while disabled | `selectedRow` stays 0, zero listener fires |
| `VK_DOWN` dispatched while disabled | `selectedRow` stays 0, zero listener fires |
| `requestFocusInWindow()` while disabled | `false` — focus cannot be granted |
| same click after `setEnabled(true)` (control) | `selectedRow` becomes 1 |

The mechanisms are `SwingUtilities2.shouldIgnore`, which `BasicTableUI`'s mouse handler
consults and which short-circuits on `!c.isEnabled()`, and `JComponent.processKeyBinding`,
which is gated on `isEnabled()` — so neither the mouse nor the keyboard path can reach the
selection model. `setEnabled` does not touch the `ListSelectionModel`, so
`restoreSelection`'s programmatic `setRowSelectionInterval` still works on a disabled table
(confirmed in the probe). **W3-5 is closed as the brief described, not worked around.** With
the buttons and the table both frozen there is exactly one writer to the view model while a
worker is in flight, so the non-atomic read-modify-write in `prepareFailView` no longer has a
concurrent writer.

Side effects of the fix are real but minor: W4-4 (focus dead-end) and W4-5 (no visual cue).

### Ownership — `WatchlistView.java` is Agent D's file

`agents/orchestrator.md` §3 assigns `src/main/java/view/WatchlistView.java` to Agent D, and
§2 does not list it among orchestrator-owned files. The orchestrator edited it anyway.

I am **not** calling this a FAIL, and the reasoning matters: `agents/reviewer.md` defines the
critical ownership finding as "a file touched by two agents in one phase", and the rule exists
to prevent concurrent writers from clobbering each other. Phase 4 spawns no agents, so
`WatchlistView.java` had exactly one writer; the edit is one line plus javadoc inside a
private method; and W3-5 was booked "Owner D, Due Phase 4" by Phase 3's own close-out, which
is unsatisfiable as written since Phase 4 has no D. The deviation is the only way the board
closes. It must be **recorded as a Phase 4 deviation in `plan/decisions.md`** rather than left
implicit (W4-8), and Member 1's own file list should note that `WatchlistView` now carries an
orchestrator edit so a future Agent D spawn does not branch from a stale assumption.

### Launch card — accepted, but it is Member 4's screen that moved

Nothing calls `setActiveView` before `setVisible`, so `CardLayout` shows the first-added card,
and `Main.java:107` now adds `watchlistView` before `Main.java:116` adds `comparisonView`.
`WatchlistView` is therefore the launch screen and "Watchlist" the leftmost nav button. Good
for the Member 1 demo; it is nonetheless a behavioural change to Member 4's app in Member 4's
file, and `vision.md` §9 asks for a ping (W4-7). The non-invasive alternative, if he objects,
is one line — `viewManagerModel.setActiveView(ComparisonViewModel.VIEW_NAME)` inside the
existing `invokeLater` — not a reordering of the `addView` calls.

### D3-c — resolved correctly

`plan/phases.md:95` and `plan/phase-4.md:85` now say **newest-first**. That matches
`WatchlistSnapshot`'s javadoc ("ordered *newest first*"), `WatchlistSnapshotFactory`, and both
`WatchlistPresenter` javadocs (lines 323, 348), and it leaves `entity.Stock`'s
oldest-to-newest guarantee to the strategies untouched. The prose was corrected rather than
the frozen, tested contract — the cheaper and safer of the two directions. Closed.

### W3-6 — deferred to Phase 5 as agreed, not reported as new.

---

## Critical (blocking — must fix before next phase)

None.

---

## Warnings (non-blocking but fix soon)

**W4-1 — W3-8 is *not* closed by this change; do not record it as closed.**
`src/main/java/view/WatchlistView.java:322-327`, `:369`. W3-8 is: "`render` calls
`tickerField.setText(...)` unconditionally on every state event, so a user typing the next
symbol **while a refresh is in flight** loses it and the caret jumps." `setButtonsEnabled` now
disables the four buttons and `tickerTable` — but **not `tickerField`**. The user can still
type throughout the in-flight window, and when the worker's `setState` lands, `render` at
line 369 still overwrites the field unconditionally. What the fix actually closes is the
*selection-driven* overwrite, which is what the new javadoc claims ("incidentally stops a
selection from overwriting a symbol the user is midway through typing") — accurate as written,
but narrower than W3-8. Phase 3's close-out note in `plan/status.md:124-126` predicted this
one edit would fix both; it does not. Carry W3-8 to Phase 5, owner D, with the fix it already
names: guard the `setText` on the value actually differing, or disable `tickerField` alongside
the buttons.

**W4-2 — the offline notice clobbers the only "what do I do next" line on a first launch.**
`src/main/java/app/Main.java:126-135`. There is no `watchlist.dat` in the repo, so a fresh
clone launches with an empty watchlist, which is precisely the grader's path. Verified through
the real object graph: `showWatchlist("")` produces
`"Your watchlist is empty. Add a ticker to begin."`, and the notice then replaces it, leaving
the user with `"Sample data is in use - no market data API key is configured, so these prices
are synthetic."` and no instruction. The two facts are not in competition — both fit. Cheapest
fix, one line, no new prose: pass `WatchlistViewModel.SAMPLE_DATA_STATUS + " " +
shown.getStatusMessage()` as the status. Combined length is ~137 characters against a 900px
default window; if that measures too wide, the alternative is to apply the notice only when
`shown.getTickerRows()` is non-empty. Worth fixing before the "after" screenshot is captured,
since that screenshot is a presentation artifact.

**W4-3 — `Main` hand-assembles a `WatchlistState` positionally, and four of the six arguments
are interchangeable `String`s.** `src/main/java/app/Main.java:128-134`. `WatchlistState`'s
constructor is `(List, List, String selectedSymbol, String statusMessage, String errorMessage,
String tickerFieldText)`. Any future reordering of those four `String` parameters compiles
silently here and is caught nowhere, because `Main` has no test and every presenter test
constructs states through the presenter. `WatchlistState` is orchestrator-owned, so the fix is
in-scope and cheap: add `WatchlistState withStatusMessage(String statusMessage)` and reduce
lines 127-135 to `watchlistViewModel.setState(shown.withStatusMessage(SAMPLE_DATA_STATUS))`.
That also removes the only place outside `WatchlistPresenter` that builds a state.

**W4-4 — Tab dies on the disabled table while a worker is in flight.**
`src/main/java/view/WatchlistView.java:326` with `:219-223` and `:432-441`.
`OrderedFocusTraversalPolicy` returns the next component from a fixed list without consulting
`accept()`, so `getComponentAfter` hands back `tickerTable` even when it is disabled;
`Component.transferFocus` does not retry the following candidate, and my probe confirms
`requestFocusInWindow()` returns `false` on a disabled table. So Tab from "Load prices" moves
focus nowhere until the worker finishes. This pre-dates Phase 4 for the four buttons — the new
line extends it to the table, and the accessibility deliverable is keyboard operability.
Owner D, Phase 5; the fix belongs with W3-10, which is already open against the same class:
have the policy skip components failing `isEnabled() && isFocusable() && isShowing()`.

**W4-5 — a disabled `JTable` gives no visual cue that it is frozen.**
`src/main/java/view/WatchlistView.java:326`. `DefaultTableCellRenderer` does not grey cell
text on `!table.isEnabled()`, so in Metal and Windows L&F the table looks live while ignoring
clicks. The greyed buttons are a partial cue, so this is cosmetic, not a correctness issue —
but if it is ever tightened, do it in the view (a disabled-state renderer), not by reverting
the W3-5 fix.

**W4-6 — three orchestrator-owned documents now contradict the shipped code.** This is the
same class of drift Phase 3 fixed as W3-1/W3-2, and it matters because a Phase 5 agent reads
these as frozen truth:
- `agents/orchestrator.md` §5.4 lists `WatchlistViewModel`'s public surface and does not
  mention `SAMPLE_DATA_STATUS`; an agent treating §5.4 as exhaustive would delete it as dead.
- `agents/orchestrator.md` §7 states flatly "User-facing prose exists only in
  `WatchlistPresenter`", which the constant now violates in the letter. Amend it with the
  documented exception rather than leaving the code as the odd one out.
- `plan/phase-4.md` §"Files to modify" names two files; five source files were touched.

**W4-7 — Member 4's file carries two now-false comments and an unpinged behaviour change.**
`src/main/java/view/MainView.java:12` still reads "for now only the Comparison view (yours) is
wired in", and `:32` still reads "Add nav buttons for each screen here, e.g.:" immediately
above live code rather than a template. Both are exactly the "comment contradicts its code"
pattern `agents/reviewer.md` calls out. They are in Member 4's file, so the right move is to
raise them with the launch-card change in the `vision.md` §9 ping rather than silently rewrite
his javadoc. Also `MainView.java:34` is 106 characters, over the ~100 limit in §7 — the only
new line in the phase that is over (the three long lines in `Main.java:111-115` are Member 4's
and untouched).

**W4-8 — record the `WatchlistView.java` edit as a Phase 4 deviation.** `plan/decisions.md`
has no Phase 4 section yet, and the ownership crossing analysed above is exactly the kind of
deliberate deviation that file exists to hold. It should also note that `WatchlistView` now
contains an edit no Agent D run produced.

**W4-9 — save and load failures are invisible to the user.** `src/main/java/app/Main.java:56`.
`PersistenceViewModel` is constructed and fed by `PersistencePresenter`, but it is bound to no
view anywhere in the app — `grep` finds no consumer outside `interface_adapter/persistence`.
So when `SaveWatchlist` fails after an Add, the interactor cannot know (`InputBoundary.execute`
returns `void`) and `WatchlistPresenter` truthfully reports `"Added AAPL (Apple Inc.) with 120
days of price history."` while nothing reached disk; a load failure is equally silent. This is
inherited from Member 4's boundary shape, not introduced here, but Phase 4 is the phase that
made those two interactors reachable, so it is now a user-visible gap rather than a latent
one. Not fixable inside Member 1's vertical — raise it in the Phase 5 hand-off notes.

---

## Notes

- **I fixed nothing.** The only file I wrote is this one. No application code, no plan
  document, no test was modified by the reviewer. (I compiled two throwaway probe classes into
  the scratchpad and deleted the one class file I copied into `target/classes` to run it;
  `target/` is build output and is not in the tree.)
- **Coverage margin for Phase 5: ~22 lines, not D3-g's ~45.** D3-g estimated the post-`Main`
  margin at roughly 45 lines; the measured figure is 71.56% with **21.9 lines** of slack over
  70%. `Main`'s 47 uncovered lines are the whole difference and they are not testable as
  written. Phase 5 adds a hand-off test, which should net positive, but the buffer is now thin
  enough that any new uncovered production code eats it. Carry it forward as a live constraint:
  the levers are already on the books and are not Swing tests (H6) — W2-7's
  `apiKeyFrom(String)` extraction and W2-8's factory assertion. Note that W2-7 would also make
  the gateway-selection *decision* testable, which is the one piece of Phase 4 logic with no
  coverage at all.
- **The offline notice has no test and cannot get one** while it lives inside
  `Main.main`'s lambda. If W4-3's `withStatusMessage` lands, the composition of "Show
  Watchlist status + offline notice" becomes assertable without Swing.
- **Phase 4 verification items 5, 6 and 7 are not evidenced in the tree.** Resize behaviour
  and Tab order are manual gates with no artifact, and `plan/handoffs/screenshots.md` has a
  "before" section only — no place to record the "after" shot item 7 requires. Add that
  section (and see W4-2: fix the status line first, so the shot shows the intended launch
  state). Minor: the "before" shot was taken on a window wider than the 900px default
  `MainView` sets, where the spec said "at default size". Immaterial to what it proves.
- **The whole Swing object graph is still built off the EDT** — `MainView`, `ComparisonView`
  and now `WatchlistView` are constructed on the main thread, and only `setVisible` and the two
  `setState` calls are marshalled. This is Member 4's pre-existing pattern and the components
  are unrealized at construction, so it is tolerated rather than correct. Not a Phase 4
  regression; do not "fix" it inside Member 1's vertical.
- **Quota safety at launch is intact.** With a key present, launch does no network I/O:
  `showWatchlist("")` reads only `Watchlist` and `StockRepository`, and hydration stays behind
  the "Load prices" button. Hazard H5 holds through the composition root.
- **`SAMPLE_DATA_STATUS` is not dead code.** Exactly one production caller
  (`Main.java:132`), which is the documented composition-root hand-off.
