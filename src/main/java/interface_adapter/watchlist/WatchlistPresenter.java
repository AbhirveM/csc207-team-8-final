package interface_adapter.watchlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import use_case.watchlist.AddTickerOutputBoundary;
import use_case.watchlist.AddTickerOutputData;
import use_case.watchlist.RefreshTickerOutputBoundary;
import use_case.watchlist.RefreshTickerOutputData;
import use_case.watchlist.RemoveTickerOutputBoundary;
import use_case.watchlist.RemoveTickerOutputData;
import use_case.watchlist.ShowWatchlistOutputBoundary;
import use_case.watchlist.ShowWatchlistOutputData;
import use_case.watchlist.WatchlistFailure;
import use_case.watchlist.WatchlistSnapshot;

/**
 * Turns every watchlist use-case result into the prose and the rows the view paints.
 *
 * <p>One presenter implements all four output boundaries because all four produce the same
 * {@link WatchlistState}: a single class is what guarantees an add and a refresh cannot
 * drift into wording the user reads as two different applications.
 *
 * <p>This class is the <em>only</em> place in the codebase where user-facing prose may
 * live. The view never builds a message and never formats a number; every field of
 * {@link WatchlistState} leaves here display-ready. Errors are conveyed in words through
 * {@link WatchlistState#getErrorMessage()} - never by colour alone (vision.md principle 9).
 *
 * <p>The presenter is synchronous and is called from the view's background worker, so
 * {@link WatchlistViewModel#setState(WatchlistState)} fires off the event dispatch thread.
 * Re-entering Swing safely is the view listener's job, not this class's.
 */
public final class WatchlistPresenter
        implements AddTickerOutputBoundary,
                   RemoveTickerOutputBoundary,
                   RefreshTickerOutputBoundary,
                   ShowWatchlistOutputBoundary {

    /**
     * What a zero price count reads as. A restored-from-disk ticker has no prices until the
     * user loads them, and a literal {@code 0} in that cell reads as a failure rather than
     * as "not fetched yet".
     */
    private static final String NOT_LOADED = "Not loaded";

    /**
     * The em dash standing in for an absent date or close, so no cell is ever blank.
     * Written as an escape so the constant cannot be corrupted by a mis-set source encoding.
     */
    private static final String ABSENT = "\u2014";

    /** How a blank symbol is referred to, so a blank-input message still reads as English. */
    private static final String UNTYPED_SYMBOL = "the symbol you typed";

    /**
     * How a price is written into a chart's axis labels and summary. Two decimals and no
     * currency mark, matching the figures in the daily-price table the chart sits above -
     * a gutter label reading {@code $249.68} beside a column reading {@code 249.68} would
     * look like two different quantities.
     */
    private static final String MONEY_FORMAT = "%.2f";

    private final WatchlistViewModel viewModel;

    /**
     * @param viewModel the view model this presenter writes every result into
     * @throws NullPointerException if {@code viewModel} is null
     */
    public WatchlistPresenter(WatchlistViewModel viewModel) {
        this.viewModel = Objects.requireNonNull(viewModel, "View model cannot be null");
    }

    /**
     * Renders a successful add.
     *
     * <p>Three outcomes are distinguishable and all three are successes: the company name
     * resolved, the provider simply has no company record for the symbol, or the name
     * lookup itself failed. The ticker and its prices were stored in every case, so none of
     * them sets an error message. The third message deliberately does not name the
     * underlying {@code MarketDataException.Kind} - the user can do nothing with
     * "rate limited on a name lookup", and a stable sentence stays pinnable by one test.
     *
     * @param outputData the symbol added, its name, its price count and the new watchlist
     * @throws NullPointerException if {@code outputData} is null
     */
    @Override
    public void prepareSuccessView(AddTickerOutputData outputData) {
        Objects.requireNonNull(outputData, "Output data cannot be null");
        final String symbol = outputData.getAddedSymbol();
        final int priceCount = outputData.getPriceCount();

        final String status;
        if (outputData.isCompanyNameAvailable()) {
            status = String.format("Added %s (%s) with %d days of price history.",
                    symbol, outputData.getCompanyName(), priceCount);
        }
        else if (outputData.getCompanyNameFailureKind() == null) {
            status = String.format(
                    "Added %s with %d days of price history. No company name was available.",
                    symbol, priceCount);
        }
        else {
            status = String.format("Added %s with %d days of price history. "
                    + "The company name could not be looked up right now.", symbol, priceCount);
        }

        publishSuccess(outputData.getSnapshot(), status);
    }

    /**
     * Renders a successful remove.
     *
     * @param outputData the symbol removed and the watchlist as it stands after the removal
     * @throws NullPointerException if {@code outputData} is null
     */
    @Override
    public void prepareSuccessView(RemoveTickerOutputData outputData) {
        Objects.requireNonNull(outputData, "Output data cannot be null");
        final String status = String.format("Removed %s from your watchlist.",
                outputData.getRemovedSymbol());
        publishSuccess(outputData.getSnapshot(), status);
    }

    /**
     * Renders a successful refresh.
     *
     * <p>A refresh that returned nothing is still a success - the request reached the
     * provider - so it reports the emptier sentence rather than an error. History counts as
     * present only when both the count and the latest date are populated, so the message can
     * never degrade to a dangling "latest .".
     *
     * @param outputData the symbol refreshed, its price count, its newest date and the
     *                   watchlist as it stands
     * @throws NullPointerException if {@code outputData} is null
     */
    @Override
    public void prepareSuccessView(RefreshTickerOutputData outputData) {
        Objects.requireNonNull(outputData, "Output data cannot be null");
        final String symbol = outputData.getSymbol();
        final String latestDate = outputData.getLatestDate();

        final String status;
        if (outputData.getPriceCount() > 0 && hasText(latestDate)) {
            status = String.format("Refreshed %s: %d days of price history, latest %s.",
                    symbol, outputData.getPriceCount(), latestDate);
        }
        else {
            status = String.format("Refreshed %s, but no price history was returned.", symbol);
        }

        publishSuccess(outputData.getSnapshot(), status);
    }

    /**
     * Renders the watchlist, with the requested selection applied.
     *
     * <p>This is the one success path that <em>populates</em> {@code tickerFieldText}
     * instead of clearing it, and the difference is deliberate - do not "fix" it back.
     * Add, Remove and Refresh are text <em>submissions</em>: the user typed something, it
     * was consumed, and leaving it behind would invite a double-submit, so those three
     * clear the field. Show Watchlist is a <em>selection</em>: the user clicked a row to
     * say "this is the ticker I mean". The view reads one ticker field for Add, Remove and
     * Refresh alike, so if a selection left the field empty, clicking the AAPL row and
     * pressing Refresh would answer {@code Enter a ticker symbol before continuing.} with
     * the row plainly selected.
     *
     * <p>Putting the selected symbol in the field is what keeps a single source of truth
     * for "which symbol is the user talking about" - the alternative was a second one in
     * the view. The snapshot's selected symbol is already {@code ""} when nothing is
     * selected, so an empty selection still clears the field.
     *
     * @param outputData how many tickers are held and the watchlist as it stands
     * @throws NullPointerException if {@code outputData} is null
     */
    @Override
    public void prepareSuccessView(ShowWatchlistOutputData outputData) {
        Objects.requireNonNull(outputData, "Output data cannot be null");
        final int tickerCount = outputData.getTickerCount();

        final String status;
        if (tickerCount > 0) {
            status = String.format("Showing %d tickers.", tickerCount);
        }
        else {
            status = "Your watchlist is empty. Add a ticker to begin.";
        }

        final WatchlistSnapshot snapshot = outputData.getSnapshot();
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        publishSuccess(snapshot, status, snapshot.getSelectedSymbol());
    }

    /**
     * Reports a failure without disturbing anything the user can still read or reuse.
     *
     * <p>Three deliberate preservations, each of which would be a worse bug than the failure
     * being reported if it were dropped:
     * <ul>
     *   <li>{@code tickerRows} and {@code priceRows} are copied from the current state, so a
     *       rate-limited refresh never blanks the watchlist the user already had.</li>
     *   <li>{@code tickerFieldText} is preserved, so a typo does not have to be retyped.
     *       Every success path clears it instead.</li>
     *   <li>{@code statusMessage} keeps the previous status rather than being emptied.
     *       {@link WatchlistState} rejects a blank status because an empty status label
     *       collapses and makes the layout jump, so the choice is between the previous
     *       status and an invented one; keeping the previous status leaves the last thing
     *       that actually happened on screen next to the error explaining what just did
     *       not. {@link WatchlistState#initial()} guarantees it is never blank.</li>
     * </ul>
     *
     * <p>No interactor drives this from Show Watchlist - that use case has no failure mode
     * of its own - but the boundary declares it, so it is implemented and tested directly.
     *
     * @param failure why the operation could not be completed
     * @throws NullPointerException if {@code failure} is null
     */
    @Override
    public void prepareFailView(WatchlistFailure failure) {
        Objects.requireNonNull(failure, "Failure cannot be null");
        final WatchlistState current = viewModel.getState();
        viewModel.setState(new WatchlistState(
                current.getTickerRows(),
                current.getPriceRows(),
                current.getSelectedSymbol(),
                current.getStatusMessage(),
                messageFor(failure),
                current.getTickerFieldText(),
                // Preserved for the same reason the rows above it are: the chart is drawn
                // directly over the price table, so clearing one and keeping the other would
                // leave the screen contradicting itself while the error is on display.
                current.getPriceChart()));
    }

    /**
     * Maps a failure onto the sentence the user reads.
     *
     * <p>An exhaustive {@code switch} expression with no {@code default} branch, so a
     * twelfth {@code Kind} becomes a compile error here rather than a silent fallthrough to
     * a generic apology.
     *
     * @param failure the failure to describe
     * @return the error message, naming what the user can do about it wherever there is
     *         something to do
     */
    private static String messageFor(WatchlistFailure failure) {
        final String symbol = quoted(failure.getSymbol());
        return switch (failure.getKind()) {
            case BLANK_INPUT -> "Enter a ticker symbol before continuing.";
            case BAD_FORMAT -> String.format(
                    "%s is not a valid ticker symbol. "
                            + "Use letters, digits, dots, and hyphens only.", symbol);
            case TOO_LONG -> String.format(
                    "%s is too long. Ticker symbols are at most 10 characters.", symbol);
            case DUPLICATE -> String.format("%s is already on your watchlist.", symbol);
            case NOT_ON_WATCHLIST -> String.format(
                    "%s is not on your watchlist. Add it first.", symbol);
            case NETWORK -> String.format(
                    "Could not reach the market data service for %s. "
                            + "Check your connection and try again.", symbol);
            case RATE_LIMIT -> String.format(
                    "The market data service request limit has been reached. "
                            + "Wait a minute, then try %s again.", symbol);
            case INVALID_SYMBOL -> String.format(
                    "The market data service does not recognize %s.", symbol);
            case EMPTY_RESPONSE -> String.format(
                    "The market data service returned no price history for %s.", symbol);
            case MALFORMED_RESPONSE -> String.format(
                    "The market data for %s could not be read. Try again later.", symbol);
            case MISSING_API_KEY -> String.format(
                    "No market data API key is configured, so %s cannot be loaded. "
                            + "Set ALPHA_VANTAGE_API_KEY and restart.", symbol);
        };
    }

    /**
     * Renders a symbol for inclusion in a sentence.
     *
     * @param symbol the symbol involved, possibly blank on a validation failure
     * @return the symbol in double quotes, or the words {@code the symbol you typed} when it
     *         is blank - so a blank-input failure reads as English rather than as
     *         {@code "" is not valid}
     */
    private static String quoted(String symbol) {
        final String result;
        if (hasText(symbol)) {
            result = "\"" + symbol + "\"";
        }
        else {
            result = UNTYPED_SYMBOL;
        }
        return result;
    }

    /**
     * Writes a success into the view model: the snapshot's rows, the given prose, no error,
     * and a cleared ticker field.
     *
     * <p>The default for a success is a cleared field, because Add, Remove and Refresh are
     * text submissions whose input has been consumed. Only Show Watchlist overrides it, via
     * {@link #publishSuccess(WatchlistSnapshot, String, String)}.
     *
     * @param snapshot      the watchlist as the use case left it
     * @param statusMessage the sentence describing what just happened
     */
    private void publishSuccess(WatchlistSnapshot snapshot, String statusMessage) {
        Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        publishSuccess(snapshot, statusMessage, "");
    }

    /**
     * Writes a success into the view model with an explicit ticker field.
     *
     * @param snapshot        the watchlist as the use case left it
     * @param statusMessage   the sentence describing what just happened
     * @param tickerFieldText what the ticker field should contain afterwards; {@code ""}
     *                        for a consumed submission, and the selected symbol for a
     *                        selection
     */
    private void publishSuccess(WatchlistSnapshot snapshot, String statusMessage,
                                String tickerFieldText) {
        viewModel.setState(new WatchlistState(
                toTickerRows(snapshot.getTickerRows()),
                toPriceRows(snapshot.getSelectedPriceRows()),
                snapshot.getSelectedSymbol(),
                statusMessage,
                "",
                tickerFieldText,
                chartFor(snapshot)));
    }

    /**
     * Builds the close-price series for the selected ticker, with every label already
     * formatted - the chart itself composes no text.
     *
     * <p>The dates come off the snapshot's price rows rather than being carried separately.
     * Those rows are newest-first, so the series start is the <em>last</em> of them and the
     * series end is the first; the closes beside them run the other way. That inversion is the
     * one thing in this method worth reading twice.
     *
     * @param snapshot the watchlist as the use case left it
     * @return the chart to paint, or an empty one when the selection has no prices
     */
    private static WatchlistState.PriceChart chartFor(WatchlistSnapshot snapshot) {
        final List<Double> closes = snapshot.getSelectedCloses();
        final List<WatchlistSnapshot.PriceRow> rows = snapshot.getSelectedPriceRows();
        if (closes.isEmpty() || rows.isEmpty()) {
            return WatchlistState.PriceChart.empty();
        }

        double low = closes.get(0);
        double high = closes.get(0);
        for (final Double close : closes) {
            low = Math.min(low, close);
            high = Math.max(high, close);
        }

        final double first = closes.get(0);
        final double latest = closes.get(closes.size() - 1);
        final String lowLabel = money(low);
        final String highLabel = money(high);

        // The band's meta slot is a few words wide, so it carries only the part the plotted
        // line cannot be trusted to convey on its own: the direction, with an explicit sign.
        // The whole sentence goes to the accessible description instead.
        final String meta = String.format("%dD %s", closes.size(), signedChange(first, latest));
        final String summary = String.format(
                "Close price for %s, %d days, low %s, high %s, latest %s, %s over the window.",
                snapshot.getSelectedSymbol(), closes.size(), lowLabel, highLabel, money(latest),
                signedChange(first, latest));

        return new WatchlistState.PriceChart(closes, lowLabel, highLabel,
                rows.get(rows.size() - 1).date(), rows.get(0).date(), meta, summary);
    }

    /**
     * Writes the movement across the window with an explicit sign, which is what makes the
     * plotted line's colour redundant rather than load-bearing.
     *
     * @param first  the oldest close in the series
     * @param latest the newest close in the series
     * @return the change and, when the opening price allows one to be computed, the percentage
     *         beside it - both signed, as {@code TableStyler.SignedRenderer} signs a cell
     */
    private static String signedChange(double first, double latest) {
        final double change = latest - first;
        final String signedAmount = signed(change);
        final String result;
        if (first > 0.0) {
            result = signedAmount + " (" + signed(change / first * 100.0) + "%)";
        }
        else {
            result = signedAmount;
        }
        return result;
    }

    /**
     * @param value a movement, which may be negative
     * @return the value to two decimals, carrying an explicit {@code +} when it is positive;
     *         the minus sign the format already supplies covers the other direction
     */
    private static String signed(double value) {
        final String formatted = money(value);
        final String result;
        if (value > 0.0) {
            result = "+" + formatted;
        }
        else {
            result = formatted;
        }
        return result;
    }

    /**
     * @param value a price to write into a chart label
     * @return the price to two decimals, in a fixed locale so the decimal mark cannot drift
     *         from the one the price table beside it uses
     */
    private static String money(double value) {
        return String.format(Locale.ROOT, MONEY_FORMAT, value);
    }

    /**
     * Maps snapshot ticker rows into display rows, substituting the placeholders.
     *
     * <p>The snapshot's lists are unmodifiable, and {@code WatchlistSnapshotFactory} has
     * already ordered prices newest-first and formatted every money value to two decimal
     * places. Nothing is re-ordered or re-formatted here; a fresh list is built rather than
     * the source being touched.
     *
     * @param rows the snapshot's ticker rows
     * @return one display row per input row, in the same order
     */
    private static List<WatchlistState.TickerRow> toTickerRows(
            List<WatchlistSnapshot.TickerRow> rows) {
        final List<WatchlistState.TickerRow> result = new ArrayList<>(rows.size());
        for (final WatchlistSnapshot.TickerRow row : rows) {
            result.add(new WatchlistState.TickerRow(
                    row.symbol(),
                    orSymbol(row.companyName(), row.symbol()),
                    priceCountText(row.priceCount()),
                    orAbsent(row.latestDate()),
                    orAbsent(row.latestClose())));
        }
        return result;
    }

    /**
     * Copies snapshot price rows into display rows.
     *
     * <p>A straight field-for-field copy: every value arrives already formatted and already
     * newest-first, and the copy exists only so the view holds no {@code use_case} type.
     *
     * @param rows the snapshot's price rows for the selected symbol
     * @return one display row per input row, in the same order
     */
    private static List<WatchlistState.PriceRow> toPriceRows(
            List<WatchlistSnapshot.PriceRow> rows) {
        final List<WatchlistState.PriceRow> result = new ArrayList<>(rows.size());
        for (final WatchlistSnapshot.PriceRow row : rows) {
            result.add(new WatchlistState.PriceRow(row.date(), row.open(), row.high(),
                    row.low(), row.close(), row.volume()));
        }
        return result;
    }

    /**
     * @param priceCount how many daily prices are held for a ticker
     * @return {@code "Not loaded"} for zero, the number otherwise
     */
    private static String priceCountText(int priceCount) {
        final String result;
        if (priceCount == 0) {
            result = NOT_LOADED;
        }
        else {
            result = String.valueOf(priceCount);
        }
        return result;
    }

    /**
     * @param companyName the company name from the snapshot, possibly empty
     * @param symbol      the ticker symbol to fall back on
     * @return the company name, or the symbol itself when no name is known - a missing
     *         company name must never be visible as a blank cell
     */
    private static String orSymbol(String companyName, String symbol) {
        final String result;
        if (hasText(companyName)) {
            result = companyName;
        }
        else {
            result = symbol;
        }
        return result;
    }

    /**
     * @param value a snapshot cell that may be empty
     * @return the value, or an em dash when there is nothing to show
     */
    private static String orAbsent(String value) {
        final String result;
        if (hasText(value)) {
            result = value;
        }
        else {
            result = ABSENT;
        }
        return result;
    }

    /**
     * @param value the string to test
     * @return whether {@code value} carries something worth displaying
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
