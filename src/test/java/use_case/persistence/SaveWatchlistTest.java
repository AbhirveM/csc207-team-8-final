package use_case.persistence;

import entity.Ticker;
import entity.Watchlist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaveWatchlistTest {

    /** In-memory data access that records what was saved, or throws on demand. */
    private static class FakeDataAccess implements WatchlistDataAccessInterface {
        Watchlist saved;
        boolean throwOnSave;

        @Override
        public void save(Watchlist watchlist) throws PersistenceException {
            if (throwOnSave) {
                throw new PersistenceException("disk full", null);
            }
            this.saved = watchlist;
        }

        @Override
        public Watchlist load() throws PersistenceException {
            throw new PersistenceException("not used in these tests", null);
        }
    }

    /** Capturing presenter so tests can inspect what the interactor produced. */
    private static class RecordingPresenter implements SaveWatchlist.OutputBoundary {
        boolean successCalled;
        String lastError;

        @Override
        public void prepareSuccessView() {
            this.successCalled = true;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.lastError = errorMessage;
        }
    }

    @Test
    void successfulSavePersistsWatchlistAndShowsSuccess() {
        FakeDataAccess dataAccess = new FakeDataAccess();
        RecordingPresenter presenter = new RecordingPresenter();
        SaveWatchlist.Interactor interactor = new SaveWatchlist.Interactor(dataAccess, presenter);

        Watchlist watchlist = new Watchlist();
        watchlist.addTicker(new Ticker("AAPL", "Apple Inc."));

        interactor.execute(watchlist);

        assertTrue(presenter.successCalled);
        assertNull(presenter.lastError);
        assertSame(watchlist, dataAccess.saved);
    }

    @Test
    void persistenceExceptionOnSavePresentsFailView() {
        FakeDataAccess dataAccess = new FakeDataAccess();
        dataAccess.throwOnSave = true;
        RecordingPresenter presenter = new RecordingPresenter();
        SaveWatchlist.Interactor interactor = new SaveWatchlist.Interactor(dataAccess, presenter);

        interactor.execute(new Watchlist());

        assertFalse(presenter.successCalled);
        assertNotNull(presenter.lastError);
        assertTrue(presenter.lastError.contains("Could not save watchlist"));
        assertTrue(presenter.lastError.contains("disk full"));
        assertNull(dataAccess.saved);
    }
}
