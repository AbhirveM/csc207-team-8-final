package view;

import javax.swing.*;
import java.awt.*;

import interface_adapter.backtest.BacktestViewModel;
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
        watchlistBtn.addActionListener(
                e -> viewManagerModel.setActiveView(WatchlistViewModel.VIEW_NAME));
        navBar.add(watchlistBtn);
        JButton backtestBtn = new JButton("Backtest");
        backtestBtn.addActionListener(
                e -> viewManagerModel.setActiveView(BacktestViewModel.VIEW_NAME));
        navBar.add(backtestBtn);
        JButton comparisonBtn = new JButton("Compare Strategies");
        comparisonBtn.addActionListener(e -> viewManagerModel.setActiveView("comparison"));
        navBar.add(comparisonBtn);
    }

    /** Register a finished view under the given name so the nav bar / ViewManager can show it. */
    public void addView(String name, JPanel view) {
        cardPanel.add(view, name);
    }

    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getCardPanel() { return cardPanel; }
}
