# Accessibility Report — MarketLens

Team 8, CSC207. This report covers the seven Principles of Universal Design as they apply to
MarketLens, followed by our target market and a discussion of who may be excluded.

**Scope note, stated up front so nothing below is misread.** All five screens are now reachable in
the running application: `Main` registers the watchlist, both strategy-configuration screens, the
backtest and the comparison view with `MainView.addView`, and the navigation bar reaches each of
them by button and by the `F1`–`F5` keys. An earlier version of this report limited its claims to
the **Watchlist** screen (`view/WatchlistView.java`) because the others were tested code with no
user path into them; that is no longer true and the claims below are made for the screens a user
can actually open. The watchlist remains the screen with the deepest accessibility work, and it is
still the one we cite most often, because it is where the keyboard walkthrough was performed.

---

## 1. Equitable Use

*The design is useful and marketable to people with diverse abilities.*

Every action on the watchlist screen is reachable by both mouse and keyboard, and neither route is
degraded relative to the other. Each of the four controls carries a mnemonic (`Alt+T` to reach the
ticker field, `Alt+A` add, `Alt+M` remove, `Alt+R` refresh, `Alt+L` load prices), and an explicit
focus traversal policy walks the screen in its reading order rather than in the arbitrary order
components happened to be added (`installFocusOrder`, `WatchlistView.java:304`). A keyboard-only user
and a mouse user reach the same functions by equivalent means, rather than the keyboard being a
lesser fallback path.

Every interactive component also carries a programmatic accessible name for screen readers — the
ticker field, both tables, and the status and error lines all call
`getAccessibleContext().setAccessibleName(...)` — so assistive technology announces the same
information a sighted user reads from the visible labels.

## 2. Flexibility in Use

*The design accommodates a wide range of individual preferences and abilities.*

The two data tables sit in a `JSplitPane` with a draggable divider (`buildSplitPane`,
`WatchlistView.java:206`), so a user can give whichever table they care about more of the window —
useful for someone running a magnifier or a large system font. The window itself is resizable and
the layout reflows.

Loading price history is user-driven rather than automatic, through the "Load prices" button, which
lets a user work at their own pace instead of the application deciding when to spend their time and
their API quota.

**A gap we found and closed:** below roughly 700px of window width the "Load prices" button clipped
off the right edge of the control strip and became unclickable, because `MainView` set no minimum
window size. We found it during the manual walkthrough (`plan/handoffs/walkthrough.md` step 7)
rather than by inspection, which is the argument for walking the interface by hand at all.
`MainView` now sets `setMinimumSize(new Dimension(820, 500))`, so the window cannot be resized to a
width that pushes a control out of reach.

## 3. Simple and Intuitive Use

*Use of the design is easy to understand, regardless of the user's experience, knowledge, language
skills, or current concentration level.*

The screen is arranged in one top-to-bottom reading order: controls, then the labelled data
regions, then the status and error lines. Every region sits under a visible heading ("Watchlist",
"Close price", "Daily prices") which is bound to its content with `setLabelFor`, so the heading is
both a visual and a programmatic label rather than decoration. The close-price chart and the
daily-price table are deliberately kept together on the right-hand side of the split, because both
describe the selected ticker and the watchlist table on the left does not — the layout groups what
changes together.

The view contains no business logic and composes no prose: every value arrives from the presenter
already formatted as a display-ready string. In practice this means the wording a user sees is
written in one place and is consistent everywhere, rather than being assembled ad hoc at several
call sites. Feedback is phrased as a complete sentence — "Added AAPL with 120 days of price
history." — rather than as a code or a bare status flag.

Table cells are not editable (`ReadOnlyTableModel`, `WatchlistView.java:496`), which removes a whole
class of "I clicked in the table and something changed" confusion; the watchlist is modified only
through the clearly labelled buttons.

## 4. Perceptible Information

*The design communicates necessary information effectively to the user, regardless of ambient
conditions or the user's sensory abilities.*

This is the principle we designed hardest against, because it is where a Swing application most
easily fails colour-blind and screen-reader users.

**Errors are never signalled by colour alone.** The error line is always prose, prefixed with the
literal word `"Error: "` (`renderError`, `WatchlistView.java:468`). The red foreground is set once at
construction and is decoration layered on top of the words — remove the colour entirely and no
information is lost. The same text is pushed into the label's accessible description, so a screen
reader announces it too.

The error line is never hidden when empty; it renders a blank space so the row keeps its height and
nothing below it jumps when an error appears or clears. Layout stability matters for users tracking
the screen with a magnifier.

**The visual restyle kept this rule and extended it to three new surfaces.** Nothing added by the
restyle carries meaning in colour alone:

- **Signed figures** — the returns columns on the backtest and comparison screens are rendered with
  an explicit `+` or `-` in front of every value, and the up/down colour is applied on top of the
  sign rather than instead of it (`TableStyler.SignedRenderer`). On a selected row, where the accent
  fill would put the direction colour below contrast, the colour is dropped and the sign alone
  carries the meaning.
- **The active screen in the navigation bar** — marked with a two-pixel accent rule, a bold weight,
  *and* the accent foreground, so the current screen is identifiable without distinguishing amber
  from grey (`MainView.markActiveView`). The screens are also numbered `F1` to `F5` in their labels,
  and those keys really are bound, so the current screen can be reached without reading the bar at
  all.
- **The window's save-status line** — a failed save is now prefixed with the literal word
  `"Error: "` before the colour is applied, matching what the watchlist error line already did, and
  the same text is pushed into the label's accessible description
  (`MainView.setPersistenceStatus`).

### The two charts

Adding graphics to a program that was entirely text is the largest perceptible-information change
we have made, and it is the one most likely to quietly exclude someone, so it is recorded in full.
Two charts were added, both drawn by the same component (`view/LineChart.java`): a **close-price
line** on the Watchlist, above the daily-price table, and an **equity curve** on the Backtest
results, between the summary metrics and the trade log.

Neither chart is the only place its information exists. That is the design rule they were built
to, and it is what keeps them from being a barrier:

| | Close price | Portfolio value |
|---|---|---|
| Accessible name | `"Close price"` | `"Portfolio value"` |
| Spoken description | `"Close price for AAPL, 120 days, low 216.74, high 262.46, latest 249.68, +38.94 (+18.53%) over the window."` | `"Portfolio value over 120 days, $10000.00 to $11240.00, +12.40%."` |
| Visible readout in the band | `120D +38.94 (+18.53%)` | `120D +12.40%` |
| Visible on the plot itself | the low and high in the gutter, the first and last date on the foot | the same |
| Underlying figures | the daily-price table directly beneath, every OHLCV row in full | the six summary metrics above and the complete trade log below |

The spoken description and the visible band readout are deliberately *different lengths of the
same fact* rather than the same string twice. The band is a fixed-height strip a few words wide,
sitting beside the region title; the first version of this work put the whole sentence there and
it painted straight over the title, which left the title present in the component tree and
invisible on screen — a worse accessibility outcome than the one it was trying to achieve, and
one that only showed up on a real display. The band now carries the signed direction, which is
the part that has to be visible for the line's colour to be redundant, and the full sentence
goes to the screen reader. `PanelHeaderTest` pins both halves: that a long readout can never
overlap a title again, and that a short one still sits hard against the right edge.

Four points about how this is built:

- **The description is not set once and forgotten.** `LineChart.setSeries` writes the summary onto
  the accessible description every time the plotted data changes, so what a screen reader announces
  cannot drift away from what is drawn. `ViewConstructionTest` asserts this on the watchlist chart.
- **The sentence is composed by a presenter, never by the chart.** Both summaries are built in the
  interface-adapter layer (`WatchlistPresenter.chartFor`, `BacktestPresenter.curveFor`) alongside
  every other piece of user-facing prose, which is what keeps the wording consistent with the
  status lines and table cells around it.
- **The series colour is direction only, and it is redundant.** A line whose last value is above
  its first is drawn in `UP`, below in `DOWN`, and level in `FG` — exactly the rule
  `TableStyler.SignedRenderer` applies to a signed cell, lifted from a cell to a line. The same
  direction is stated with an explicit `+` or `-` in both the visible band readout and the
  spoken description. This is the identical argument we make for the signed columns above:
  remove the colour entirely and no information is lost. The `UP`/`DOWN` ratios in the table below
  therefore cover the plotted line as well as the table cells.
- **Both charts are focusable and are in the reading order.** `closeChart` sits between the
  watchlist table and the price table in `WatchlistView.installFocusOrder`, which is where it sits
  on screen, so a keyboard-only user reaches it in the order they would read it rather than having
  it skipped as decoration.

**A limitation we are not going to paper over.** A line chart communicates *shape* — the drawdown
partway through, whether a gain was steady or one lucky trade — and a one-sentence summary
communicates only the endpoints and the bounds. A user relying on the description gets the facts
but not the shape. The honest mitigation, which we have not implemented, is a textual description
of the path itself (direction changes, largest drawdown) rather than only its extremes. What we
have ensured is that nothing is *only* in the chart: every number plotted is on screen as text in
the table beside it.

### Measured contrast

The interface is amber on near-black. Colours are contrast-checked rather than eyeballed: `ThemeTest`
asserts the ratios below against the surface each colour is actually drawn on, so a token that drifts
fails the build rather than relying on someone remembering to re-measure. Ratios are WCAG 2.1
relative-luminance, computed by the same formula the test uses.

| Foreground | Surface | Ratio | AA 4.5:1 |
|---|---|---|---|
| `FG` `#E8E8E8` — values | `BG` `#0A0A0A` | 16.16 | pass |
| `FG` | `FIELD_BG` `#121212` — inputs | 15.29 | pass |
| `FG_MUTED` `#9AA0A6` — secondary text | `BG` | 7.50 | pass |
| `FG_MUTED` | `CHROME` `#141414` — bars and bands | 6.98 | pass |
| `ACCENT` `#FF9E1B` — headings, active screen | `BG` | 9.58 | pass |
| `ACCENT` | `CHROME` | 8.91 | pass |
| `ACCENT_FG` `#0A0A0A` — text on a selected row | `ACCENT` | 9.58 | pass |
| `KEY` `#4FC3F7` — field labels | `BG` | 9.88 | pass |
| `KEY` | `CHROME` | 9.20 | pass |
| `UP` `#26A65B` — positive change | `BG` | 6.31 | pass |
| `UP` | `ROW_ALT` `#101013` — striped rows | 6.05 | pass |
| `DOWN` `#E5484D` — negative change | `BG` | 5.06 | pass |
| `DOWN` | `ROW_ALT` | 4.85 | pass |
| `FG_FAINT` `#6B7075` — placeholder, disabled | `BG` | 3.96 | exempt |

`FG_FAINT` is the one value below the threshold. It is used only for placeholder text, disabled
controls, and the punctuation between status-bar segments — never for text a user has to read to
operate the program — which is the exemption WCAG 1.4.3 grants for inactive components. Nothing it
marks is the only way to reach a piece of information.

Half the rows in every table are `ROW_ALT` rather than `BG`, so the direction colours are measured
against both. Checking only the base surface would leave every second row unverified.

Table cells gained interior padding during the restyle, and the focused cell keeps the look and
feel's focus highlight nested inside that padding rather than having it replaced — the highlight is
the only thing showing a keyboard user which cell they are on
(`TableStyler.applyPadding`).

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

**A gap we found and closed:** save failures used to be invisible. `PersistenceViewModel` was bound
to no view, so if a save failed the user still saw the success message — a genuine
tolerance-for-error defect, not a cosmetic one. `Main` now subscribes to the view model's status
property and forwards it to a persistence status line in `MainView`, so a failed save says so on
screen instead of failing silently.

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
and spatial arrangement: the controls are standard-sized Swing buttons separated by a consistent 8px
gap (`Theme.SM` struts in the control row's `Box`, which replaced a centring `FlowLayout` during the
restyle), which keeps them comfortably clickable and reduces mis-clicks between adjacent controls,
and the split pane lets a user allocate screen space to whichever region they need.

One genuine trade-off the restyle made here: table rows are now 22px rather than the look and feel's
default. That density is deliberate for a table of figures, but it does make a row a slightly smaller
mouse target, and it is the change most worth revisiting if a user reports difficulty selecting rows.
Cell padding was kept at 8px each side rather than tightened further, precisely because the row is
already shorter.

**A new known gap, found by rendering the window at its minimum size.** At the 820px minimum width
the daily-prices table cannot fit six columns of figures, and every price truncates to `24...`. The
figures are still reachable — the split-pane divider is draggable and the window is resizable — but
a user working at the minimum size sees a table of ellipses rather than numbers, which is worse than
a truncated company name because a partial figure is not merely shortened, it is misleading. The
honest fix is to drop or combine a column below a threshold width rather than to shrink the type.

The one way we used to fail this principle was the minimum-size defect noted under Flexibility in
Use: below ~700px a control was pushed outside the reachable area entirely. `MainView` now sets a
minimum window size of 820×500, which is the floor at which every control in the widest screen
still fits. It is worth saying plainly that we did not find this by reading the layout code — we
found it by resizing the window during a manual walkthrough.

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
