package view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;

import interface_adapter.watchlist.WatchlistController;
import interface_adapter.watchlist.WatchlistState;
import interface_adapter.watchlist.WatchlistViewModel;

/**
 * The Swing panel for watchlist management.
 *
 * <p>This class is deliberately dumb. Every field of {@link WatchlistState} arrives as a
 * display-ready {@code String}, so the panel never composes, pads, rounds or localises
 * anything - it copies strings into widgets. The only thing it branches on is
 * {@link WatchlistState#isErrorPresent()}, and the only user-facing prose it owns is the
 * literal {@code "Error: "} prefix and the static control labels.
 *
 * <p>Threading. The controller blocks on the network, so every button handler runs its
 * call inside a {@link SwingWorker} and the buttons stay disabled until {@code done}.
 * Because the presenter is synchronous it then updates the view model from that worker
 * thread, which means {@link #onViewModelChanged(PropertyChangeEvent)} is the sole
 * re-entry point into Swing and must marshal itself back onto the event dispatch thread.
 *
 * <p>Quota. Price history is never hydrated automatically at start-up: a restored
 * eight-ticker watchlist would spend eight of a roughly twenty-five request daily
 * allowance the moment the window opens. Hydration is user-driven through the
 * "Load prices" button, which refreshes tickers one at a time and stops the moment the
 * service reports that the request limit has been reached.
 */
public class WatchlistView extends JPanel {

    /** The prefix that turns an error message into prose rather than a colour cue. */
    private static final String ERROR_PREFIX = "Error: ";

    /**
     * The opening sentence of the presenter's rate-limit message. "Load prices" compares
     * the current error against this to decide whether to keep spending the daily quota.
     */
    private static final String RATE_LIMIT_PREFIX =
            "The market data service request limit has been reached.";

    /** What a label with no text to show renders, so its row keeps its height. */
    private static final String BLANK_LINE = " ";

    /** The column of the watchlist table that holds the symbol. */
    private static final int SYMBOL_COLUMN = 0;

    /**
     * The watchlist columns that hold figures - the price-day count and the latest close -
     * indexed as {@code WatchlistViewModel.TICKER_COLUMNS} declares them.
     */
    private static final int[] TICKER_NUMERIC_COLUMNS = {2, 4};

    /**
     * The daily-price columns that hold figures: everything except the date, indexed as
     * {@code WatchlistViewModel.PRICE_COLUMNS} declares them.
     */
    private static final int[] PRICE_NUMERIC_COLUMNS = {1, 2, 3, 4, 5};

    /**
     * Relative column widths for the watchlist table. The price-day count is wider than a
     * count suggests because the presenter substitutes the words "Not loaded" into it, and
     * the numeric renderer sets that column in monospace.
     */
    private static final int[] TICKER_COLUMN_WIDTHS = {58, 113, 88, 88, 78};

    /** Relative column widths for the daily-price table: dates and volumes are the long ones. */
    private static final int[] PRICE_COLUMN_WIDTHS = {95, 68, 68, 68, 68, 78};

    private final WatchlistViewModel viewModel;
    private final WatchlistController controller;

    private final JTextField tickerField = new JTextField(10);
    private final JButton addButton = new JButton("Add");
    private final JButton removeButton = new JButton("Remove");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton loadPricesButton = new JButton("Load prices");
    private final List<JButton> buttons =
            List.of(addButton, removeButton, refreshButton, loadPricesButton);

    private final DefaultTableModel tickerTableModel =
            new ReadOnlyTableModel(WatchlistViewModel.TICKER_COLUMNS);
    private final DefaultTableModel priceTableModel =
            new ReadOnlyTableModel(WatchlistViewModel.PRICE_COLUMNS);
    private final JTable tickerTable = new JTable(tickerTableModel);
    private final JTable priceTable = new JTable(priceTableModel);

    private final JLabel statusLabel = new JLabel(BLANK_LINE);
    private final JLabel errorLabel = new JLabel(BLANK_LINE);

    /**
     * Whether the watchlist table is being repopulated. Repopulating fires selection
     * events, and acting on them would call Show Watchlist recursively.
     */
    private boolean repopulating;

    /**
     * Builds the panel and subscribes it to the view model.
     *
     * @param viewModel  the observable state this panel paints; must be non-null
     * @param controller the boundary every control calls; must be non-null
     * @throws NullPointerException if either argument is null
     */
    public WatchlistView(WatchlistViewModel viewModel, WatchlistController controller) {
        this.viewModel = Objects.requireNonNull(viewModel, "View model cannot be null");
        this.controller = Objects.requireNonNull(controller, "Controller cannot be null");

        setLayout(new BorderLayout(Theme.MD, Theme.MD));
        setBackground(Theme.BG);
        // The one outer margin for the screen. Children keep no padding of their own, so the
        // gap at the window edge stays a single number rather than a sum of four opinions.
        setBorder(BorderFactory.createEmptyBorder(Theme.LG, Theme.LG, Theme.LG, Theme.LG));
        add(buildControls(), BorderLayout.NORTH);
        add(buildSplitPane(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        // After the tables exist: the renderers attach to columns, which only exist once the
        // models have been handed to the tables.
        TableStyler.numericColumns(tickerTable, TICKER_NUMERIC_COLUMNS);
        TableStyler.numericColumns(priceTable, PRICE_NUMERIC_COLUMNS);
        TableStyler.preferredWidths(tickerTable, TICKER_COLUMN_WIDTHS);
        TableStyler.preferredWidths(priceTable, PRICE_COLUMN_WIDTHS);
        installFocusOrder();

        addButton.addActionListener(event -> onAdd());
        removeButton.addActionListener(event -> onRemove());
        refreshButton.addActionListener(event -> onRefresh());
        loadPricesButton.addActionListener(event -> onLoadPrices());
        tickerTable.getSelectionModel().addListSelectionListener(this::onTickerSelected);

        viewModel.addPropertyChangeListener(this::onViewModelChanged);
        render(viewModel.getState());
    }

    /**
     * Builds the northern control strip: the labelled ticker field and the four buttons.
     *
     * @return the assembled panel
     */
    private JComponent buildControls() {
        final JLabel tickerFieldLabel = Controls.fieldLabel(new JLabel("Ticker symbol:"));
        tickerFieldLabel.setLabelFor(tickerField);
        tickerFieldLabel.setDisplayedMnemonic('T');
        tickerField.setToolTipText("A ticker symbol such as AAPL.");
        tickerField.getAccessibleContext().setAccessibleName("Ticker symbol");
        Controls.styleField(tickerField);

        describe(addButton, 'A', "Add the ticker symbol in the field to your watchlist.");
        describe(removeButton, 'M', "Remove the ticker symbol in the field from your watchlist.");
        describe(refreshButton, 'R', "Re-fetch price history for the ticker symbol in the field.");
        describe(loadPricesButton, 'L',
                "Fetch price history for every ticker on the watchlist, one at a time.");

        // Adding a ticker is what this screen is for, so it is the one primary action; the
        // other three operate on something already in the list.
        Controls.primary(addButton);
        Controls.secondary(removeButton);
        Controls.secondary(refreshButton);
        Controls.secondary(loadPricesButton);

        // A box row rather than a FlowLayout: FlowLayout centres its children and re-wraps
        // them as the window narrows, which is what made this strip drift out of alignment
        // with the table headings below it.
        final Box controls = Box.createHorizontalBox();
        controls.add(tickerFieldLabel);
        controls.add(Box.createHorizontalStrut(Theme.SM));
        controls.add(tickerField);
        controls.add(Box.createHorizontalStrut(Theme.MD));
        for (final JButton button : buttons) {
            controls.add(button);
            controls.add(Box.createHorizontalStrut(Theme.SM));
        }
        controls.add(Box.createHorizontalGlue());
        controls.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.MD, 0));
        return controls;
    }

    /**
     * Builds the two tables, each under its own visible heading.
     *
     * @return the split pane holding the watchlist and the daily prices
     */
    private JSplitPane buildSplitPane() {
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildTablePanel("Watchlist", tickerTable),
                buildTablePanel("Daily prices", priceTable));
        splitPane.setResizeWeight(0.5);
        // The divider is a rule the user can drag, not a piece of furniture: one pixel wide,
        // no bevel, and no one-touch arrows hanging off it.
        splitPane.setDividerSize(Theme.XS);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(Theme.RULE);
        splitPane.setOneTouchExpandable(false);
        splitPane.setContinuousLayout(true);
        return splitPane;
    }

    /**
     * Builds the southern status and error lines.
     *
     * @return a panel holding the status label above the error label
     */
    private JPanel buildFooter() {
        // BorderLayout, not GridLayout(2, 1): the grid gave the status line and the error
        // line equal heights, which reads as two equally important rows when the error is
        // the one that matters and is usually blank.
        final JPanel footer = new JPanel(new BorderLayout(0, Theme.XS));
        footer.setBackground(Theme.BG);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.MD, 0, 0, 0));
        statusLabel.setFocusable(true);
        statusLabel.setFont(Theme.FONT_UI);
        statusLabel.setForeground(Theme.FG_MUTED);
        statusLabel.getAccessibleContext().setAccessibleName("Status");
        errorLabel.setFont(Theme.FONT_UI);
        errorLabel.setForeground(Theme.DOWN);
        errorLabel.getAccessibleContext().setAccessibleName("Error");
        footer.add(statusLabel, BorderLayout.NORTH);
        footer.add(errorLabel, BorderLayout.SOUTH);
        return footer;
    }

    /**
     * Wraps a table in a scroll pane under a visible heading that labels it.
     *
     * @param heading the visible heading text
     * @param table   the table the heading labels
     * @return the assembled panel
     */
    private static JPanel buildTablePanel(final String heading, final JTable table) {
        final JLabel headingLabel = Controls.heading(new JLabel(heading));
        headingLabel.setLabelFor(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getAccessibleContext().setAccessibleName(heading);
        TableStyler.style(table);

        /*
         * Release Tab and Shift+Tab back to the focus traversal policy.
         *
         * JTable installs its own traversal key sets so that Tab moves between
         * *cells* rather than between components. In a grid that is arguably
         * useful; here it is a keyboard trap. The watchlist normally holds a
         * handful of rows, so Tab walks the cells and returns to the first one
         * forever, and a keyboard-only user can enter either table but never
         * leave it. With a single ticker it is unmissable: Tab just cycles
         * across that one row's five columns.
         *
         * Passing null does not disable traversal - it clears JTable's override
         * so the component inherits the container's keys, which is exactly the
         * Tab / Shift+Tab that installFocusOrder's policy already sequences.
         * Arrow keys still move between cells, so nothing is lost.
         */
        table.setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        table.setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        final JPanel panel = new JPanel(new BorderLayout(0, Theme.XS));
        panel.setBackground(Theme.BG);
        panel.setBorder(BorderFactory.createEmptyBorder(0, Theme.SM, 0, Theme.SM));
        panel.add(headingLabel, BorderLayout.NORTH);
        panel.add(TableStyler.wrap(table), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Gives a button its keyboard mnemonic and its tooltip.
     *
     * @param button   the button to describe
     * @param mnemonic the mnemonic character
     * @param tooltip  the tooltip prose
     */
    private static void describe(final JButton button, final char mnemonic,
                                 final String tooltip) {
        button.setMnemonic(mnemonic);
        button.setToolTipText(tooltip);
    }

    /**
     * Installs the tab order, which is the same as the reading order of the layout.
     */
    private void installFocusOrder() {
        final List<Component> order = List.of(tickerField, addButton, removeButton,
                refreshButton, loadPricesButton, tickerTable, priceTable, statusLabel);
        setFocusTraversalPolicy(new OrderedFocusTraversalPolicy(order));
        setFocusTraversalPolicyProvider(true);
    }

    /** Adds the symbol currently in the ticker field. */
    private void onAdd() {
        final String rawSymbol = tickerField.getText();
        runInBackground(() -> controller.addTicker(rawSymbol));
    }

    /** Removes the symbol currently in the ticker field. */
    private void onRemove() {
        final String rawSymbol = tickerField.getText();
        runInBackground(() -> controller.removeTicker(rawSymbol));
    }

    /** Re-fetches price history for the symbol currently in the ticker field. */
    private void onRefresh() {
        final String rawSymbol = tickerField.getText();
        runInBackground(() -> controller.refreshTicker(rawSymbol));
    }

    /**
     * Hydrates the whole watchlist from one worker, one ticker at a time, stopping as soon
     * as the service reports that the request limit has been reached. Sequential rather
     * than parallel on purpose: the free tier allows roughly twenty-five requests a day.
     */
    private void onLoadPrices() {
        final List<String> symbols = new ArrayList<>();
        for (final WatchlistState.TickerRow row : viewModel.getState().getTickerRows()) {
            symbols.add(row.symbol());
        }
        runInBackground(() -> {
            for (final String symbol : symbols) {
                controller.refreshTicker(symbol);
                if (viewModel.getState().getErrorMessage().startsWith(RATE_LIMIT_PREFIX)) {
                    return;
                }
            }
        });
    }

    /**
     * Shows the prices for the newly selected ticker.
     *
     * <p>Ignored while the table is being repopulated, because replacing the rows fires a
     * selection event that would otherwise drive Show Watchlist recursively.
     *
     * @param event the selection event
     */
    private void onTickerSelected(final ListSelectionEvent event) {
        if (event.getValueIsAdjusting() || repopulating) {
            return;
        }
        final int row = tickerTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        controller.showWatchlist((String) tickerTableModel.getValueAt(row, SYMBOL_COLUMN));
    }

    /**
     * Runs a blocking controller call off the event thread with the buttons disabled.
     *
     * <p>{@code done} runs on the event thread whether the call returned or threw, so the
     * buttons come back either way; a call that fails has already pushed an error state
     * through the presenter, which leaves {@code done} nothing else to do.
     *
     * @param call the controller call to make on the worker thread
     */
    private void runInBackground(final Runnable call) {
        setButtonsEnabled(false);
        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                call.run();
                return null;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
            }
        }.execute();
    }

    /**
     * Enables or disables every control that can start a use case.
     *
     * <p>The watchlist table is disabled alongside the buttons, and that is not cosmetic:
     * selecting a row drives Show Watchlist, so a click landing while a worker is in flight
     * would push a second {@code setState} from the event thread concurrently with the
     * worker's own. {@code prepareFailView} reads the current state and writes a derived one,
     * which is not atomic, so the interleaving loses an update and renders something stale.
     * Freezing the table for the duration is the cheapest way to remove the only genuine race
     * in this vertical, and it incidentally stops a selection from overwriting a symbol the
     * user is midway through typing.
     *
     * @param enabled whether the controls should accept input
     */
    private void setButtonsEnabled(final boolean enabled) {
        for (final JButton button : buttons) {
            button.setEnabled(enabled);
        }
        tickerTable.setEnabled(enabled);
    }

    /**
     * The sole re-entry point into Swing, and therefore the place the worker thread is
     * marshalled back onto the event dispatch thread.
     *
     * @param event the property change event, whose payload is ignored in favour of
     *              re-reading the whole state from the view model
     */
    private void onViewModelChanged(final PropertyChangeEvent event) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onViewModelChanged(event));
            return;
        }
        render(viewModel.getState());
    }

    /**
     * Copies the state into the widgets. Every value is already display-ready.
     *
     * @param state the state to paint
     */
    private void render(final WatchlistState state) {
        repopulating = true;
        try {
            tickerTableModel.setRowCount(0);
            for (final WatchlistState.TickerRow row : state.getTickerRows()) {
                tickerTableModel.addRow(new Object[] {row.symbol(), row.companyName(),
                        row.priceCount(), row.latestDate(), row.latestClose()});
            }
            restoreSelection(state.getSelectedSymbol());
        }
        finally {
            repopulating = false;
        }

        priceTableModel.setRowCount(0);
        for (final WatchlistState.PriceRow row : state.getPriceRows()) {
            priceTableModel.addRow(new Object[] {row.date(), row.open(), row.high(),
                    row.low(), row.close(), row.volume()});
        }

        tickerField.setText(state.getTickerFieldText());
        statusLabel.setText(state.getStatusMessage());
        statusLabel.getAccessibleContext().setAccessibleDescription(state.getStatusMessage());
        renderError(state);
    }

    /**
     * Paints the error line. The error is carried in words, always; the colour set once at
     * construction is decoration layered on top and never the only signal. When there is no
     * error the line holds a space rather than being hidden, so nothing below it moves.
     *
     * @param state the state to paint
     */
    private void renderError(final WatchlistState state) {
        if (state.isErrorPresent()) {
            errorLabel.setText(ERROR_PREFIX + state.getErrorMessage());
            errorLabel.getAccessibleContext()
                    .setAccessibleDescription(ERROR_PREFIX + state.getErrorMessage());
        }
        else {
            errorLabel.setText(BLANK_LINE);
            errorLabel.getAccessibleContext().setAccessibleDescription("No error.");
        }
    }

    /**
     * Re-selects the row the state says is selected, after the model was replaced.
     *
     * @param selectedSymbol the symbol to select, or "" when nothing is selected
     */
    private void restoreSelection(final String selectedSymbol) {
        for (int row = 0; row < tickerTableModel.getRowCount(); row++) {
            if (selectedSymbol.equals(tickerTableModel.getValueAt(row, SYMBOL_COLUMN))) {
                tickerTable.setRowSelectionInterval(row, row);
                return;
            }
        }
        tickerTable.clearSelection();
    }

    /** A table model whose cells are never editable; the watchlist is changed by button. */
    private static final class ReadOnlyTableModel extends DefaultTableModel {

        private ReadOnlyTableModel(final String[] columnNames) {
            super(columnNames, 0);
        }

        @Override
        public boolean isCellEditable(final int row, final int column) {
            return false;
        }
    }

    /** A focus traversal policy that simply follows a fixed list of components. */
    private static final class OrderedFocusTraversalPolicy extends FocusTraversalPolicy {

        private final List<Component> order;

        private OrderedFocusTraversalPolicy(final List<Component> order) {
            this.order = List.copyOf(order);
        }

        @Override
        public Component getComponentAfter(final Container container,
                                           final Component component) {
            return order.get(Math.floorMod(order.indexOf(component) + 1, order.size()));
        }

        @Override
        public Component getComponentBefore(final Container container,
                                            final Component component) {
            return order.get(Math.floorMod(order.indexOf(component) - 1, order.size()));
        }

        @Override
        public Component getFirstComponent(final Container container) {
            return order.get(0);
        }

        @Override
        public Component getLastComponent(final Container container) {
            return order.get(order.size() - 1);
        }

        @Override
        public Component getDefaultComponent(final Container container) {
            return order.get(0);
        }
    }
}
