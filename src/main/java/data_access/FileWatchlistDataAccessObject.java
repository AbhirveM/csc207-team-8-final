package data_access;

import entity.Watchlist;
import use_case.persistence.WatchlistDataAccessInterface;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.nio.file.AtomicMoveNotSupportedException;
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
        // Write to a temp file first and swap it into place, so a crash partway
        // through a save can't truncate the previous good file - a truncated
        // file is exactly the corruption load() has to recover from below.
        Path target = Path.of(filePath);
        Path temp = Path.of(filePath + ".tmp");
        try {
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
                out.writeObject(watchlist);
            }
            moveIntoPlace(temp, target);
        } catch (IOException exception) {
            deleteQuietly(temp);
            throw new PersistenceException("Failed to write watchlist to " + filePath, exception);
        }
    }

    @Override
    public Watchlist load() throws PersistenceException {
        File file = new File(filePath);
        if (!file.exists()) {
            // Nothing saved yet - return a fresh, empty watchlist rather than erroring.
            return new Watchlist();
        }

        try (FileInputStream fis = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fis)) {
            Object loaded = in.readObject();
            if (!(loaded instanceof Watchlist watchlist)) {
                // The file deserialized fine but isn't a Watchlist - throw rather
                // than backing up inline, so recovery happens in the catch clause
                // below with the streams already closed. Moving a file we still
                // hold open fails on Windows.
                throw new InvalidClassException(
                        loaded == null ? "null" : loaded.getClass().getName(),
                        "file does not contain a Watchlist");
            }
            return watchlist;
        } catch (ObjectStreamException | EOFException | ClassNotFoundException exception) {
            // The save file itself is corrupted/unreadable as a Watchlist -
            // recover by backing it up and starting fresh, instead of
            // blocking the user from ever opening the app again.
            // (ObjectStreamException covers StreamCorrupted/OptionalData/
            // InvalidClass and the other malformed-stream cases in one clause.)
            backUpCorruptedFile(file);
            return new Watchlist();
        } catch (IOException exception) {
            // Anything else (permission denied, disk error, etc.) is a real
            // problem the user should know about - don't silently reset their data.
            throw new PersistenceException("Failed to read watchlist from " + filePath, exception);
        }
    }

    private void backUpCorruptedFile(File file) throws PersistenceException {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String base = file.getPath() + ".corrupted-" + timestamp;
            // The timestamp is only second-precision, so two recoveries in the
            // same second would collide. Pick an unused name instead of
            // overwriting - a backup that clobbers the previous backup is no backup.
            Path backupPath = Path.of(base);
            for (int suffix = 2; Files.exists(backupPath); suffix++) {
                backupPath = Path.of(base + "-" + suffix);
            }
            Files.move(file.toPath(), backupPath);
        } catch (IOException exception) {
            // If we can't even back up the corrupted file (e.g. permission denied),
            // that's a real problem the user should know about - don't silently
            // reset their data and leave the corrupted file untouched.
            throw new PersistenceException("Failed to back up corrupted watchlist at " + filePath, exception);
        }
    }

    /** Atomic where the filesystem supports it, plain replace where it doesn't. */
    private static void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Best-effort cleanup of the temp file after a failed save. */
    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Nothing useful to do here - the caller is already throwing.
        }
    }
}
