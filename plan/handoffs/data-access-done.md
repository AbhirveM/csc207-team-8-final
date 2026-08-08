# Agent B (data-access) — Phase 5 hand-off

**Branch:** `phase5/agent-b` (worktree, based on `2be20e1`)
**Scope this phase:** warning **W2-7** only. Nothing else was touched.

---

## What was built

`AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment()` was split in two:

- `apiKeyFrom(String rawValue)` — new, **package-private, static, pure**. Carries the
  whole key policy: null or blank means "no key configured", anything else is returned
  `strip()`ped.
- `apiKeyFromEnvironment()` — now a one-line delegate,
  `return apiKeyFrom(System.getenv(API_KEY_ENV_VARIABLE));`. Public signature and
  behaviour are unchanged, so `app/Main.java` (orchestrator-owned) needs no edit.

Both are javadoc'd, and `apiKeyFromEnvironment`'s javadoc states *why* the split exists:
the environment read is the one line a test cannot control, so it is isolated alone and
the testable policy is pinned directly next to it.

Four new tests in `AlphaVantageMarketDataAccessObjectTest`, under a new
`// --- API key policy ---` section:

- `aNullEnvironmentValueYieldsNoApiKey` — null → empty (the unset-variable case)
- `aBlankEnvironmentValueYieldsNoApiKey` — `""`, `"   "`, `"\t"`, `"\n"`, `" \t\n "` → empty
- `aNormalEnvironmentValueYieldsThatExactKey` — present, exact value
- `aPaddedEnvironmentValueYieldsTheStrippedKey` — `"  ABC123XYZ  "` and `"\tABC123XYZ\n"`
  → present and **stripped**. This is the case Phase 4's gateway selection depends on.

**`apiKeyFromEnvironment()` is still uncovered, on purpose.** That is the correct
outcome, not a gap: covering it requires reading the real environment and branching on
what is there, which is verbatim the pattern defect D13 / warning W2 already deleted from
this test file once and `agents/reviewer.md` bans. The uncovered surface is now one line
instead of four, which is the whole point of W2-7.

## Files modified

- `src/main/java/data_access/AlphaVantageMarketDataAccessObject.java`
- `src/test/java/data_access/AlphaVantageMarketDataAccessObjectTest.java`

No files created other than this note. No new dependency. No test touches the network.

## Numbers

`mvn -o clean verify` — **green**, both before and after.

| | before | after |
|---|---|---|
| tests | 403 | **407** |
| `AlphaVantageMarketDataAccessObject` lines | 82 / 86 (95%), 4 missed | **86 / 87 (99%), 1 missed** |
| `AlphaVantageMarketDataAccessObject` instructions | 96% | **98%** |
| `AlphaVantageMarketDataAccessObject` branches | 82% (7 missed) | **92% (3 missed)** |
| project lines | 1004 / 1403 = 71.56% (399 missed) | **1008 / 1404 = 71.79% (396 missed)** |

Net **+3 lines of margin** over the 70% target — a genuine coverage gain, measured in the
JaCoCo report rather than assumed. This slightly eases warning W4-10's thin margin.

## Open needs

None. Nothing was requested from another agent's domain; no
`plan/handoffs/data-access-needs.md` was needed.

## For the next phase

- **W2-4 (import order) is still open in this file** and was left alone deliberately, to
  keep this diff narrow for review. `AlphaVantageMarketDataAccessObject.java` and its test
  still order imports project-first instead of `java.*`-first. W2-1, W2-2, W2-3, W2-5,
  W2-6 and W2-12 are likewise untouched and still open.
- `apiKeyFrom` is package-private by design. If a future caller outside `data_access`
  needs it, widen it deliberately rather than by accident — `apiKeyFromEnvironment()` is
  the intended public surface.
- The remaining 1 uncovered line in this class is `apiKeyFromEnvironment()`'s delegate
  call. It is the correct floor. It belongs in `plan/handoffs/coverage.md` as a documented
  exclusion alongside the Swing views and the composition root.
