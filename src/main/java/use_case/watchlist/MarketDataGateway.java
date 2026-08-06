package use_case.watchlist;

import entity.DailyPrice;

import java.util.List;
import java.util.Optional;

/**
 * The market-data port used by the watchlist use cases.
 *
 * <p>This interface is declared in the use-case layer and implemented in
 * {@code data_access} - the Dependency Inversion that keeps the Dependency Rule
 * intact. Interactors depend only on this interface, so they know nothing about
 * HTTP, JSON, Alpha Vantage, or API keys, and can be unit-tested against an
 * offline fake.
 */
public interface MarketDataGateway {

    /**
     * Returns the available daily price history for a symbol.
     *
     * @param normalizedSymbol an already-normalized symbol (trimmed, upper-cased);
     *                         callers should run {@link TickerSymbolValidator} first
     * @return the history ordered oldest to newest, never null and never containing
     *         nulls. May be served from a cache.
     * @throws MarketDataException if the provider is unreachable, over quota, does
     *                             not recognize the symbol, or returns unusable data
     */
    List<DailyPrice> fetchDailyPrices(String normalizedSymbol) throws MarketDataException;

    /**
     * Returns the available daily price history, forcing a provider round-trip.
     *
     * <p>This exists because freshness is a use-case concern: the Refresh Ticker use
     * case exists precisely to bypass any cached copy. The caching <em>mechanism</em>
     * stays hidden in the implementation - only a caching gateway needs to override
     * this, and non-caching implementations inherit the correct behaviour for free.
     *
     * @param normalizedSymbol an already-normalized symbol
     * @return the history ordered oldest to newest
     * @throws MarketDataException on any provider failure
     */
    default List<DailyPrice> fetchDailyPricesFresh(String normalizedSymbol)
            throws MarketDataException {
        return fetchDailyPrices(normalizedSymbol);
    }

    /**
     * Returns the company name for a symbol when the provider supplies one.
     *
     * <p>An absent name is expressed as an empty {@link Optional} rather than an
     * exception, because many valid symbols (notably ETFs) simply have no company
     * record. Callers should treat a missing name as cosmetic and continue.
     *
     * @param normalizedSymbol an already-normalized symbol
     * @return the company name, or empty when the provider has none
     * @throws MarketDataException on any provider failure
     */
    Optional<String> fetchCompanyName(String normalizedSymbol) throws MarketDataException;
}
