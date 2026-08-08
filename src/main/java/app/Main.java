package app;

import data_access.AlphaVantageMarketDataAccessObject;
import data_access.CachingMarketDataGateway;
import data_access.FileWatchlistDataAccessObject;
import data_access.InMemoryMarketDataGateway;
import data_access.InMemoryStockRepository;
import entity.BacktestEngine;
import entity.Watchlist;
import view.BacktestView;
import view.ComparisonView;
import view.MainView;
import view.ViewManager;
import view.ViewManagerModel;
import view.WatchlistView;
import interface_adapter.backtest.BacktestController;
import interface_adapter.backtest.BacktestPresenter;
import interface_adapter.backtest.BacktestViewModel;
import interface_adapter.comparison.ComparisonController;
import interface_adapter.comparison.ComparisonPresenter;
import interface_adapter.comparison.ComparisonViewModel;
import interface_adapter.comparison.CompletedBacktestStore;
import interface_adapter.persistence.PersistencePresenter;
import interface_adapter.persistence.PersistenceViewModel;
import interface_adapter.watchlist.WatchlistController;
import interface_adapter.watchlist.WatchlistPresenter;
import interface_adapter.watchlist.WatchlistState;
import interface_adapter.watchlist.WatchlistViewModel;
import use_case.backtest.RunBacktestInteractor;
import use_case.backtest.RunBacktestOutputBoundary;
import use_case.backtest.RunBacktestOutputData;
import use_case.comparison.CompareStrategies;
import use_case.persistence.LoadWatchlist;
import use_case.persistence.SaveWatchlist;
import use_case.persistence.WatchlistDataAccessInterface;
import use_case.watchlist.AddTickerInputBoundary;
import use_case.watchlist.AddTickerInteractor;
import use_case.watchlist.MarketDataGateway;
import use_case.watchlist.RefreshTickerInputBoundary;
import use_case.watchlist.RefreshTickerInteractor;
import use_case.watchlist.RemoveTickerInputBoundary;
import use_case.watchlist.RemoveTickerInteractor;
import use_case.watchlist.ShowWatchlistInputBoundary;
import use_case.watchlist.ShowWatchlistInteractor;
import use_case.watchlist.StockRepository;

import javax.swing.SwingUtilities;
import java.util.Optional;

/**
 * Application builder: wires interactors, presenters, controllers, and views
 * together (dependency injection by hand). This is where Member 4's
 * "connecting all modules through the application builder" responsibility lives.
 *
 * <p>Wired and reachable today: the four watchlist use cases, watchlist
 * persistence, and the Compare Strategies screen.
 *
 * <p><strong>Not yet wired:</strong> the run-backtest use case and the
 * configure-moving-average use case. Both are implemented and unit-tested, but
 * nothing here constructs them, so no user path reaches a backtest and the
 * Compare screen has nothing to rank. Add their controller/presenter/view
 * construction below the same way the Comparison feature is wired, register the
 * view with mainView.addView(...), add a nav button in MainView, and have
 * BacktestPresenter record each finished run in the CompletedBacktestStore that
 * is already constructed here. See plan/handoffs/team-raise-2026-08-08.md.
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
        // Bind the persistence view model to the window's status bar. Without this the view
        // model fired into the void, so a failed save was invisible: the watchlist reported
        // "Added AAPL..." while nothing reached watchlist.dat. Saves run on the watchlist's
        // background worker, so setPersistenceStatus marshals back onto the event thread.
        persistenceViewModel.addPropertyChangeListener(event -> {
            if (PersistenceViewModel.STATUS_PROPERTY.equals(event.getPropertyName())) {
                mainView.setPersistenceStatus(persistenceViewModel.getStatusMessage());
            }
        });
        SaveWatchlist.InputBoundary saveWatchlistInteractor =
                new SaveWatchlist.Interactor(watchlistDataAccess, persistencePresenter);
        LoadWatchlist.InputBoundary loadWatchlistInteractor =
                new LoadWatchlist.Interactor(watchlistDataAccess, persistencePresenter);
        // --- Watchlist (Member 1) ---
        // Gateway selection. This is the only place in the codebase that reads the
        // environment: with no key configured the app runs fully offline against synthetic
        // sample data rather than failing, so a grader without a key still sees the feature.
        // There is no .env, no default key in code, and the key never appears in a message.
        Optional<String> apiKey = AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment();
        MarketDataGateway marketDataGateway;
        if (apiKey.isPresent()) {
            marketDataGateway = new CachingMarketDataGateway(
                    new AlphaVantageMarketDataAccessObject(apiKey.get()));
        }
        else {
            marketDataGateway = InMemoryMarketDataGateway.withSampleData();
        }

        // Seed the watchlist from disk. This is what consumes loadWatchlistInteractor above;
        // a load failure leaves the view model's watchlist null, which degrades to empty.
        loadWatchlistInteractor.execute();
        Watchlist watchlist = persistenceViewModel.getWatchlist();
        if (watchlist == null) {
            watchlist = new Watchlist();
        }

        StockRepository stockRepository = new InMemoryStockRepository();
        WatchlistViewModel watchlistViewModel = new WatchlistViewModel();
        // One presenter across all four interactors: a single instance is what keeps an add
        // and a refresh from drifting into wording the user reads as two applications.
        WatchlistPresenter watchlistPresenter = new WatchlistPresenter(watchlistViewModel);

        // The constructor asymmetry is deliberate. Refresh changes prices, not membership, so
        // it takes no SaveWatchlist; Show Watchlist performs no I/O at all, so it takes
        // neither a gateway nor a save.
        AddTickerInputBoundary addTicker = new AddTickerInteractor(
                watchlist, marketDataGateway, stockRepository,
                saveWatchlistInteractor, watchlistPresenter);
        RemoveTickerInputBoundary removeTicker = new RemoveTickerInteractor(
                watchlist, stockRepository, saveWatchlistInteractor, watchlistPresenter);
        RefreshTickerInputBoundary refreshTicker = new RefreshTickerInteractor(
                watchlist, marketDataGateway, stockRepository, watchlistPresenter);
        ShowWatchlistInputBoundary showWatchlist = new ShowWatchlistInteractor(
                watchlist, stockRepository, watchlistPresenter);

        WatchlistController watchlistController =
                new WatchlistController(addTicker, removeTicker, refreshTicker, showWatchlist);
        WatchlistView watchlistView = new WatchlistView(watchlistViewModel, watchlistController);
        mainView.addView(WatchlistViewModel.VIEW_NAME, watchlistView);

        // --- Backtesting (Member 3's engine, wired by Member 4's integration) ---
        // Every piece below already existed and was tested; nothing constructed them, so the
        // completed-backtest store had no callers and the Compare screen stayed empty.
        BacktestEngine backtestEngine = new BacktestEngine();
        BacktestViewModel backtestViewModel = new BacktestViewModel();
        BacktestPresenter backtestPresenter = new BacktestPresenter(backtestViewModel);
        // The shared store of finished backtests that the Compare Strategies screen ranks. The
        // backtest and comparison features never reference each other; they meet only through
        // this one instance.
        CompletedBacktestStore completedBacktests = new CompletedBacktestStore();
        // A thin decorator over the presenter: on a successful run it also files the result
        // into the shared store the Comparison feature reads from, so "run a backtest" and
        // "compare completed backtests" are joined without either feature knowing the other.
        RunBacktestOutputBoundary backtestOutput = new RunBacktestOutputBoundary() {
            @Override
            public void prepareSuccessView(RunBacktestOutputData outputData) {
                completedBacktests.add(outputData.getBacktestResult());
                backtestPresenter.prepareSuccessView(outputData);
            }

            @Override
            public void prepareFailView(String errorMessage) {
                backtestPresenter.prepareFailView(errorMessage);
            }
        };
        RunBacktestInteractor runBacktest =
                new RunBacktestInteractor(backtestEngine, backtestOutput);
        BacktestController backtestController = new BacktestController(runBacktest);
        BacktestView backtestView =
                new BacktestView(backtestViewModel, backtestController, stockRepository);
        mainView.addView(BacktestViewModel.VIEW_NAME, backtestView);

        // --- Strategy comparison (Member 4's own feature) ---
        ComparisonViewModel comparisonViewModel = new ComparisonViewModel();
        CompareStrategies.OutputBoundary comparisonPresenter = new ComparisonPresenter(comparisonViewModel);
        CompareStrategies.InputBoundary comparisonInteractor = new CompareStrategies.Interactor(comparisonPresenter);
        ComparisonController comparisonController =
                new ComparisonController(comparisonInteractor, completedBacktests);

        ComparisonView comparisonView = new ComparisonView(comparisonViewModel, comparisonController);
        mainView.addView(ComparisonViewModel.VIEW_NAME, comparisonView);

        // --- Show the app ---
        SwingUtilities.invokeLater(() -> {
            mainView.setVisible(true);
            // Constructing WatchlistView alone paints WatchlistState.initial() - "Ready." with
            // empty tables - so without this call a watchlist restored from disk would not
            // render until the user's first action.
            watchlistController.showWatchlist("");
            // Applied after Show Watchlist, which writes its own status message. The notice is
            // prepended to that message rather than replacing it: on a first launch there is
            // no watchlist.dat, so Show Watchlist's line is "Your watchlist is empty. Add a
            // ticker to begin." - the only thing on screen telling a new user what to do.
            if (apiKey.isEmpty()) {
                WatchlistState shown = watchlistViewModel.getState();
                watchlistViewModel.setState(new WatchlistState(
                        shown.getTickerRows(),
                        shown.getPriceRows(),
                        shown.getSelectedSymbol(),
                        WatchlistViewModel.SAMPLE_DATA_STATUS + " " + shown.getStatusMessage(),
                        shown.getErrorMessage(),
                        shown.getTickerFieldText()));
            }
        });
    }
}
