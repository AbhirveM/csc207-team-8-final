package use_case.watchlist;

/** Entry point for the Add Ticker use case. */
public interface AddTickerInputBoundary {

    /**
     * Adds a ticker to the watchlist and loads its price history.
     *
     * <p>Every user-level problem - blank, malformed, over-long or duplicate input, and
     * every provider failure - is reported through {@code prepareFailView} rather than
     * thrown. A null {@code inputData} is not a user-level problem but a wiring error,
     * so it fails fast instead.
     *
     * @param inputData the raw symbol the user typed; must be non-null
     * @throws NullPointerException if {@code inputData} is null
     */
    void execute(AddTickerInputData inputData);
}
