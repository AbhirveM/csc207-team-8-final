package view;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

import interface_adapter.watchlist.WatchlistViewModel;

/**
 * The main application window: a nav bar + a CardLayout panel that
 * ViewManager swaps between. Add each member's real view to cardPanel
 * with cardPanel.add(view, ViewName.VIEW_NAME) as they're finished -
 * for now only the Comparison view (yours) is wired in.
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

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        add(cardPanel, BorderLayout.CENTER);

        JPanel navBar = new JPanel();
        add(navBar, BorderLayout.NORTH);
        // Add nav buttons for each screen here, e.g.:
        JButton watchlistBtn = new JButton("Watchlist");
        watchlistBtn.addActionListener(
                e -> viewManagerModel.setActiveView(WatchlistViewModel.VIEW_NAME));
        navBar.add(watchlistBtn);
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
