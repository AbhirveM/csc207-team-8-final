package use_case.watchlist;

import java.util.Objects;

import entity.Watchlist;
import use_case.persistence.SaveWatchlist;

/**
 * Removes a ticker, and its cached price history, from the watchlist.
 *
 * <p>Has no market-data dependency at all: removing a ticker never needs the
 * provider, so this use case cannot fail because of the network or the request quota.
 */
public final class RemoveTickerInteractor implements RemoveTickerInputBoundary {

    private final Watchlist watchlist;
    private final StockRepository stockRepository;
    private final SaveWatchlist.InputBoundary saveWatchlist;
    private final RemoveTickerOutputBoundary presenter;

    public RemoveTickerInteractor(Watchlist watchlist,
                                  StockRepository stockRepository,
                                  SaveWatchlist.InputBoundary saveWatchlist,
                                  RemoveTickerOutputBoundary presenter) {
        this.watchlist = Objects.requireNonNull(watchlist, "Watchlist cannot be null");
        this.stockRepository = Objects.requireNonNull(stockRepository, "Stock repository cannot be null");
        this.saveWatchlist = Objects.requireNonNull(saveWatchlist, "Save watchlist cannot be null");
        this.presenter = Objects.requireNonNull(presenter, "Presenter cannot be null");
    }

    @Override
    public void execute(RemoveTickerInputData inputData) {
        Objects.requireNonNull(inputData, "Input data cannot be null");

        final WatchlistInputSupport.Resolution resolution = WatchlistInputSupport.resolve(
                inputData.getRawSymbol(), watchlist,
                WatchlistInputSupport.Membership.MUST_BE_PRESENT);

        if (!resolution.isResolved()) {
            presenter.prepareFailView(resolution.getFailure());
            return;
        }

        final String symbol = resolution.getSymbol();

        watchlist.removeTicker(WatchlistInputSupport.lookupKey(symbol));
        stockRepository.remove(symbol);
        saveWatchlist.execute(watchlist);

        // Nothing is selected after a removal, so the price table clears.
        presenter.prepareSuccessView(new RemoveTickerOutputData(
                symbol,
                WatchlistSnapshotFactory.build(watchlist, stockRepository, "")));
    }
}
