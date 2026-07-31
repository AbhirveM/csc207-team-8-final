package data_access;

import entity.Watchlist;
import use_case.persistence.WatchlistDataAccessInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simplest possible working persistence: Java object serialization to a local file.
 * Swap this out later for JSON/DB if your team wants - the use case layer
 * never has to change because it only depends on WatchlistDataAccessInterface.
 *
 * Handles two failure cases differently:
 *  - Missing file (first run, nothing saved yet): return a fresh empty Watchlist.
 *  - Corrupted file (truncated/garbled save, incompatible serialized class):
 *    back the bad file up alongside itself and return a fresh empty Watchlist,
 *    rather than permanently locking the user out of the app until they
 *    manually delete the file. Other IO problems (e.g. permission denied)
 *    still surface as a PersistenceException, since silently discarding data
 *    there would hide a real problem rather than fix a corrupted save.
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
        } catch (StreamCorruptedException | EOFException | OptionalDataException
                 | InvalidClassException | ClassNotFoundException e) {
            // The save file itself is corrupted/unreadable as a Watchlist -
            // recover by backing it up and starting fresh, instead of
            // blocking the user from ever opening the app again.
            backUpCorruptedFile(file);
            return new Watchlist();
        } catch (IOException e) {
            // Anything else (permission denied, disk error, etc.) is a real
            // problem the user should know about - don't silently reset their data.
            throw new PersistenceException("Failed to read watchlist from " + filePath, e);
        }
    }

    private void backUpCorruptedFile(File file) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path backupPath = Path.of(file.getPath() + ".corrupted-" + timestamp);
            Files.move(file.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Best-effort backup - if even this fails, we still return a fresh
            // Watchlist below rather than crashing the app on startup.
        }
    }
}
