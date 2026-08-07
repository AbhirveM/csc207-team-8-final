package interface_adapter.watchlist;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * The observable holder for {@link WatchlistState}.
 *
 * <p>No Swing imports here on purpose, matching
 * {@code interface_adapter.comparison.ComparisonViewModel}: the view observes this through
 * {@link PropertyChangeSupport}, which keeps the interface-adapter layer framework-agnostic
 * and the presenter trivially testable without a display.
 *
 * <p>The presenter is synchronous and is called from the view's background worker, so
 * {@link #setState(WatchlistState)} and the event it fires run <em>off</em> the event
 * dispatch thread. Re-entering Swing safely is the listener's job: the view's handler must
 * re-dispatch through {@code SwingUtilities.invokeLater} when it is not already on the EDT.
 */
public class WatchlistViewModel {

    /** The key {@code MainView} registers this view under. */
    public static final String VIEW_NAME = "watchlist";

    /** The property fired whenever the state is replaced. */
    public static final String STATE_PROPERTY = "state";

    /**
     * Headers for the watchlist table, in the order {@link WatchlistState.TickerRow}
     * declares its components. They live here rather than in the view so the presenter and
     * the view cannot drift; the length must stay equal to that record's component count.
     */
    public static final String[] TICKER_COLUMNS =
            {"Symbol", "Company", "Days of prices", "Latest date", "Latest close"};

    /**
     * Headers for the daily-price table, in the order {@link WatchlistState.PriceRow}
     * declares its components. The length must stay equal to that record's component count.
     */
    public static final String[] PRICE_COLUMNS =
            {"Date", "Open", "High", "Low", "Close", "Volume"};

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private WatchlistState state = WatchlistState.initial();

    /** @return the current state; never null, and {@link WatchlistState#initial()} to begin with. */
    public WatchlistState getState() {
        return state;
    }

    /**
     * Replaces the state and notifies listeners.
     *
     * <p>The old value passed to {@link PropertyChangeSupport} is deliberately {@code null}
     * rather than the previous state, because {@code firePropertyChange} suppresses the
     * event when the two values are equal. A use case that succeeds twice with an identical
     * result must still produce exactly one event, or the view silently misses a repaint.
     *
     * @param state the new state
     * @throws NullPointerException if {@code state} is null
     */
    public void setState(WatchlistState state) {
        this.state = Objects.requireNonNull(state, "State cannot be null");
        support.firePropertyChange(STATE_PROPERTY, null, state);
    }

    /**
     * Registers a listener for {@link #STATE_PROPERTY}.
     *
     * @param listener the listener to notify; must be non-null
     * @throws NullPointerException if {@code listener} is null
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(
                Objects.requireNonNull(listener, "Listener cannot be null"));
    }
}
