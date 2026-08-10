package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvTest {

    @Test
    void parse_readsSimpleKeyValue() {
        Map<String, String> values = DotEnv.parse("ALPHA_VANTAGE_API_KEY=abc123");

        assertEquals("abc123", values.get("ALPHA_VANTAGE_API_KEY"));
    }

    @Test
    void parse_ignoresBlankLinesAndComments() {
        String contents = "# a comment\n\n   \nKEY=value\n# trailing comment";

        Map<String, String> values = DotEnv.parse(contents);

        assertEquals(1, values.size());
        assertEquals("value", values.get("KEY"));
    }

    @Test
    void parse_stripsWhitespaceAroundKeyAndValue() {
        Map<String, String> values = DotEnv.parse("  KEY  =   value  ");

        assertEquals("value", values.get("KEY"));
    }

    @Test
    void parse_removesSurroundingQuotes() {
        Map<String, String> values = DotEnv.parse("DOUBLE=\"d\"\nSINGLE='s'");

        assertEquals("d", values.get("DOUBLE"));
        assertEquals("s", values.get("SINGLE"));
    }

    @Test
    void parse_keepsValueContainingEqualsSign() {
        Map<String, String> values = DotEnv.parse("KEY=a=b=c");

        assertEquals("a=b=c", values.get("KEY"));
    }

    @Test
    void parse_lastLineWinsForRepeatedKey() {
        Map<String, String> values = DotEnv.parse("KEY=first\nKEY=second");

        assertEquals("second", values.get("KEY"));
    }

    @Test
    void parse_skipsLinesWithNoKey() {
        Map<String, String> values = DotEnv.parse("=orphan\nKEY=value");

        assertFalse(values.containsKey(""));
        assertEquals("value", values.get("KEY"));
    }

    @Test
    void valueOf_readsFromFile(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "ALPHA_VANTAGE_API_KEY=filekey\n", StandardCharsets.UTF_8);

        assertEquals(Optional.of("filekey"), DotEnv.valueOf(env, "ALPHA_VANTAGE_API_KEY"));
    }

    @Test
    void valueOf_missingFileIsEmpty(@TempDir Path dir) {
        Path env = dir.resolve("does-not-exist.env");

        assertTrue(DotEnv.valueOf(env, "ALPHA_VANTAGE_API_KEY").isEmpty());
    }

    @Test
    void valueOf_blankValueIsEmpty(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "ALPHA_VANTAGE_API_KEY=   \n", StandardCharsets.UTF_8);

        assertTrue(DotEnv.valueOf(env, "ALPHA_VANTAGE_API_KEY").isEmpty());
    }

    @Test
    void valueOf_absentKeyIsEmpty(@TempDir Path dir) throws IOException {
        Path env = dir.resolve(".env");
        Files.writeString(env, "OTHER=value\n", StandardCharsets.UTF_8);

        assertTrue(DotEnv.valueOf(env, "ALPHA_VANTAGE_API_KEY").isEmpty());
    }
}
