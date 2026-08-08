# Accessibility Report — MarketLens

Team 8, CSC207. This report covers the seven Principles of Universal Design as they apply to
MarketLens, followed by our target market and a discussion of who may be excluded.

**Scope note, stated up front so nothing below is misread.** The screen that is reachable in the
running application today is the **Watchlist** screen (`view/WatchlistView.java`). The strategy
configuration, backtest and comparison features exist as tested code but are not yet wired into a
user path, so we do not claim accessibility work for screens a user cannot open. Where a principle is
served by the watchlist screen we cite the specific mechanism; where it is not yet served, we say so
and describe what we would implement.

---

## 1. Equitable Use

*The design is useful and marketable to people with diverse abilities.*

Every action on the watchlist screen is reachable by both mouse and keyboard, and neither route is
degraded relative to the other. Each of the four controls carries a mnemonic (`Alt+T` to reach the
ticker field, `Alt+A` add, `Alt+M` remove, `Alt+R` refresh, `Alt+L` load prices), and an explicit
focus traversal policy walks the screen in its reading order rather than in the arbitrary order
components happened to be added (`installFocusOrder`, `WatchlistView.java:218`). A keyboard-only user
and a mouse user reach the same functions by equivalent means, rather than the keyboard being a
lesser fallback path.

Every interactive component also carries a programmatic accessible name for screen readers — the
ticker field, both tables, and the status and error lines all call
`getAccessibleContext().setAccessibleName(...)` — so assistive technology announces the same
information a sighted user reads from the visible labels.

## 2. Flexibility in Use

*The design accommodates a wide range of individual preferences and abilities.*

The two data tables sit in a `JSplitPane` with a draggable divider (`buildSplitPane`,
`WatchlistView.java:157`), so a user can give whichever table they care about more of the window —
useful for someone running a magnifier or a large system font. The window itself is resizable and
the layout reflows.

Loading price history is user-driven rather than automatic, through the "Load prices" button, which
lets a user work at their own pace instead of the application deciding when to spend their time and
their API quota.

**Known gap, recorded honestly:** below roughly 700px of window width the "Load prices" button clips
off the right edge of the control strip and becomes unclickable, because `MainView` sets no minimum
window size. This is a real flexibility failure for anyone working in a small or split-screen window,
it is logged in `plan/handoffs/walkthrough.md` step 7, and the fix is a minimum window size plus a
wrapping layout for the control strip.

## 3. Simple and Intuitive Use

*Use of the design is easy to understand, regardless of the user's experience, knowledge, language
skills, or current concentration level.*

The screen is arranged in one top-to-bottom reading order: controls, then the two labelled tables,
then the status and error lines. Both tables sit under a visible heading ("Watchlist", "Daily
prices") which is bound to the table with `setLabelFor`, so the heading is both a visual and a
programmatic label rather than decoration.

The view contains no business logic and composes no prose: every value arrives from the presenter
already formatted as a display-ready string. In practice this means the wording a user sees is
written in one place and is consistent everywhere, rather than being assembled ad hoc at several
call sites. Feedback is phrased as a complete sentence — "Added AAPL with 120 days of price
history." — rather than as a code or a bare status flag.

Table cells are not editable (`ReadOnlyTableModel`, `WatchlistView.java:410`), which removes a whole
class of "I clicked in the table and something changed" confusion; the watchlist is modified only
through the clearly labelled buttons.

## 4. Perceptible Information

*The design communicates necessary information effectively to the user, regardless of ambient
conditions or the user's sensory abilities.*

This is the principle we designed hardest against, because it is where a Swing application most
easily fails colour-blind and screen-reader users.

**Errors are never signalled by colour alone.** The error line is always prose, prefixed with the
literal word `"Error: "` (`renderError`, `WatchlistView.java:382`). The red foreground is set once at
construction and is decoration layered on top of the words — remove the colour entirely and no
information is lost. The same text is pushed into the label's accessible description, so a screen
reader announces it too.

The error line is never hidden when empty; it renders a blank space so the row keeps its height and
nothing below it jumps when an error appears or clears. Layout stability matters for users tracking
the screen with a magnifier.

**A defect we found and fixed here.** During the manual accessibility walkthrough we discovered that
`JTable` installs its own focus traversal key sets, so Tab moved between table *cells* forever
instead of leaving the table. A keyboard-only user could enter either table and never get out — and
critically, could never reach the status line, which is where every message is announced. That is a
keyboard trap under WCAG 2.1.2. It is fixed by clearing both tables' traversal key overrides so Tab
returns to the panel's own focus order (PR #27).

**Known gap:** while a network call is in flight the watchlist table is disabled, and a disabled
`JTable` in the default look and feel gives almost no visual cue, so it can read as a hang. A visible
"Loading…" state would be the correct fix.

## 5. Tolerance for Error

*The design minimizes hazards and the adverse consequences of accidental or unintended actions.*

Input is validated before anything is attempted: `TickerSymbolValidator` rejects empty, malformed and
over-long symbols, and the user gets a worded explanation rather than a stack trace or a silent
no-op. Invalid input never reaches the network.

Destructive and expensive operations are guarded. Every button is disabled for the duration of a
network call, so a user cannot fire a second request by double-clicking. "Load prices" refreshes
tickers one at a time and **stops the moment the service reports the daily request limit is
reached**, rather than burning the remainder of a roughly twenty-five request daily allowance on
calls that will fail. Price history is deliberately never hydrated automatically at start-up for the
same reason — an eight-ticker watchlist would spend eight requests the instant the window opened.

Saved data is protected against the most likely real hazard, a crash mid-write:
`FileWatchlistDataAccessObject` writes to a temporary file and atomically moves it into place, and on
load a corrupted `watchlist.dat` is backed up and recovered from rather than throwing the user out of
the application.

**Known gap:** save failures are currently invisible. `PersistenceViewModel` is bound to no view, so
if a save fails the user still sees the success message. This is logged in
`plan/handoffs/team-notes.md` and is a genuine tolerance-for-error defect, not a cosmetic one.

## 6. Low Physical Effort

*The design can be used efficiently and comfortably and with a minimum of fatigue.*

The common path is short: type a symbol, press one button. Mnemonics mean a user need not travel
between keyboard and mouse to complete a task, which matters for users with limited fine motor
control or repetitive strain injury. Selecting a ticker in the watchlist automatically populates the
ticker field, so acting on an existing row does not require retyping the symbol.

"Load prices" is one keystroke for what would otherwise be one refresh per ticker — the batching
exists specifically to remove repeated identical actions.

## 7. Size and Space for Approach and Use

*Appropriate size and space is provided for approach, reach, manipulation and use regardless of the
user's body size, posture or mobility.*

This principle is written for physical products and applies only partially to a desktop application,
which does not control the physical space it is used in. The parts that do translate are target size
and spatial arrangement: the controls are standard-sized Swing buttons with 8px padding between them
(`FlowLayout(LEFT, 8, 8)`), which keeps them comfortably clickable and reduces mis-clicks between
adjacent controls, and the split pane lets a user allocate screen space to whichever region they
need.

Where we fail this principle is the minimum-size defect noted under Flexibility in Use: below ~700px
a control is pushed outside the reachable area entirely. Setting a minimum window size and wrapping
the control strip is the fix, and it is the single highest-value accessibility change still
outstanding.

---

## Target market

We would market MarketLens to **people learning to evaluate trading strategies rather than people
executing them** — most concretely, undergraduate students in finance, economics and computer science
courses, members of university investment clubs, and self-directed retail investors who want to
sanity-check an idea before risking money on it. These users typically have some quantitative comfort
but no access to professional tools like Bloomberg terminals, which cost more per year than their
tuition. What they want is the ability to ask "if I had followed a moving-average crossover rule on
this stock for the last hundred trading days, would I have made money?" and get an honest,
reproducible answer. MarketLens is deliberately a *learning and analysis* tool, not a brokerage: it
never places a trade, never holds funds, and never asks for banking credentials, which makes it
appropriate for classroom use and lowers the stakes of using it while still teaching the underlying
concepts.

## Who may be less likely to use this program

MarketLens is most likely to be used by people who already have the resources and background that
make investing feel accessible, and that is a real limitation rather than a neutral fact. Drawing on
the embedded ethics material on **algorithmic bias and the digital divide**, three exclusions stand
out.

First, the application assumes uninterrupted broadband and a desktop computer running a JDK. Users
whose primary or only computing device is a phone, and users on metered or intermittent connections —
disproportionately lower-income users and users in rural areas — are excluded at the door by the
choice of platform, before any design decision inside the program applies.

Second, the interface is **English-only and jargon-dense**. Terms like "ticker", "crossover", "RSI",
"win rate" and "backtest" are never defined in the interface; they are assumed. This creates a
barrier for non-native English speakers and for anyone without prior exposure to financial
vocabulary, which correlates strongly with class background and with whether a user's family invested
at all. A user who does not already know what a moving-average crossover is cannot learn it from our
application, which narrows our audience to people who were already on the inside of this knowledge.

Third, and most seriously, there is a **risk of the tool being read as advice it is not qualified to
give**. Backtested returns are a well-known source of overconfidence: a strategy that would have
performed well on a hundred days of historical data tells you very little about tomorrow, and our
sample data set is deliberately small. A user without statistical training may reasonably read a
positive backtest as a prediction. This is a harm that falls hardest on inexperienced users — exactly
the students and first-time investors we are targeting. We consider the honest mitigation to be a
prominent, non-dismissible disclaimer on any results screen stating that past performance does not
predict future returns and that the sample window is short, and we would treat that as a requirement
of shipping the backtest screen rather than a nice-to-have.

A fourth, narrower point: the program is currently usable by keyboard and announces its state to
screen readers, but it has **never been tested with an actual screen reader** (NVDA, JAWS or
VoiceOver). We have written the accessible names and descriptions correctly as far as we can verify
by inspection and by keyboard walkthrough, but we should not claim screen-reader support we have not
observed. Verifying with a real assistive technology is the next thing we would do.
