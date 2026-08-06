package use_case.watchlist;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes and validates raw ticker input typed by the user.
 *
 * <p>Normalization is "trim, drop internal whitespace, upper-case", so
 * {@code "  aapl "} becomes {@code "AAPL"}. Running this before constructing an
 * {@link entity.Ticker} is what guarantees the symbol is never null or blank -
 * {@code Ticker.hashCode()} would otherwise throw on a null symbol.
 *
 * <p>Pure and static by design: no collaborators, no state, so it is exhaustively
 * testable with a parameterized test.
 */
public final class TickerSymbolValidator {

    /** Alpha Vantage symbols comfortably fit within this length. */
    public static final int MAX_LENGTH = 10;

    /** Dots and hyphens are allowed so class shares such as BRK.B and RY-A work. */
    private static final Pattern ALLOWED = Pattern.compile("[A-Z0-9.\\-]+");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /** Why a raw input was rejected. */
    public enum Reason {
        /** Nothing was typed, or only whitespace. */
        BLANK,
        /** Longer than {@link #MAX_LENGTH} characters. */
        TOO_LONG,
        /** Contains characters that cannot appear in a ticker symbol. */
        ILLEGAL_CHARACTERS
    }

    /** The outcome of validation: either a normalized symbol or a reason it failed. */
    public static final class Result {
        private final String symbol;
        private final Reason reason;

        private Result(String symbol, Reason reason) {
            this.symbol = symbol;
            this.reason = reason;
        }

        public static Result valid(String symbol) {
            return new Result(symbol, null);
        }

        public static Result invalid(Reason reason) {
            return new Result(null, reason);
        }

        public boolean isValid() {
            return reason == null;
        }

        /** @return the normalized symbol; only meaningful when {@link #isValid()}. */
        public String getSymbol() {
            return symbol;
        }

        /** @return why validation failed; null when {@link #isValid()}. */
        public Reason getReason() {
            return reason;
        }
    }

    private TickerSymbolValidator() {
    }

    /**
     * Normalizes and validates a raw ticker symbol.
     *
     * @param rawInput whatever the user typed; may be null
     * @return a valid result carrying the normalized symbol, or an invalid result
     *         carrying the reason
     */
    public static Result validate(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Result.invalid(Reason.BLANK);
        }

        final String normalized = WHITESPACE.matcher(rawInput.strip())
                .replaceAll("")
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return Result.invalid(Reason.BLANK);
        }
        /*
         * The character set is checked before the length, so that a long run of
         * illegal characters reports ILLEGAL_CHARACTERS - the more specific and more
         * actionable problem - rather than TOO_LONG. Swapping these two checks would
         * silently change which Reason such an input reports.
         */
        if (!ALLOWED.matcher(normalized).matches()) {
            return Result.invalid(Reason.ILLEGAL_CHARACTERS);
        }
        if (normalized.length() > MAX_LENGTH) {
            return Result.invalid(Reason.TOO_LONG);
        }

        return Result.valid(normalized);
    }
}
