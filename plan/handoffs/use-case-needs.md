# Agent A (use-case) — open requests

Requests for changes outside this agent's ownership. Filed rather than made, per
`agents/use-case.md` § Never Touch.

---

## A-N1 — `AddTickerOutputData` needs to carry the company-name failure kind (blocks half of D5)

**Owner of the file:** orchestrator (`src/main/java/use_case/watchlist/AddTickerOutputData.java`,
listed as an orchestrator carve-out, read-only to Agent A).

**Why.** D5 / W4 asks Agent A to "thread `exception.getKind()` into the outcome so the
user can tell 'no name available' from 'we were rate-limited'". The only vehicle from
`AddTickerInteractor` to the presenter is `AddTickerOutputData`, and it is frozen. Every
other half of D5 is done in commit `D5:` — the `catch` no longer performs a redundant
assignment, the variable is named `exception`, and the absent name is now `""` rather
than `null`. What cannot be done from inside Agent A's ownership is the last hop.

Today an ETF with genuinely no `OVERVIEW` record and a symbol whose `OVERVIEW` call was
refused for quota are indistinguishable at the presenter: both arrive as
`companyName == ""`, `isCompanyNameAvailable() == false`. The first deserves silence;
the second deserves "company name unavailable — provider quota reached".

**Requested shape** — additive, and `null`-tolerant so no existing call site breaks:

```java
/**
 * @param companyNameFailureKind why the company name could not be fetched, or null
 *                               when it was simply absent (or was fetched fine)
 */
public AddTickerOutputData(String addedSymbol, String companyName, int priceCount,
                           WatchlistSnapshot snapshot,
                           MarketDataException.Kind companyNameFailureKind)

/** @return why the name is missing, or null when nothing failed. */
MarketDataException.Kind getCompanyNameFailureKind()
```

Keeping the existing four-argument constructor as a delegate (`this(..., null)`) makes
this a pure addition.

**Agent A's side once it lands** — two lines in `AddTickerInteractor`: assign
`companyNameFailureKind = exception.getKind()` in the `catch`, and pass it as the fifth
constructor argument. The `catch` block in `AddTickerInteractor.java` marks the exact
spot.

**Agent C (adapter) impact.** Additive. Agent C's failure table (`agents/adapter.md`
§Interface Contract) would gain one more *success-with-caveat* status string, not a new
failure row — the add still succeeds. If the orchestrator would rather not extend the
contract, Agent C should be told explicitly that this distinction is unavailable so it
does not promise it in the status prose.

**Status:** open.

---

## A-N2 — `ShowWatchlistOutputBoundary` javadoc contradicts D6 (documentation only)

**Owner of the file:** orchestrator
(`src/main/java/use_case/watchlist/ShowWatchlistOutputBoundary.java`).

`prepareFailView`'s javadoc says it is "Present for symmetry with the other three use
cases and **for the null `inputData` case**". D6 instructs all four `execute` methods to
open with `Objects.requireNonNull(inputData, "Input data cannot be null")`, so a null
`inputData` throws `NullPointerException` and never reaches `prepareFailView`. Agent A
followed D6 literally, as instructed, and `ShowWatchlistInteractorTest` pins the NPE.

The consequence is that, as designed, **`ShowWatchlistOutputBoundary.prepareFailView`
has no caller at all** — Show Watchlist has no other failure mode: an unknown selected
symbol degrades silently to `""` by contract. That is a deliberate symmetry choice, but
it should be stated as such rather than justified by a case that cannot occur.

Suggested replacement for the second sentence: "Present for symmetry with the other
three use cases. Show Watchlist has no failure mode of its own — an unrecognized
selection degrades silently to no selection, and a null `inputData` is a wiring error
that fails fast — so no interactor currently calls this."

**Agent C impact.** Agent C must still implement the method (it is on the interface),
but should not expect the interactor to drive it, and should not write a test that
asserts Show Watchlist produces a `WatchlistFailure`.

**Status:** open.

---

## A-N3 — `WatchlistSnapshot` price rows now follow watchlist membership (notice, not a request)

Not a request; recorded here so the reviewer and Agent C see it in one place.

`WatchlistSnapshotFactory.build` used to resolve `stocks.findBySymbol(selectedSymbol)` a
second time, independently of the ticker loop. Tidying that double lookup (an explicit
Phase 2 task) means the selected price rows are now taken from the stock found *while
walking the watchlist*.

Observable difference: a `selectedSymbol` that has stored prices but is **not on the
watchlist** used to yield price rows; it now yields none, while
`snapshot.getSelectedSymbol()` still echoes the requested symbol. No caller does this —
add and refresh always select a symbol they just confirmed is on the watchlist, remove
selects `""`, and show degrades an off-watchlist selection to `""` before building. The
new behaviour is also the more defensible one: the snapshot describes the watchlist.

**Status:** informational.
