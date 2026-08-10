package use_case.backtest;

import entity.BacktestEngine;
import entity.BacktestResult;
import entity.DailyPrice;
import entity.Stock;
import entity.Ticker;
import use_case.watchlist.StockRepository;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interactor owns three steps a caller no longer performs: resolving a ticker symbol
 * against the price repository, building the strategy from the numbers it was handed, and
 * running the engine over the two. These tests drive it the way a controller does - with
 * text and numbers only - and cover each way that can fail.
 */
class RunBacktestInteractorTest {

    private static final double DELTA = 0.0001;

    @Test
    void aMovingAverageRunResolvesTheSymbolAndPreparesSuccessView() {
        final TestPresenter presenter = new TestPresenter();
        final TestStockRepository repository = new TestStockRepository();
        repository.save(stockWith("TEST", 12));

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        repository,
                        presenter);

        interactor.execute(
                RunBacktestInputData.movingAverageCrossover(
                        "TEST", 2, 4));

        assertNull(presenter.errorMessage);
        assertNotNull(presenter.successData);

        final BacktestResult result =
                presenter.successData.getBacktestResult();

        assertEquals(
                "TEST",
                result.getTicker().getSymbol());

        assertEquals(
                "Moving Average Crossover",
                result.getStrategyName());
    }

    @Test
    void aMomentumRunBuildsTheStrategyFromTheNumbersItWasHanded() {
        final TestPresenter presenter = new TestPresenter();
        final TestStockRepository repository = new TestStockRepository();
        repository.save(stockWith("TEST", 20));

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        repository,
                        presenter);

        interactor.execute(
                RunBacktestInputData.rsiMomentum(
                        "TEST", 5, 25.0, 75.0));

        assertNull(presenter.errorMessage);
        assertNotNull(presenter.successData);

        assertEquals(
                "RSI Momentum Strategy",
                presenter.successData
                        .getBacktestResult()
                        .getStrategyName());
    }

    @Test
    void aSymbolWithNoLoadedPricesPreparesFailViewAndNeverReachesTheEngine() {
        final TestPresenter presenter = new TestPresenter();

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        new TestStockRepository(),
                        presenter);

        interactor.execute(
                RunBacktestInputData.movingAverageCrossover(
                        "MISSING", 2, 4));

        assertNull(presenter.successData);

        // The wording has to name the symbol and say where to fix it: this is the state a
        // user is in before they have ever pressed "Load prices".
        assertTrue(
                presenter.errorMessage.startsWith(
                        "No loaded prices for MISSING"),
                presenter.errorMessage);

        assertTrue(
                presenter.errorMessage.contains("Watchlist"),
                presenter.errorMessage);
    }

    @Test
    void nullInputPreparesFailView() {
        final TestPresenter presenter =
                new TestPresenter();

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        new TestStockRepository(),
                        presenter);

        interactor.execute(null);

        assertNull(presenter.successData);

        assertEquals(
                "Backtest input cannot be null",
                presenter.errorMessage);
    }

    @Test
    void emptyPriceHistoryPreparesFailView() {
        final TestPresenter presenter =
                new TestPresenter();

        final TestStockRepository repository =
                new TestStockRepository();

        repository.save(
                new Stock(
                        new Ticker(
                                "TEST",
                                "Test Company"),
                        List.of()));

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        repository,
                        presenter);

        interactor.execute(
                RunBacktestInputData.movingAverageCrossover(
                        "TEST", 2, 4));

        assertNull(presenter.successData);

        assertEquals(
                "Price history cannot be empty",
                presenter.errorMessage);
    }

    @Test
    void strategyParametersOutsideTheirBoundsPrepareFailView() {
        final TestPresenter presenter =
                new TestPresenter();

        final TestStockRepository repository =
                new TestStockRepository();

        repository.save(stockWith("TEST", 12));

        final RunBacktestInteractor interactor =
                new RunBacktestInteractor(
                        new BacktestEngine(),
                        repository,
                        presenter);

        // The windows are the user's, so their bounds are enforced by the configuration
        // entity and worded through the same failure path as an engine complaint rather
        // than escaping as an exception.
        interactor.execute(
                RunBacktestInputData.movingAverageCrossover(
                        "TEST", 20, 5));

        assertNull(presenter.successData);

        assertEquals(
                "Long window must be greater than short window",
                presenter.errorMessage);
    }

    @Test
    void loadAvailableTickersPresentsEverySymbolWithPriceHistory() {
        final TestPresenter presenter =
                new TestPresenter();

        final TestStockRepository repository =
                new TestStockRepository();

        repository.save(stockWith("AAPL", 3));
        repository.save(stockWith("NVDA", 3));

        new RunBacktestInteractor(
                new BacktestEngine(),
                repository,
                presenter)
                .loadAvailableTickers();

        assertEquals(
                List.of("AAPL", "NVDA"),
                presenter.availableTickers);
    }

    @Test
    void loadAvailableTickersPresentsAnEmptyListWhenNothingHasBeenLoaded() {
        final TestPresenter presenter =
                new TestPresenter();

        new RunBacktestInteractor(
                new BacktestEngine(),
                new TestStockRepository(),
                presenter)
                .loadAvailableTickers();

        assertEquals(
                List.of(),
                presenter.availableTickers);
    }

    @Test
    void constructorRejectsNullEngine() {
        final TestPresenter presenter =
                new TestPresenter();

        assertThrows(
                NullPointerException.class,
                () -> new RunBacktestInteractor(
                        null,
                        new TestStockRepository(),
                        presenter));
    }

    @Test
    void constructorRejectsNullStockRepository() {
        final TestPresenter presenter =
                new TestPresenter();

        assertThrows(
                NullPointerException.class,
                () -> new RunBacktestInteractor(
                        new BacktestEngine(),
                        null,
                        presenter));
    }

    @Test
    void constructorRejectsNullPresenter() {
        assertThrows(
                NullPointerException.class,
                () -> new RunBacktestInteractor(
                        new BacktestEngine(),
                        new TestStockRepository(),
                        null));
    }

    /**
     * Builds a stock whose closes fall then rise, so a short/long crossover strategy has
     * something to cross on.
     *
     * @param symbol the ticker symbol
     * @param days how many trading days of history to produce
     * @return the stock
     */
    private static Stock stockWith(String symbol, int days) {
        final List<DailyPrice> prices = new ArrayList<>();
        for (int day = 1; day <= days; day++) {
            final double close = day <= days / 2
                    ? 100.0 - day * 2.0
                    : 100.0 + day * 2.0;
            prices.add(createPrice(day, close, close));
        }
        return new Stock(new Ticker(symbol, symbol + " Company"), prices);
    }

    private static DailyPrice createPrice(
            int day,
            double open,
            double close) {

        return new DailyPrice(
                LocalDate.of(2026, 1, day),
                open,
                Math.max(open, close),
                Math.min(open, close),
                close,
                1000L);
    }

    /**
     * Presenter used only for testing the interactor.
     */
    private static class TestPresenter
            implements RunBacktestOutputBoundary {

        private RunBacktestOutputData successData;
        private String errorMessage;
        private List<String> availableTickers;

        @Override
        public void prepareSuccessView(
                RunBacktestOutputData outputData) {

            this.successData = outputData;
            this.errorMessage = null;
        }

        @Override
        public void prepareFailView(
                String errorMessage) {

            this.errorMessage = errorMessage;
            this.successData = null;
        }

        @Override
        public void presentAvailableTickers(
                List<String> tickerSymbols) {

            this.availableTickers = tickerSymbols;
        }
    }

    /**
     * A repository holding whatever the test put in it, sorted by symbol like the real one.
     */
    private static class TestStockRepository implements StockRepository {

        private final Map<String, Stock> bySymbol = new LinkedHashMap<>();

        @Override
        public void save(Stock stock) {
            bySymbol.put(stock.getTicker().getSymbol(), stock);
        }

        @Override
        public Optional<Stock> findBySymbol(String normalizedSymbol) {
            return Optional.ofNullable(bySymbol.get(normalizedSymbol));
        }

        @Override
        public void remove(String normalizedSymbol) {
            bySymbol.remove(normalizedSymbol);
        }

        @Override
        public List<Stock> findAll() {
            final List<Stock> all = new ArrayList<>(bySymbol.values());
            all.sort((left, right) ->
                    left.getTicker().getSymbol().compareTo(right.getTicker().getSymbol()));
            return List.copyOf(all);
        }
    }
}
