package interface_adapter.comparison;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;

/**
 * The observable state of the Compare Strategies screen.
 *
 * <p>No Swing imports here on purpose - the view observes this via {@link PropertyChangeSupport},
 * keeping the view model framework-agnostic. It also carries no entities: every field is a
 * display-ready {@code String} produced by the presenter, so the view never formats, rounds
 * or localises anything and the view layer never needs to import {@code entity}.
 */
public class ComparisonViewModel {

    public static final String VIEW_NAME = "comparison";
    public static final String RESULTS_PROPERTY = "results";

    /**
     * One row of the comparison table, already formatted for display.
     *
     * <p>The two {@code Value} components carry the same numbers as the text components, unrounded.
     * A table needs the text - it must not decide how many decimal places to show - but a chart
     * needs the number, and parsing {@code "12.40"} back out of the string in the view would put
     * formatting knowledge in exactly the place this record exists to keep it out of. Both are
     * present so neither consumer has to convert.
     *
     * @param ticker           the ticker symbol the backtest ran against
     * @param strategyName     the name of the strategy
     * @param totalReturn      the total return as a percentage, as text
     * @param numberOfTrades   how many trades the run produced, as text
     * @param winRate          the win rate as a percentage, as text
     * @param totalReturnValue the total return as a percentage, unrounded, for plotting
     * @param winRateValue     the win rate as a percentage, unrounded, for plotting
     */
    public record ResultRow(String ticker, String strategyName, String totalReturn,
                            String numberOfTrades, String winRate,
                            double totalReturnValue, double winRateValue) {
    }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private List<ResultRow> rankedResults = Collections.emptyList();
    private String bestStrategyName = "";
    private String errorMessage = "";

    /**
     * Shows a completed comparison.
     *
     * @param rankedResults    the rows to show, best first
     * @param bestStrategyName the name of the winning strategy
     */
    public void setResults(List<ResultRow> rankedResults, String bestStrategyName) {
        this.rankedResults = List.copyOf(rankedResults);
        this.bestStrategyName = bestStrategyName;
        this.errorMessage = "";
        support.firePropertyChange(RESULTS_PROPERTY, null, this.rankedResults);
    }

    /**
     * Shows a failure, and clears any results that were on screen.
     *
     * <p>Clearing is deliberate: leaving the previous ranking visible underneath an error message
     * would present stale data as though it were current.
     *
     * @param errorMessage the worded explanation to show
     */
    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.rankedResults = Collections.emptyList();
        this.bestStrategyName = "";
        support.firePropertyChange(RESULTS_PROPERTY, null, null);
    }

    public List<ResultRow> getRankedResults() {
        return rankedResults;
    }

    public String getBestStrategyName() {
        return bestStrategyName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Subscribes a listener to state changes.
     *
     * @param listener the listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
