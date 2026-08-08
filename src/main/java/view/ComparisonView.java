package view;

import interface_adapter.comparison.ComparisonController;
import interface_adapter.comparison.ComparisonViewModel;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;

/**
 * The Swing panel for comparing completed backtests.
 *
 * <p>Every value arrives from {@link ComparisonViewModel} already formatted, so this panel imports
 * no entities and does no formatting of its own.
 */
public class ComparisonView extends JPanel {

    private final DefaultTableModel tableModel;
    private final JLabel bestStrategyLabel;
    private final JLabel statusLabel;
    private final ComparisonViewModel viewModel;

    public ComparisonView(ComparisonViewModel viewModel, ComparisonController controller) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(8, 8));

        final String[] columns = {"Ticker", "Strategy", "Total Return %", "# Trades", "Win Rate %"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        final JTable table = new JTable(tableModel);
        table.getAccessibleContext().setAccessibleName("Strategy comparison");
        // Release Tab / Shift+Tab so focus can leave the table instead of cycling its cells.
        // JTable installs its own traversal keys (Tab moves between cells); without this a
        // keyboard-only user who tabs into the table can never tab back out. Arrow keys still
        // move between cells. Same fix WatchlistView applies to its tables.
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        add(new JScrollPane(table), BorderLayout.CENTER);

        final JPanel topPanel = new JPanel(new BorderLayout());
        bestStrategyLabel = new JLabel("Run a backtest for at least one ticker, then click Compare.");
        final JButton compareButton = new JButton("Compare Completed Backtests");
        compareButton.setMnemonic('C');
        compareButton.setToolTipText("Rank every backtest completed this session by total return.");
        topPanel.add(bestStrategyLabel, BorderLayout.CENTER);
        topPanel.add(compareButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        statusLabel.getAccessibleContext().setAccessibleName("Status");
        add(statusLabel, BorderLayout.SOUTH);

        compareButton.addActionListener(event -> controller.compare());

        viewModel.addPropertyChangeListener(this::onViewModelChanged);
    }

    private void onViewModelChanged(PropertyChangeEvent event) {
        tableModel.setRowCount(0);
        if (!viewModel.getErrorMessage().isEmpty()) {
            bestStrategyLabel.setText("Run a backtest for at least one ticker, then click Compare.");
            statusLabel.setText(viewModel.getErrorMessage());
            return;
        }
        statusLabel.setText(" ");
        bestStrategyLabel.setText("Best performing strategy: " + viewModel.getBestStrategyName());
        for (final ComparisonViewModel.ResultRow row : viewModel.getRankedResults()) {
            tableModel.addRow(new Object[] {row.ticker(), row.strategyName(), row.totalReturn(),
                    row.numberOfTrades(), row.winRate()});
        }
    }
}
