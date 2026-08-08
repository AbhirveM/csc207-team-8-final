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

6. **The restart round trip — the one nothing has verified.**
   - [ ] Close the app. Confirm `watchlist.dat` exists in the repo root.
   - [ ] Relaunch. The watchlist renders `AAPL` immediately, with no click needed.
   - [ ] Note: prices are *not* persisted by design — only tickers are. A restored row
         showing "Not loaded" until you Refresh is correct behaviour, not a bug.
   - ⚠ This step also silently exercises **W4-9**: if the save had failed, the app would
     have said "Added AAPL…" anyway and you would only find out here. If the row does not
     come back, that is the bug W4-9 predicts, not a load bug.

7. **The visual checks a harness cannot make.**
   - [ ] **Resize** the window, small and large. Tables and buttons should reflow without
         clipping or the price table swallowing the controls.
   - [ ] **Tab order** matches visual order — top to bottom, left to right, no surprises.
   - [ ] **Mid-refresh freeze (W3-5).** Start a Refresh and immediately try to click a
         different row and press ↓. Both should be ignored while the worker runs. That is
         correct and deliberate — but note **W4-5**: the frozen table gives no visual cue,
         so it reads as a hang. And **W4-4**: Tab dies on the disabled table while the
         worker is in flight. Both are known and logged; you are confirming the behaviour,
         not fixing it.
   - [ ] **W4-1, still open.** Start a Refresh, and while it runs, type the next symbol
         into the ticker field. Your typing will be lost when the success state lands. Known
         and deliberately not claimed closed — worth knowing before you do it live on stage.

8. **The "after" screenshot.**
   - [ ] Full application window, default size, Watchlist card selected, with `AAPL`
         populated and the price table full — the mirror of the "before" shot.
   - [ ] Save it **outside the repo** (screenshots do not belong in version control here).
   - [ ] Record the absolute path, the date, and the git SHA below.

---

## "After" screenshot record

- **Path:** _(to fill in)_
- **Captured on:** _(to fill in)_
- **Git SHA at capture:** _(to fill in)_

---

## If something fails

Everything in steps 1–5 is already covered by automated tests, so a failure there means the
wiring in `app/Main.java` diverged from what the tests exercise — check the composition root
before suspecting the use cases. Steps 6 and 7 are genuinely unverified territory; a failure
there is a real finding and belongs in `plan/decisions.md`.
