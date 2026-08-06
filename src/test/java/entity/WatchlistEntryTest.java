package entity;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class WatchlistEntryTest {

    @Test
    void newEntryHasTickerAndNoConfigs() {
        Ticker aapl = new Ticker("AAPL", "Apple Inc.");
        WatchlistEntry entry = new WatchlistEntry(aapl);

        assertEquals(aapl, entry.getTicker());
        assertNull(entry.getMovingAverageConfiguration());
        assertNull(entry.getMomentumConfiguration());
    }

    @Test
    void configurationsRoundTripThroughSetters() {
        WatchlistEntry entry = new WatchlistEntry(new Ticker("TSLA", "Tesla Inc."));
        MovingAverageConfiguration ma = new MovingAverageConfiguration(10, 50);
        MomentumConfiguration momentum = new MomentumConfiguration(14, 30.0, 70.0);

        entry.setMovingAverageConfiguration(ma);
        entry.setMomentumConfiguration(momentum);

        assertSame(ma, entry.getMovingAverageConfiguration());
        assertSame(momentum, entry.getMomentumConfiguration());
    }

    @Test
    void configurationsCanBeClearedBackToNull() {
        WatchlistEntry entry = new WatchlistEntry(new Ticker("NVDA", "NVIDIA Corp."));
        entry.setMovingAverageConfiguration(new MovingAverageConfiguration(5, 20));
        entry.setMomentumConfiguration(new MomentumConfiguration(14, 25.0, 75.0));

        entry.setMovingAverageConfiguration(null);
        entry.setMomentumConfiguration(null);

        assertNull(entry.getMovingAverageConfiguration());
        assertNull(entry.getMomentumConfiguration());
    }

    /**
     * The whole reason both config classes have to be Serializable: persistence
     * serializes the entire Watchlist graph, so a configured entry must survive
     * a serialize/deserialize cycle. This exercises that at the entity level
     * (in memory), independent of the file-based DAO.
     */
    @Test
    void configuredWatchlistSurvivesSerializationRoundTrip() throws Exception {
        Watchlist original = new Watchlist();
        Ticker aapl = new Ticker("AAPL", "Apple Inc.");
        original.addTicker(aapl);
        WatchlistEntry entry = original.findEntry(aapl).orElseThrow();
        entry.setMovingAverageConfiguration(new MovingAverageConfiguration(12, 26));
        entry.setMomentumConfiguration(new MomentumConfiguration(14, 30.0, 70.0));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        Watchlist restored;
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (Watchlist) in.readObject();
        }

        WatchlistEntry restoredEntry = restored.findEntry(aapl).orElseThrow();
        assertNotNull(restoredEntry.getMovingAverageConfiguration());
        assertNotNull(restoredEntry.getMomentumConfiguration());
        assertEquals(12, restoredEntry.getMovingAverageConfiguration().getShortWindow());
        assertEquals(26, restoredEntry.getMovingAverageConfiguration().getLongWindow());
        assertEquals(14, restoredEntry.getMomentumConfiguration().getPeriod());
        assertEquals(30.0, restoredEntry.getMomentumConfiguration().getOversoldThreshold());
        assertEquals(70.0, restoredEntry.getMomentumConfiguration().getOverboughtThreshold());
    }
}
