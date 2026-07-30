package use_case.persistence;

import entity.Watchlist;

/**
 * Boundary interface implemented by the actual storage mechanism
 * (frameworks_drivers layer). The use case layer only ever depends on
 * this interface, never on a concrete file/DB implementation -
 * this is the Dependency Inversion example for your presentation.
 */
public interface WatchlistDataAccessInterface {
    void save(Watchlist watchlist) throws PersistenceException;
    Watchlist load() throws PersistenceException;

    class PersistenceException extends Exception {
        public PersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
