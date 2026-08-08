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
     * <p>Present for symmetry with the other three use cases. Show Watchlist has no
     * failure mode of its own — a selected symbol that is absent from the watchlist
     * degrades silently to no selection rather than failing, and a null
     * {@code inputData} is a wiring error that fails fast with
     * {@link NullPointerException} — so no interactor currently calls this.
     *
     * @param failure why the watchlist could not be shown
     */
    void prepareFailView(WatchlistFailure failure);
}
