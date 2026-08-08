package use_case.watchlist;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickerSymbolValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "AAPL,      AAPL",
            "aapl,      AAPL",
            "'  aapl ', AAPL",
            "'A A P L', AAPL",
            "MsFt,      MSFT",
            "BRK.B,     BRK.B",
            "brk.b,     BRK.B",
            "RY-A,      RY-A",
            "X,         X",
            "SPY,       SPY",
            "1234,      1234",
            "ABCDEFGHIJ, ABCDEFGHIJ"
    })
    void acceptsAndNormalizesValidSymbols(String rawInput, String expectedSymbol) {
        TickerSymbolValidator.Result result = TickerSymbolValidator.validate(rawInput);

        assertTrue(result.isValid(), "Expected " + rawInput + " to be valid");
        assertEquals(expectedSymbol, result.getSymbol());
        assertNull(result.getReason());
    }

    @Test
    void rejectsNullAsBlank() {
        TickerSymbolValidator.Result result = TickerSymbolValidator.validate(null);

        assertTrue(!result.isValid());
        assertEquals(TickerSymbolValidator.Reason.BLANK, result.getReason());
        assertNull(result.getSymbol());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "     ", "\t", "\n"})
    void rejectsBlankInput(String rawInput) {
        TickerSymbolValidator.Result result = TickerSymbolValidator.validate(rawInput);

        assertEquals(TickerSymbolValidator.Reason.BLANK, result.getReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AA;PL", "AA_PL", "AAPL!", "@AAPL", "AA/PL", "AAPL$", "café"})
    void rejectsIllegalCharacters(String rawInput) {
        TickerSymbolValidator.Result result = TickerSymbolValidator.validate(rawInput);

        assertEquals(TickerSymbolValidator.Reason.ILLEGAL_CHARACTERS, result.getReason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDEFGHIJK", "ABCDEFGHIJKLMNOP"})
    void rejectsSymbolsLongerThanTheMaximum(String rawInput) {
        TickerSymbolValidator.Result result = TickerSymbolValidator.validate(rawInput);

        assertEquals(TickerSymbolValidator.Reason.TOO_LONG, result.getReason());
    }

    @Test
    void internalWhitespaceIsRemovedBeforeTheLengthCheck() {
        // "A A P L" is 7 raw characters but only 4 once normalized.
        TickerSymbolValidator.Result result = TickerSymbolValidator.validate("A A P L");

        assertTrue(result.isValid());
        assertEquals("AAPL", result.getSymbol());
    }

    @Test
    void illegalCharactersAreReportedAheadOfLength() {
        // A long run of illegal characters should report the more specific problem.
        TickerSymbolValidator.Result result = TickerSymbolValidator.validate("!!!!!!!!!!!!!!!");

        assertEquals(TickerSymbolValidator.Reason.ILLEGAL_CHARACTERS, result.getReason());
    }

    @Test
    void maxLengthIsTen() {
        assertEquals(10, TickerSymbolValidator.MAX_LENGTH);
    }

    @ParameterizedTest
    @CsvSource({
            "aapl,   AAPL",
            "AAPL,   AAPL",
            "MsFt,   MSFT",
            "brk.b,  BRK.B",
            "ry-a,   RY-A"
    })
    void normalizeKeyFoldsCaseAndNothingElse(String symbol, String expected) {
        assertEquals(expected, TickerSymbolValidator.normalizeKey(symbol));
    }

    @Test
    void normalizeKeyIsIdempotentSoAnAlreadyValidatedSymbolIsUnchanged() {
        final String once = TickerSymbolValidator.normalizeKey("aapl");

        assertEquals(once, TickerSymbolValidator.normalizeKey(once));
        assertEquals(TickerSymbolValidator.validate("  aapl ").getSymbol(), once);
    }

    @Test
    void normalizeKeyDoesNotStripWhitespaceOrValidate() {
        // Unlike validate, this folds case only - callers validate first.
        assertEquals("A A P L", TickerSymbolValidator.normalizeKey("a a p l"));
        assertEquals("!!!", TickerSymbolValidator.normalizeKey("!!!"));
        assertEquals("", TickerSymbolValidator.normalizeKey(""));
    }

    @Test
    void normalizeKeyRejectsNull() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> TickerSymbolValidator.normalizeKey(null));

        assertEquals("Symbol cannot be null", exception.getMessage());
    }

    /**
     * The reason this helper exists rather than three copies of
     * {@code toUpperCase(Locale.ROOT)} in {@code data_access}.
     *
     * <p>Under a Turkish default locale {@code "titan".toUpperCase()} is
     * {@code "TITAN"} with a dotted capital I, so a symbol cached in one case would be
     * invisible when looked up in the other - and the same holding would be fetched,
     * and paid for, twice against the daily quota.
     */
    @Test
    void normalizeKeyIsLocaleIndependentEvenOnATurkishJvm() {
        final Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));

            assertEquals("TITAN", TickerSymbolValidator.normalizeKey("titan"));
            assertEquals("TITAN", TickerSymbolValidator.normalizeKey("TITAN"));
            assertEquals(TickerSymbolValidator.normalizeKey("titan"),
                    TickerSymbolValidator.normalizeKey("TITAN"));

            // The same guarantee has to hold through validate, which delegates here.
            assertEquals("TITAN", TickerSymbolValidator.validate("titan").getSymbol());
            assertEquals("TITAN", TickerSymbolValidator.validate("TITAN").getSymbol());

            // The naive form is what this test exists to rule out.
            assertNotEquals("TITAN", "titan".toUpperCase());
        }
        finally {
            Locale.setDefault(original);
        }
    }
}
