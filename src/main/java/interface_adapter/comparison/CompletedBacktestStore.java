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
 * <p><strong>Nothing calls {@link #add} yet.</strong> The run-backtest use case is implemented and
 * tested but is not constructed in {@code Main}, so no backtest can finish and this store is
 * always empty - which is why the Compare screen shows its empty state. Whoever wires the backtest
 * path should have {@code BacktestPresenter} call {@link #add} once a run succeeds.
 */
public class CompletedBacktestStore {

    private final List<BacktestResult> completedResults = new ArrayList<>();

    /**
     * Records a finished backtest.
     *
     * @param result the completed result; must be non-null
     */
    public void add(BacktestResult result) {
        completedResults.add(result);
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
