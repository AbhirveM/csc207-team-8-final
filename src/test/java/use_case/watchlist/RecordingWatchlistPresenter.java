package use_case.watchlist;

/**
 * A hand-written presenter that records what the interactors report.
 *
 * <p>Implements all three output boundaries so one instance can be shared across the
 * add, remove, and refresh tests. The team writes doubles by hand rather than adding a
 * mocking library.
 */
class RecordingWatchlistPresenter implements AddTickerOutputBoundary,
        RemoveTickerOutputBoundary, RefreshTickerOutputBoundary {

    private AddTickerOutputData addResult;
    private RemoveTickerOutputData removeResult;
    private RefreshTickerOutputData refreshResult;
    private WatchlistFailure failure;
    private int successCount;
    private int failureCount;

    @Override
    public void prepareSuccessView(AddTickerOutputData outputData) {
        this.addResult = outputData;
        this.successCount++;
    }

    @Override
    public void prepareSuccessView(RemoveTickerOutputData outputData) {
        this.removeResult = outputData;
        this.successCount++;
    }

    @Override
    public void prepareSuccessView(RefreshTickerOutputData outputData) {
        this.refreshResult = outputData;
        this.successCount++;
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

    WatchlistFailure getFailure() {
        return failure;
    }

    WatchlistFailure.Kind getFailureKind() {
        return failure == null ? null : failure.getKind();
    }

    int getSuccessCount() {
        return successCount;
    }

    int getFailureCount() {
        return failureCount;
    }
}
