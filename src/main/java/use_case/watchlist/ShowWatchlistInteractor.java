package use_case.watchlist;

import java.util.Objects;

import entity.Watchlist;

/**
 * Re-emits the watchlist with a selection applied, without touching the provider.
 *
 * <p>This use case exists so that two things are possible without an adapter reaching
 * into the use-case layer: repopulating the price table when the user clicks a
 * different row, and rendering a watchlist that Load Watchlist has just restored from
 * disk. The alternative - making {@link WatchlistSnapshotFactory} public and calling it
 * from a controller - would be exactly the Dependency Rule violation this design is
 * meant to have zero of.
 *
 * <p>It performs <strong>no I/O</strong>: no {@link MarketDataGateway} call, so
 * clicking a row costs nothing against the daily request quota, and no
 * {@code SaveWatchlist} call, because selecting a row changes no membership. That is
 * why its constructor takes neither collaborator - the asymmetry with the other three
 * interactors is deliberate and enforced by the constructor's arity.
 *
 * <p>A selected symbol that is not on the watchlist is not an error. It degrades
 * silently to "nothing selected", because the only ways to reach that state - a stale
 * table selection, or a symbol removed between the click and the render - are both
 * ordinary and both self-correcting.
 */
public final class ShowWatchlistInteractor implements ShowWatchlistInputBoundary {

    private final Watchlist watchlist;
    private final StockRepository stockRepository;
    private final ShowWatchlistOutputBoundary presenter;

    /**
     * @param watchlist       the watchlist to render; must be non-null
     * @param stockRepository where price history is read from; must be non-null
     * @param presenter       the output boundary to report through; must be non-null
     * @throws NullPointerException if any argument is null
     */
    public ShowWatchlistInteractor(Watchlist watchlist,
                                   StockRepository stockRepository,
                                   ShowWatchlistOutputBoundary presenter) {
        this.watchlist = Objects.requireNonNull(watchlist, "Watchlist cannot be null");
        this.stockRepository = Objects.requireNonNull(stockRepository, "Stock repository cannot be null");
        this.presenter = Objects.requireNonNull(presenter, "Presenter cannot be null");
    }

    @Override
    public void execute(ShowWatchlistInputData inputData) {
        Objects.requireNonNull(inputData, "Input data cannot be null");

        final String selectedSymbol = resolveSelection(inputData.getSelectedSymbol());

        presenter.prepareSuccessView(new ShowWatchlistOutputData(
                watchlist.getEntries().size(),
                WatchlistSnapshotFactory.build(watchlist, stockRepository, selectedSymbol,
                        inputData.getChartPeriod())));
    }

    /**
     * Normalizes a requested selection down to something the watchlist actually holds.
     *
     * <p>The symbol is normalized rather than trusted, so a selection arriving from
     * anywhere other than the table - a restored session, a test, a future keyboard
     * shortcut - behaves the same as one that came from a click.
     *
     * @param requestedSymbol the symbol the caller asked to select; never null
     * @return the normalized symbol when it is on the watchlist, otherwise {@code ""}
     */
    private String resolveSelection(String requestedSymbol) {
        final TickerSymbolValidator.Result validation =
                TickerSymbolValidator.validate(requestedSymbol);

        if (!validation.isValid()) {
            return "";
        }

        final String symbol = validation.getSymbol();
        if (!watchlist.contains(WatchlistInputSupport.lookupKey(symbol))) {
            return "";
        }

        return symbol;
    }
}
