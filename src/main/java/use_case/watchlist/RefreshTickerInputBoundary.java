package use_case.watchlist;

/** Entry point for the Refresh Ticker use case. */
public interface RefreshTickerInputBoundary {

    void execute(RefreshTickerInputData inputData);
}
