package use_case.watchlist;

/** Entry point for the Remove Ticker use case. */
public interface RemoveTickerInputBoundary {

    /**
     * Removes a ticker, and its stored price history, from the watchlist.
     *
     * @param inputData the raw symbol the user typed; must be non-null
     * @throws NullPointerException if {@code inputData} is null
     */
    void execute(RemoveTickerInputData inputData);
}
