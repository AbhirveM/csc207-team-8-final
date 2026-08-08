package view;

import entity.BacktestResult;
import interface_adapter.comparison.ComparisonController;
import interface_adapter.comparison.ComparisonViewModel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.util.List;

public class ComparisonView extends JPanel {
    private final DefaultTableModel tableModel;
    private final JLabel bestStrategyLabel;
    private final JLabel statusLabel;
    private final ComparisonViewModel viewModel;

    public ComparisonView(ComparisonViewModel viewModel, ComparisonController controller) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(8, 8));

        String[] columns = {"Ticker", "Strategy", "Total Return %", "# Trades", "Win Rate %"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        bestStrategyLabel = new JLabel("Run a backtest for at least one ticker, then click Compare.");
        JButton compareButton = new JButton("Compare Completed Backtests");
        topPanel.add(bestStrategyLabel, BorderLayout.CENTER);
        topPanel.add(compareButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        add(statusLabel, BorderLayout.SOUTH);

        // Wire the button to the controller - MainView is responsible for supplying
        // the actual list of completed BacktestResults (it owns overall app state).
        compareButton.addActionListener(e -> {
            List<BacktestResult> completed = MainAppState.getInstance().getCompletedResults();
            controller.compare(completed);
        });

        viewModel.addPropertyChangeListener(this::onViewModelChanged);
    }

    private void onViewModelChanged(PropertyChangeEvent evt) {
        tableModel.setRowCount(0);
        if (!viewModel.getErrorMessage().isEmpty()) {
            statusLabel.setText(viewModel.getErrorMessage());
            return;
        }
        statusLabel.setText(" ");
        bestStrategyLabel.setText("Best performing strategy: " + viewModel.getBestStrategyName());
        for (BacktestResult r : viewModel.getRankedResults()) {
            tableModel.addRow(new Object[]{
                    r.getTicker().getSymbol(),
                    r.getStrategyName(),
                    String.format("%.2f", r.getTotalReturn()),
                    r.getNumberOfTrades(),
                    String.format("%.2f", r.getWinRate())
            });
        }
    }
}
