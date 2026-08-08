package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

import data_access.InMemoryMarketDataGateway;
import data_access.InMemoryStockRepository;
import entity.BacktestEngine;
import entity.BacktestResult;
import entity.MovingAverageConfiguration;
import entity.MovingAverageCrossoverStrategy;
import entity.Stock;
import entity.Watchlist;
import interface_adapter.backtest.BacktestController;
import interface_adapter.backtest.BacktestPresenter;
import interface_adapter.backtest.BacktestViewModel;
import interface_adapter.comparison.ComparisonController;
import interface_adapter.comparison.ComparisonPresenter;
import interface_adapter.comparison.ComparisonViewModel;
import use_case.backtest.RunBacktestInteractor;
import use_case.backtest.RunBacktestOutputBoundary;
import use_case.backtest.RunBacktestOutputData;
import use_case.comparison.CompareStrategies;
import use_case.persistence.SaveWatchlist;
import use_case.watchlist.AddTickerInputData;
import use_case.watchlist.AddTickerInteractor;
import use_case.watchlist.AddTickerOutputBoundary;
import use_case.watchlist.AddTickerOutputData;
import use_case.watchlist.StockRepository;
import use_case.watchlist.WatchlistFailure;
import view.MainAppState;

/**
 * The end-to-end wiring the application builder assembles, exercised without Swing.
 *
 * <p>This is the integration proof for the single biggest presentation risk: every piece
 * of the backtest -> compare story was built and unit-tested, but nothing constructed the
 * chain, so {@code MainAppState.addCompletedResult} had no callers and the Compare screen
 * only ever rendered its empty state. This test builds the same object graph {@code Main}
 * builds - sample market data -> Add Ticker -> {@link StockRepository} -> the backtest
 * controller wired through the {@code MainAppState}-feeding decorator -> the comparison
 * controller - and asserts a real result flows the whole way through.
 */
class IntegrationWiringTest {

    /** A presenter that ignores everything: the Add Ticker output is not under test here. */
    private static final AddTickerOutputBoundary NOOP_ADD_PRESENTER =
            new AddTickerOutputBoundary() {
                @Override
                public void prepareSuccessView(AddTickerOutputData outputData) {
                    // intentionally empty
                }

                @Override
                public void prepareFailView(WatchlistFailure failure) {
                    // intentionally empty
                }
            };

    /** A save boundary that does nothing: no file is written during the test. */
    private static final SaveWatchlist.InputBoundary NOOP_SAVE = watchlist -> {
        // intentionally empty
    };

    @Test
    void aBacktestRunThroughTheWiredGraphReachesTheComparisonScreen() {
        // --- The watchlist + market-data half, seeded with offline sample data. ---
        final InMemoryMarketDataGateway gateway = InMemoryMarketDataGateway.withSampleData();
        final StockRepository stockRepository = new InMemoryStockRepository();
        final Watchlist watchlist = new Watchlist();
        final AddTickerInteractor addTicker = new AddTickerInteractor(
                watchlist, gateway, stockRepository, NOOP_SAVE, NOOP_ADD_PRESENTER);

        addTicker.execute(new AddTickerInputData("AAPL"));

        final Stock stock = stockRepository.findBySymbol("AAPL").orElseThrow();

        // --- The backtest half, wired exactly as Main wires it. ---
        final BacktestViewModel backtestViewModel = new BacktestViewModel();
        final BacktestPresenter backtestPresenter = new BacktestPresenter(backtestViewModel);
        final MainAppState appState = MainAppState.getInstance();
        final int resultsBefore = appState.getCompletedResults().size();

        final RunBacktestOutputBoundary backtestOutput = new RunBacktestOutputBoundary() {
            @Override
            public void prepareSuccessView(RunBacktestOutputData outputData) {
                appState.addCompletedResult(outputData.getBacktestResult());
                backtestPresenter.prepareSuccessView(outputData);
            }

            @Override
            public void prepareFailView(String errorMessage) {
                backtestPresenter.prepareFailView(errorMessage);
            }
        };
        final BacktestController backtestController = new BacktestController(
                new RunBacktestInteractor(new BacktestEngine(), backtestOutput));

        backtestController.runBacktest(
                stock.getTicker(),
                new MovingAverageCrossoverStrategy(new MovingAverageConfiguration(5, 20)),
                stock.getDailyPrices());

        // The presenter wrote a result, and the decorator filed it for comparison.
        final BacktestResult result = backtestViewModel.getResult();
        assertNotNull(result, "the wired backtest should produce a result");
        assertEquals("AAPL", result.getTicker().getSymbol());
        assertEquals(resultsBefore + 1, appState.getCompletedResults().size(),
                "a completed backtest should be filed into the shared app state");

        // --- The comparison half reads from the same shared app state. ---
        final ComparisonViewModel comparisonViewModel = new ComparisonViewModel();
        final ComparisonController comparisonController = new ComparisonController(
                new CompareStrategies.Interactor(
                        new ComparisonPresenter(comparisonViewModel)));

        comparisonController.compare(appState.getCompletedResults());

        assertTrue(comparisonViewModel.getErrorMessage().isEmpty(),
                "comparison should succeed once a backtest has been run");
        final List<BacktestResult> ranked = comparisonViewModel.getRankedResults();
        assertFalse(ranked.isEmpty(), "the comparison screen should have rows to show");
    }
}
