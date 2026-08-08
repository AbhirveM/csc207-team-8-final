package use_case.watchlist;

/** Entry point for the Refresh Ticker use case. */
public interface RefreshTickerInputBoundary {

    /**
     * Re-fetches the price history for a ticker already on the watchlist.
     *
     * @param inputData the raw symbol the user typed; must be non-null
     * @throws NullPointerException if {@code inputData} is null
     */
    void execute(RefreshTickerInputData inputData);
}
