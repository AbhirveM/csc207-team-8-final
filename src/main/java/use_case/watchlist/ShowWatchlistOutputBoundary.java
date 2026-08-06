package use_case.watchlist;

/**
 * Output boundary for the Show Watchlist use case.
 *
 * <p>Implemented by the presenter, called by the interactor.
 */
public interface ShowWatchlistOutputBoundary {

    /**
     * Renders the watchlist with the requested selection applied.
     *
     * @param outputData the watchlist as it stands
     */
    void prepareSuccessView(ShowWatchlistOutputData outputData);

    /**
     * Reports that the watchlist could not be shown.
     *
     * <p>Present for symmetry with the other three use cases and for the null
     * {@code inputData} case. A selected symbol that is absent from the watchlist is
     * <em>not</em> a failure.
     *
     * @param failure why the watchlist could not be shown
     */
    void prepareFailView(WatchlistFailure failure);
}
