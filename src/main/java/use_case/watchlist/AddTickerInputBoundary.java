package use_case.watchlist;

/** Entry point for the Add Ticker use case. */
public interface AddTickerInputBoundary {

    /**
     * Adds a ticker to the watchlist and loads its price history.
     *
     * @param inputData the raw symbol the user typed
     */
    void execute(AddTickerInputData inputData);
}
