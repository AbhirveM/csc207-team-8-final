package use_case.watchlist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link WatchlistFailure}'s value semantics and its two mapping factories.
 *
 * <p>The value semantics matter beyond tidiness: the presenter uses them to tell a
 * repeated failure from a new one, so an interactor test that asserts on a whole
 * failure is only meaningful if {@code equals} is right.
 */
class WatchlistFailureTest {

    @Test
    void equalFailuresAreEqualAndShareAHashCode() {
        final WatchlistFailure left =
                new WatchlistFailure(WatchlistFailure.Kind.DUPLICATE, "AAPL");
        final WatchlistFailure right =
                new WatchlistFailure(WatchlistFailure.Kind.DUPLICATE, "AAPL");

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertEquals(left, left);
    }

    @Test
    void failuresDifferingInKindOrSymbolAreNotEqual() {
        final WatchlistFailure base =
                new WatchlistFailure(WatchlistFailure.Kind.DUPLICATE, "AAPL");

        assertNotEquals(base, new WatchlistFailure(WatchlistFailure.Kind.NETWORK, "AAPL"));
        assertNotEquals(base, new WatchlistFailure(WatchlistFailure.Kind.DUPLICATE, "MSFT"));
        assertNotEquals(base, null);
        assertNotEquals(base, "not a failure");
    }

    @Test
    void aNullSymbolIsNormalizedToTheEmptyString() {
        final WatchlistFailure failure =
                new WatchlistFailure(WatchlistFailure.Kind.BLANK_INPUT, null);

        assertEquals("", failure.getSymbol());
        assertEquals(failure, new WatchlistFailure(WatchlistFailure.Kind.BLANK_INPUT, ""));
    }

    @Test
    void aNullKindIsRejected() {
        assertThrows(NullPointerException.class, () -> new WatchlistFailure(null, "AAPL"));
    }

    @Test
    void toStringNamesTheKindAndTheSymbolSoAFailingAssertionReads() {
        final String text =
                new WatchlistFailure(WatchlistFailure.Kind.RATE_LIMIT, "AAPL").toString();

        assertTrue(text.contains("RATE_LIMIT"));
        assertTrue(text.contains("AAPL"));
    }

    @ParameterizedTest
    @EnumSource(MarketDataException.Kind.class)
    void everyGatewayFailureKindMapsOntoTheSameNamedWatchlistKind(
            MarketDataException.Kind kind) {
        final WatchlistFailure failure = WatchlistFailure.from(
                new MarketDataException(kind, "AAPL", "simulated"));

        assertEquals(WatchlistFailure.Kind.valueOf(kind.name()), failure.getKind());
        assertEquals("AAPL", failure.getSymbol());
    }

    @Test
    void validationReasonsMapOntoTheirWatchlistKinds() {
        assertEquals(WatchlistFailure.Kind.BLANK_INPUT,
                WatchlistFailure.from(TickerSymbolValidator.Reason.BLANK, "  ").getKind());
        assertEquals(WatchlistFailure.Kind.TOO_LONG,
                WatchlistFailure.from(TickerSymbolValidator.Reason.TOO_LONG, "ABCDEFGHIJK").getKind());
        assertEquals(WatchlistFailure.Kind.BAD_FORMAT,
                WatchlistFailure.from(TickerSymbolValidator.Reason.ILLEGAL_CHARACTERS, "A$").getKind());
    }

    @Test
    void aValidationFailureQuotesBackTheRawTextRatherThanANormalizedSymbol() {
        final WatchlistFailure failure =
                WatchlistFailure.from(TickerSymbolValidator.Reason.ILLEGAL_CHARACTERS, " aa$pl ");

        assertEquals(" aa$pl ", failure.getSymbol());
    }
}
