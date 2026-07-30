package entity;

import java.io.Serializable;

/**
 * One row on a user's watchlist: a ticker, plus whichever strategy config(s)
 * the user has set up for it (either may be null if not configured yet).
 *
 * NOTE: MovingAverageConfiguration must implement Serializable for
 * FileWatchlistDataAccessObject's save() to work (it's a one-line addition -
 * check with Ratnabh, since two int fields make it trivially serializable).
 * MomentumConfiguration field below is a placeholder until Zhou's PR adds
 * the real class - swap the type once it exists.
 */
public class WatchlistEntry implements Serializable {
    private final Ticker ticker;
    private MovingAverageConfiguration movingAverageConfiguration; // nullable
    // private MomentumConfiguration momentumConfiguration; // TODO: uncomment once Zhou's entity lands

    public WatchlistEntry(Ticker ticker) {
        this.ticker = ticker;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public MovingAverageConfiguration getMovingAverageConfiguration() {
        return movingAverageConfiguration;
    }

    public void setMovingAverageConfiguration(MovingAverageConfiguration config) {
        this.movingAverageConfiguration = config;
    }
}
