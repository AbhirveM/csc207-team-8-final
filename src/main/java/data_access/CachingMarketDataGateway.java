package data_access;

import entity.DailyPrice;
import use_case.watchlist.MarketDataException;
import use_case.watchlist.MarketDataGateway;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Wraps any {@link MarketDataGateway} with a short-lived cache.
 *
 * <p>This exists because the free Alpha Vantage plan allows only a small number of
 * requests per day, which a user clicking around a watchlist would exhaust quickly.
 *
 * <p>It is a decorator rather than logic inside the data access object for three
 * reasons: the data access object keeps a single responsibility, so its parsing tests
 * stay readable; the cache can be tested deterministically with an injected
 * {@link Clock} and a counting fake, with no HTTP involved and no sleeping; and it
 * wraps the offline fake just as happily as the real provider, so caching becomes a
 * wiring decision rather than behaviour baked into every implementation.
 *
 * <p>Failures are deliberately not cached, so a transient network problem does not
 * lock the user out until the entry expires.
 */
public class CachingMarketDataGateway implements MarketDataGateway {

    /** Long enough to absorb repeated clicks, short enough that data stays current. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final MarketDataGateway delegate;
    private final Duration timeToLive;
    private final Clock clock;

    private final Map<String, CacheEntry> priceCache = new HashMap<>();
    /** Company names are cached without expiry: they effectively never change. */
    private final Map<String, Optional<String>> companyNameCache = new HashMap<>();

    private record CacheEntry(List<DailyPrice> prices, Instant storedAt) {
    }

    public CachingMarketDataGateway(MarketDataGateway delegate) {
        this(delegate, DEFAULT_TTL, Clock.systemUTC());
    }

    public CachingMarketDataGateway(MarketDataGateway delegate, Duration timeToLive, Clock clock) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.timeToLive = Objects.requireNonNull(timeToLive, "Time to live cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    @Override
    public List<DailyPrice> fetchDailyPrices(String normalizedSymbol) throws MarketDataException {
        final String key = key(normalizedSymbol);
        final CacheEntry cached = priceCache.get(key);

        if (cached != null && !isExpired(cached)) {
            return cached.prices();
        }

        return storeAndReturn(key, delegate.fetchDailyPrices(normalizedSymbol));
    }

    @Override
    public List<DailyPrice> fetchDailyPricesFresh(String normalizedSymbol) throws MarketDataException {
        // Always goes to the delegate; the refreshed value replaces any cached copy.
        return storeAndReturn(key(normalizedSymbol), delegate.fetchDailyPricesFresh(normalizedSymbol));
    }

    @Override
    public Optional<String> fetchCompanyName(String normalizedSymbol) throws MarketDataException {
        final String key = key(normalizedSymbol);
        final Optional<String> cached = companyNameCache.get(key);

        if (cached != null) {
            return cached;
        }

        final Optional<String> fetched = delegate.fetchCompanyName(normalizedSymbol);
        companyNameCache.put(key, fetched);
        return fetched;
    }

    /** Empties both caches, forcing the next read to hit the provider. */
    public void clear() {
        priceCache.clear();
        companyNameCache.clear();
    }

    public int getCachedSymbolCount() {
        return priceCache.size();
    }

    private List<DailyPrice> storeAndReturn(String key, List<DailyPrice> prices) {
        priceCache.put(key, new CacheEntry(prices, clock.instant()));
        return prices;
    }

    private boolean isExpired(CacheEntry entry) {
        return Duration.between(entry.storedAt(), clock.instant()).compareTo(timeToLive) >= 0;
    }

    private static String key(String symbol) {
        return symbol == null ? "" : symbol.toUpperCase(Locale.ROOT);
    }
}
