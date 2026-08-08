package interface_adapter.backtest;

import entity.BacktestResult;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Stores the result of the most recently completed backtest.
 *
 * No Swing imports are used here so the view model remains
 * framework-independent.
 */
public class BacktestViewModel {

    public static final String VIEW_NAME = "backtest";
    public static final String RESULT_PROPERTY = "result";

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    private BacktestResult result;
    private String errorMessage = "";

    public void setResult(BacktestResult result) {
        this.result = result;
        this.errorMessage = "";

        support.firePropertyChange(
                RESULT_PROPERTY,
                null,
                result);
    }

    public void setError(String errorMessage) {
        this.result = null;
        this.errorMessage = errorMessage;

        support.firePropertyChange(
                RESULT_PROPERTY,
                null,
                null);
    }

    public BacktestResult getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {

        support.addPropertyChangeListener(listener);
    }
}
