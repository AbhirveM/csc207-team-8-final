package use_case.watchlist;

import java.util.Objects;

import entity.Ticker;
import entity.Watchlist;

/**
 * The validate - normalize - check-membership preamble shared by the watchlist
 * interactors.
 *
 * <p>All three mutating use cases open the same way: normalize the raw symbol, reject
 * it if it is not a usable ticker, then confirm the symbol is (or is not) already on
 * the watchlist. Keeping that sequence in one place is what stops the three interactors
 * drifting apart on which {@link WatchlistFailure.Kind} a given bad input reports.
 *
 * <p>Package-private and stateless by design: this is an implementation detail of the
 * use-case package, not part of any boundary.
 */
final class WatchlistInputSupport {

    /** What the caller requires of the symbol's membership before it will proceed. */
    enum Membership {
        /** Add: the symbol must not already be on the watchlist. */
        MUST_BE_ABSENT,
        /** Remove and refresh: the symbol must already be on the watchlist. */
        MUST_BE_PRESENT
    }

    /**
     * Either a normalized symbol the caller may act on, or the failure to report.
     *
     * <p>Exactly one of the two is present, which is why the caller checks
     * {@link #isResolved()} before reading either.
     */
    static final class Resolution {

        private final String symbol;
        private final WatchlistFailure failure;

        private Resolution(String symbol, WatchlistFailure failure) {
            this.symbol = symbol;
            this.failure = failure;
        }

        static Resolution resolved(String symbol) {
            return new Resolution(symbol, null);
        }

        static Resolution failed(WatchlistFailure failure) {
            return new Resolution(null, failure);
        }

        /** @return whether the caller may proceed with {@link #getSymbol()}. */
        boolean isResolved() {
            return failure == null;
        }

        /** @return the normalized symbol; only meaningful when {@link #isResolved()}. */
        String getSymbol() {
            return symbol;
        }

        /** @return what to report; null when {@link #isResolved()}. */
        WatchlistFailure getFailure() {
            return failure;
        }
    }

    private WatchlistInputSupport() {
    }

    /**
     * Normalizes a raw symbol and checks it against the watchlist.
     *
     * @param rawSymbol the symbol exactly as the user typed it; may be null
     * @param watchlist the watchlist to check membership against
     * @param required  whether the symbol must already be present or must be absent
     * @return a resolution carrying either the normalized symbol or the failure to
     *         report through {@code prepareFailView}
     * @throws NullPointerException if {@code watchlist} or {@code required} is null
     */
    static Resolution resolve(String rawSymbol, Watchlist watchlist, Membership required) {
        Objects.requireNonNull(watchlist, "Watchlist cannot be null");
        Objects.requireNonNull(required, "Membership requirement cannot be null");

        final TickerSymbolValidator.Result validation = TickerSymbolValidator.validate(rawSymbol);
        if (!validation.isValid()) {
            // The raw text is carried, not the normalized symbol, so the presenter can
            // quote back exactly what the user typed.
            return Resolution.failed(WatchlistFailure.from(validation.getReason(), rawSymbol));
        }

        final String symbol = validation.getSymbol();
        final boolean present = watchlist.contains(lookupKey(symbol));

        if (required == Membership.MUST_BE_ABSENT && present) {
            return Resolution.failed(
                    new WatchlistFailure(WatchlistFailure.Kind.DUPLICATE, symbol));
        }
        if (required == Membership.MUST_BE_PRESENT && !present) {
            return Resolution.failed(
                    new WatchlistFailure(WatchlistFailure.Kind.NOT_ON_WATCHLIST, symbol));
        }

        return Resolution.resolved(symbol);
    }

    /**
     * Builds the key used to look a symbol up in a {@link Watchlist}.
     *
     * <p>{@code Ticker.equals} compares the symbol case-insensitively and ignores the
     * company name, so a null name is correct for a lookup and never overwrites a name
     * already stored on the watchlist.
     *
     * @param normalizedSymbol an already-normalized symbol
     * @return a ticker usable only as a lookup key
     */
    static Ticker lookupKey(String normalizedSymbol) {
        return new Ticker(normalizedSymbol, null);
    }
}
