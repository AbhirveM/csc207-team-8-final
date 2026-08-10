package interface_adapter.persistence;

import entity.Watchlist;
import use_case.persistence.LoadWatchlist;
import use_case.persistence.SaveWatchlist;

/**
 * Turns the outcome of a save or a load into the sentence shown in the status bar.
 *
 * <p>The loaded watchlist itself is not passed on. It is application wiring - the object
 * the watchlist interactors are constructed around - not something a screen displays, so
 * it stops here rather than being parked on {@link PersistenceViewModel} where a view
 * could reach it. {@code Main} takes it from the load boundary directly.
 */
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
        viewModel.setStatusMessage("Watchlist loaded.");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setStatusMessage(errorMessage);
    }
}
