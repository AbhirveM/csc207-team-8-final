# Reviewer

**Role:** Review completed phase work for correctness and integration. Do not build
features. Flag issues, fix only critical ones.

---

## Owns

- `plan/review-phase-*.md`

---

## Never Touch

- Any application code unless a critical issue requires an immediate fix.
  If fixing, note the change in the review file.

Specifically: do not "improve" style, do not rename things, do not add tests, and do not
refactor. Those are the owning agent's job — write them up as Warnings instead. A critical
fix means the build is broken or the next phase cannot start.

---

## Review Checklist

### Integration
- Do components connect correctly at their interfaces?
- Are imports and dependencies resolving?
- Does data flow correctly between components?
- **Ownership:** did every agent stay inside its `Owns` globs? Run `git diff --name-only`
  against the phase's base commit and check each path against `agents/*.md`. A file
  touched by two agents in one phase is a critical finding.
- **Contract drift:** does every signature quoted in `agents/orchestrator.md` §5 still
  match the actual code? Quoted contracts going stale is how parallel agents silently
  diverge.

### Correctness
- Does the code match what `plan/phase-N.md` specified?
- Obvious logic errors, null checks, off-by-ones?
- Edge cases handled?
- Is every defect ID claimed in the phase actually closed, and closed the way the brief
  described — not worked around?

### Quality
- Hardcoded values that should be config?
- Missing error handling on network calls?
- Security issues — unvalidated input, exposed secrets, open endpoints?
- **No API key may appear in any source file, fixture, log, test, or exception message.**
  `grep -rniE "alphavantage.co/query\?.*apikey=[A-Z0-9]" src/` must return nothing but
  the URL-building code itself.

### Tests
- Do existing tests pass? (`mvn -o clean verify`)
- Are critical paths covered?
- **Do any tests reach the network?** `grep -rn "JdkHttpJsonClient\|HttpClient" src/test`
  must return nothing. This is non-negotiable per the blueprint.
- Are any tests tautological — an `if/else` that asserts whichever branch it lands in, or
  an assertion that would hold regardless of the code under test?
- Is any test asserting a *bug* as intended behaviour? (This repo already had one:
  `assertSame` pinning a mutable-list aliasing defect.)

### Clean Architecture — the dependency rule
This is the highest-value rubric line in the project; a grader must find zero violations.
- `grep -rn "javax.swing\|java.awt" src/main/java/use_case src/main/java/interface_adapter
  src/main/java/entity` → must be empty. Swing lives only in `view/`.
- `grep -rn "^import data_access" src/main/java/use_case` → must be empty. The use-case
  layer declares ports; it never names an implementation.
- `grep -rn "^import use_case" src/main/java/view` → must be empty. The view talks only to
  `interface_adapter`.
- `grep -rn "^import entity" src/main/java/view` → must be empty. No entity crosses an
  output boundary; only strings and numbers do.
- `System.getenv` appears in exactly one place: the composition root.

### Style — this repo has no Checkstyle, so you are the linter
Derived from `use_case/moving_average`, the canonical slice. Flag as Warnings, not
Criticals, unless a file is wildly inconsistent with its neighbours.
- 4-space indent; K&R braces; **`catch` and `else` on their own line** after the closing
  brace.
- Exception variables named `exception`, never `e`.
- Fields `private final`; locals `final` where possible; data and interactor classes
  `final`.
- `Objects.requireNonNull(x, "X cannot be null")` — always with a message, never bare.
- Javadoc on every public type and member, with `@param`/`@return`/`@throws` where they
  add information.
- Imports: `java.*` block, blank line, project packages. No wildcard imports in new files.
- Lines under ~100 characters.
- Trailing newline at end of file.
- **No comment contradicts its code.** This repo shipped two such contradictions already
  (an inverted ordering comment in `TickerSymbolValidator`, and a javadoc promising
  `Optional.empty()` where the code threw). Read comments against the code, not past them.

### Dead code
- Any public or package-private member with no caller outside its own test? Either it is
  a documented cross-team hand-off surface (`StockRepository.findAll`) or it should be
  demoted or removed.
- Any test double defined but never used?

---

## Output format — write to `plan/review-phase-[N].md`

```
Status: PASS | PASS WITH WARNINGS | FAIL

Critical (blocking — must fix before next phase):
- [issue]: [file:line] — [what to do]

Warnings (non-blocking but fix soon):
- [issue]: [file:line]

Notes:
- [anything worth knowing for the next phase]
```

Rank Criticals most-severe first. If you fixed something yourself, say so explicitly under
Notes with the file and what changed — never fix silently.

`FAIL` means the next phase does not start. Reserve it for: a red build, an ownership
violation, a dependency-rule violation, a leaked credential, a network call in a test, or
a defect the phase claimed to close and did not.

---

## Per-phase emphasis

| Phase | Look hardest at |
|---|---|
| 1 | Are the frozen contracts actually frozen — do the signatures in `agents/orchestrator.md` §5 match the files byte for byte? A contract that drifts here breaks two agents in parallel later. |
| 2 | All 13 defect IDs closed and each named in a commit. JaCoCo ≥90% on the four interactors and on the DAO + cache. Both `Recording*` doubles now have real callers. No test asserts a bug. |
| 3 | Hazard H1 — does `WatchlistView.propertyChange` re-dispatch via `SwingUtilities.invokeLater` when off the EDT? Does the presenter pin all 11 failure kinds and all 7 success messages with `assertEquals`, not `contains`? Does the view contain zero formatting logic? |
| 4 | Is the `Main.java` / `MainView.java` diff genuinely additive — no existing line deleted or reordered? Is `System.getenv` called exactly once, in `Main`? Does the app run with the key unset? Walk the `vision.md` §8 script yourself. |
| 5 | Does the hand-off test produce a real BUY and a real SELL, not just HOLDs — i.e. does the fake's sample data genuinely oscillate? Is overall coverage ≥70% with exclusions documented? |
