package use_case.watchlist;

/**
 * A hand-written presenter that records what the interactors report.
 *
 * <p>Implements all four watchlist output boundaries so one instance can be shared
 * across the add, remove, refresh, and show tests - which is what makes a multi-step
 * test (add, then refresh, then remove) readable. The team writes doubles by hand
 * rather than adding a mocking library.
 *
 * <p>Success counts are kept <em>per use case</em> rather than aggregated. With one
 * shared counter, a multi-step test could not tell "add succeeded and refresh did
 * nothing" from "add did nothing and refresh succeeded", so an assertion on the total
 * would pass for the wrong reason.
 */
class RecordingWatchlistPresenter implements AddTickerOutputBoundary,
        RemoveTickerOutputBoundary, RefreshTickerOutputBoundary,
        ShowWatchlistOutputBoundary {

    private AddTickerOutputData addResult;
    private RemoveTickerOutputData removeResult;
    private RefreshTickerOutputData refreshResult;
    private ShowWatchlistOutputData showResult;
    private WatchlistFailure failure;
    private int addSuccessCount;
    private int removeSuccessCount;
    private int refreshSuccessCount;
    private int showSuccessCount;
    private int failureCount;

    @Override
    public void prepareSuccessView(AddTickerOutputData outputData) {
        this.addResult = outputData;
        this.addSuccessCount++;
    }

    @Override
    public void prepareSuccessView(RemoveTickerOutputData outputData) {
        this.removeResult = outputData;
        this.removeSuccessCount++;
    }

    @Override
    public void prepareSuccessView(RefreshTickerOutputData outputData) {
        this.refreshResult = outputData;
        this.refreshSuccessCount++;
    }

    @Override
    public void prepareSuccessView(ShowWatchlistOutputData outputData) {
        this.showResult = outputData;
        this.showSuccessCount++;
    }

    @Override
    public void prepareFailView(WatchlistFailure watchlistFailure) {
        this.failure = watchlistFailure;
        this.failureCount++;
    }

    AddTickerOutputData getAddResult() {
        return addResult;
    }

    RemoveTickerOutputData getRemoveResult() {
        return removeResult;
    }

    RefreshTickerOutputData getRefreshResult() {
        return refreshResult;
    }

    ShowWatchlistOutputData getShowResult() {
        return showResult;
    }

    WatchlistFailure getFailure() {
        return failure;
    }

    /** @return the kind of the most recent failure, or null when none was reported. */
    WatchlistFailure.Kind getFailureKind() {
        return failure == null ? null : failure.getKind();
    }

    int getAddSuccessCount() {
        return addSuccessCount;
    }

    int getRemoveSuccessCount() {
        return removeSuccessCount;
    }

    int getRefreshSuccessCount() {
        return refreshSuccessCount;
    }

    int getShowSuccessCount() {
        return showSuccessCount;
    }

    int getFailureCount() {
        return failureCount;
    }
}
