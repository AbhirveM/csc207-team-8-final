package interface_adapter.watchlist;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import use_case.watchlist.AddTickerOutputData;
import use_case.watchlist.MarketDataException;
import use_case.watchlist.RefreshTickerOutputData;
import use_case.watchlist.RemoveTickerOutputData;
import use_case.watchlist.ShowWatchlistOutputData;
import use_case.watchlist.WatchlistFailure;
import use_case.watchlist.WatchlistSnapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins every user-facing string the watchlist can produce.
 *
 * <p>Each assertion is {@code assertEquals} on the whole message rather than a substring
 * check: these strings are the deliverable, and {@code contains} lets a typo through.
 *
 * <p>No test doubles. The presenter's only collaborator is a real {@link WatchlistViewModel},
 * and the output data classes are plain values, so a fake would only add a place for the
 * contract to drift.
 */
class WatchlistPresenterTest {

    /** The em dash the presenter substitutes for an absent cell, as an escape. */
    private static final String ABSENT = "\u2014";

    private WatchlistViewModel viewModel;
    private WatchlistPresenter presenter;

    @BeforeEach
    void setUp() {
        viewModel = new WatchlistViewModel();
        presenter = new WatchlistPresenter(viewModel);
    }

    // ----------------------------------------------------------------- construction

    @Test
    void theConstructorRejectsANullViewModel() {
        final NullPointerException exception =
                assertThrows(NullPointerException.class, () -> new WatchlistPresenter(null));
        assertEquals("View model cannot be null", exception.getMessage());
    }

    // -------------------------------------------------------- the eleven failure rows

    @Test
    void blankInputFailureAsksForASymbolWithoutQuotingAnEmptyString() {
        presenter.prepareFailView(new WatchlistFailure(WatchlistFailure.Kind.BLANK_INPUT, ""));

        assertEquals("Enter a ticker symbol before continuing.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void badFormatFailureNamesTheCharactersThatAreAllowed() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.BAD_FORMAT, "AA_PL"));

        assertEquals("\"AA_PL\" is not a valid ticker symbol. "
                        + "Use letters, digits, dots, and hyphens only.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void tooLongFailureNamesTheTenCharacterLimit() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.TOO_LONG, "ABCDEFGHIJK"));

        assertEquals("\"ABCDEFGHIJK\" is too long. Ticker symbols are at most 10 characters.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void duplicateFailureSaysTheSymbolIsAlreadyOnTheWatchlist() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.DUPLICATE, "AAPL"));

        assertEquals("\"AAPL\" is already on your watchlist.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void notOnWatchlistFailureTellsTheUserToAddItFirst() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.NOT_ON_WATCHLIST, "MSFT"));

        assertEquals("\"MSFT\" is not on your watchlist. Add it first.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void networkFailureSuggestsCheckingTheConnection() {
        presenter.prepareFailView(new WatchlistFailure(WatchlistFailure.Kind.NETWORK, "AAPL"));

        assertEquals("Could not reach the market data service for \"AAPL\". "
                        + "Check your connection and try again.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void rateLimitFailureTellsTheUserToWaitAMinute() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.RATE_LIMIT, "AAPL"));

        assertEquals("The market data service request limit has been reached. "
                        + "Wait a minute, then try \"AAPL\" again.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void invalidSymbolFailureSaysTheServiceDoesNotRecognizeIt() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.INVALID_SYMBOL, "ZZZZ"));

        assertEquals("The market data service does not recognize \"ZZZZ\".",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void emptyResponseFailureSaysNoPriceHistoryCameBack() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.EMPTY_RESPONSE, "AAPL"));

        assertEquals("The market data service returned no price history for \"AAPL\".",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void malformedResponseFailureSaysTheDataCouldNotBeRead() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.MALFORMED_RESPONSE, "AAPL"));

        assertEquals("The market data for \"AAPL\" could not be read. Try again later.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void missingApiKeyFailureNamesTheEnvironmentVariableToSet() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.MISSING_API_KEY, "AAPL"));

        assertEquals("No market data API key is configured, so \"AAPL\" cannot be loaded. "
                        + "Set ALPHA_VANTAGE_API_KEY and restart.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void aFailureCarryingABlankSymbolStillReadsAsASentence() {
        presenter.prepareFailView(new WatchlistFailure(WatchlistFailure.Kind.DUPLICATE, ""));

        assertEquals("the symbol you typed is already on your watchlist.",
                viewModel.getState().getErrorMessage());
    }

    @Test
    void everyFailureKindProducesANonEmptyErrorThatTheViewWillShow() {
        for (final WatchlistFailure.Kind kind : WatchlistFailure.Kind.values()) {
            presenter.prepareFailView(new WatchlistFailure(kind, "AAPL"));

            assertTrue(viewModel.getState().isErrorPresent(), "no error shown for " + kind);
        }
    }

    // --------------------------------------------------------- the eight success rows

    @Test
    void addWithAKnownCompanyNameNamesTheCompany() {
        presenter.prepareSuccessView(new AddTickerOutputData(
                "AAPL", "Apple Inc.", 100, snapshotWith(tickerRow("AAPL", "Apple Inc.", 100))));

        assertEquals("Added AAPL (Apple Inc.) with 100 days of price history.",
                viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    @Test
    void addWithNoCompanyNameAndNoLookupFailureSaysNoNameWasAvailable() {
        presenter.prepareSuccessView(new AddTickerOutputData(
                "AAPL", "", 100, snapshotWith(tickerRow("AAPL", "", 100))));

        assertEquals("Added AAPL with 100 days of price history. "
                        + "No company name was available.",
                viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    @Test
    void addWithNoCompanyNameButALookupFailureSaysTheLookupCouldNotBeDone() {
        presenter.prepareSuccessView(new AddTickerOutputData(
                "AAPL", "", 100, snapshotWith(tickerRow("AAPL", "", 100)),
                MarketDataException.Kind.RATE_LIMIT));

        assertEquals("Added AAPL with 100 days of price history. "
                        + "The company name could not be looked up right now.",
                viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    @Test
    void aFailedCompanyNameLookupIsStillASuccessAndNotAnError() {
        presenter.prepareSuccessView(new AddTickerOutputData(
                "AAPL", "", 100, snapshotWith(tickerRow("AAPL", "", 100)),
                MarketDataException.Kind.NETWORK));

        assertFalse(viewModel.getState().isErrorPresent());
    }

    @Test
    void removeSaysTheSymbolWasTakenOffTheWatchlist() {
        presenter.prepareSuccessView(
                new RemoveTickerOutputData("AAPL", snapshotWith()));

        assertEquals("Removed AAPL from your watchlist.",
                viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    @Test
    void refreshWithHistoryReportsTheCountAndTheLatestDate() {
        presenter.prepareSuccessView(new RefreshTickerOutputData(
                "AAPL", 100, "2024-05-31", snapshotWith(tickerRow("AAPL", "Apple Inc.", 100))));

        assertEquals("Refreshed AAPL: 100 days of price history, latest 2024-05-31.",
                viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    @Test
    void refreshWithNoHistorySaysNonePriceHistoryWasReturned() {
        presenter.prepareSuccessView(new RefreshTickerOutputData(
                "AAPL", 0, "", snapshotWith(tickerRow("AAPL", "Apple Inc.", 0))));

        assertEquals("Refreshed AAPL, but no price history was returned.",
                viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    @Test
    void showWithTickersReportsHowManyAreOnTheWatchlist() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                2, snapshotWith(tickerRow("AAPL", "Apple Inc.", 100),
                        tickerRow("MSFT", "Microsoft Corporation", 100))));

        assertEquals("Showing 2 tickers.", viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    @Test
    void showWithAnEmptyWatchlistInvitesTheUserToAddATicker() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(0, snapshotWith()));

        assertEquals("Your watchlist is empty. Add a ticker to begin.",
                viewModel.getState().getStatusMessage());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    // ---------------------------------------------------------------- row placeholders

    @Test
    void aZeroPriceCountRendersAsNotLoadedRatherThanZero() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                1, snapshotWith(new WatchlistSnapshot.TickerRow(
                        "AAPL", "Apple Inc.", 0, "", ""))));

        assertEquals("Not loaded", viewModel.getState().getTickerRows().get(0).priceCount());
    }

    @Test
    void aNonZeroPriceCountRendersAsTheNumber() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                1, snapshotWith(tickerRow("AAPL", "Apple Inc.", 100))));

        assertEquals("100", viewModel.getState().getTickerRows().get(0).priceCount());
    }

    @Test
    void anAbsentCompanyNameRendersAsTheSymbolItselfAndNeverAsABlankCell() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                1, snapshotWith(tickerRow("AAPL", "", 100))));

        assertEquals("AAPL", viewModel.getState().getTickerRows().get(0).companyName());
    }

    @Test
    void anAbsentLatestDateOrCloseRendersAsAnEmDash() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                1, snapshotWith(new WatchlistSnapshot.TickerRow(
                        "AAPL", "Apple Inc.", 0, "", ""))));

        final WatchlistState.TickerRow row = viewModel.getState().getTickerRows().get(0);
        assertEquals(ABSENT, row.latestDate());
        assertEquals(ABSENT, row.latestClose());
    }

    @Test
    void aPresentLatestDateAndCloseArePassedThroughUnchanged() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                1, snapshotWith(tickerRow("AAPL", "Apple Inc.", 100))));

        final WatchlistState.TickerRow row = viewModel.getState().getTickerRows().get(0);
        assertEquals("2024-05-31", row.latestDate());
        assertEquals("190.50", row.latestClose());
    }

    @Test
    void priceRowsAreCopiedFieldForFieldWithoutReorderingOrReformatting() {
        final WatchlistSnapshot snapshot = new WatchlistSnapshot(
                List.of(tickerRow("AAPL", "Apple Inc.", 2)),
                "AAPL",
                List.of(new WatchlistSnapshot.PriceRow(
                                "2024-05-31", "188.00", "191.00", "187.50", "190.50", "51000000"),
                        new WatchlistSnapshot.PriceRow(
                                "2024-05-30", "186.00", "189.00", "185.00", "188.00", "49000000")));

        presenter.prepareSuccessView(new ShowWatchlistOutputData(1, snapshot));

        final List<WatchlistState.PriceRow> rows = viewModel.getState().getPriceRows();
        assertEquals(2, rows.size());
        assertEquals(new WatchlistState.PriceRow(
                        "2024-05-31", "188.00", "191.00", "187.50", "190.50", "51000000"),
                rows.get(0));
        assertEquals("2024-05-30", rows.get(1).date());
    }

    @Test
    void successAdoptsTheSelectedSymbolFromTheSnapshot() {
        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                1, new WatchlistSnapshot(
                        List.of(tickerRow("AAPL", "Apple Inc.", 1)), "AAPL", List.of())));

        assertEquals("AAPL", viewModel.getState().getSelectedSymbol());
    }

    // ------------------------------------------------------- what a failure must not do

    @Test
    void prepareFailViewLeavesTheTickerAndPriceRowsExactlyAsTheyWere() {
        final WatchlistState before = populatedState("aapl");
        viewModel.setState(before);

        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.RATE_LIMIT, "AAPL"));

        assertEquals(before.getTickerRows(), viewModel.getState().getTickerRows());
        assertEquals(before.getPriceRows(), viewModel.getState().getPriceRows());
    }

    @Test
    void prepareFailViewPreservesTheTickerFieldTextSoATypoNeedNotBeRetyped() {
        viewModel.setState(populatedState("aaplx"));

        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.BAD_FORMAT, "aaplx"));

        assertEquals("aaplx", viewModel.getState().getTickerFieldText());
    }

    @Test
    void prepareFailViewKeepsThePreviousStatusMessageBecauseABlankStatusIsRejected() {
        viewModel.setState(populatedState("aapl"));

        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.NETWORK, "AAPL"));

        assertEquals("Showing 1 tickers.", viewModel.getState().getStatusMessage());
    }

    @Test
    void prepareFailViewKeepsTheSelectedSymbol() {
        viewModel.setState(populatedState("aapl"));

        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.NETWORK, "AAPL"));

        assertEquals("AAPL", viewModel.getState().getSelectedSymbol());
    }

    @Test
    void everySuccessClearsTheTickerFieldText() {
        viewModel.setState(populatedState("aapl"));

        presenter.prepareSuccessView(new ShowWatchlistOutputData(0, snapshotWith()));

        assertEquals("", viewModel.getState().getTickerFieldText());
    }

    @Test
    void aSuccessAfterAFailureClearsTheErrorMessage() {
        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.NETWORK, "AAPL"));
        assertTrue(viewModel.getState().isErrorPresent());

        presenter.prepareSuccessView(new ShowWatchlistOutputData(0, snapshotWith()));

        assertFalse(viewModel.getState().isErrorPresent());
        assertEquals("", viewModel.getState().getErrorMessage());
    }

    // ------------------------------------------------------------------- event firing

    @Test
    void eachBoundaryCallFiresTheStateEventExactlyOnce() {
        final AtomicInteger events = new AtomicInteger();
        viewModel.addPropertyChangeListener(event -> {
            if (WatchlistViewModel.STATE_PROPERTY.equals(event.getPropertyName())) {
                events.incrementAndGet();
            }
        });

        presenter.prepareSuccessView(new AddTickerOutputData(
                "AAPL", "Apple Inc.", 100, snapshotWith(tickerRow("AAPL", "Apple Inc.", 100))));
        assertEquals(1, events.get());

        presenter.prepareSuccessView(new RemoveTickerOutputData("AAPL", snapshotWith()));
        assertEquals(2, events.get());

        presenter.prepareSuccessView(new RefreshTickerOutputData(
                "AAPL", 100, "2024-05-31", snapshotWith(tickerRow("AAPL", "Apple Inc.", 100))));
        assertEquals(3, events.get());

        presenter.prepareSuccessView(new ShowWatchlistOutputData(0, snapshotWith()));
        assertEquals(4, events.get());

        presenter.prepareFailView(
                new WatchlistFailure(WatchlistFailure.Kind.NETWORK, "AAPL"));
        assertEquals(5, events.get());
    }

    @Test
    void anIdenticalRepeatedResultStillFiresTheEvent() {
        final AtomicInteger events = new AtomicInteger();
        viewModel.addPropertyChangeListener(event -> events.incrementAndGet());

        presenter.prepareSuccessView(new ShowWatchlistOutputData(0, snapshotWith()));
        presenter.prepareSuccessView(new ShowWatchlistOutputData(0, snapshotWith()));

        assertEquals(2, events.get());
    }

    // ------------------------------------------------------------------------ fixtures

    /** @return a snapshot holding the given ticker rows, with nothing selected. */
    private static WatchlistSnapshot snapshotWith(WatchlistSnapshot.TickerRow... rows) {
        return new WatchlistSnapshot(List.of(rows), "", List.of());
    }

    /** @return a ticker row with a plausible latest date and close. */
    private static WatchlistSnapshot.TickerRow tickerRow(String symbol, String companyName,
                                                         int priceCount) {
        return new WatchlistSnapshot.TickerRow(
                symbol, companyName, priceCount, "2024-05-31", "190.50");
    }

    /**
     * A state that already holds rows, a selection, a status and typed text, so a failure
     * test can prove that none of them is lost.
     *
     * @param tickerFieldText what the user has typed into the ticker field
     * @return the populated state
     */
    private static WatchlistState populatedState(String tickerFieldText) {
        return new WatchlistState(
                List.of(new WatchlistState.TickerRow(
                        "AAPL", "Apple Inc.", "100", "2024-05-31", "190.50")),
                List.of(new WatchlistState.PriceRow(
                        "2024-05-31", "188.00", "191.00", "187.50", "190.50", "51000000")),
                "AAPL",
                "Showing 1 tickers.",
                "",
                tickerFieldText);
    }
}
