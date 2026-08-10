package use_case.backtest;

/**
 * Input boundary for running a backtest.
 */
public interface RunBacktestInputBoundary {

    /**
     * Runs the named strategy over the named ticker's loaded price history.
     *
     * @param inputData which ticker, which strategy, and the strategy's parameters
     */
    void execute(RunBacktestInputData inputData);

    /**
     * Reports which tickers currently have price history a backtest could run against.
     *
     * <p>Separate from {@link #execute} because a screen needs the list before the user has
     * chosen anything, and it goes out through the output boundary rather than a return
     * value so the caller stays a one-way controller.
     */
    void loadAvailableTickers();
}
