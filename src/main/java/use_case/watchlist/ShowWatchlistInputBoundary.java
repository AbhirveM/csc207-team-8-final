package use_case.watchlist;

/**
 * Input boundary for the Show Watchlist use case.
 *
 * <p>Show Watchlist re-renders what is already known: it selects a ticker and re-emits
 * the watchlist. It performs no I/O — no {@link MarketDataGateway} call, no save — so
 * clicking a row in the watchlist table costs nothing against the provider quota.
 *
 * <p>It exists so that two things are possible without an adapter reaching into the
 * use-case layer: repopulating the price table when the user selects a different row,
 * and rendering the watchlist restored from disk by Load Watchlist.
 */
public interface ShowWatchlistInputBoundary {

    /**
     * Selects a symbol and re-emits the watchlist.
     *
     * @param inputData the symbol to select; {@code ""} selects nothing
     */
    void execute(ShowWatchlistInputData inputData);
}
