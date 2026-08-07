# Presentation artifact — the "before" screenshot

`vision.md` §8 requires a screenshot of the app **as it looks before the watchlist view is
wired in**. Phase 4 uncomments the nav button in `view/MainView.java` and registers
`WatchlistView` from `app/Main.java`; once that lands, this artifact cannot be recreated
without reverting.

**Capture it before Phase 4 starts.** Agent D was told not to attempt it — see
`plan/decisions.md` D3-b.

## How to run the app as it stands

The repo has no `exec-maven-plugin`, so either:

- run `app.Main` from IntelliJ (Run ▸ `Main`), or
- from the repo root:

```
mvn -o clean compile
mvn -o dependency:build-classpath -Dmdep.outputFile=target/cp.txt -q
java -cp "target/classes;$(cat target/cp.txt)" app.Main
```

`ALPHA_VANTAGE_API_KEY` does not need to be set — nothing on the current `main` behaviour
reads it. The window should show **only** the Compare Strategies tab, with no Watchlist nav
button. That absence is the whole point of the shot.

## What to capture

The full application window, at default size, with the Compare Strategies tab selected.

## Where it lives

Save it **outside the repo** (screenshots do not belong in version control here) and record
the absolute path below.

- **Path:** _(to be filled in by the owner)_
- **Captured on:** _(date)_
- **Git SHA at capture:** _(output of `git rev-parse --short HEAD`)_
