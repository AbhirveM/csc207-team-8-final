package app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal reader for a project-local {@code .env} file, used only by the
 * composition root as a fallback when a real environment variable is not set.
 *
 * <p>The real process environment always wins: {@link Main} calls
 * {@link data_access.AlphaVantageMarketDataAccessObject#apiKeyFromEnvironment()}
 * first and consults this class only when that comes back empty. So exporting
 * {@code ALPHA_VANTAGE_API_KEY} in the shell or an IntelliJ run configuration
 * still overrides whatever the file contains, and the file is a convenience for
 * the common {@code java -jar} case rather than a second source of truth.
 *
 * <p>Parsing is deliberately tiny and deliberately not a general dotenv
 * implementation: blank lines and {@code #} comment lines are ignored, each
 * remaining line is split on its first {@code =}, keys and values are stripped,
 * and a single layer of surrounding single or double quotes is removed from the
 * value. There is no variable interpolation and no multi-line values, because
 * the only thing this project keeps in {@code .env} is a flat API key.
 */
public final class DotEnv {

    /** The file this reader looks for, relative to the process working directory. */
    public static final String DEFAULT_FILENAME = ".env";

    private DotEnv() {
    }

    /**
     * Looks up a single key in {@code ./.env}, applying the same "blank means
     * absent" policy the rest of the app uses for configuration.
     *
     * <p>A missing, unreadable, or malformed file is treated as "no value": the
     * file is an optional convenience, so its absence must never be an error.
     *
     * @param key the variable name to read; must not be null
     * @return the stripped value, or empty when the file is missing, the key is
     *         absent, or the value is blank
     */
    public static Optional<String> valueOf(String key) {
        return valueOf(Path.of(DEFAULT_FILENAME), key);
    }

    /**
     * Looks up a single key in a specific {@code .env} file. Exposed for tests,
     * which supply a temporary file rather than relying on the working directory.
     *
     * @param envFile the file to read; must not be null
     * @param key     the variable name to read; must not be null
     * @return the stripped value, or empty when the file is missing, the key is
     *         absent, or the value is blank
     */
    public static Optional<String> valueOf(Path envFile, String key) {
        final String value = read(envFile).get(key);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.strip());
    }

    /**
     * Reads and parses the file, returning an empty map on any I/O failure so a
     * missing {@code .env} is silently equivalent to an empty one.
     *
     * @param envFile the file to read; must not be null
     * @return the parsed key/value pairs, or an empty map when the file cannot be read
     */
    private static Map<String, String> read(Path envFile) {
        if (!Files.isReadable(envFile)) {
            return Map.of();
        }
        try {
            return parse(Files.readString(envFile, StandardCharsets.UTF_8));
        }
        catch (IOException exception) {
            return Map.of();
        }
    }

    /**
     * Parses {@code .env} contents into key/value pairs. Pure and side-effect
     * free, so the whole of the parsing behaviour can be pinned by tests without
     * touching the filesystem.
     *
     * @param contents the raw file contents; must not be null
     * @return the parsed pairs, in no particular order; later lines win over
     *         earlier ones for a repeated key
     */
    static Map<String, String> parse(String contents) {
        final Map<String, String> values = new HashMap<>();
        for (String rawLine : contents.split("\n", -1)) {
            final String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            final int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            final String key = line.substring(0, equals).strip();
            final String value = unquote(line.substring(equals + 1).strip());
            values.put(key, value);
        }
        return values;
    }

    /**
     * Removes one matching pair of surrounding single or double quotes, so a key
     * written as {@code KEY="value"} yields {@code value} rather than {@code "value"}.
     *
     * @param value the stripped raw value; must not be null
     * @return the value with a single layer of surrounding quotes removed, if present
     */
    private static String unquote(String value) {
        final boolean quoted = value.length() >= 2
                && (value.charAt(0) == '"' || value.charAt(0) == '\'')
                && value.charAt(value.length() - 1) == value.charAt(0);
        if (quoted) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
