package interface_adapter.watchlist;

import java.util.Objects;

import use_case.watchlist.AddTickerInputBoundary;
import use_case.watchlist.AddTickerInputData;
import use_case.watchlist.ChartPeriod;
import use_case.watchlist.RefreshTickerInputBoundary;
import use_case.watchlist.RefreshTickerInputData;
import use_case.watchlist.RemoveTickerInputBoundary;
import use_case.watchlist.RemoveTickerInputData;
import use_case.watchlist.ShowWatchlistInputBoundary;
import use_case.watchlist.ShowWatchlistInputData;

/**
 * The single entry point the watchlist view calls into.
 *
 * <p>Four one-line delegations. This class performs <em>no</em> validation, no trimming and
 * no upper-casing: the raw text the user typed reaches the interactor untouched, because
 * normalization is a use-case rule that {@code TickerSymbolValidator} owns and Agent A
 * tests. A controller that quietly trimmed would move a tested rule into an untested layer
 * and hide the regression if the two ever disagreed.
 *
 * <p>All four methods are {@code void} and synchronous. The view is responsible for calling
 * them off the event dispatch thread; results arrive through
 * {@link WatchlistViewModel}, not through a return value.
 */
public final class WatchlistController {

    private final AddTickerInputBoundary addTicker;
    private final RemoveTickerInputBoundary removeTicker;
    private final RefreshTickerInputBoundary refreshTicker;
    private final ShowWatchlistInputBoundary showWatchlist;

    /**
     * @param addTicker     the Add Ticker use case
     * @param removeTicker  the Remove Ticker use case
     * @param refreshTicker the Refresh Ticker use case
     * @param showWatchlist the Show Watchlist use case
     * @throws NullPointerException if any boundary is null
     */
    public WatchlistController(AddTickerInputBoundary addTicker,
                               RemoveTickerInputBoundary removeTicker,
                               RefreshTickerInputBoundary refreshTicker,
                               ShowWatchlistInputBoundary showWatchlist) {
        this.addTicker = Objects.requireNonNull(addTicker,
                "Add ticker input boundary cannot be null");
        this.removeTicker = Objects.requireNonNull(removeTicker,
                "Remove ticker input boundary cannot be null");
        this.refreshTicker = Objects.requireNonNull(refreshTicker,
                "Refresh ticker input boundary cannot be null");
        this.showWatchlist = Objects.requireNonNull(showWatchlist,
                "Show watchlist input boundary cannot be null");
    }

    /**
     * Adds a ticker to the watchlist and loads its price history.
     *
     * @param rawSymbol exactly what the user typed, passed through unmodified
     */
    public void addTicker(String rawSymbol) {
        addTicker.execute(new AddTickerInputData(rawSymbol));
    }

    /**
     * Removes a ticker from the watchlist.
     *
     * @param rawSymbol exactly what the user typed or selected, passed through unmodified
     */
    public void removeTicker(String rawSymbol) {
        removeTicker.execute(new RemoveTickerInputData(rawSymbol));
    }

    /**
     * Re-fetches the price history for a ticker already on the watchlist.
     *
     * @param rawSymbol exactly what the user typed or selected, passed through unmodified
     */
    public void refreshTicker(String rawSymbol) {
        refreshTicker.execute(new RefreshTickerInputData(rawSymbol));
    }

    /**
     * Re-renders the watchlist with the given symbol selected, plotting its whole history.
     * Performs no I/O.
     *
     * @param selectedSymbol the symbol to select; {@code ""} selects nothing
     */
    public void showWatchlist(String selectedSymbol) {
        showWatchlist(selectedSymbol, ChartPeriod.ALL);
    }

    /**
     * Re-renders the watchlist with the given symbol selected and its chart narrowed to a
     * window. Performs no I/O.
     *
     * @param selectedSymbol the symbol to select; {@code ""} selects nothing
     * @param chartPeriod    how much of the price history to plot; null means everything
     */
    public void showWatchlist(String selectedSymbol, ChartPeriod chartPeriod) {
        showWatchlist.execute(new ShowWatchlistInputData(selectedSymbol, chartPeriod));
    }
}
