package view;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

import interface_adapter.comparison.ComparisonViewModel;
import interface_adapter.watchlist.WatchlistViewModel;

/**
 * The main application window: a nav bar + a CardLayout panel that
 * ViewManager swaps between. Register each finished view with
 * {@link #addView(String, java.awt.Component)} and add a nav button for it here.
 *
 * <p>Two screens are reachable today: Watchlist and Compare Strategies. The
 * backtest results screen exists but has no nav button because nothing
 * constructs it yet - see the note on {@code app.Main}.
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
        // Add a nav button for each screen here.
        JButton watchlistBtn = new JButton("Watchlist");
        watchlistBtn.setMnemonic('W');
        watchlistBtn.addActionListener(
                event -> viewManagerModel.setActiveView(WatchlistViewModel.VIEW_NAME));
        navBar.add(watchlistBtn);
        JButton comparisonBtn = new JButton("Compare Strategies");
        comparisonBtn.setMnemonic('S');
        comparisonBtn.addActionListener(
                event -> viewManagerModel.setActiveView(ComparisonViewModel.VIEW_NAME));
        navBar.add(comparisonBtn);
    }

    /** Register a finished view under the given name so the nav bar / ViewManager can show it. */
    public void addView(String name, JPanel view) {
        cardPanel.add(view, name);
    }

    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getCardPanel() { return cardPanel; }
}
