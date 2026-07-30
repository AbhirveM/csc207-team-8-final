package interface_adapter.comparison;

import entity.BacktestResult;

import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;

/**
 * No Swing imports here on purpose - the view observes this via
 * PropertyChangeSupport, keeping the view model framework-agnostic.
 */
public class ComparisonViewModel {
    public static final String VIEW_NAME = "comparison";
    public static final String RESULTS_PROPERTY = "results";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private List<BacktestResult> rankedResults = Collections.emptyList();
    private String bestStrategyName = "";
    private String errorMessage = "";

    public void setResults(List<BacktestResult> rankedResults, String bestStrategyName) {
        this.rankedResults = rankedResults;
        this.bestStrategyName = bestStrategyName;
        this.errorMessage = "";
        support.firePropertyChange(RESULTS_PROPERTY, null, rankedResults);
    }

    public void setError(String errorMessage) {
        this.errorMessage = errorMessage;
        support.firePropertyChange(RESULTS_PROPERTY, null, null);
    }

    public List<BacktestResult> getRankedResults() { return rankedResults; }
    public String getBestStrategyName() { return bestStrategyName; }
    public String getErrorMessage() { return errorMessage; }

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
