package data_access;

import entity.Ticker;
import entity.Watchlist;
import entity.WatchlistEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.persistence.WatchlistDataAccessInterface.PersistenceException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileWatchlistDataAccessObjectTest {

    @TempDir
    Path tempDir;

    @Test
    void loadMissingFileReturnsEmptyWatchlist() throws PersistenceException {
        Path filePath = tempDir.resolve("watchlist.dat");
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        Watchlist loaded = dao.load();

        assertNotNull(loaded);
        assertTrue(loaded.getEntries().isEmpty());
    }

    @Test
    void saveThenLoadRoundTripsWatchlistContents() throws PersistenceException {
        Path filePath = tempDir.resolve("watchlist.dat");
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        Watchlist saved = new Watchlist();
        saved.addTicker(new Ticker("AAPL", "Apple Inc."));
        saved.addTicker(new Ticker("TSLA", "Tesla Inc."));
        dao.save(saved);

        Watchlist loaded = dao.load();

        assertEquals(2, loaded.getEntries().size());
        assertTrue(loaded.contains(new Ticker("AAPL", "Apple Inc.")));
        assertTrue(loaded.contains(new Ticker("TSLA", "Tesla Inc.")));
    }

    @Test
    void loadCorruptedFileReturnsEmptyWatchlistAndBacksUpTheFile() throws IOException, PersistenceException {
        Path filePath = tempDir.resolve("watchlist.dat");
        byte[] corruptedBytes = {1, 2, 3, 4, 5};
        try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
            out.write(corruptedBytes);
        }
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        Watchlist loaded = dao.load();

        assertNotNull(loaded);
        assertTrue(loaded.getEntries().isEmpty());
        assertFalse(Files.exists(filePath), "corrupted file should have been moved to a backup");

        List<Path> backups = backupFiles(tempDir);
        assertEquals(1, backups.size(), "expected a watchlist.dat.corrupted-* backup file");
        assertArrayEquals(corruptedBytes, Files.readAllBytes(backups.get(0)),
                "backup should preserve the original bytes so the data is recoverable");
    }

    @Test
    void loadWrongTypeReturnsEmptyWatchlistAndBacksUpTheFile() throws IOException, PersistenceException {
        Path filePath = tempDir.resolve("watchlist.dat");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            out.writeObject("a perfectly valid serialized String, but not a Watchlist");
        }
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        Watchlist loaded = dao.load();

        assertNotNull(loaded);
        assertTrue(loaded.getEntries().isEmpty());
        assertFalse(Files.exists(filePath), "wrong-type file should have been moved to a backup");
        assertEquals(1, backupFiles(tempDir).size(), "expected a watchlist.dat.corrupted-* backup file");
    }

    @Test
    void repeatedRecoveriesKeepEveryBackup() throws IOException, PersistenceException {
        Path filePath = tempDir.resolve("watchlist.dat");
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        for (int i = 0; i < 3; i++) {
            try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
                out.write(new byte[] {(byte) i, 2, 3, 4, 5});
            }
            dao.load();
        }

        assertEquals(3, backupFiles(tempDir).size(),
                "each recovery should get its own backup, even within the same second");
    }

    @Test
    void failedSaveLeavesNoTempFileBehind() throws PersistenceException, IOException {
        // A directory where the data file should be makes the swap-into-place fail.
        Path filePath = tempDir.resolve("watchlist.dat");
        Files.createDirectory(filePath);
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        assertThrows(PersistenceException.class, () -> dao.save(new Watchlist()));
        assertFalse(Files.exists(tempDir.resolve("watchlist.dat.tmp")),
                "temp file should be cleaned up after a failed save");
    }

    @Test
    void failedSaveLeavesThePreviousGoodFileIntact() throws PersistenceException, IOException {
        Path filePath = tempDir.resolve("watchlist.dat");
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        Watchlist original = new Watchlist();
        original.addTicker(new Ticker("AAPL", "Apple Inc."));
        dao.save(original);
        byte[] goodBytes = Files.readAllBytes(filePath);

        // Watchlist itself is serializable, but a non-serializable payload inside it
        // makes writeObject blow up partway through - the old file must survive.
        Watchlist unsaveable = new Watchlist();
        unsaveable.getEntries().add(new NotSerializableEntry());

        assertThrows(PersistenceException.class, () -> dao.save(unsaveable));
        assertArrayEquals(goodBytes, Files.readAllBytes(filePath),
                "a failed save must not clobber the last good file");
        assertEquals(1, dao.load().getEntries().size());
    }

    /** A WatchlistEntry that can't be serialized, used to force a mid-write failure. */
    private static class NotSerializableEntry extends WatchlistEntry {
        @SuppressWarnings("unused")
        private final Object unserializable = new Object();

        NotSerializableEntry() {
            super(new Ticker("BAD", "Not Serializable"));
        }
    }

    private List<Path> backupFiles(Path dir) throws IOException {
        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "watchlist.dat.corrupted-*")) {
            stream.forEach(found::add);
        }
        return found;
    }
}
