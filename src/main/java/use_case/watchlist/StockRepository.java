package use_case.watchlist;

import entity.Stock;

import java.util.List;
import java.util.Optional;

/**
 * In-memory store of the price history behind each watchlist ticker.
 *
 * <p>This is the cross-feature hand-off surface: once a ticker has been added or
 * refreshed, the backtesting and strategy features read its {@link Stock} from here
 * and get a documented oldest-to-newest price list, with no knowledge of Alpha
 * Vantage required.
 *
 * <p><strong>Why prices are not stored on {@code WatchlistEntry}.</strong> The
 * watchlist is persisted by Java serialization, and price history is query data
 * rather than user data: it is re-fetched from the provider, not saved. Keeping it
 * here means the saved watchlist file stores only tickers, so its format - and the
 * persistence feature that owns it - is unaffected by market data entirely.
 *
 * <p>This is also distinct from any provider-side cache. A cache expires in order to
 * protect the request quota; this repository never expires, because a backtest must
 * be able to read a ticker's prices for as long as it stays on the watchlist.
 */
public interface StockRepository {

    /** Inserts or replaces the stock stored for its symbol. */
    void save(Stock stock);

    /**
     * @param normalizedSymbol an already-normalized symbol
     * @return the stored stock, or empty when the symbol has no price history yet
     */
    Optional<Stock> findBySymbol(String normalizedSymbol);

    /** Removes any stock stored for the symbol; a no-op when absent. */
    void remove(String normalizedSymbol);

    /** @return every stored stock, unmodifiable and sorted by symbol. */
    List<Stock> findAll();
}
