package view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;

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
    private final JPanel cardPanel;
    private final CardLayout cardLayout;
    private final ViewManagerModel viewManagerModel;
    private final JLabel persistenceStatusLabel = new JLabel(" ");

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
        add(cardPanel, BorderLayout.CENTER);

        JPanel navBar = new JPanel();
        add(navBar, BorderLayout.NORTH);
        // One nav button per screen. Each switches the CardLayout via the shared
        // ViewManagerModel; the matching view is registered from app.Main by addView(...).
        JButton watchlistBtn = new JButton("Watchlist");
        watchlistBtn.setMnemonic('W');
        watchlistBtn.addActionListener(
                event -> viewManagerModel.setActiveView(WatchlistViewModel.VIEW_NAME));
        navBar.add(watchlistBtn);
        JButton movingAverageBtn = new JButton("Moving Average Strategy");
        movingAverageBtn.addActionListener(
                e -> viewManagerModel.setActiveView(MovingAverageViewModel.VIEW_NAME));
        navBar.add(movingAverageBtn);
        JButton momentumBtn = new JButton("Momentum Strategy");
        momentumBtn.addActionListener(
                e -> viewManagerModel.setActiveView(MomentumViewModel.VIEW_NAME));
        navBar.add(momentumBtn);
        JButton backtestBtn = new JButton("Backtest");
        backtestBtn.addActionListener(
                e -> viewManagerModel.setActiveView(BacktestViewModel.VIEW_NAME));
        navBar.add(backtestBtn);
        JButton comparisonBtn = new JButton("Compare Strategies");
        comparisonBtn.setMnemonic('S');
        comparisonBtn.addActionListener(
                event -> viewManagerModel.setActiveView(ComparisonViewModel.VIEW_NAME));
        navBar.add(comparisonBtn);

        // Application-wide persistence status. Saving happens as a side effect of watchlist
        // actions and its view model was previously bound to nothing, so a failed write to
        // watchlist.dat was completely silent - the watchlist still said "Added AAPL..." while
        // nothing had been saved. This one line is the surface Main binds PersistenceViewModel
        // to; the message is carried in words, never colour alone.
        persistenceStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        persistenceStatusLabel.getAccessibleContext().setAccessibleName("Save status");
        add(persistenceStatusLabel, BorderLayout.SOUTH);
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
        final String text = message == null || message.isBlank() ? " " : message;
        SwingUtilities.invokeLater(() -> {
            persistenceStatusLabel.setText(text);
            persistenceStatusLabel.getAccessibleContext().setAccessibleDescription(text);
        });
    }

    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getCardPanel() { return cardPanel; }
}
