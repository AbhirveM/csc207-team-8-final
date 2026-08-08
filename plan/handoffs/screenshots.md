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
java -cp "target/classes;C:\Users\abhir\.m2\repository\org\json\json\20240303\json-20240303.jar" app.Main
```

**Corrected in Phase 4.** This file previously suggested
`mvn -o dependency:build-classpath -Dmdep.outputFile=target/cp.txt -q` to assemble the
classpath. That does not work here: `maven-dependency-plugin` is not in the local repository
and `-o` forbids fetching it, and `mvn exec:java` fails the same way. Naming the one runtime
dependency directly avoids both. `org.json` is the only non-test dependency in `pom.xml`;
if that ever changes, this line has to change with it.

`ALPHA_VANTAGE_API_KEY` does not need to be set — nothing on the current `main` behaviour
reads it. The window should show **only** the Compare Strategies tab, with no Watchlist nav
button. That absence is the whole point of the shot.

## What to capture

The full application window, at default size, with the Compare Strategies tab selected.

## Where it lives

Save it **outside the repo** (screenshots do not belong in version control here) and record
the absolute path below.

- **Path:** `C:\Users\abhir\Pictures\Screenshots\Screenshot 2026-08-08 113636.png`
- **Captured on:** 2026-08-08
- **Git SHA at capture:** `fc27b3c`
