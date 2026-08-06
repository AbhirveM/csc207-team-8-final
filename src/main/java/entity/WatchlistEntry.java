package entity;

import java.io.Serializable;

/**
 * One row on a user's watchlist: a ticker, plus whichever strategy config(s)
 * the user has set up for it (either may be null if not configured yet).
 *
 * Both configuration types implement {@link Serializable}, so a whole
 * {@link Watchlist} graph containing configured entries round-trips through
 * {@code FileWatchlistDataAccessObject}'s object serialization.
 */
public class WatchlistEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Ticker ticker;

    // Either may be null if the user hasn't configured that strategy for this ticker.
    private MovingAverageConfiguration movingAverageConfiguration;
    private MomentumConfiguration momentumConfiguration;

    public WatchlistEntry(Ticker ticker) {
        this.ticker = ticker;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public MovingAverageConfiguration getMovingAverageConfiguration() {
        return movingAverageConfiguration;
    }

    public void setMovingAverageConfiguration(MovingAverageConfiguration movingAverageConfiguration) {
        this.movingAverageConfiguration = movingAverageConfiguration;
    }

    public MomentumConfiguration getMomentumConfiguration() {
        return momentumConfiguration;
    }

    public void setMomentumConfiguration(MomentumConfiguration momentumConfiguration) {
        this.momentumConfiguration = momentumConfiguration;
    }
}
