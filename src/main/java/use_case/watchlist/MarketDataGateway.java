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
 *
 * <h2>Symbol contract</h2>
 *
 * <p>Every method below takes a {@code normalizedSymbol} and is bound by the same
 * normative rules, which all implementations must satisfy identically:
 *
 * <ul>
 *   <li>{@code normalizedSymbol} must be non-null. Implementations reject {@code null}
 *       with {@link NullPointerException}, via
 *       {@code Objects.requireNonNull(normalizedSymbol, "Symbol cannot be null")}. A
 *       null symbol is a programming error, not a user error: callers run
 *       {@link TickerSymbolValidator} first.</li>
 *   <li>A <em>blank</em> symbol (empty or whitespace-only) must be rejected with a
 *       {@link MarketDataException} of kind
 *       {@link MarketDataException.Kind#INVALID_SYMBOL}, must not reach the network,
 *       and must never be cached.</li>
 * </ul>
 */
public interface MarketDataGateway {

    /**
     * Returns the available daily price history for a symbol.
     *
     * @param normalizedSymbol an already-normalized symbol (trimmed, upper-cased);
     *                         callers should run {@link TickerSymbolValidator} first.
     *                         Must be non-null; must not be blank.
     * @return the history ordered oldest to newest, never null and never containing
     *         nulls. May be served from a cache.
     * @throws NullPointerException if {@code normalizedSymbol} is null
     * @throws MarketDataException if {@code normalizedSymbol} is blank (kind
     *                             {@code INVALID_SYMBOL}, without reaching the network
     *                             and without caching), or if the provider is
     *                             unreachable, over quota, does not recognize the
     *                             symbol, or returns unusable data
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
     * @param normalizedSymbol an already-normalized symbol; must be non-null and must
     *                         not be blank
     * @return the history ordered oldest to newest
     * @throws NullPointerException if {@code normalizedSymbol} is null
     * @throws MarketDataException if {@code normalizedSymbol} is blank (kind
     *                             {@code INVALID_SYMBOL}, without reaching the network
     *                             and without caching), or on any provider failure
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
     * @param normalizedSymbol an already-normalized symbol; must be non-null and must
     *                         not be blank
     * @return the company name, or empty when the provider has none
     * @throws NullPointerException if {@code normalizedSymbol} is null
     * @throws MarketDataException if {@code normalizedSymbol} is blank (kind
     *                             {@code INVALID_SYMBOL}, without reaching the network
     *                             and without caching), or on any provider failure
     */
    Optional<String> fetchCompanyName(String normalizedSymbol) throws MarketDataException;
}
