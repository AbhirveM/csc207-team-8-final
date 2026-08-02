package data_access;

import entity.Ticker;
import entity.Watchlist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import use_case.persistence.WatchlistDataAccessInterface.PersistenceException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
        try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
            out.write(new byte[] {1, 2, 3, 4, 5});
        }
        FileWatchlistDataAccessObject dao = new FileWatchlistDataAccessObject(filePath.toString());

        Watchlist loaded = dao.load();

        assertNotNull(loaded);
        assertTrue(loaded.getEntries().isEmpty());
        assertFalse(Files.exists(filePath), "corrupted file should have been moved to a backup");
        assertTrue(backupFileExists(tempDir), "expected a watchlist.dat.corrupted-* backup file");
    }

    private boolean backupFileExists(Path dir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "watchlist.dat.corrupted-*")) {
            return stream.iterator().hasNext();
        }
    }
}
