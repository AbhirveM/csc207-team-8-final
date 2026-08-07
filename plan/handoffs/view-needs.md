# Agent D — open needs

Two requests, both raised rather than absorbed, because acting on either inside
`WatchlistView` would mean a conditional about *data* in the view (hazard H6).

---

## D-N1 — Remove and Refresh are armed only by typing (for Agent C, presenter)

**Status:** open. Cosmetic-but-visible during the demo.

`WatchlistView` reads the ticker field for all three of Add, Remove and Refresh. That is
the literal reading of the layout — the field and the three buttons sit in one strip, and
the failure table has a `NOT_ON_WATCHLIST` row, which only makes sense if the user can
name a symbol that is not selected.

The consequence: a successful Add clears `tickerFieldText`, so pressing Refresh straight
afterwards produces `Enter a ticker symbol before continuing.` even though a row is
plainly selected in the table. Clicking a row does not help either, because Show Watchlist
success also clears the field.

**Requested change, presenter side:** in
`prepareSuccessView(ShowWatchlistOutputData)`, set `tickerFieldText` to the snapshot's
selected symbol rather than to `""`. Selecting a row would then arm Remove and Refresh,
and the demo reads as "click AAPL, press Refresh". Optionally do the same on Add success,
so the ticker just added stays in the field.

**Why not fix it in the view.** The view would have to branch on whether the field is
empty and fall back to the table selection — a data conditional, and a second source of
truth for "which symbol is the user talking about". `WatchlistState.getTickerFieldText()`
is already that source of truth; it just needs to be populated.

**If C declines:** nothing breaks. The view keeps working; the user retypes the symbol.
No change is required on my side either way — the view already copies
`getTickerFieldText()` into the field on every state change.

---

## D-N2 — the rate-limit stop condition is a prose prefix match (for C and orchestrator)

**Status:** accepted risk, recorded so it is not a surprise.

"Load prices" must stop spending the daily quota at the first rate-limit failure (hazard
H5). The only signal available to the view is the error prose, so it compares
`getErrorMessage()` against the opening sentence of the `RATE_LIMIT` row:

```
The market data service request limit has been reached.
```

This is the one place the view depends on the *content* of a presenter string rather than
just displaying it. If that sentence is ever reworded, `RATE_LIMIT_PREFIX` in
`WatchlistView` must be reworded with it, or "Load prices" will keep hammering the API
after the quota is gone.

The clean fix would be a structured signal on `WatchlistState` — say
`boolean isQuotaExhausted()` — but `WatchlistState` is orchestrator-owned and its shape is
frozen in `agents/orchestrator.md` §5.3, so this is filed, not taken. `WatchlistPresenterTest`
already pins the exact string, which is what makes the current coupling safe enough.
