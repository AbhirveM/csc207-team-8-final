# Phase 2 — Core Remediation and Interactor Tests

**Active agents:** `use-case` (A) ∥ `data-access` (B), then reviewer.
**Goal:** close every audited defect below the adapter layer and get the four interactors
— the graded heart of this vertical — above 90% line coverage.

**Before spawning:** provision a separate git worktree per agent. A and B otherwise share
`target/` and clobber each other's JaCoCo `.exec` files (hazard H4). If worktrees are
unavailable, run A to completion, then B.

---

## Dependencies to install

None.

---

## Defect assignment — each defect belongs to exactly one agent

| ID | Defect | Owner |
|---|---|---|
| D1 | `parseCompanyName` javadoc promises `Optional.empty()` for a `{}` OVERVIEW body; code throws `EMPTY_RESPONSE` | B |
| D2 | `parseDailyPrices` returns a mutable list that the cache aliases; a test `assertSame`s the bug into place | B |
| D3 | Cache uses plain `HashMap` under an explicitly-anticipated background thread; no eviction, unbounded name cache, non-positive TTL accepted | B |
| D4 | `Stock`'s `IllegalArgumentException` escapes `execute` in Add and Refresh, unmapped | A |
| D5 | `AddTickerInteractor` discards the caught `MarketDataException` entirely | A |
| D6 | No interactor null-checks `inputData` | A |
| D7 | Inverted ordering comment in `TickerSymbolValidator` | A |
| D8 | `MarketDataGateway` null/blank contract unimplemented in all three gateways | B |
| D9 | ~12-line validate/normalize/contains preamble copy-pasted across three interactors | A |
| D12 | Dead code: cache `clear`/`getCachedSymbolCount`, `StubHttpJsonClient.getRequestedUrls`, both `Recording*` doubles | B (data_access) + A (the doubles) |
| D13 | Tautological env-var test; `BRK.B` URL-encoding test proves nothing | B |

D10 and D11 were closed in Phase 1 by the orchestrator.

---

## Agent A — files

**Modify:** `AddTickerInteractor`, `RefreshTickerInteractor`, `RemoveTickerInteractor`,
`TickerSymbolValidator`, `WatchlistSnapshotFactory`.
**Create:** `ShowWatchlistInteractor`; a package-private `WatchlistInputSupport` for the
extracted preamble; `AddTickerInteractorTest`, `RemoveTickerInteractorTest`,
`RefreshTickerInteractorTest`, `ShowWatchlistInteractorTest`.
**Extend:** `RecordingWatchlistPresenter` to implement `ShowWatchlistOutputBoundary` and
to use per-use-case counters instead of one aggregated `successCount`.

Full task detail: `agents/use-case.md` § Phase Tasks.

Key points:
- `ShowWatchlistInteractor` takes `(Watchlist, StockRepository, ShowWatchlistOutputBoundary)`
  — no gateway, no `SaveWatchlist`, no I/O.
- Do **not** collapse the three `*InputData` classes. The five-file flat convention is a
  deliberate, rubric-visible choice.
- The `TickerSymbolValidator` fix is to the **comment**, not the code — the code checks
  the character set first, which is correct.

## Agent B — files

**Modify:** `AlphaVantageMarketDataAccessObject`, `CachingMarketDataGateway`,
`InMemoryMarketDataGateway`, `InMemoryStockRepository`, `StubHttpJsonClient`,
`JsonFixtures`, `AlphaVantageMarketDataAccessObjectTest`,
`CachingMarketDataGatewayTest`, `InMemoryStockRepositoryTest`.
**Create:** `src/test/resources/alphavantage/overview_unknown_symbol.json` (a bare `{}`).

Full task detail: `agents/data-access.md` § Phase Tasks.

Key points:
- On D1, **the javadoc is right and the code is wrong** — a `{}` OVERVIEW body must yield
  `Optional.empty()`, because a missing company name may never block adding a ticker.
- On D2, flipping the DAO to `List.copyOf` requires *deleting* the `assertSame` at
  `CachingMarketDataGatewayTest:125` and replacing it with an unmodifiability assertion.
- Keep `StockRepository.findAll()` — it is Members 2/3's hand-off surface. Test it.
- Confirm the fake's sample series genuinely oscillates enough to produce a crossover at
  the default MA windows; Phase 5's hand-off test depends on it.

---

## Verification

1. `mvn -o clean verify` — green (orchestrator runs this once, after merging both
   worktrees).
2. JaCoCo line coverage ≥90% on: `AddTickerInteractor`, `RemoveTickerInteractor`,
   `RefreshTickerInteractor`, `ShowWatchlistInteractor`,
   `AlphaVantageMarketDataAccessObject`, `CachingMarketDataGateway`.
3. Every defect ID above appears in a commit message.
4. `grep -rn "JdkHttpJsonClient" src/test` — empty.
5. `RecordingWatchlistPresenter` and `RecordingSaveWatchlist` have real callers.
6. `git diff --name-only` shows no file touched by both agents.
7. Spot-check the fetch-before-mutate test: a provider failure on add leaves
   `watchlist.getEntries()` empty.
8. Reviewer writes `plan/review-phase-2.md`; status is not FAIL.
