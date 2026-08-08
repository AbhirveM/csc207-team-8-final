# Team raise — 2026-08-08

Post this to the team channel. It is written to be pasted as-is.

*(Supersedes the earlier version of this file, which was written before PR #24 merged and is now
out of date. The three "nobody owns" gaps in it have since been closed — see §0.)*

---

Hey all — status before the presentation, and **one thing I need someone else to pick up today**.

**Build is green:** `mvn clean verify` → 0 Checkstyle violations, **461 tests**, 0 failures,
**73.1% overall line coverage**, and every use-case interactor in the project at 100% lines. Both
team quality targets clear.

### 0. What I have already closed, so nobody duplicates it

These were the "nobody owns them" gaps from my last message. I said I would do them if unclaimed by
this morning; nobody claimed them, so they are done and on `chore/presentation-readiness`:

- **Checkstyle is enforced.** `checkstyle.xml` plus a plugin bound to `validate`, so it fails the
  build — including `mvn test` — on any violation in `src/main/java`. It found 172; all 172 are
  resolved. Two rules are deliberately off with a written reason in the config: `PackageName` is
  widened because `data_access` / `use_case` / `interface_adapter` are the course's own convention,
  and `LeftCurly` is off because reformatting single-line accessors now would collide with PR #23.
  **This will fail your build if you push a violation — that is the point.** Run `mvn checkstyle:check`
  before you push.
- **`accessibility-report.md` exists.** All seven Principles of Universal Design, the target-market
  paragraph, and the who-may-be-excluded paragraph using the E3I module terminology. It names two
  real gaps honestly (the <700px clipping, and the invisible save failures) rather than claiming we
  are perfect. **At least one principle has to be discussed out loud during the presentation** for
  the category to score above 1/5 — please pick one and put it in your section.
- **`LICENSE` added** and the README no longer claims a license it does not have.
- **README now matches the app.** Features opens with a status table separating "runs today" from
  "implemented and tested, not reachable from the UI". Usage no longer tells the reader to launch
  with `mvn exec:java`, which the same README said eleven lines earlier could not work.
- **`docs/architecture.md`** — whole-project layer diagram, the design patterns and SOLID evidence
  with file-level examples, and both Alpha Vantage endpoints named. Copy from it freely for slides.
- **The `view` layer no longer imports `entity`.** `ComparisonView`, `BacktestResultsView` and the
  old `MainAppState` all did. Details in §2 below because it changes an API you may be mid-way
  through using.

### 1. ⚠️ The one thing I am NOT doing: wiring the backtest path

This is deliberate — it is DD's and Ziyad's code and your contribution credit, not mine. But it is
**the single biggest risk to our Functionality mark** and it needs to happen today.

`app/Main.java` constructs **zero** backtest objects. Nothing builds `RunBacktestInteractor`,
`BacktestController`, `BacktestPresenter` or `BacktestViewModel`; `BacktestResultsView` is never
registered; `MainView`'s nav bar has only two buttons. Nothing calls `CompletedBacktestStore.add`.

**So the end-to-end team story — add a stock → configure a strategy → run it → see a performance
summary — cannot be demonstrated live.** Every piece of it is written and unit-tested. It is a
wiring job, roughly 40–60 lines, not new logic.

The rubric band we lose without it reads *"Core workflows are demonstrated successfully. Little to
no promised functionality is missing or buggy."* Three of our four verticals are currently invisible
to a user, and it also drags down Project Scope, which caps the entire group score.

**Concretely, in `Main.java`, next to the comparison block:**

1. Construct `RunBacktestInteractor` with a `BacktestPresenter` over a new `BacktestViewModel`.
2. Register `new BacktestResultsView(backtestViewModel)` under `BacktestViewModel.VIEW_NAME`.
3. Add a nav button in `MainView` pointing at it.
4. Have `BacktestPresenter.prepareSuccessView` also call `completedBacktests.add(result)` — I have
   already constructed `CompletedBacktestStore` in `Main` and passed it to `ComparisonController`,
   so the Compare screen starts working the moment something lands in it.

Issue #9 is the ticket. **If this is not going to happen, say so today** so we script the demo
honestly around the watchlist rather than discovering it live in front of the class.

### 2. An API change you need to know about (my fault to flag, not to hide)

Fixing the `view → entity` imports changed three signatures:

- `ComparisonController.compare(List<BacktestResult>)` → **`compare()`**, no arguments. The
  controller reads `CompletedBacktestStore` itself.
- `view.MainAppState` is **deleted**, replaced by
  `interface_adapter.comparison.CompletedBacktestStore`. It is no longer a singleton — inject it.
- `BacktestViewModel.getResult()` → **`getSummary()`** returning a display-ready `Summary` record,
  plus `getTradeRows()`. `BacktestPresenter` now does the `$%.2f` / `%.2f%%` formatting that used to
  live in the view.

If you have local work against any of those, pull before you go further.

### 3. Decisions I cannot make alone

- **PR #23 (`wire-strategy-configs-into-watchlistentry`).** This is exactly the change `vision.md`
  §5.2 warns will make existing `watchlist.dat` files throw `InvalidClassException` — our recovery
  code then renames the file and hands back an empty watchlist, which during demo week looks like a
  persistence bug. Either merge it **and** everyone deletes their local `watchlist.dat`, or hold it
  until after the presentation. Ziyad's call, but we need one.
- **`refactor/cave-layer-folder-names` (PR #26).** Please do not attempt this before the
  presentation. It renames two packages that now contain ~15 more files than when it was cut.
- **`serialVersionUID` on the seven Serializable entities.** ⚠️ **Still do not "fix" this by adding
  `= 1L`** — same `InvalidClassException` problem. If we do it at all, it is
  `serialver -classpath target/classes entity.Ticker entity.Watchlist ...` and pasting the real
  computed values, verified before and after. My honest recommendation is to leave it until after
  the demo.
- **Minimum window size (`view/MainView.java`, Member 4).** Below ~700px wide the "Load prices"
  button clips off the right edge and becomes unclickable. `MainView` only calls
  `setSize(900, 600)` with no minimum. One line — `setMinimumSize(...)` — plus a wrapping layout for
  the control strip. This is written up as a real failure in the accessibility report, so it would
  be good to be able to say it is fixed.

### 4. For the presentation itself

- **Name `TIME_SERIES_DAILY` and `OVERVIEW` verbatim on a slide.** API Usage 5/5 requires two
  specific endpoints be mentioned there. We have tests asserting both appear in the generated
  request URLs, so we can say it and prove it.
- **Put an architecture diagram on the slides.** Clean Architecture 3/5 needs at least one. Take it
  from `docs/architecture.md`.
- **Discuss at least one Universal Design principle out loud**, plus our target users and the group
  who may struggle — that is worth 3 extra points in that category over just having the file.
- **Every member must speak**, and we must land inside the time limit.

---

## Reminders that apply to everyone individually

The individual rubric is 20 points and 5 of them are for showing **all three** of: a before view, an
after view, and a class diagram of your full use case. Missing any one drops that category to ≤1/5.
The before shot cannot be recreated once your view is wired in — if you have not taken yours, do it
before you wire anything.

One more: **reading from notes caps your verbal presentation at 3/5.**
