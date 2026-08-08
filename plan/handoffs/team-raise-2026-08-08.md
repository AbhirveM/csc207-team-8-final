# Team raise — 2026-08-08

Post this to the team channel. It is written to be pasted as-is.

---

Hey all — my watchlist / Alpha Vantage vertical is done and up as **PR #24**, ready for review.
Reviews requested from all three of you; one approval unblocks the merge. It already merges current
`main`, so Ratnabh's momentum strategy and DD's backtest engine are included in it.

State after the merge: **460 tests, 0 failures, 72.28% overall line coverage, and every one of the
nine use-case interactors in the project is at 100%.** That clears both team quality targets.

Five things need a decision from someone who isn't me.

### 1. Three gaps nobody owns

Full detail is in `plan/handoffs/team-notes.md` §5. Short version:

- **No Checkstyle config.** The rubric caps Code Quality at 3/5 without an automated style tool —
  4/5 explicitly requires one. This is a `checkstyle.xml` plus a plugin block in `pom.xml`, maybe
  half an hour. I have been using a hand-written style checklist as a stand-in for my own reviews,
  and that stand-in is gone now that my plan is closed.
- **No `accessibility-report.md`.** A required course deliverable that does not exist, and it is a
  whole 5-point category scoring zero. It needs 2–3 sentences per Principle of Universal Design, a
  paragraph on who we would market this to, and a paragraph on who might be less likely to use it
  using the embedded-ethics module terminology. The accessible *behaviour* is already implemented in
  the watchlist view — visible labels, worded errors rather than colour alone, keyboard focus order,
  mnemonics — so there is real material to write about.
- **No `serialVersionUID` on seven Serializable entities.** ⚠️ **Do not "fix" this by adding
  `= 1L`.** That changes the computed UID and makes every existing `watchlist.dat` throw
  `InvalidClassException`, which our own recovery code handles by renaming the file `.corrupted-*`
  and handing back an empty watchlist. It would destroy saved data during demo week and look like a
  bug in persistence. The only safe route is `serialver -classpath target/classes entity.Ticker
  entity.Watchlist`, pasting the real values, verified before and after. Leave
  `entity/WatchlistEntry.java` alone entirely.

**If nobody claims Checkstyle and the accessibility report by tomorrow morning, I will do both** —
they are cheap and we are otherwise donating the marks.

### 2. The backtest engine is merged but not reachable from the UI

DD's PR #25 added `BacktestEngine`, the run-backtest use case, and `BacktestResultsView` — good, and
well tested. But it added **zero lines to `Main.java`**, so nothing constructs the controller or
registers the view. `MainAppState.addCompletedResult` still has no callers, which means the Compare
Strategies screen still only ever renders its empty state.

So the end-to-end team story — add a stock → configure a strategy → run it → view a performance
summary — **still has no working path through the UI**, even though every piece of it now exists.
This is the single biggest functionality risk for the presentation and it is a wiring job, not new
logic. Whoever owns integration (issue #9) should pick it up today.

### 3. `refactor/cave-layer-folder-names` needs to be redone

That branch renames `view/` → `views/` and `data_access/` → `database/`. It was cut before my work
existed and will collide with roughly fifteen new files in exactly those two packages. Please let
PR #24 land first, then redo the rename on top of it — it will be much less painful in that order.

### 4. The README described features we had not built

It documented Momentum, Performance Analysis and strategy comparison as delivered. Some of that is
true now that #21 and #25 have landed, but graders use the README as supporting evidence and an
overclaim is discoverable in about thirty seconds. I have fixed my own sections — the run commands
did not actually work (`mvn exec:java` needs a plugin we do not declare, and the `-cp target/classes`
fallback throws `NoClassDefFoundError`), and I have added the free-tier limits. **Someone should
re-read the Features section against the app as it actually runs now.**

Also still missing repo-wide: a `LICENSE` file, though the README claims a license.

### 5. Two things that matter for how we present

- **The API rubric's top band needs two specific endpoints named on the slides.** We use
  `TIME_SERIES_DAILY` and `OVERVIEW`, and there are tests asserting both appear in the generated
  request URLs, so we can say it and prove it. Please put both names on a slide verbatim.
- **Clean Architecture 3/5 requires at least one architecture diagram on the slides**, and the repo
  had none of any kind. I have added `docs/add-ticker-use-case.md` with a class diagram for my use
  case plus a Dependency Rule view. Everyone needs a class diagram for their own use case for the
  individual rubric anyway — that file is a template you are welcome to copy.

---

## Reminders that apply to everyone individually

The individual rubric is 20 points and 5 of them are for showing **all three** of: a before view, an
after view, and a class diagram of your full use case. Missing any one drops that category to ≤1/5.
The before shot in particular cannot be recreated once your view is wired in, so if you have not
taken yours, do it now.

One more: **reading from notes caps your verbal presentation at 3/5.**
