package view;

import entity.BacktestResult;
import entity.Trade;
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
 */
public class BacktestResultsView extends JPanel {

    private final BacktestViewModel viewModel;

    private final JLabel tickerLabel;
    private final JLabel strategyLabel;
    private final JLabel finalCapitalLabel;
    private final JLabel totalReturnLabel;
    private final JLabel numberOfTradesLabel;
    private final JLabel winRateLabel;
    private final JLabel statusLabel;

    private final DefaultTableModel tableModel;

    public BacktestResultsView(
            BacktestViewModel viewModel) {

        this.viewModel = viewModel;

        setLayout(
                new BorderLayout(8, 8));

        /*
         * Summary information.
         */
        final JPanel summaryPanel =
                new JPanel(
                        new GridLayout(
                                3,
                                2,
                                8,
                                8));

        tickerLabel =
                new JLabel("Ticker: ");

        strategyLabel =
                new JLabel("Strategy: ");

        finalCapitalLabel =
                new JLabel("Final Capital: ");

        totalReturnLabel =
                new JLabel("Total Return: ");

        numberOfTradesLabel =
                new JLabel("Number of Trades: ");

        winRateLabel =
                new JLabel("Win Rate: ");

        summaryPanel.add(tickerLabel);
        summaryPanel.add(strategyLabel);
        summaryPanel.add(finalCapitalLabel);
        summaryPanel.add(totalReturnLabel);
        summaryPanel.add(numberOfTradesLabel);
        summaryPanel.add(winRateLabel);

        add(
                summaryPanel,
                BorderLayout.NORTH);

        /*
         * Completed trade log.
         */
        final String[] columns = {
                "Entry Date",
                "Entry Price",
                "Exit Date",
                "Exit Price",
                "Return %"
        };

        tableModel =
                new DefaultTableModel(
                        columns,
                        0) {

                    @Override
                    public boolean isCellEditable(
                            int row,
                            int column) {

                        return false;
                    }
                };

        final JTable table =
                new JTable(tableModel);

        add(
                new JScrollPane(table),
                BorderLayout.CENTER);

        statusLabel =
                new JLabel(" ");

        add(
                statusLabel,
                BorderLayout.SOUTH);

        viewModel.addPropertyChangeListener(
                this::onViewModelChanged);
    }

    /**
     * Updates the displayed result whenever the
     * BacktestViewModel changes.
     *
     * @param evt the property-change event
     */
    private void onViewModelChanged(
            PropertyChangeEvent evt) {

        tableModel.setRowCount(0);

        if (!viewModel
                .getErrorMessage()
                .isEmpty()) {

            clearSummary();

            statusLabel.setText(
                    viewModel.getErrorMessage());

            return;
        }

        final BacktestResult result =
                viewModel.getResult();

        if (result == null) {
            clearSummary();
            statusLabel.setText(" ");
            return;
        }

        statusLabel.setText("");

        tickerLabel.setText(
                "Ticker: "
                        + result.getTicker().getSymbol());

        strategyLabel.setText(
                "Strategy: "
                        + result.getStrategyName());

        finalCapitalLabel.setText(
                String.format(
                        "Final Capital: $%.2f",
                        result.getFinalCapital()));

        totalReturnLabel.setText(
                String.format(
                        "Total Return: %.2f%%",
                        result.getTotalReturn()));

        numberOfTradesLabel.setText(
                "Number of Trades: "
                        + result.getNumberOfTrades());

        winRateLabel.setText(
                String.format(
                        "Win Rate: %.2f%%",
                        result.getWinRate()));

        for (Trade trade : result.getTradeLog()) {

            tableModel.addRow(
                    new Object[] {
                            trade.getEntryDate(),
                            String.format(
                                    "$%.2f",
                                    trade.getEntryPrice()),
                            trade.getExitDate(),
                            String.format(
                                    "$%.2f",
                                    trade.getExitPrice()),
                            String.format(
                                    "%.2f",
                                    trade.getReturnPercent())
                    });
        }
    }

    /**
     * Clears all displayed summary information.
     */
    private void clearSummary() {
        tickerLabel.setText("Ticker: ");
        strategyLabel.setText("Strategy: ");
        finalCapitalLabel.setText("Final Capital: ");
        totalReturnLabel.setText("Total Return: ");
        numberOfTradesLabel.setText("Number of Trades: ");
        winRateLabel.setText("Win Rate: ");
    }
}
