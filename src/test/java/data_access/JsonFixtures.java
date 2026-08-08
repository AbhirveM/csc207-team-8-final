package data_access;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads the canned provider responses under {@code src/test/resources/alphavantage}.
 *
 * <p>Every market-data test reads from these files rather than the network, so the
 * suite is deterministic, runs offline, and spends none of the daily request quota.
 */
final class JsonFixtures {

    private JsonFixtures() {
    }

    static String read(String fileName) {
        final String path = "/alphavantage/" + fileName;
        try (InputStream stream = JsonFixtures.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("Missing test fixture: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
