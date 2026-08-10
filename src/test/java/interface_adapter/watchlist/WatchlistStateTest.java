package interface_adapter.watchlist;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the value semantics {@link WatchlistState} promises.
 *
 * <p>These are not coverage filler. {@code WatchlistViewModel} skips a redundant repaint by
 * comparing states, {@code WatchlistPresenter.prepareFailView} rebuilds a state by reading
 * fields back off the current one, and presenter tests assert on whole states - so an
 * unexercised {@code equals} or a defensive copy that is not actually defensive would show
 * up as a stale table rather than as a failing assertion.
 */
class WatchlistStateTest {

    private static final WatchlistState.TickerRow APPLE =
            new WatchlistState.TickerRow("AAPL", "Apple Inc.", "100", "2024-05-31", "190.50");

    private static final WatchlistState.PriceRow MAY_31 = new WatchlistState.PriceRow(
            "2024-05-31", "188.00", "191.00", "187.50", "190.50", "51000000");

    // ---------------------------------------------------------------------- initial()

    @Test
    void theInitialStateIsAnEmptyWatchlistWithNothingSelectedAndNoError() {
        final WatchlistState state = WatchlistState.initial();

        assertTrue(state.getTickerRows().isEmpty());
        assertTrue(state.getPriceRows().isEmpty());
        assertEquals("", state.getSelectedSymbol());
        assertEquals("", state.getErrorMessage());
        assertEquals("", state.getTickerFieldText());
        assertFalse(state.isErrorPresent());
    }

    @Test
    void theInitialStatusMessageIsNonBlankSoTheStatusLabelNeverCollapses() {
        assertFalse(WatchlistState.initial().getStatusMessage().isBlank());
    }

    @Test
    void twoInitialStatesAreEqual() {
        assertEquals(WatchlistState.initial(), WatchlistState.initial());
    }

    // ------------------------------------------------------------------ value semantics

    @Test
    void aStateEqualsItself() {
        final WatchlistState state = state("AAPL", "Ready.", "", "");

        assertEquals(state, state);
    }

    @Test
    void twoIndependentlyBuiltStatesWithTheSameContentsAreEqualAndShareAHashCode() {
        final WatchlistState first = state("AAPL", "Ready.", "", "");
        final WatchlistState second = state("AAPL", "Ready.", "", "");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void statesDifferingInTheirTickerRowsAreUnequal() {
        final WatchlistState withRow = new WatchlistState(
                List.of(APPLE), List.of(MAY_31), "AAPL", "Ready.", "", "");
        final WatchlistState withoutRow = new WatchlistState(
                List.of(), List.of(MAY_31), "AAPL", "Ready.", "", "");

        assertNotEquals(withRow, withoutRow);
    }

    @Test
    void statesDifferingInTheirPriceRowsAreUnequal() {
        final WatchlistState withRow = new WatchlistState(
                List.of(APPLE), List.of(MAY_31), "AAPL", "Ready.", "", "");
        final WatchlistState withoutRow = new WatchlistState(
                List.of(APPLE), List.of(), "AAPL", "Ready.", "", "");

        assertNotEquals(withRow, withoutRow);
    }

    @Test
    void statesDifferingInTheirSelectedSymbolAreUnequal() {
        assertNotEquals(state("AAPL", "Ready.", "", ""), state("MSFT", "Ready.", "", ""));
    }

    @Test
    void statesDifferingInTheirStatusMessageAreUnequal() {
        assertNotEquals(state("AAPL", "Ready.", "", ""),
                state("AAPL", "Showing 1 ticker.", "", ""));
    }

    @Test
    void statesDifferingInTheirErrorMessageAreUnequal() {
        assertNotEquals(state("AAPL", "Ready.", "", ""),
                state("AAPL", "Ready.", "Something went wrong.", ""));
    }

    @Test
    void statesDifferingInTheirTickerFieldTextAreUnequal() {
        assertNotEquals(state("AAPL", "Ready.", "", ""), state("AAPL", "Ready.", "", "aapl"));
    }

    // The state must be the *first* argument in these two: assertNotEquals compares with
    // Objects.equals(expected, actual), which invokes expected.equals(actual). With the
    // state second, String.equals runs instead and the instanceof branch never executes.

    @Test
    void aStateIsNotEqualToNull() {
        assertNotEquals(state("AAPL", "Ready.", "", ""), null);
    }

    @Test
    void aStateIsNotEqualToAnUnrelatedType() {
        assertNotEquals(state("AAPL", "Ready.", "", ""), "WatchlistState");
    }

    // -------------------------------------------------------------------- list copying

    @Test
    void mutatingTheTickerListAfterConstructionDoesNotChangeTheState() {
        final List<WatchlistState.TickerRow> rows = new ArrayList<>(List.of(APPLE));
        final WatchlistState state = new WatchlistState(
                rows, new ArrayList<>(), "AAPL", "Ready.", "", "");

        rows.add(new WatchlistState.TickerRow("MSFT", "Microsoft", "50", "2024-05-31", "415.00"));
        rows.clear();

        assertEquals(List.of(APPLE), state.getTickerRows());
    }

    @Test
    void mutatingThePriceListAfterConstructionDoesNotChangeTheState() {
        final List<WatchlistState.PriceRow> rows = new ArrayList<>(List.of(MAY_31));
        final WatchlistState state = new WatchlistState(
                new ArrayList<>(), rows, "AAPL", "Ready.", "", "");

        rows.clear();

        assertEquals(List.of(MAY_31), state.getPriceRows());
    }

    @Test
    void theReturnedTickerRowsCannotBeModifiedByTheView() {
        final WatchlistState state = new WatchlistState(
                List.of(APPLE), List.of(MAY_31), "AAPL", "Ready.", "", "");

        assertThrows(UnsupportedOperationException.class,
                () -> state.getTickerRows().add(APPLE));
    }

    @Test
    void theReturnedPriceRowsCannotBeModifiedByTheView() {
        final WatchlistState state = new WatchlistState(
                List.of(APPLE), List.of(MAY_31), "AAPL", "Ready.", "", "");

        assertThrows(UnsupportedOperationException.class,
                () -> state.getPriceRows().add(MAY_31));
    }

    // -------------------------------------------------------------- constructor guards

    @Test
    void theConstructorRejectsNullTickerRows() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistState(null, List.of(), "", "Ready.", "", ""));
        assertEquals("Ticker rows cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsNullPriceRows() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistState(List.of(), null, "", "Ready.", "", ""));
        assertEquals("Price rows cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsANullSelectedSymbol() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistState(List.of(), List.of(), null, "Ready.", "", ""));
        assertEquals("Selected symbol cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsANullStatusMessage() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistState(List.of(), List.of(), "", null, "", ""));
        assertEquals("Status message cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsANullErrorMessage() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistState(List.of(), List.of(), "", "Ready.", null, ""));
        assertEquals("Error message cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsANullTickerFieldText() {
        final NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new WatchlistState(List.of(), List.of(), "", "Ready.", "", null));
        assertEquals("Ticker field text cannot be null", exception.getMessage());
    }

    @Test
    void theConstructorRejectsAnEmptyStatusMessage() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new WatchlistState(List.of(), List.of(), "", "", "", ""));
        assertEquals("Status message cannot be blank", exception.getMessage());
    }

    @Test
    void theConstructorRejectsAWhitespaceOnlyStatusMessage() {
        assertThrows(IllegalArgumentException.class,
                () -> new WatchlistState(List.of(), List.of(), "", "   ", "", ""));
    }

    // ------------------------------------------------------------------ isErrorPresent

    @Test
    void isErrorPresentIsTrueWhenThereIsAnErrorToShow() {
        final WatchlistState state = state("AAPL", "Ready.", "Could not reach the service.", "");

        assertTrue(state.isErrorPresent());
        assertEquals("Could not reach the service.", state.getErrorMessage());
    }

    @Test
    void isErrorPresentIsFalseWhenTheErrorMessageIsEmpty() {
        assertFalse(state("AAPL", "Ready.", "", "").isErrorPresent());
    }

    @Test
    void isErrorPresentAlwaysAgreesWithWhetherTheErrorMessageIsEmpty() {
        for (final String error : List.of("", "Something went wrong.", " ")) {
            final WatchlistState state = state("AAPL", "Ready.", error, "");

            assertEquals(!state.getErrorMessage().isEmpty(), state.isErrorPresent(),
                    "disagreed for error " + "'" + error + "'");
        }
    }

    // ------------------------------------------------------------------------ toString

    // toString is a debugging aid, not a deliverable, so these assert on substrings.
    // That is the opposite of the rule for the message table in WatchlistPresenterTest,
    // where an exact assertEquals is required because those strings are the deliverable.

    @Test
    void toStringSummarisesTheRowListsByCountRatherThanPrintingThem() {
        final WatchlistState state = new WatchlistState(
                List.of(APPLE), List.of(MAY_31), "AAPL", "Ready.", "", "");

        final String text = state.toString();

        assertTrue(text.contains("tickerRows=1"), text);
        assertTrue(text.contains("priceRows=1"), text);
        assertFalse(text.contains("Apple Inc."), text);
    }

    @Test
    void toStringNamesTheProseFieldsSoAFailingAssertionIsReadable() {
        final WatchlistState state = state("AAPL", "Showing 1 ticker.", "No network.", "aapl");

        final String text = state.toString();

        assertTrue(text.contains("WatchlistState{"), text);
        assertTrue(text.contains("AAPL"), text);
        assertTrue(text.contains("Showing 1 ticker."), text);
        assertTrue(text.contains("No network."), text);
        assertTrue(text.contains("aapl"), text);
    }

    // ------------------------------------------------------------------------ fixtures

    /**
     * A state holding one ticker row and one price row, varying only in its prose.
     *
     * @param selectedSymbol  the symbol whose prices are shown
     * @param statusMessage   what the status label reads; must not be blank
     * @param errorMessage    what the error label reads, or ""
     * @param tickerFieldText what the ticker field contains
     * @return the state
     */
    private static WatchlistState state(String selectedSymbol, String statusMessage,
                                        String errorMessage, String tickerFieldText) {
        return new WatchlistState(List.of(APPLE), List.of(MAY_31), selectedSymbol,
                statusMessage, errorMessage, tickerFieldText);
    }
}
