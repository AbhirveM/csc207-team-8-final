package use_case.watchlist;

import entity.DailyPrice;
import entity.Stock;
import entity.Ticker;
import entity.Watchlist;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Re-fetches the price history for a ticker already on the watchlist.
 *
 * <p>Uses {@link MarketDataGateway#fetchDailyPricesFresh} rather than the cached
 * read: bypassing any cached copy is the entire point of this use case.
 *
 * <p>It does not save the watchlist, because refreshing changes only price history
 * and prices are not persisted. It also leaves the previously stored history in place
 * when the provider fails, so a network problem or an exhausted quota degrades to
 * stale-but-usable data rather than losing what the user already had.
 */
public final class RefreshTickerInteractor implements RefreshTickerInputBoundary {

    private final Watchlist watchlist;
    private final MarketDataGateway marketDataGateway;
    private final StockRepository stockRepository;
    private final RefreshTickerOutputBoundary presenter;

    public RefreshTickerInteractor(Watchlist watchlist,
                                   MarketDataGateway marketDataGateway,
                                   StockRepository stockRepository,
                                   RefreshTickerOutputBoundary presenter) {
        this.watchlist = Objects.requireNonNull(watchlist, "Watchlist cannot be null");
        this.marketDataGateway = Objects.requireNonNull(marketDataGateway, "Gateway cannot be null");
        this.stockRepository = Objects.requireNonNull(stockRepository, "Stock repository cannot be null");
        this.presenter = Objects.requireNonNull(presenter, "Presenter cannot be null");
    }

    @Override
    public void execute(RefreshTickerInputData inputData) {
        final String rawSymbol = inputData.getRawSymbol();
        final TickerSymbolValidator.Result validation = TickerSymbolValidator.validate(rawSymbol);

        if (!validation.isValid()) {
            presenter.prepareFailView(WatchlistFailure.from(validation.getReason(), rawSymbol));
            return;
        }

        final String symbol = validation.getSymbol();

        /*
         * Guard before touching the provider, so refreshing something that was never
         * added cannot spend a request from the daily quota.
         */
        if (!watchlist.contains(new Ticker(symbol, null))) {
            presenter.prepareFailView(
                    new WatchlistFailure(WatchlistFailure.Kind.NOT_ON_WATCHLIST, symbol));
            return;
        }

        final List<DailyPrice> prices;
        try {
            prices = marketDataGateway.fetchDailyPricesFresh(symbol);
        }
        catch (MarketDataException e) {
            presenter.prepareFailView(WatchlistFailure.from(e));
            return;
        }

        // Keep whatever company name was already discovered for this ticker.
        final Optional<Stock> existing = stockRepository.findBySymbol(symbol);
        final Stock refreshed = existing
                .map(stock -> stock.withDailyPrices(prices))
                .orElseGet(() -> new Stock(tickerFor(symbol), prices));
        stockRepository.save(refreshed);

        presenter.prepareSuccessView(new RefreshTickerOutputData(
                symbol,
                refreshed.getPriceCount(),
                refreshed.getLatestPrice().map(price -> String.valueOf(price.getDate())).orElse(""),
                WatchlistSnapshotFactory.build(watchlist, stockRepository, symbol)));
    }

    /** Recovers the ticker from the watchlist so its company name is not lost. */
    private Ticker tickerFor(String symbol) {
        return watchlist.findEntry(new Ticker(symbol, null))
                .map(entry -> entry.getTicker())
                .orElseGet(() -> new Ticker(symbol, null));
    }
}
