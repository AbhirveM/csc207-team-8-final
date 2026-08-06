# Agent B — open needs

**None. All Agent B needs are closed.**

| # | Need | From | Status |
|---|---|---|---|
| N1 | A single `Locale.ROOT` symbol-normalization helper in `use_case`, so the copies of the `key(String)` idiom in `data_access` could delegate to it instead of duplicating it. | Agent A (D9) | **CLOSED** — helper landed in `1144daa`, consumed in `b7bb19c` |

## N1 — closed

Agent A added `TickerSymbolValidator.normalizeKey(String)` (public static,
`Objects.requireNonNull` then `toUpperCase(Locale.ROOT)`, never null, idempotent, folds
case only) in commit `1144daa`, and pinned the locale with a `tr-TR` default-locale test.

In `b7bb19c` all four copies of the idiom in `data_access` were collapsed onto it:
`CachingMarketDataGateway.key`, `InMemoryMarketDataGateway.key`,
`InMemoryStockRepository.key`, and the symbol fold that seeds
`InMemoryMarketDataGateway.syntheticSeries`. No `toUpperCase` and no `java.util.Locale`
import remains in `data_access` outside `AlphaVantageMarketDataAccessObject`, where
`Locale` is used to lower-case *provider messages* for matching, not to build keys.

No port signature change was ever needed, so `plan/handoffs/agent-b-request.md` was not
written.
