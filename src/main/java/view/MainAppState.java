package view;

import entity.BacktestResult;

import java.util.ArrayList;
import java.util.List;

/**
 * TEMPORARY integration seam: a shared in-memory list of completed backtests.
 * Whoever finishes the backtesting engine (Member 3) should call
 * MainAppState.getInstance().addCompletedResult(result) once a backtest finishes,
 * so the Comparison feature has something to compare. This keeps your feature
 * unblocked while the real engine is still being built - replace with a cleaner
 * shared repository/interactor if your team wants something less quick-and-dirty later.
 */
public class MainAppState {
    private static final MainAppState INSTANCE = new MainAppState();
    private final List<BacktestResult> completedResults = new ArrayList<>();

    private MainAppState() {}

    public static MainAppState getInstance() {
        return INSTANCE;
    }

    public void addCompletedResult(BacktestResult result) {
        completedResults.add(result);
    }

    public List<BacktestResult> getCompletedResults() {
        return completedResults;
    }
}
