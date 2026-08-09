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

    /**
     * The path the run took, together with the axis labels and the spoken summary the
     * presenter has already formatted for it.
     *
     * <p>The same six components as {@code WatchlistState.PriceChart}, and for the same reason:
     * it mirrors {@code LineChart.Series} so the view can hand it straight over without either
     * layer importing the other's types.
     *
     * @param values     the portfolio value at every close, oldest first
     * @param lowLabel   the lowest value the portfolio reached, formatted
     * @param highLabel  the highest value the portfolio reached, formatted
     * @param startLabel the date of the first close
     * @param endLabel   the date of the last close
     * @param meta       the compact signed readout for the header band's meta slot, which has
     *                   room for about four words beside the region title
     * @param summary    the full sentence, spoken as the chart's accessible description; it
     *                   states the direction in words and with an explicit sign, so the line's
     *                   colour carries nothing on its own
     */
    public record EquityCurve(List<Double> values, String lowLabel, String highLabel,
                              String startLabel, String endLabel, String meta, String summary) {

        /** @return the curve shown before a run, and after one that failed. */
        public static EquityCurve empty() {
            return new EquityCurve(List.of(), "", "", "", "", "", "No data.");
        }
    }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private Summary summary;
    private List<TradeRow> tradeRows = Collections.emptyList();
    private EquityCurve equityCurve = EquityCurve.empty();
    private String errorMessage = "";

    /**
     * Shows a completed run.
     *
     * @param summary   the headline figures
     * @param tradeRows the trade log, oldest first
     * @param equityCurve the path the portfolio took, for the chart between the two
     */
    public void setResult(Summary summary, List<TradeRow> tradeRows, EquityCurve equityCurve) {
        this.summary = summary;
        this.tradeRows = List.copyOf(tradeRows);
        this.equityCurve = equityCurve;
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
        // Cleared with the rest of the result: a curve left under an error message is a picture
        // of a run that is no longer on screen.
        this.equityCurve = EquityCurve.empty();
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

    /**
     * @return the path the last successful run took. Never null; carries no values before a
     *         run and after a failure.
     */
    public EquityCurve getEquityCurve() {
        return equityCurve;
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
