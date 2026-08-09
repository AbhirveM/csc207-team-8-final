package view;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

import interface_adapter.backtest.BacktestViewModel;
import interface_adapter.comparison.ComparisonViewModel;
import interface_adapter.momentum.MomentumViewModel;
import interface_adapter.moving_average.MovingAverageViewModel;
import interface_adapter.watchlist.WatchlistViewModel;

/**
 * The main application window: a nav bar + a CardLayout panel that
 * ViewManager swaps between. Each member's real view is registered with
 * addView(name, view) from the application builder ({@code app.Main}); the
 * nav bar below carries one button per registered screen.
 */
public class MainView extends JFrame {

    /** Text prefix that marks a status message as a failure without relying on colour. */
    private static final String ERROR_PREFIX = "Error: ";

    /**
     * Prefix every persistence failure message carries. Both failure paths
     * ({@code SaveWatchlist} and {@code LoadWatchlist}) phrase their message this way. The
     * status bar has no other signal to work from: the output boundary reports success and
     * failure through the same {@code String}, and widening it is a use-case change rather
     * than a styling one.
     */
    private static final String FAILURE_MARKER = "Could not ";

    /** Thickness of the accent rule under the active nav button. */
    private static final int ACTIVE_RULE = 2;

    private final JPanel cardPanel;
    private final CardLayout cardLayout;
    private final ViewManagerModel viewManagerModel;
    private final JLabel persistenceStatusLabel = new JLabel(" ");
    /** Nav buttons by the view name each one activates, so the active screen can be marked. */
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    public MainView(ViewManagerModel viewManagerModel) {
        super("Market Watchlist & Backtester");
        this.viewManagerModel = viewManagerModel;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        // Below ~700px wide the control row stops fitting and the watchlist's
        // "Load prices" button clips off the right edge. A floor keeps every
        // control reachable by mouse; each one already has a mnemonic for the keyboard.
        setMinimumSize(new Dimension(820, 500));

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(Theme.BG);
        add(cardPanel, BorderLayout.CENTER);

        add(buildNavBar(), BorderLayout.NORTH);
        add(buildStatusBar(), BorderLayout.SOUTH);

        // The active screen is marked with an accent rule and bold weight, so the nav has to
        // follow navigation rather than only lead it: any code path that switches the card -
        // a nav button, or app.Main's opening call - repaints the same indicator.
        viewManagerModel.addPropertyChangeListener(event -> {
            if (ViewManagerModel.ACTIVE_VIEW_PROPERTY.equals(event.getPropertyName())) {
                markActiveView(String.valueOf(event.getNewValue()));
            }
        });
    }

    /**
     * Builds the top chrome: wordmark on the left, then the screen buttons, all pushed left
     * against a chrome fill with a rule under it.
     *
     * @return the assembled nav bar
     */
    private JPanel buildNavBar() {
        JPanel navBar = new JPanel();
        navBar.setLayout(new BoxLayout(navBar, BoxLayout.X_AXIS));
        navBar.setBackground(Theme.CHROME);
        navBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.RULE_STRONG),
                BorderFactory.createEmptyBorder(0, Theme.LG, 0, Theme.LG)));
        navBar.setPreferredSize(new Dimension(0, Theme.NAV_HEIGHT));

        JLabel wordmark = new JLabel("MarketLens");
        wordmark.setFont(Theme.FONT_TITLE);
        wordmark.setForeground(Theme.FG);
        navBar.add(wordmark);
        navBar.add(Box.createHorizontalStrut(Theme.XL));

        // One nav button per screen. Each switches the CardLayout via the shared
        // ViewManagerModel; the matching view is registered from app.Main by addView(...).
        JButton watchlistBtn = addNavButton(navBar, "Watchlist", WatchlistViewModel.VIEW_NAME);
        watchlistBtn.setMnemonic('W');
        addNavButton(navBar, "Moving Average Strategy", MovingAverageViewModel.VIEW_NAME);
        addNavButton(navBar, "Momentum Strategy", MomentumViewModel.VIEW_NAME);
        addNavButton(navBar, "Backtest", BacktestViewModel.VIEW_NAME);
        JButton comparisonBtn = addNavButton(navBar, "Compare Strategies", ComparisonViewModel.VIEW_NAME);
        comparisonBtn.setMnemonic('S');

        // Buttons sit left; the glue absorbs the rest of the width so they never centre.
        navBar.add(Box.createHorizontalGlue());
        return navBar;
    }

    /**
     * Creates a borderless nav button, registers it against the view it activates, and adds
     * it to the bar.
     *
     * @param navBar the bar to add the button to
     * @param text the button label
     * @param viewName the view name this button switches the card layout to
     * @return the created button, so the caller can attach a mnemonic
     */
    private JButton addNavButton(JPanel navBar, String text, String viewName) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_UI);
        button.setForeground(Theme.FG_MUTED);
        button.setBorder(inactiveNavBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.addActionListener(event -> viewManagerModel.setActiveView(viewName));
        navButtons.put(viewName, button);
        navBar.add(button);
        return button;
    }

    /**
     * Marks one nav button as the active screen. The state is carried twice over - an accent
     * rule and a bold weight - so it does not depend on colour vision.
     *
     * @param viewName the name of the view that is now showing
     */
    private void markActiveView(String viewName) {
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(viewName);
            JButton button = entry.getValue();
            button.setFont(active ? Theme.FONT_UI.deriveFont(Font.BOLD) : Theme.FONT_UI);
            button.setForeground(active ? Theme.FG : Theme.FG_MUTED);
            button.setBorder(active ? activeNavBorder() : inactiveNavBorder());
        }
    }

    /**
     * Padding for an inactive nav button. Its bottom inset matches the active border's rule
     * plus padding, so marking a button active does not shift the row.
     *
     * @return the inactive nav button border
     */
    private static Border inactiveNavBorder() {
        return BorderFactory.createEmptyBorder(Theme.SM, Theme.MD, Theme.SM, Theme.MD);
    }

    /**
     * Padding plus the accent rule for the active nav button.
     *
     * @return the active nav button border
     */
    private static Border activeNavBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, ACTIVE_RULE, 0, Theme.ACCENT),
                BorderFactory.createEmptyBorder(Theme.SM, Theme.MD, Theme.SM - ACTIVE_RULE, Theme.MD));
    }

    /**
     * Builds the bottom status bar around the persistence status label.
     *
     * @return the assembled status bar
     */
    private JPanel buildStatusBar() {
        // Application-wide persistence status. Saving happens as a side effect of watchlist
        // actions and its view model was previously bound to nothing, so a failed write to
        // watchlist.dat was completely silent - the watchlist still said "Added AAPL..." while
        // nothing had been saved. This one line is the surface Main binds PersistenceViewModel
        // to; the message is carried in words, never colour alone.
        persistenceStatusLabel.setFont(Theme.FONT_UI);
        persistenceStatusLabel.setForeground(Theme.FG_MUTED);
        persistenceStatusLabel.getAccessibleContext().setAccessibleName("Save status");

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Theme.CHROME);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.RULE_STRONG),
                BorderFactory.createEmptyBorder(Theme.XS, Theme.LG, Theme.XS, Theme.LG)));
        statusBar.add(persistenceStatusLabel, BorderLayout.WEST);
        return statusBar;
    }

    /** Register a finished view under the given name so the nav bar / ViewManager can show it. */
    public void addView(String name, JPanel view) {
        cardPanel.add(view, name);
    }

    /**
     * Shows the latest persistence status (a save/load success or an error) in the window's
     * status bar. Safe to call from any thread; it marshals onto the event dispatch thread.
     *
     * @param message the status text to display; a blank message clears the bar
     */
    public void setPersistenceStatus(String message) {
        final boolean failed = message != null && message.startsWith(FAILURE_MARKER);
        final String body = message == null || message.isBlank() ? " " : message;
        final String text = failed ? ERROR_PREFIX + body : body;
        SwingUtilities.invokeLater(() -> {
            persistenceStatusLabel.setText(text);
            persistenceStatusLabel.setForeground(failed ? Theme.DOWN : Theme.FG_MUTED);
            persistenceStatusLabel.getAccessibleContext().setAccessibleDescription(text);
        });
    }

    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getCardPanel() { return cardPanel; }
}
