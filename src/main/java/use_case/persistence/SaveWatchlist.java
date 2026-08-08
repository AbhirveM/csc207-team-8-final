package use_case.persistence;

import entity.Watchlist;

public class SaveWatchlist {

    public interface InputBoundary {
        void execute(Watchlist watchlist);
    }

    public interface OutputBoundary {
        void prepareSuccessView();
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
        public void execute(Watchlist watchlist) {
            try {
                dataAccess.save(watchlist);
                presenter.prepareSuccessView();
            } catch (WatchlistDataAccessInterface.PersistenceException exception) {
                presenter.prepareFailView("Could not save watchlist: " + exception.getMessage());
            }
        }
    }
}
