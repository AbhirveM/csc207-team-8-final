# Phase 1 — Contract Freeze

**Active agents:** orchestrator only, then reviewer.
**Goal:** every byte that crosses an agent boundary exists and compiles, so Phases 2 and
3 can fan out without either agent waiting on the other's file.

---

## Dependencies to install

None. `org.json:json:20240303` and `jacoco-maven-plugin:0.8.13` are already in `pom.xml`.
No `pom.xml` edit is needed in this plan at all — which is the ideal outcome given it is a
shared file with an open PR against it.

---

## Files to modify

| File | Change |
|---|---|
| `use_case/watchlist/WatchlistFailure.java` | make `final`; add `equals`/`hashCode`/`toString` |
| `use_case/watchlist/WatchlistSnapshot.java` | make `final`; add `equals`/`hashCode`; named `requireNonNull` before each `List.copyOf` |
| `use_case/watchlist/MarketDataGateway.java` | javadoc only — the normative null/blank contract |
| `use_case/watchlist/AddTickerOutputData.java` | collapse two company-name fields into one |
| `use_case/watchlist/AddTickerInteractor.java` | **one-line call-site fix only**, to keep the build green |

## Files to create

| File | Purpose |
|---|---|
| `use_case/watchlist/ShowWatchlistInputBoundary.java` | `void execute(ShowWatchlistInputData)` |
| `use_case/watchlist/ShowWatchlistInputData.java` | carries `getSelectedSymbol()`; `""` means none |
| `use_case/watchlist/ShowWatchlistOutputBoundary.java` | `prepareSuccessView(ShowWatchlistOutputData)`, `prepareFailView(WatchlistFailure)` |
| `use_case/watchlist/ShowWatchlistOutputData.java` | `getTickerCount()`, `getSnapshot()` |

---

## What to implement

### 1. Value semantics on the two cross-boundary types (defect D11)

`WatchlistFailure` and `WatchlistSnapshot` are currently non-final with no `equals`. Agent
C's presenter tests need to compare failures, and a presenter cannot dedupe or diff state
without them. Make both `final` and give both `equals`/`hashCode`; give `WatchlistFailure`
a `toString` too, so a failing assertion prints something readable.

`WatchlistSnapshot`'s constructor calls bare `List.copyOf(...)`, which throws NPE with an
opaque message. Precede each with
`Objects.requireNonNull(tickerRows, "Ticker rows cannot be null")` so it fails like every
other class in the package.

### 2. The gateway null/blank contract (defect D8)

Javadoc on `fetchDailyPrices`, `fetchDailyPricesFresh` and `fetchCompanyName`:

> `normalizedSymbol` must be non-null; implementations reject `null` with
> `NullPointerException`. A blank symbol must be rejected with `MarketDataException` of
> kind `INVALID_SYMBOL`, must not reach the network, and must never be cached.

No signature changes. Agent B implements this in all three gateways in Phase 2.

This exists because the three implementations currently disagree: one NPEs out of
`URLEncoder`, one caches under `""`, one returns `INVALID_SYMBOL`.

### 3. `AddTickerOutputData` reshape (defect D10)

Today it carries a nullable `companyName` **and** a `boolean companyNameAvailable`, so
`new AddTickerOutputData("X", "Apple", false, ...)` compiles and is nonsense. Collapse to
a single non-null `String companyName` that is `""` when unavailable, and derive
`isCompanyNameAvailable()` as `return !companyName.isEmpty();`.

Then make the corresponding one-line change at the construction site in
`AddTickerInteractor`. **This is the only time the orchestrator edits an agent-owned
file** — it is compile-preserving, and Agent A branches from the resulting commit.

Doing this now rather than in Phase 3 is deliberate: the shape change breaks Agent A and
Agent C simultaneously, and they run in different phases.

### 4. The four `ShowWatchlist*` files

Exactly as specified in `agents/orchestrator.md` §5.2. Contract files only — the
interactor is Agent A's, in Phase 2.

Why the orchestrator writes these rather than Agent A: Agent C's presenter must implement
`ShowWatchlistOutputBoundary` in Phase 3, and Agent A's interactor must call it in Phase
2. If A owned the files, C could not compile until A finished, serialising two phases that
should be independent. Authoring the ports centrally breaks the cycle (hazard H2).

---

## Verification

1. `mvn -o clean test` — green.
2. `WatchlistFailure` and `WatchlistSnapshot` are `final` and have `equals`/`hashCode`.
3. `AddTickerOutputData` has exactly one company-name field.
4. All four `ShowWatchlist*` files compile.
5. `git diff --stat` shows exactly the nine files above and nothing else — in particular
   nothing under `data_access/`, `interface_adapter/`, `view/`, `app/`, or `entity/`.
6. Commit on `feature/watchlist-use-cases`; record the SHA in `plan/status.md`.
7. Reviewer writes `plan/review-phase-1.md`; status is not FAIL.
