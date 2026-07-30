package interface_adapter.persistence;

import entity.Watchlist;
import use_case.persistence.LoadWatchlist;
import use_case.persistence.SaveWatchlist;

public class PersistencePresenter implements SaveWatchlist.OutputBoundary, LoadWatchlist.OutputBoundary {
    private final PersistenceViewModel viewModel;

    public PersistencePresenter(PersistenceViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView() {
        viewModel.setStatusMessage("Watchlist saved.");
    }

    @Override
    public void presentWatchlist(Watchlist watchlist) {
        viewModel.setLoadedWatchlist(watchlist);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setStatusMessage(errorMessage);
    }
}
