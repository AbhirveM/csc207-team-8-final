package use_case.persistence;

import entity.Watchlist;

public class LoadWatchlist {

    public interface InputBoundary {
        void execute();
    }

    public interface OutputBoundary {
        void presentWatchlist(Watchlist watchlist);
        void prepareFailView(String errorMessage);
    }

    public static class Interactor implements InputBoundary {
        private final WatchlistDataAccessInterface dataAccess;
        private final OutputBoundary presenter;

        public Interactor(WatchlistDataAccessInterface dataAccess, OutputBoundary presenter) {
            this.dataAccess = dataAccess;
            this.presenter = presenter;
        }

        @Override
        public void execute() {
            try {
                Watchlist watchlist = dataAccess.load();
                presenter.presentWatchlist(watchlist);
            } catch (WatchlistDataAccessInterface.PersistenceException e) {
                presenter.prepareFailView("Could not load watchlist: " + e.getMessage());
            }
        }
    }
}
