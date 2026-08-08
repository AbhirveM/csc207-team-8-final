package use_case.watchlist;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import entity.DailyPrice;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;
import use_case.persistence.SaveWatchlist;

/**
 * Adds a validated ticker to the watchlist along with its price history.
 *
 * <p>The order of steps is deliberate:
 * <ol>
 *   <li>Normalize and validate the raw input, so no blank or malformed symbol ever
 *       reaches an entity.</li>
 *   <li>Reject duplicates. The interactor has to check this itself, because
 *       {@code Watchlist.addTicker} silently ignores duplicates and reports
 *       nothing back.</li>
 *   <li>Fetch prices <em>before</em> mutating anything, so a provider failure can
 *       never leave a half-added ticker behind.</li>
 *   <li>Look up the company name, treating failure as cosmetic.</li>
 *   <li>Build the {@code Stock}, which is where a malformed price series is
 *       rejected - still before any mutation.</li>
 *   <li>Only then update the watchlist, store the prices, and save.</li>
 * </ol>
 *
 * <p>Nothing a collaborator can throw escapes {@link #execute}: provider failures
 * arrive as a checked {@link MarketDataException} and {@code Stock}'s invariant
 * violations as an unchecked {@code IllegalArgumentException}, and both are mapped to
 * a {@link WatchlistFailure}. The caller is a {@code SwingWorker}, which would
 * otherwise swallow the stack trace and leave the window silently unchanged.
 *
 * <p>This interactor depends only on interfaces it or its own layer declares
 * ({@link MarketDataGateway}, {@link StockRepository}, and the persistence feature's
 * input boundary), so it knows nothing about Alpha Vantage, HTTP, JSON, API keys, or
 * Swing, and is fully unit-testable offline.
 */
public final class AddTickerInteractor implements AddTickerInputBoundary {

    private final Watchlist watchlist;
    private final MarketDataGateway marketDataGateway;
    private final StockRepository stockRepository;
    private final SaveWatchlist.InputBoundary saveWatchlist;
    private final AddTickerOutputBoundary presenter;

    public AddTickerInteractor(Watchlist watchlist,
                               MarketDataGateway marketDataGateway,
                               StockRepository stockRepository,
                               SaveWatchlist.InputBoundary saveWatchlist,
                               AddTickerOutputBoundary presenter) {
        this.watchlist = Objects.requireNonNull(watchlist, "Watchlist cannot be null");
        this.marketDataGateway = Objects.requireNonNull(marketDataGateway, "Gateway cannot be null");
        this.stockRepository = Objects.requireNonNull(stockRepository, "Stock repository cannot be null");
        this.saveWatchlist = Objects.requireNonNull(saveWatchlist, "Save watchlist cannot be null");
        this.presenter = Objects.requireNonNull(presenter, "Presenter cannot be null");
    }

    @Override
    public void execute(AddTickerInputData inputData) {
        Objects.requireNonNull(inputData, "Input data cannot be null");

        final WatchlistInputSupport.Resolution resolution = WatchlistInputSupport.resolve(
                inputData.getRawSymbol(), watchlist, WatchlistInputSupport.Membership.MUST_BE_ABSENT);

        if (!resolution.isResolved()) {
            presenter.prepareFailView(resolution.getFailure());
            return;
        }

        final String symbol = resolution.getSymbol();

        final List<DailyPrice> prices;
        try {
            prices = marketDataGateway.fetchDailyPrices(symbol);
        }
        catch (MarketDataException exception) {
            presenter.prepareFailView(WatchlistFailure.from(exception));
            return;
        }

        /*
         * A missing company name must never block an add. The provider returns no
         * company record for many valid symbols (exchange-traded funds especially),
         * and this endpoint is the first to be cut off when the request quota runs
         * out, so treating it as fatal would make the feature fail on symbols that
         * worked the day before.
         *
         * "" rather than null, matching AddTickerOutputData's single non-null
         * companyName field: absence is expressed once, not twice.
         */
        String companyName = "";
        MarketDataException.Kind companyNameFailureKind = null;

        try {
            final Optional<String> fetchedName = marketDataGateway.fetchCompanyName(symbol);
            if (fetchedName.isPresent() && !fetchedName.get().isBlank()) {
                companyName = fetchedName.get();
            }
        }
        catch (MarketDataException exception) {
            /*
             * The decision stands - a missing name never blocks the add - but the
             * diagnostic is no longer discarded. The kind is what tells "this symbol
             * has no company record" apart from "the name lookup was rate-limited":
             * the same blank cell, very different advice. The presenter chooses the
             * wording; the add succeeds either way.
             */
            companyNameFailureKind = exception.getKind();
        }

        /*
         * Stock enforces the oldest-to-newest, no-duplicate-dates invariant in its
         * constructor, and signals a violation with an unchecked
         * IllegalArgumentException. That is a malformed provider response, not a
         * programming error, so it is caught and mapped rather than allowed to escape
         * execute and kill the SwingWorker that called it.
         *
         * Building the Stock before touching the watchlist is what keeps the
         * fetch-before-mutate guarantee intact for this failure too: a rejected price
         * series must not leave a ticker behind with no history.
         */
        final Stock stock;
        final Ticker ticker = new Ticker(symbol, companyName);

        try {
            stock = new Stock(ticker, prices);
        }
        catch (IllegalArgumentException exception) {
            presenter.prepareFailView(
                    new WatchlistFailure(WatchlistFailure.Kind.MALFORMED_RESPONSE, symbol));
            return;
        }

        watchlist.addTicker(ticker);
        stockRepository.save(stock);
        saveWatchlist.execute(watchlist);

        presenter.prepareSuccessView(new AddTickerOutputData(
                symbol,
                companyName,
                prices.size(),
                WatchlistSnapshotFactory.build(watchlist, stockRepository, symbol),
                companyNameFailureKind));
    }
}
