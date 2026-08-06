package use_case.watchlist;

import java.util.Objects;

/**
 * A market-data provider failure, classified so the presenter can choose the
 * wording the user sees.
 *
 * <p>Deliberately a <em>checked</em> exception, and deliberately a use-case-layer
 * type rather than {@code IOException} or {@code org.json.JSONException}: making
 * it checked forces every call site to decide what the user is told, and keeping
 * it out of the framework layers stops HTTP and JSON details from leaking inward
 * past the {@link MarketDataGateway} port.
 *
 * <p>The technical detail is for logs and pull-request debugging. It must never be
 * shown to the user verbatim, and must never contain the API key - the request URL
 * carries the key, so implementations report only the endpoint name and symbol.
 */
public class MarketDataException extends Exception {

    private static final long serialVersionUID = 1L;

    /** The classes of failure the watchlist use cases must handle distinctly. */
    public enum Kind {
        /** The provider could not be reached, or the request timed out. */
        NETWORK,
        /** The provider's request quota has been exhausted. */
        RATE_LIMIT,
        /** The provider does not recognize the symbol. */
        INVALID_SYMBOL,
        /** The provider recognized the request but returned no price history. */
        EMPTY_RESPONSE,
        /** The response could not be parsed into price data. */
        MALFORMED_RESPONSE,
        /** No API key is configured, or the provider rejected the one supplied. */
        MISSING_API_KEY
    }

    private final Kind kind;
    private final String symbol;
    private final String technicalDetail;

    public MarketDataException(Kind kind, String symbol, String technicalDetail) {
        this(kind, symbol, technicalDetail, null);
    }

    public MarketDataException(Kind kind, String symbol, String technicalDetail, Throwable cause) {
        super(kind + " for symbol " + symbol + ": " + technicalDetail, cause);
        this.kind = Objects.requireNonNull(kind, "Kind cannot be null");
        this.symbol = symbol;
        this.technicalDetail = technicalDetail;
    }

    public Kind getKind() {
        return kind;
    }

    public String getSymbol() {
        return symbol;
    }

    /** @return diagnostic text for logs; never render this to the user. */
    public String getTechnicalDetail() {
        return technicalDetail;
    }
}
