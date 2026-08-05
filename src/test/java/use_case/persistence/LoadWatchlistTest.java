package use_case.persistence;

import entity.Ticker;
import entity.Watchlist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoadWatchlistTest {

    /** In-memory data access that returns a preset watchlist, or throws on demand. */
    private static class FakeDataAccess implements WatchlistDataAccessInterface {
        Watchlist toReturn;
        boolean throwOnLoad;

        @Override
        public void save(Watchlist watchlist) throws PersistenceException {
            throw new PersistenceException("not used in these tests", null);
        }

        @Override
        public Watchlist load() throws PersistenceException {
            if (throwOnLoad) {
                throw new PersistenceException("file corrupted", null);
            }
            return toReturn;
        }
    }

    /** Capturing presenter so tests can inspect what the interactor produced. */
    private static class RecordingPresenter implements LoadWatchlist.OutputBoundary {
        Watchlist presented;
        String lastError;

        @Override
        public void presentWatchlist(Watchlist watchlist) {
            this.presented = watchlist;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.lastError = errorMessage;
        }
    }

    @Test
    void successfulLoadPresentsWatchlist() {
        FakeDataAccess dataAccess = new FakeDataAccess();
        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("TSLA", "Tesla Inc."));
        dataAccess.toReturn = watchlist;

        RecordingPresenter presenter = new RecordingPresenter();
        LoadWatchlist.Interactor interactor = new LoadWatchlist.Interactor(dataAccess, presenter);

        interactor.execute();

        assertSame(watchlist, presenter.presented);
        assertNull(presenter.lastError);
    }

    @Test
    void persistenceExceptionOnLoadPresentsFailView() {
        FakeDataAccess dataAccess = new FakeDataAccess();
        dataAccess.throwOnLoad = true;
        RecordingPresenter presenter = new RecordingPresenter();
        LoadWatchlist.Interactor interactor = new LoadWatchlist.Interactor(dataAccess, presenter);

        interactor.execute();

        assertNull(presenter.presented);
        assertNotNull(presenter.lastError);
        assertTrue(presenter.lastError.contains("Could not load watchlist"));
        assertTrue(presenter.lastError.contains("file corrupted"));
    }
}
