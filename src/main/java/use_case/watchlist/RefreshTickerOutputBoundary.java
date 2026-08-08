package use_case.watchlist;

/** Output port for the Refresh Ticker use case. */
public interface RefreshTickerOutputBoundary {

    void prepareSuccessView(RefreshTickerOutputData outputData);

    void prepareFailView(WatchlistFailure failure);
}
