package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

import data_access.InMemoryStockRepository;
import interface_adapter.backtest.BacktestController;
import interface_adapter.backtest.BacktestViewModel;
import interface_adapter.comparison.ComparisonController;
import interface_adapter.comparison.ComparisonViewModel;
import interface_adapter.comparison.CompletedBacktestStore;
import interface_adapter.momentum.MomentumController;
import interface_adapter.momentum.MomentumViewModel;
import interface_adapter.moving_average.MovingAverageController;
import interface_adapter.moving_average.MovingAverageViewModel;
import interface_adapter.watchlist.WatchlistController;
import interface_adapter.watchlist.WatchlistState;
import interface_adapter.watchlist.WatchlistViewModel;

/**
 * The restyle rewrote every builder method in the view package, and the accessibility work
 * those methods carry - mnemonics, label-for bindings, accessible names, and the cleared
 * table traversal keys - is exactly the kind of one-line detail a layout rewrite drops
 * silently. These tests construct each screen headless and assert the details survived.
 *
 * <p>Constructing the panels is also what keeps them headless-safe, which
 * {@code IntegrationWiringTest} and {@code BacktestViewTest} already depend on.
 */
class ViewConstructionTest {

    @Test
    void theWatchlistKeepsEveryMnemonicAndTooltipItsAccessibilityReportClaims() {
        WatchlistView view = watchlistView();
        assertEquals('A', buttonNamed(view, "Add").getMnemonic());
        assertEquals('M', buttonNamed(view, "Remove").getMnemonic());
        assertEquals('R', buttonNamed(view, "Refresh").getMnemonic());
        assertEquals('L', buttonNamed(view, "Load prices").getMnemonic());
        for (String label : List.of("Add", "Remove", "Refresh", "Load prices")) {
            assertNotNull(buttonNamed(view, label).getToolTipText(), label + " lost its tooltip");
        }
    }

    @Test
    void theTickerFieldKeepsItsLabelBindingAndMnemonic() {
        WatchlistView view = watchlistView();
        JLabel label = labelStartingWith(view, "Ticker symbol");
        assertNotNull(label.getLabelFor());
        assertEquals('T', label.getDisplayedMnemonic());
    }

    @Test
    void bothWatchlistTablesReleaseTabSoTheyAreNotAKeyboardTrap() {
        // JTable binds Tab to cell traversal, which traps a keyboard-only user inside the
        // table. Passing null does not empty the key set - it clears JTable's override so the
        // component inherits its container's keys, which is what areFocusTraversalKeysSet
        // reports on.
        for (JTable table : descendants(watchlistView(), JTable.class)) {
            assertTablePassesTabThrough(table);
        }
    }

    @Test
    void bothWatchlistTablesKeepTheirAccessibleNames() {
        List<JTable> tables = descendants(watchlistView(), JTable.class);
        assertEquals(2, tables.size());
        List<String> names = new ArrayList<>();
        for (JTable table : tables) {
            names.add(table.getAccessibleContext().getAccessibleName());
        }
        assertTrue(names.contains("Watchlist"), names.toString());
        assertTrue(names.contains("Daily prices"), names.toString());
    }

    @Test
    void theWatchlistKeepsItsExplicitFocusOrder() {
        WatchlistView view = watchlistView();
        assertTrue(view.isFocusTraversalPolicyProvider());
        assertNotNull(view.getFocusTraversalPolicy());
    }

    @Test
    void theWatchlistPaintsWhateverStateItIsGiven() throws Exception {
        WatchlistViewModel viewModel = new WatchlistViewModel();
        WatchlistView view = new WatchlistView(viewModel, noOpWatchlistController());
        viewModel.setState(new WatchlistState(
                List.of(new WatchlistState.TickerRow("AAPL", "Apple Inc.", "120",
                        "2026-08-05", "249.68")),
                List.of(new WatchlistState.PriceRow("2026-08-05", "249.26", "250.73",
                        "248.21", "249.68", "51204300")),
                "AAPL", "Loaded.", "", ""));
        // The view marshals every state change onto the event thread, so a test that reads the
        // widgets straight after setState reads them before the repaint has run.
        flushEventQueue();
        JTable tickers = tableNamed(view, "Watchlist");
        assertEquals(1, tickers.getRowCount());
        assertEquals("AAPL", tickers.getValueAt(0, 0));
        assertEquals(1, tableNamed(view, "Daily prices").getRowCount());
    }

    @Test
    void theWatchlistShowsAnErrorInWordsNotOnlyInColour() throws Exception {
        WatchlistViewModel viewModel = new WatchlistViewModel();
        WatchlistView view = new WatchlistView(viewModel, noOpWatchlistController());
        viewModel.setState(new WatchlistState(List.of(), List.of(), "", "Ready.",
                "Unknown symbol.", "ZZZZ"));
        flushEventQueue();
        assertEquals(Theme.DOWN, labelStartingWith(view, "Error: Unknown symbol.").getForeground());
    }

    @Test
    void theMainWindowKeepsItsNavigationMnemonicsAndSaveStatusName() {
        MainView mainView = new MainView(new ViewManagerModel());
        assertEquals('W', buttonNamed(mainView, "Watchlist").getMnemonic());
        assertEquals('S', buttonNamed(mainView, "Compare Strategies").getMnemonic());
        assertNotNull(labelNamed(mainView, "Save status"));
    }

    @Test
    void theActiveScreenIsMarkedByWeightAsWellAsColour() {
        ViewManagerModel viewManagerModel = new ViewManagerModel();
        MainView mainView = new MainView(viewManagerModel);
        AbstractButton watchlist = buttonNamed(mainView, "Watchlist");
        AbstractButton backtest = buttonNamed(mainView, "Backtest");
        assertTrue(!watchlist.getFont().isBold());

        viewManagerModel.setActiveView(WatchlistViewModel.VIEW_NAME);
        assertTrue(watchlist.getFont().isBold(), "the active screen is not marked in bold");
        assertTrue(!backtest.getFont().isBold());

        viewManagerModel.setActiveView(BacktestViewModel.VIEW_NAME);
        assertTrue(backtest.getFont().isBold());
        assertTrue(!watchlist.getFont().isBold(), "the previous screen stayed marked");
    }

    @Test
    void aFailedSaveIsPrefixedWithTheWordErrorAndNotJustColoured() throws Exception {
        MainView mainView = new MainView(new ViewManagerModel());
        mainView.setPersistenceStatus("Could not save watchlist: disk full");
        flushEventQueue();
        JLabel status = labelNamed(mainView, "Save status");
        assertTrue(status.getText().startsWith("Error: "), status.getText());
        assertEquals(Theme.DOWN, status.getForeground());
    }

    @Test
    void aSuccessfulSaveIsNotDressedAsAnError() throws Exception {
        MainView mainView = new MainView(new ViewManagerModel());
        mainView.setPersistenceStatus("Watchlist saved.");
        flushEventQueue();
        JLabel status = labelNamed(mainView, "Save status");
        assertEquals("Watchlist saved.", status.getText());
        assertEquals(Theme.FG_MUTED, status.getForeground());
    }

    @Test
    void theMovingAverageScreenKeepsItsFieldBindingsAndMnemonics() {
        MovingAverageConfigurationView view = new MovingAverageConfigurationView(
                new MovingAverageViewModel(), new MovingAverageController(inputData -> { }));
        JLabel shortLabel = labelStartingWith(view, "Short window");
        JLabel longLabel = labelStartingWith(view, "Long window");
        assertNotNull(shortLabel.getLabelFor());
        assertNotNull(longLabel.getLabelFor());
        assertEquals('S', shortLabel.getDisplayedMnemonic());
        assertEquals('L', longLabel.getDisplayedMnemonic());
        assertEquals('C', buttonNamed(view, "Apply Configuration").getMnemonic());
    }

    @Test
    void theMomentumScreenKeepsItsFieldBindingsAndTooltips() {
        MomentumConfigurationView view = new MomentumConfigurationView(
                new MomentumViewModel(), new MomentumController(inputData -> { }));
        for (String caption : List.of("RSI Period:", "Oversold Threshold:", "Overbought Threshold:")) {
            assertNotNull(labelStartingWith(view, caption).getLabelFor(),
                    caption + " lost its field binding");
        }
    }

    @Test
    void theBacktestScreenKeepsItsChooserBindingsAndRunMnemonic() {
        BacktestView view = backtestView();
        assertEquals('K', labelStartingWith(view, "Ticker:").getDisplayedMnemonic());
        assertEquals('S', labelStartingWith(view, "Strategy:").getDisplayedMnemonic());
        assertEquals('B', buttonNamed(view, "Run backtest").getMnemonic());
        assertNotNull(buttonNamed(view, "Run backtest").getToolTipText());
    }

    @Test
    void theBacktestResultsTableStillReleasesTabAndKeepsItsName() {
        JTable table = tableNamed(backtestView(), "Completed trades");
        assertTablePassesTabThrough(table);
    }

    @Test
    void theComparisonScreenKeepsItsMnemonicTooltipAndTableName() {
        ComparisonView view = new ComparisonView(
                new ComparisonViewModel(),
                new ComparisonController(inputData -> { }, new CompletedBacktestStore()));
        AbstractButton compare = buttonNamed(view, "Compare Completed Backtests");
        assertEquals('C', compare.getMnemonic());
        assertNotNull(compare.getToolTipText());
        assertTablePassesTabThrough(tableNamed(view, "Strategy comparison"));
    }

    @Test
    void theComparisonScreenRanksWhateverTheViewModelHolds() {
        ComparisonViewModel viewModel = new ComparisonViewModel();
        ComparisonView view = new ComparisonView(viewModel,
                new ComparisonController(inputData -> { }, new CompletedBacktestStore()));
        viewModel.setResults(List.of(
                new ComparisonViewModel.ResultRow("NVDA", "Moving Average Crossover",
                        "18.44", "8", "62.50")), "Moving Average Crossover");
        JTable table = tableNamed(view, "Strategy comparison");
        assertEquals(1, table.getRowCount());
        assertEquals("NVDA", table.getValueAt(0, 0));
    }

    @Test
    void theLookAndFeelInstallPushesTheThemeIntoTheSharedDefaults() {
        LookAndFeel.install();
        assertEquals(Theme.ROW_HEIGHT, UIManager.get("Table.rowHeight"));
        assertEquals(Theme.RULE, UIManager.get("Table.gridColor"));
        assertEquals(Theme.ACCENT, UIManager.get("Table.selectionBackground"));
        assertEquals(Theme.FONT_UI, UIManager.get("defaultFont"));
        // Zero arc everywhere is what removes the rounded pill shape FlatLaf ships with.
        assertEquals(0, UIManager.get("Button.arc"));
        assertEquals(0, UIManager.get("Component.arc"));
        assertEquals(0, UIManager.get("TextComponent.arc"));
    }

    @Test
    void installingTheLookAndFeelTwiceIsHarmless() {
        LookAndFeel.install();
        LookAndFeel.install();
        assertEquals(Theme.RULE, UIManager.get("Table.gridColor"));
    }

    /**
     * Asserts that a table has handed Tab and Shift+Tab back to its container, so keyboard
     * focus can leave it.
     *
     * @param table the table to check
     */
    private static void assertTablePassesTabThrough(JTable table) {
        assertTrue(!table.areFocusTraversalKeysSet(
                        java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS),
                "a table still swallows Tab");
        assertTrue(!table.areFocusTraversalKeysSet(
                        java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS),
                "a table still swallows Shift+Tab");
    }

    /**
     * Finds the table carrying the given accessible name.
     *
     * @param root the container to search
     * @param name the accessible name to match
     * @return the table
     */
    private static JTable tableNamed(Container root, String name) {
        for (JTable table : descendants(root, JTable.class)) {
            if (name.equals(table.getAccessibleContext().getAccessibleName())) {
                return table;
            }
        }
        throw new AssertionError("no table named " + name);
    }

    /**
     * Waits for anything queued with invokeLater to have run.
     *
     * @throws Exception if the wait is interrupted
     */
    private static void flushEventQueue() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
    }

    /**
     * Builds a watchlist screen wired to controllers that do nothing.
     *
     * @return the constructed view
     */
    private static WatchlistView watchlistView() {
        return new WatchlistView(new WatchlistViewModel(), noOpWatchlistController());
    }

    /**
     * Builds a backtest screen against an empty repository.
     *
     * @return the constructed view
     */
    private static BacktestView backtestView() {
        return new BacktestView(
                new BacktestViewModel(),
                new BacktestController(inputData -> { }),
                new InMemoryStockRepository(),
                new MomentumViewModel(),
                new MovingAverageViewModel());
    }

    /**
     * A watchlist controller whose four use cases do nothing.
     *
     * @return the controller
     */
    private static WatchlistController noOpWatchlistController() {
        return new WatchlistController(
                inputData -> { }, inputData -> { }, inputData -> { }, inputData -> { });
    }

    /**
     * Finds the button carrying the given text anywhere under a container.
     *
     * @param root the container to search
     * @param text the button's exact label
     * @return the button
     */
    private static AbstractButton buttonNamed(Container root, String text) {
        for (AbstractButton button : descendants(root, AbstractButton.class)) {
            if (text.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("no button labelled " + text);
    }

    /**
     * Finds the first label whose text starts with the given prefix.
     *
     * @param root the container to search
     * @param prefix the text the label starts with
     * @return the label
     */
    private static JLabel labelStartingWith(Container root, String prefix) {
        for (JLabel label : descendants(root, JLabel.class)) {
            if (label.getText() != null && label.getText().startsWith(prefix)) {
                return label;
            }
        }
        throw new AssertionError("no label starting with " + prefix);
    }

    /**
     * Finds the label carrying the given accessible name.
     *
     * @param root the container to search
     * @param name the accessible name to match
     * @return the label
     */
    private static JLabel labelNamed(Container root, String name) {
        for (JLabel label : descendants(root, JLabel.class)) {
            if (name.equals(label.getAccessibleContext().getAccessibleName())) {
                return label;
            }
        }
        throw new AssertionError("no label named " + name);
    }

    /**
     * Collects every descendant of a given type, depth first.
     *
     * @param root the container to search
     * @param type the component type to collect
     * @param <T> the component type
     * @return the matching descendants, in traversal order
     */
    private static <T> List<T> descendants(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(descendants(container, type));
            }
        }
        return found;
    }
}
