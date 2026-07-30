package data_access;

import entity.Watchlist;
import use_case.persistence.WatchlistDataAccessInterface;

import java.io.*;

/**
 * Simplest possible working persistence: Java object serialization to a local file.
 * Swap this out later for JSON/DB if your team wants - the use case layer
 * never has to change because it only depends on WatchlistDataAccessInterface.
 */
public class FileWatchlistDataAccessObject implements WatchlistDataAccessInterface {
    private final String filePath;

    public FileWatchlistDataAccessObject(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Watchlist watchlist) throws PersistenceException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(watchlist);
        } catch (IOException e) {
            throw new PersistenceException("Failed to write watchlist to " + filePath, e);
        }
    }

    @Override
    public Watchlist load() throws PersistenceException {
        File file = new File(filePath);
        if (!file.exists()) {
            // Nothing saved yet - return a fresh, empty watchlist rather than erroring.
            return new Watchlist();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (Watchlist) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException("Failed to read watchlist from " + filePath, e);
        }
    }
}
