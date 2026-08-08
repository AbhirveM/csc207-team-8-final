package use_case.watchlist;

/** Output port for the Add Ticker use case, implemented by the presenter. */
public interface AddTickerOutputBoundary {

    void prepareSuccessView(AddTickerOutputData outputData);

    void prepareFailView(WatchlistFailure failure);
}
