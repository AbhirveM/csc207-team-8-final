# Manual walkthrough — owed by the owner, by hand

`vision.md` §8 defines "done" for this vertical as a script a human runs. Most of it is
already machine-verified: a Phase 4 harness replayed the whole §8 sequence through the
exact object graph `app/Main.java` builds, and every step produced the right state
(deviation D4-f). What a harness could not check is everything **visual** — how the window
behaves, what focus does, and whether a real `watchlist.dat` survives a restart.

That is what this checklist is. It is the last thing owed before the individual
presentation.

**Companion file:** `screenshots.md` holds the "before" screenshot, already captured at
`C:\Users\abhir\Pictures\Screenshots\Screenshot 2026-08-08 113636.png` (SHA `fc27b3c`).
The "after" shot is step 8 below.

---

## Launch

`mvn exec:java` and `mvn -o dependency:build-classpath` both fail here — neither plugin is
in the local repository and `-o` forbids fetching one (deviation D4-c). Name the single
runtime dependency directly instead:

```
mvn -o clean compile
java -cp "target/classes;C:\Users\abhir\.m2\repository\org\json\json\20240303\json-20240303.jar" app.Main
```

Or just Run ▸ `Main` from IntelliJ.

**Do not set `ALPHA_VANTAGE_API_KEY`.** The whole point is that this runs offline. You
should see a status line saying sample data is in use, and the app should open on the
**Watchlist** card (that is deviation D4-e — it used to open on Compare Strategies).

---

## The §8 script

1. **Type `aapl`** — lower case, deliberately — and press Add.
   - [ ] The row appears as **`AAPL`**, normalized.
   - [ ] The company column reads **Apple Inc.**
   - [ ] The price table fills, **newest date first** (2026-08-05 at the top, per D4-d).
   - [ ] The ticker field clears itself.

2. **Press Refresh** with `AAPL` selected.
   - [ ] The days-of-prices count and latest date update rather than duplicating the row.

3. **Add `MSFT`, then click back on the `AAPL` row.**
   - [ ] The price table repopulates for whichever row is selected. (This is the entire
         reason the Show Watchlist use case exists — no other use case can repopulate that
         table without a network call.)

4. **Type `!!junk!!` and press Add.**
   - [ ] A *specific, worded* error appears — not a stack trace, not a generic "error".
   - [ ] The existing rows are untouched.

5. **Select `MSFT` and press Remove.**
   - [ ] The row disappears; `AAPL` survives.

6. **The restart round trip — the one nothing has verified.** ✅ **PASSED 2026-08-08**
   - [x] Close the app. Confirm `watchlist.dat` exists in the repo root. *(284 bytes, present)*
   - [x] Relaunch. The watchlist renders `AAPL` immediately, with no click needed.
         *Verified: a cold launch with `ALPHA_VANTAGE_API_KEY` unset repainted the row as
         `AAPL | Apple Inc. | Not loaded | — | —` with no interaction. The company name
         survived the round trip, so `Ticker`'s companyName is being persisted, not just the
         symbol. W4-9 did not fire — the save had genuinely succeeded.*
   - [x] Note: prices are *not* persisted by design — only tickers are. A restored row
         showing "Not loaded" until you Refresh is correct behaviour, not a bug.
         *Confirmed exactly as designed — "watchlist membership is durable, market data is
         cached and re-fetched" is observably true, not just a claim in vision.md §5.2.*
   - ⚠ This step also silently exercises **W4-9**: if the save had failed, the app would
     have said "Added AAPL…" anyway and you would only find out here. If the row does not
     come back, that is the bug W4-9 predicts, not a load bug.

7. **The visual checks a harness cannot make.**
   - [x] **Resize** — ✅ **PASSED 2026-08-08.** Driven from 900×600 to 1000×640 via
         `MoveWindow`. The nav bar stayed centred, the control row kept its left alignment,
         the split pane divider held its proportion, and both tables grew without clipping
         or swallowing the controls. *Still worth one manual drag to a very small size —
         the automated resize only covered growing the window.*
   - [x] **Tab order** — ✅ **PASSES after a fix. A real defect was found here 2026-08-08.**

         Tab reached the ticker field, the four buttons and the watchlist table in the right
         order, and then **trapped**. `JTable` installs its own focus traversal key sets so
         that Tab moves between *cells* instead of between components, so Tab cycled across
         the single AAPL row's five columns forever. A keyboard-only user could enter either
         table and never leave it — and could never reach the status line, which is where
         every error message is announced.

         This is a keyboard trap (WCAG 2.1.2), not a cosmetic ordering problem, and it
         silently defeated the `OrderedFocusTraversalPolicy` that was already installed: the
         policy was correct all along, but Tab never reached it.

         **Fix:** `WatchlistView.buildTablePanel` now clears both tables' traversal key
         overrides with `setFocusTraversalKeys(..., null)`, which makes them inherit the
         container's Tab / Shift+Tab so the existing policy runs. Arrow keys still move
         between cells, so no table navigation was lost.
   - [ ] **Mid-refresh freeze (W3-5).** Start a Refresh and immediately try to click a
         different row and press ↓. Both should be ignored while the worker runs. That is
         correct and deliberate — but note **W4-5**: the frozen table gives no visual cue,
         so it reads as a hang. And **W4-4**: Tab dies on the disabled table while the
         worker is in flight. Both are known and logged; you are confirming the behaviour,
         not fixing it.
   - [ ] **W4-1, still open.** Start a Refresh, and while it runs, type the next symbol
         into the ticker field. Your typing will be lost when the success state lands. Known
         and deliberately not claimed closed — worth knowing before you do it live on stage.

8. **The "after" screenshot.** ✅ **CAPTURED 2026-08-08.**
   - [x] Full application window, Watchlist card selected, with `AAPL` populated and the
         price table full — the mirror of the "before" shot. Shows the company name resolved
         to "Apple Inc.", 120 days of history, latest 2026-08-05 at 249.68, the full OHLCV
         table, and the worded status line "Refreshed AAPL: 120 days of price history,
         latest 2026-08-05."
   - [x] Saved as `docs/after-watchlist-view.png`.

   **Deviation from the original instruction.** This step used to say "save it outside the
   repo — screenshots do not belong in version control here." That is now reversed, and
   deliberately: the README checklist requires screenshots that render on the GitHub web page,
   and the individual rubric's "Required Elements" needs the before and after views as
   submitted evidence. An artifact that lives only on one laptop is not submitted. The two
   PNGs are ~20 KB each, which is not a repository-hygiene problem. The "before" shot is
   already committed at `docs/before-watchlist-view.png`.

   It was captured by hand. Two attempts to drive the app with synthetic mouse events
   (`mouse_event` via user32) had failed — the clicks never reached the Swing components, so
   the row never showed a selection highlight. Window capture works; input injection does not.

---

## "After" screenshot record

- **Path:** `docs/after-watchlist-view.png`
- **Captured on:** 2026-08-08
- **Git SHA at capture:** `a02affc`
- **Source:** `C:\Users\abhir\Pictures\Screenshots\Screenshot 2026-08-08 132457.png`

---

## If something fails

Everything in steps 1–5 is already covered by automated tests, so a failure there means the
wiring in `app/Main.java` diverged from what the tests exercise — check the composition root
before suspecting the use cases. Steps 6 and 7 are genuinely unverified territory; a failure
there is a real finding and belongs in `plan/decisions.md`.
