package use_case.watchlist;

/** Output port for the Remove Ticker use case. */
public interface RemoveTickerOutputBoundary {

    void prepareSuccessView(RemoveTickerOutputData outputData);

    void prepareFailView(WatchlistFailure failure);
}
