package view;

import interface_adapter.comparison.ComparisonController;
import interface_adapter.comparison.ComparisonViewModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

    /** The column holding a signed total return, which gets the direction renderer. */
    private static final int TOTAL_RETURN_COLUMN = 2;

    /** The columns holding plain figures. */
    private static final int[] NUMERIC_COLUMNS = {3, 4};

    /** Relative column widths: the two name columns carry words, the rest carry figures. */
    private static final int[] COLUMN_WIDTHS = {70, 180, 110, 80, 90};

    private final DefaultTableModel tableModel;
    private final JLabel bestStrategyLabel;
    private final JLabel statusLabel;
    private final ComparisonViewModel viewModel;

    public ComparisonView(ComparisonViewModel viewModel, ComparisonController controller) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(Theme.MD, Theme.MD));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.LG, Theme.LG, Theme.LG, Theme.LG));

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
        TableStyler.style(table);
        TableStyler.numericColumns(table, NUMERIC_COLUMNS);
        // Total return is the column the whole screen exists to rank on, so it carries the
        // sign and the direction colour rather than sitting as an unsigned figure.
        TableStyler.signedColumns(table, TOTAL_RETURN_COLUMN);
        TableStyler.preferredWidths(table, COLUMN_WIDTHS);
        final JLabel rankingHeading = new JLabel("Ranking");
        rankingHeading.setLabelFor(table);
        add(PanelHeader.region(rankingHeading, PanelHeader.rowCount(table), TableStyler.wrap(table)),
                BorderLayout.CENTER);

        final JPanel topPanel = new JPanel(new BorderLayout(Theme.MD, 0));
        topPanel.setBackground(Theme.BG);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.MD, 0));
        bestStrategyLabel = new JLabel("Run a backtest for at least one ticker, then click Compare.");
        bestStrategyLabel.setFont(Theme.FONT_UI);
        bestStrategyLabel.setForeground(Theme.FG);
        final JButton compareButton = new JButton("Compare Completed Backtests");
        compareButton.setMnemonic('C');
        compareButton.setToolTipText("Rank every backtest completed this session by total return.");
        Controls.primary(compareButton);
        topPanel.add(bestStrategyLabel, BorderLayout.CENTER);
        topPanel.add(compareButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.FONT_UI);
        statusLabel.setForeground(Theme.FG_MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(Theme.SM, 0, 0, 0));
        statusLabel.getAccessibleContext().setAccessibleName("Status");
        add(statusLabel, BorderLayout.SOUTH);

        compareButton.addActionListener(event -> controller.compare());

        viewModel.addPropertyChangeListener(this::onViewModelChanged);
    }

    private void onViewModelChanged(PropertyChangeEvent event) {
        tableModel.setRowCount(0);
        if (!viewModel.getErrorMessage().isEmpty()) {
            bestStrategyLabel.setText("Run a backtest for at least one ticker, then click Compare.");
            // Prefixed as well as coloured: the words are what carry the meaning, and the
            // colour is only there to find them.
            statusLabel.setText("Error: " + viewModel.getErrorMessage());
            statusLabel.setForeground(Theme.DOWN);
            return;
        }
        statusLabel.setText(" ");
        statusLabel.setForeground(Theme.FG_MUTED);
        bestStrategyLabel.setText("Best performing strategy: " + viewModel.getBestStrategyName());
        for (final ComparisonViewModel.ResultRow row : viewModel.getRankedResults()) {
            tableModel.addRow(new Object[] {row.ticker(), row.strategyName(), row.totalReturn(),
                    row.numberOfTrades(), row.winRate()});
        }
    }
}
