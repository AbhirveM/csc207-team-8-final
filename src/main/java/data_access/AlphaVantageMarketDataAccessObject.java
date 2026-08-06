package data_access;

import entity.DailyPrice;
import org.json.JSONException;
import org.json.JSONObject;
import use_case.watchlist.MarketDataException;
import use_case.watchlist.MarketDataGateway;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads market data from the Alpha Vantage REST API.
 *
 * <p>Two endpoints are used:
 * <ul>
 *   <li>{@value #FUNCTION_TIME_SERIES_DAILY} - daily open, high, low, close and
 *       volume records.</li>
 *   <li>{@value #FUNCTION_OVERVIEW} - the company name for a symbol.</li>
 * </ul>
 *
 * <p>Indicators are computed locally by the strategy classes, so no indicator
 * endpoint is called.
 *
 * <p><strong>Free-tier limits.</strong> {@code outputsize=compact} is requested
 * because the full history is a premium feature; compact returns roughly the latest
 * 100 trading days. The free plan also allows only a limited number of requests per
 * day, which is why {@link CachingMarketDataGateway} wraps this class in production
 * and why every test runs against canned responses instead of the network.
 *
 * <p><strong>API key handling.</strong> The key is supplied to the constructor and
 * read from the environment only by {@link #apiKeyFromEnvironment()}. It is never
 * committed, never logged, and never included in an exception message - the request
 * URL contains it, so failures report only the endpoint name and symbol.
 */
public class AlphaVantageMarketDataAccessObject implements MarketDataGateway {

    public static final String BASE_URL = "https://www.alphavantage.co/query";
    public static final String FUNCTION_TIME_SERIES_DAILY = "TIME_SERIES_DAILY";
    public static final String FUNCTION_OVERVIEW = "OVERVIEW";
    public static final String API_KEY_ENV_VARIABLE = "ALPHA_VANTAGE_API_KEY";

    private static final String TIME_SERIES_KEY = "Time Series (Daily)";
    private static final String COMPANY_NAME_KEY = "Name";

    private final String apiKey;
    private final HttpJsonClient httpClient;

    /** @return the configured API key, or empty when the variable is unset or blank. */
    public static Optional<String> apiKeyFromEnvironment() {
        final String key = System.getenv(API_KEY_ENV_VARIABLE);
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(key.strip());
    }

    /**
     * Builds a data access object that talks to the real provider over HTTPS.
     *
     * <p>This is the constructor the composition root uses, paired with
     * {@link #apiKeyFromEnvironment()}.
     *
     * @param apiKey the provider key; must be non-null and non-blank
     * @throws NullPointerException     if {@code apiKey} is null
     * @throws IllegalArgumentException if {@code apiKey} is blank
     */
    public AlphaVantageMarketDataAccessObject(String apiKey) {
        this(apiKey, new JdkHttpJsonClient());
    }

    /**
     * Constructor used by tests to supply canned responses instead of real HTTP.
     *
     * <p>A blank key is rejected here rather than sent: the provider would answer with
     * an "Error Message" that this class maps to {@code MISSING_API_KEY}, which is a
     * confusing round-trip for a fault that is entirely local and costs quota to learn.
     *
     * @param apiKey     the provider key; must be non-null and non-blank
     * @param httpClient the transport to use
     * @throws NullPointerException     if either argument is null
     * @throws IllegalArgumentException if {@code apiKey} is blank
     */
    AlphaVantageMarketDataAccessObject(String apiKey, HttpJsonClient httpClient) {
        this.apiKey = Objects.requireNonNull(apiKey, "API key cannot be null");
        this.httpClient = Objects.requireNonNull(httpClient, "HTTP client cannot be null");

        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be blank");
        }
    }

    /**
     * Enforces the {@link MarketDataGateway} symbol contract before any request is built.
     *
     * <p>Rejecting here rather than letting {@code URLEncoder} throw is what makes this
     * implementation agree with the other two: a null symbol is a programming error and
     * a blank symbol is a user error, and neither may reach the provider or spend quota.
     *
     * @param normalizedSymbol the symbol to check
     * @throws NullPointerException if {@code normalizedSymbol} is null
     * @throws MarketDataException of kind {@code INVALID_SYMBOL} if it is blank
     */
    private static void requireUsableSymbol(String normalizedSymbol) throws MarketDataException {
        Objects.requireNonNull(normalizedSymbol, "Symbol cannot be null");
        if (normalizedSymbol.isBlank()) {
            throw new MarketDataException(MarketDataException.Kind.INVALID_SYMBOL,
                    normalizedSymbol, "A blank symbol cannot be requested from the provider");
        }
    }

    @Override
    public List<DailyPrice> fetchDailyPrices(String normalizedSymbol) throws MarketDataException {
        requireUsableSymbol(normalizedSymbol);

        final String url = BASE_URL
                + "?function=" + FUNCTION_TIME_SERIES_DAILY
                + "&symbol=" + encode(normalizedSymbol)
                + "&outputsize=compact"
                + "&apikey=" + encode(apiKey);

        return parseDailyPrices(normalizedSymbol, request(normalizedSymbol, url));
    }

    @Override
    public Optional<String> fetchCompanyName(String normalizedSymbol) throws MarketDataException {
        requireUsableSymbol(normalizedSymbol);

        final String url = BASE_URL
                + "?function=" + FUNCTION_OVERVIEW
                + "&symbol=" + encode(normalizedSymbol)
                + "&apikey=" + encode(apiKey);

        return parseCompanyName(normalizedSymbol, request(normalizedSymbol, url));
    }

    private String request(String symbol, String url) throws MarketDataException {
        try {
            return httpClient.get(url);
        }
        catch (IOException e) {
            // The URL is omitted on purpose: it carries the API key.
            throw new MarketDataException(MarketDataException.Kind.NETWORK, symbol,
                    "Could not reach the market data provider", e);
        }
    }

    /**
     * Converts a daily time-series response into price entities.
     *
     * <p>The response keys the series by date string, newest first, and a JSON object
     * is unordered by definition - so the parsed list is sorted explicitly. Relying on
     * the order the keys happen to arrive in would produce a price list that violates
     * the oldest-to-newest contract intermittently rather than reliably.
     *
     * <p>The result is <strong>unmodifiable</strong>. {@link CachingMarketDataGateway}
     * stores exactly the list it is handed and returns that same reference on every hit,
     * so a caller that sorted or cleared a live {@code ArrayList} would silently corrupt
     * the cache for every subsequent caller.
     *
     * @param symbol the symbol being parsed, for error reporting
     * @param json   the raw response body
     * @return the prices, oldest to newest; unmodifiable
     * @throws MarketDataException if the response reports an error or cannot be parsed
     */
    static List<DailyPrice> parseDailyPrices(String symbol, String json) throws MarketDataException {
        final JSONObject root = readRoot(symbol, json);
        rejectProviderError(symbol, root);

        if (!root.has(TIME_SERIES_KEY)) {
            throw new MarketDataException(MarketDataException.Kind.MALFORMED_RESPONSE, symbol,
                    "Response did not contain a '" + TIME_SERIES_KEY + "' section");
        }

        final List<DailyPrice> prices = new ArrayList<>();
        try {
            final JSONObject series = root.getJSONObject(TIME_SERIES_KEY);
            final Iterator<String> dates = series.keys();

            while (dates.hasNext()) {
                final String date = dates.next();
                final JSONObject row = series.getJSONObject(date);
                prices.add(new DailyPrice(
                        LocalDate.parse(date),
                        readDouble(row, "1. open"),
                        readDouble(row, "2. high"),
                        readDouble(row, "3. low"),
                        readDouble(row, "4. close"),
                        readVolume(row)));
            }
        }
        catch (JSONException | DateTimeParseException | NumberFormatException e) {
            // org.json throws unchecked, so it is converted here rather than being
            // allowed to escape the gateway and reach the interactor.
            throw new MarketDataException(MarketDataException.Kind.MALFORMED_RESPONSE, symbol,
                    "Could not parse the daily price rows", e);
        }

        if (prices.isEmpty()) {
            throw new MarketDataException(MarketDataException.Kind.EMPTY_RESPONSE, symbol,
                    "The provider returned no daily price rows");
        }

        prices.sort(Comparator.comparing(DailyPrice::getDate));
        return List.copyOf(prices);
    }

    /**
     * Extracts the company name from a company-overview response.
     *
     * <p>An unknown symbol yields {@code {}} here, which is reported as an absent name
     * rather than an error: a symbol can be perfectly valid for price data and still
     * have no company record.
     *
     * <p>That is why the empty root is checked <em>before</em>
     * {@link #rejectProviderError(String, JSONObject)}, whose own empty-root check would
     * otherwise turn it into {@code EMPTY_RESPONSE}. A missing company name is cosmetic
     * and must never block adding a ticker - and {@code OVERVIEW} is the first endpoint
     * the free-tier quota kills, so this path is common rather than exotic.
     *
     * @param symbol the symbol being parsed, for error reporting
     * @param json   the raw response body
     * @return the company name, or empty when the provider has none
     * @throws MarketDataException if the response reports a genuine provider error
     */
    static Optional<String> parseCompanyName(String symbol, String json) throws MarketDataException {
        final JSONObject root = readRoot(symbol, json);
        if (root.isEmpty()) {
            return Optional.empty();
        }
        rejectProviderError(symbol, root);

        final String name = root.optString(COMPANY_NAME_KEY, "").strip();
        if (name.isEmpty() || "None".equalsIgnoreCase(name)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    /**
     * Fails fast on the provider's various error shapes.
     *
     * <p>All of them arrive with a successful HTTP status, so they have to be detected
     * from the body, and they must be checked before looking for the payload.
     */
    static void rejectProviderError(String symbol, JSONObject root) throws MarketDataException {
        if (root.isEmpty()) {
            throw new MarketDataException(MarketDataException.Kind.EMPTY_RESPONSE, symbol,
                    "The provider returned an empty response");
        }

        if (root.has("Error Message")) {
            final String message = root.optString("Error Message", "");
            if (message.toLowerCase(Locale.ROOT).contains("apikey")) {
                throw new MarketDataException(MarketDataException.Kind.MISSING_API_KEY, symbol,
                        "The provider rejected the API key");
            }
            throw new MarketDataException(MarketDataException.Kind.INVALID_SYMBOL, symbol,
                    "The provider rejected the request for this symbol");
        }

        // "Note" is the throttling message; "Information" covers both the daily
        // request cap and premium-only endpoints. Both mean "come back later or
        // upgrade", so they share one classification.
        if (root.has("Note")) {
            throw new MarketDataException(MarketDataException.Kind.RATE_LIMIT, symbol,
                    "The provider reported a call-frequency limit");
        }

        if (root.has("Information")) {
            final String information = root.optString("Information", "").toLowerCase(Locale.ROOT);
            if (information.contains("rate limit") || information.contains("premium")
                    || information.contains("requests per day")) {
                throw new MarketDataException(MarketDataException.Kind.RATE_LIMIT, symbol,
                        "The provider reported a plan or rate limit");
            }
            throw new MarketDataException(MarketDataException.Kind.MALFORMED_RESPONSE, symbol,
                    "The provider returned an unrecognized informational response");
        }
    }

    private static JSONObject readRoot(String symbol, String json) throws MarketDataException {
        if (json == null || json.isBlank()) {
            throw new MarketDataException(MarketDataException.Kind.EMPTY_RESPONSE, symbol,
                    "The provider returned an empty body");
        }
        try {
            return new JSONObject(json);
        }
        catch (JSONException e) {
            // A non-JSON body usually means an HTML error or maintenance page.
            throw new MarketDataException(MarketDataException.Kind.MALFORMED_RESPONSE, symbol,
                    "The response was not valid JSON", e);
        }
    }

    private static double readDouble(JSONObject row, String key) {
        return Double.parseDouble(row.getString(key));
    }

    /**
     * Reads volume defensively.
     *
     * <p>The field is documented as an integer but has been observed arriving in
     * decimal form, which {@code Long.parseLong} would reject outright.
     */
    private static long readVolume(JSONObject row) {
        return (long) Double.parseDouble(row.getString("5. volume"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
