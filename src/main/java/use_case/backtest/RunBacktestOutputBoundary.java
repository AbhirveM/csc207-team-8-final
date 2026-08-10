package use_case.backtest;

import java.util.List;

/**
 * Describes how the run-backtest use case reports its result.
 */
public interface RunBacktestOutputBoundary {

    void prepareSuccessView(
            RunBacktestOutputData outputData);

    void prepareFailView(
            String errorMessage);

    /**
     * Reports the tickers a backtest can currently run against.
     *
     * <p>Symbols, not tickers: this crosses into the adapter layer, and everything the
     * screen needs to fill its chooser is the text.
     *
     * @param tickerSymbols the symbols with loaded price history, sorted; possibly empty
     */
    void presentAvailableTickers(List<String> tickerSymbols);
}
