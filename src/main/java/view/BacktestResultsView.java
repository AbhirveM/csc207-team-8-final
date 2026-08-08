package view;

import interface_adapter.backtest.BacktestViewModel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;

/**
 * Displays the result of a completed backtest.
 *
 * <p>Every value arrives from {@link BacktestViewModel} already formatted, so this panel imports
 * no entities and does no formatting of its own.
 */
public class BacktestResultsView extends JPanel {

    private static final String TICKER_LABEL = "Ticker: ";
    private static final String STRATEGY_LABEL = "Strategy: ";
    private static final String FINAL_CAPITAL_LABEL = "Final Capital: ";
    private static final String TOTAL_RETURN_LABEL = "Total Return: ";
    private static final String TRADES_LABEL = "Number of Trades: ";
    private static final String WIN_RATE_LABEL = "Win Rate: ";
    private static final String BLANK_LINE = " ";

    private final BacktestViewModel viewModel;

    private final JLabel tickerLabel = new JLabel(TICKER_LABEL);
    private final JLabel strategyLabel = new JLabel(STRATEGY_LABEL);
    private final JLabel finalCapitalLabel = new JLabel(FINAL_CAPITAL_LABEL);
    private final JLabel totalReturnLabel = new JLabel(TOTAL_RETURN_LABEL);
    private final JLabel numberOfTradesLabel = new JLabel(TRADES_LABEL);
    private final JLabel winRateLabel = new JLabel(WIN_RATE_LABEL);
    private final JLabel statusLabel = new JLabel(BLANK_LINE);

    private final DefaultTableModel tableModel;

    public BacktestResultsView(BacktestViewModel viewModel) {
        this.viewModel = viewModel;

        setLayout(new BorderLayout(8, 8));

        final JPanel summaryPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        summaryPanel.add(tickerLabel);
        summaryPanel.add(strategyLabel);
        summaryPanel.add(finalCapitalLabel);
        summaryPanel.add(totalReturnLabel);
        summaryPanel.add(numberOfTradesLabel);
        summaryPanel.add(winRateLabel);
        add(summaryPanel, BorderLayout.NORTH);

        final String[] columns = {"Entry Date", "Entry Price", "Quantity", "Exit Date",
                "Exit Price", "Return %"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        final JTable table = new JTable(tableModel);
        table.getAccessibleContext().setAccessibleName("Completed trades");
        add(new JScrollPane(table), BorderLayout.CENTER);

        statusLabel.getAccessibleContext().setAccessibleName("Status");
        add(statusLabel, BorderLayout.SOUTH);

        viewModel.addPropertyChangeListener(this::onViewModelChanged);
    }

    /**
     * Updates the displayed result whenever the view model changes.
     *
     * @param event the property-change event
     */
    private void onViewModelChanged(PropertyChangeEvent event) {
        tableModel.setRowCount(0);

        if (!viewModel.getErrorMessage().isEmpty()) {
            clearSummary();
            statusLabel.setText(viewModel.getErrorMessage());
            return;
        }

        final BacktestViewModel.Summary summary = viewModel.getSummary();
        if (summary == null) {
            clearSummary();
            statusLabel.setText(BLANK_LINE);
            return;
        }

        statusLabel.setText(BLANK_LINE);
        tickerLabel.setText(TICKER_LABEL + summary.ticker());
        strategyLabel.setText(STRATEGY_LABEL + summary.strategyName());
        finalCapitalLabel.setText(FINAL_CAPITAL_LABEL + summary.finalCapital());
        totalReturnLabel.setText(TOTAL_RETURN_LABEL + summary.totalReturn());
        numberOfTradesLabel.setText(TRADES_LABEL + summary.numberOfTrades());
        winRateLabel.setText(WIN_RATE_LABEL + summary.winRate());

        for (final BacktestViewModel.TradeRow row : viewModel.getTradeRows()) {
            tableModel.addRow(new Object[] {row.entryDate(), row.entryPrice(), row.quantity(),
                    row.exitDate(), row.exitPrice(), row.returnPercent()});
        }
    }

    /** Clears all displayed summary information. */
    private void clearSummary() {
        tickerLabel.setText(TICKER_LABEL);
        strategyLabel.setText(STRATEGY_LABEL);
        finalCapitalLabel.setText(FINAL_CAPITAL_LABEL);
        totalReturnLabel.setText(TOTAL_RETURN_LABEL);
        numberOfTradesLabel.setText(TRADES_LABEL);
        winRateLabel.setText(WIN_RATE_LABEL);
    }
}
