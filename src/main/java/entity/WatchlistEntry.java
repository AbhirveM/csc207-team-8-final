package entity;

import java.io.Serializable;

/**
 * One row on a user's watchlist: a ticker, plus whichever strategy config(s)
 * the user has set up for it (either may be null if not configured yet).
 *
 * NOTE: Strategy configurations will be added once their respective entities
 * are merged into the project.
 */
public class WatchlistEntry implements Serializable {
    private final Ticker ticker;

    // TODO: Add strategy configurations once their entities are merged.
    // private MovingAverageConfiguration movingAverageConfiguration;
    // private MomentumConfiguration momentumConfiguration;

    public WatchlistEntry(Ticker ticker) {
        this.ticker = ticker;
    }

    public Ticker getTicker() {
        return ticker;
    }

    // TODO: Re-add MovingAverageConfiguration getter/setter when the entity exists.
}
