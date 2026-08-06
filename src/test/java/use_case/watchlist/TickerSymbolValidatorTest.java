package use_case.watchlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
}
