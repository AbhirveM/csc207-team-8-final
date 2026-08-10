package interface_adapter.comparison;

import entity.BacktestResult;
import entity.Ticker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The store decides what counts as the same backtest, which is what stops the comparison
 * screen ranking one pairing twice.
 */
class CompletedBacktestStoreTest {

    private static final String MOVING_AVERAGE = "Moving Average Crossover";
    private static final String MOMENTUM = "RSI Momentum Strategy";
    private static final double DELTA = 1e-9;

    @Test
    void keepsRunsOfDifferentStrategiesOnTheSameTicker() {
        final CompletedBacktestStore store = new CompletedBacktestStore();

        store.add(result("AAPL", MOVING_AVERAGE, 0.90));
        store.add(result("AAPL", MOMENTUM, 19.08));

        assertEquals(2, store.getCompletedResults().size());
    }

    @Test
    void keepsRunsOfTheSameStrategyOnDifferentTickers() {
        final CompletedBacktestStore store = new CompletedBacktestStore();

        store.add(result("AAPL", MOMENTUM, 19.08));
        store.add(result("TSLA", MOMENTUM, -4.20));

        assertEquals(2, store.getCompletedResults().size());
    }

    @Test
    void replacesAnEarlierRunOfTheSamePairing() {
        final CompletedBacktestStore store = new CompletedBacktestStore();

        store.add(result("AAPL", MOMENTUM, 19.08));
        store.add(result("AAPL", MOMENTUM, 19.08));

        assertEquals(1, store.getCompletedResults().size());
    }

    @Test
    void keepsTheNewerFigureWhenAPairingIsRerun() {
        final CompletedBacktestStore store = new CompletedBacktestStore();

        // What happens when a user edits the strategy parameters and runs it again: the second
        // number is the one that matches the configuration on screen.
        store.add(result("AAPL", MOVING_AVERAGE, 0.90));
        store.add(result("AAPL", MOVING_AVERAGE, 7.35));

        assertEquals(1, store.getCompletedResults().size());
        assertEquals(7.35, store.getCompletedResults().get(0).getTotalReturn(), DELTA);
    }

    @Test
    void aRerunHoldsItsOriginalPositionSoTiesDoNotReshuffle() {
        final CompletedBacktestStore store = new CompletedBacktestStore();

        store.add(result("AAPL", MOVING_AVERAGE, 0.90));
        store.add(result("TSLA", MOMENTUM, 5.00));
        store.add(result("AAPL", MOVING_AVERAGE, 1.10));

        assertEquals(2, store.getCompletedResults().size());
        assertEquals("AAPL", store.getCompletedResults().get(0).getTicker().getSymbol());
        assertEquals("TSLA", store.getCompletedResults().get(1).getTicker().getSymbol());
    }

    @Test
    void treatsTickerSymbolsCaseInsensitively() {
        final CompletedBacktestStore store = new CompletedBacktestStore();

        // Ticker.equals is case-insensitive everywhere else in the app, so a store that split
        // AAPL from aapl would rank one holding as two.
        store.add(result("AAPL", MOMENTUM, 19.08));
        store.add(result("aapl", MOMENTUM, 19.08));

        assertEquals(1, store.getCompletedResults().size());
    }

    @Test
    void handsOutAListThatCannotBeMutated() {
        final CompletedBacktestStore store = new CompletedBacktestStore();
        store.add(result("AAPL", MOMENTUM, 19.08));

        final List<BacktestResult> results = store.getCompletedResults();

        assertThrows(UnsupportedOperationException.class,
                () -> results.add(result("TSLA", MOMENTUM, 1.0)));
    }

    /**
     * Builds a result carrying only the fields the store's identity rule reads.
     *
     * @param symbol       the ticker symbol
     * @param strategyName the strategy name
     * @param totalReturn  the total return as a percentage
     * @return the result
     */
    private static BacktestResult result(String symbol, String strategyName, double totalReturn) {
        return new BacktestResult(new Ticker(symbol, ""), strategyName, List.of(),
                10000.0, totalReturn, 1, 100.0, List.of(), null, null);
    }
}
