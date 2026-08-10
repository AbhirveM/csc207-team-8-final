package interface_adapter.comparison;

import entity.BacktestResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The shared collection of backtests that have finished during this session, which is what the
 * Compare Strategies screen ranks.
 *
 * <p>This used to be {@code view.MainAppState}, a singleton in the view layer that imported
 * {@link BacktestResult} directly. That import crossed from Frameworks &amp; Drivers past the
 * adapter layer into Entities, which is a Dependency Rule violation. Living in
 * {@code interface_adapter} instead makes the entity dependency legal, and taking it as a
 * constructor argument rather than reaching for a static instance makes it injectable and
 * testable.
 *
 * <p>{@link #add} is called from {@code Main}, by an anonymous
 * {@code RunBacktestOutputBoundary} decorator that files each successful result here before
 * delegating to {@code BacktestPresenter}. Decorating the boundary rather than putting the call
 * inside the presenter keeps the backtest feature unaware that a comparison feature exists.
 *
 * <p>The store is session-scoped: it is never persisted and never cleared, so the Compare screen
 * shows its empty state only until the first backtest finishes.
 */
public class CompletedBacktestStore {

    private final List<BacktestResult> completedResults = new ArrayList<>();

    /**
     * Records a finished backtest, replacing any earlier run of the same ticker and strategy.
     *
     * <p>A run is identified by its ticker and strategy name, so re-running one pairing does not
     * add a second row. Without this, running the same backtest twice put two identical rows in
     * the ranking and counted that pairing twice in the comparison.
     *
     * <p>Replacing rather than ignoring the newer run is the important half. Strategy parameters
     * are editable between runs, so the second result may be the more accurate one - a user who
     * widens a moving-average window and runs it again means the new number, and silently keeping
     * the first would show a figure that no longer matches the configuration on screen.
     *
     * <p>The replacement keeps the original position rather than moving to the end. Ranking sorts
     * by return, and {@code Stream.sorted} is stable, so position only decides ties; holding the
     * slot keeps a tie from reshuffling because something unrelated was re-run.
     *
     * @param result the completed result; must be non-null
     */
    public void add(BacktestResult result) {
        final int existing = indexOf(result);
        if (existing >= 0) {
            completedResults.set(existing, result);
        }
        else {
            completedResults.add(result);
        }
    }

    /**
     * Finds an earlier run of the same ticker and strategy.
     *
     * @param result the result being recorded
     * @return the index of the run it replaces, or -1 when it is the first of its pairing
     */
    private int indexOf(BacktestResult result) {
        for (int index = 0; index < completedResults.size(); index++) {
            final BacktestResult candidate = completedResults.get(index);
            // Ticker.equals compares symbols case-insensitively, which is the same identity the
            // watchlist uses, so AAPL and aapl are one holding here too.
            if (candidate.getTicker().equals(result.getTicker())
                    && candidate.getStrategyName().equals(result.getStrategyName())) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns every backtest completed so far.
     *
     * @return an unmodifiable view of the completed results, oldest first
     */
    public List<BacktestResult> getCompletedResults() {
        return Collections.unmodifiableList(completedResults);
    }
}
