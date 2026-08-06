package use_case.watchlist;

import java.util.Objects;

/**
 * Why a watchlist operation could not be completed.
 *
 * <p>Interactors report a {@link Kind} and the symbol involved; the presenter turns
 * that into the sentence the user reads. Keeping the wording in the interface-adapter
 * layer means interactor tests assert stable enum values rather than English prose,
 * and a single presenter test pins down every user-facing string.
 */
public final class WatchlistFailure {

    /** Validation problems come first, then the provider failures. */
    public enum Kind {
        BLANK_INPUT,
        BAD_FORMAT,
        TOO_LONG,
        DUPLICATE,
        NOT_ON_WATCHLIST,
        NETWORK,
        RATE_LIMIT,
        INVALID_SYMBOL,
        EMPTY_RESPONSE,
        MALFORMED_RESPONSE,
        MISSING_API_KEY
    }

    private final Kind kind;
    private final String symbol;

    /**
     * @param kind   what went wrong
     * @param symbol the symbol involved. For validation failures this is the raw text
     *               the user typed, so the message can quote it back; otherwise it is
     *               the normalized symbol.
     */
    public WatchlistFailure(Kind kind, String symbol) {
        this.kind = Objects.requireNonNull(kind, "Kind cannot be null");
        this.symbol = symbol == null ? "" : symbol;
    }

    /** Translates a gateway failure into the matching watchlist failure. */
    public static WatchlistFailure from(MarketDataException exception) {
        final Kind kind = switch (exception.getKind()) {
            case NETWORK -> Kind.NETWORK;
            case RATE_LIMIT -> Kind.RATE_LIMIT;
            case INVALID_SYMBOL -> Kind.INVALID_SYMBOL;
            case EMPTY_RESPONSE -> Kind.EMPTY_RESPONSE;
            case MALFORMED_RESPONSE -> Kind.MALFORMED_RESPONSE;
            case MISSING_API_KEY -> Kind.MISSING_API_KEY;
        };
        return new WatchlistFailure(kind, exception.getSymbol());
    }

    /** Maps a validation reason onto the matching watchlist failure. */
    public static WatchlistFailure from(TickerSymbolValidator.Reason reason, String rawInput) {
        final Kind kind = switch (reason) {
            case BLANK -> Kind.BLANK_INPUT;
            case TOO_LONG -> Kind.TOO_LONG;
            case ILLEGAL_CHARACTERS -> Kind.BAD_FORMAT;
        };
        return new WatchlistFailure(kind, rawInput);
    }

    public Kind getKind() {
        return kind;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * Value equality, so a presenter test can assert against an expected failure and a
     * presenter can tell a repeated failure from a new one.
     *
     * @param other the object to compare with
     * @return whether {@code other} is a failure with the same kind and symbol
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WatchlistFailure)) {
            return false;
        }
        final WatchlistFailure that = (WatchlistFailure) other;
        return kind == that.kind && symbol.equals(that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, symbol);
    }

    /** @return a readable form, so a failing assertion names the kind and the symbol. */
    @Override
    public String toString() {
        return "WatchlistFailure{kind=" + kind + ", symbol='" + symbol + "'}";
    }
}
