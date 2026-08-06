# Agent B — open needs

One open item at the end of Phase 2. Nothing blocks Phase 3.

| # | Need | From | Status |
|---|---|---|---|
| N1 | A single `Locale.ROOT` symbol-normalization helper in `use_case`, so the three copies of the `key(String)` idiom in `data_access` can delegate to it instead of duplicating it. | Agent A (D9) | Open, non-blocking |

Full detail, including the exact signature that would close it, is in
`plan/handoffs/agent-b-to-a.md` § 1.

Why it is only a request: `agents/data-access.md` § D8 instructs me to route the three
`key` methods through a `TickerSymbolValidator`-adjacent helper **if one exists** after
Agent A's D9 work, and to write a note rather than edit `use_case` if it does not. At the
commit I branched from (`e3cd012`) it did not exist, and `use_case/**` is not mine to
edit. The three copies are three lines each and behave identically, so leaving them is
correct but not ideal.

No port signature change was needed, so `plan/handoffs/agent-b-request.md` was not
written.
