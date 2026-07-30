package app;

import data_access.FileWatchlistDataAccessObject;
import view.*;
import interface_adapter.comparison.ComparisonController;
import interface_adapter.comparison.ComparisonPresenter;
import interface_adapter.comparison.ComparisonViewModel;
import interface_adapter.persistence.PersistencePresenter;
import interface_adapter.persistence.PersistenceViewModel;
import use_case.comparison.CompareStrategies;
import use_case.persistence.LoadWatchlist;
import use_case.persistence.SaveWatchlist;
import use_case.persistence.WatchlistDataAccessInterface;

import javax.swing.*;

/**
 * Application builder: wires interactors, presenters, controllers, and views
 * together (dependency injection by hand). This is where Member 4's
 * "connecting all modules through the application builder" responsibility lives.
 *
 * As Members 1-3 finish their features, add their controller/presenter/view
 * construction here the same way the Comparison feature is wired below,
 * then register their view with mainView.addView(...).
 */
public class Main {
    public static void main(String[] args) {
        // --- Shared navigation state ---
        ViewManagerModel viewManagerModel = new ViewManagerModel();
        MainView mainView = new MainView(viewManagerModel);
        new ViewManager(mainView.getCardPanel(), mainView.getCardLayout(), viewManagerModel);

        // --- Persistence (Member 4) ---
        WatchlistDataAccessInterface watchlistDataAccess =
                new FileWatchlistDataAccessObject("watchlist.dat");
        PersistenceViewModel persistenceViewModel = new PersistenceViewModel();
        PersistencePresenter persistencePresenter = new PersistencePresenter(persistenceViewModel);
        SaveWatchlist.InputBoundary saveWatchlistInteractor =
                new SaveWatchlist.Interactor(watchlistDataAccess, persistencePresenter);
        LoadWatchlist.InputBoundary loadWatchlistInteractor =
                new LoadWatchlist.Interactor(watchlistDataAccess, persistencePresenter);
        // TODO: pass saveWatchlistInteractor / loadWatchlistInteractor into whatever
        // controller Member 1's watchlist view ends up using, e.g.:
        // WatchlistController watchlistController = new WatchlistController(saveWatchlistInteractor, loadWatchlistInteractor, ...);

        // --- Strategy comparison (Member 4's own feature) ---
        ComparisonViewModel comparisonViewModel = new ComparisonViewModel();
        CompareStrategies.OutputBoundary comparisonPresenter = new ComparisonPresenter(comparisonViewModel);
        CompareStrategies.InputBoundary comparisonInteractor = new CompareStrategies.Interactor(comparisonPresenter);
        ComparisonController comparisonController = new ComparisonController(comparisonInteractor);

        ComparisonView comparisonView = new ComparisonView(comparisonViewModel, comparisonController);
        mainView.addView(ComparisonViewModel.VIEW_NAME, comparisonView);

        // --- Show the app ---
        SwingUtilities.invokeLater(() -> mainView.setVisible(true));
    }
}
