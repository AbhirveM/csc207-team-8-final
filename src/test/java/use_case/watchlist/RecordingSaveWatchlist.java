package use_case.watchlist;

import entity.Watchlist;
import use_case.persistence.SaveWatchlist;

/**
 * Records calls to the persistence feature's save boundary.
 *
 * <p>The watchlist use cases call Member 4's existing boundary rather than a data
 * access object of their own, so persistence stays that feature's responsibility.
 */
class RecordingSaveWatchlist implements SaveWatchlist.InputBoundary {

    private int callCount;
    private Watchlist lastSaved;

    @Override
    public void execute(Watchlist watchlist) {
        this.callCount++;
        this.lastSaved = watchlist;
    }

    int getCallCount() {
        return callCount;
    }

    Watchlist getLastSaved() {
        return lastSaved;
    }
}
