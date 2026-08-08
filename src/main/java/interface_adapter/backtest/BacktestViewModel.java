package interface_adapter.backtest;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.List;

/**
 * The observable state of the backtest results screen.
 *
 * <p>No Swing imports are used here so the view model remains framework-independent, and no
 * entities either: {@link BacktestPresenter} formats everything, so {@code view} never imports
 * {@code entity}.
 */
public class BacktestViewModel {

    public static final String VIEW_NAME = "backtest";
    public static final String RESULT_PROPERTY = "result";

    /**
     * The headline figures of a completed run, already formatted for display.
     *
     * @param ticker         the ticker symbol the run was against
     * @param strategyName   the name of the strategy that was run
     * @param finalCapital   the closing capital, as text
     * @param totalReturn    the total return as a percentage, as text
     * @param numberOfTrades how many trades the run produced, as text
     * @param winRate        the win rate as a percentage, as text
     */
    public record Summary(String ticker, String strategyName, String finalCapital,
                          String totalReturn, String numberOfTrades, String winRate) {
    }

    /**
     * One completed trade, already formatted for display.
     *
     * @param entryDate     the date the position was opened, as text
     * @param entryPrice    the price it was opened at, as text
     * @param quantity      how many shares were traded, as text
     * @param exitDate      the date the position was closed, as text
     * @param exitPrice     the price it was closed at, as text
     * @param returnPercent the trade's return as a percentage, as text
     */
    public record TradeRow(String entryDate, String entryPrice, String quantity,
                           String exitDate, String exitPrice, String returnPercent) {
    }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private Summary summary;
    private List<TradeRow> tradeRows = Collections.emptyList();
    private String errorMessage = "";

    /**
     * Shows a completed run.
     *
     * @param summary   the headline figures
     * @param tradeRows the trade log, oldest first
     */
    public void setResult(Summary summary, List<TradeRow> tradeRows) {
        this.summary = summary;
        this.tradeRows = List.copyOf(tradeRows);
        this.errorMessage = "";
        support.firePropertyChange(RESULT_PROPERTY, null, summary);
    }

    /**
     * Shows a failure, and clears any result that was on screen.
     *
     * @param errorMessage the worded explanation to show
     */
    public void setError(String errorMessage) {
        this.summary = null;
        this.tradeRows = Collections.emptyList();
        this.errorMessage = errorMessage;
        support.firePropertyChange(RESULT_PROPERTY, null, null);
    }

    /**
     * Returns the headline figures of the last successful run.
     *
     * @return the summary, or {@code null} when nothing has run or the last run failed
     */
    public Summary getSummary() {
        return summary;
    }

    public List<TradeRow> getTradeRows() {
        return tradeRows;
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
