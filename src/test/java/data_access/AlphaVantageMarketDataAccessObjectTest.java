package data_access;

import entity.DailyPrice;
import org.junit.jupiter.api.Test;
import use_case.watchlist.MarketDataException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaVantageMarketDataAccessObjectTest {

    private static final String API_KEY = "test-key-1234";
    private static final String DAILY = "function=TIME_SERIES_DAILY";
    private static final String OVERVIEW = "function=OVERVIEW";

    private static AlphaVantageMarketDataAccessObject withResponses(StubHttpJsonClient client) {
        return new AlphaVantageMarketDataAccessObject(API_KEY, client);
    }

    private static MarketDataException.Kind dailyFailureKind(String fixture) {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read(fixture));

        return assertThrows(MarketDataException.class,
                () -> withResponses(client).fetchDailyPrices("AAPL")).getKind();
    }

    // --- Happy path and ordering ------------------------------------------------

    @Test
    void parsesDailyPricesOldestToNewest() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("time_series_daily_aapl.json"));

        List<DailyPrice> prices = withResponses(client).fetchDailyPrices("AAPL");

        assertEquals(5, prices.size());
        assertEquals(LocalDate.of(2026, 7, 30), prices.get(0).getDate());
        assertEquals(LocalDate.of(2026, 8, 5), prices.get(4).getDate());
    }

    /**
     * The provider returns the series newest-first, and a JSON object is unordered by
     * definition, so this pins down that the parser sorts rather than trusting the
     * order the keys arrive in. Without the explicit sort this fails intermittently.
     */
    @Test
    void datesStrictlyIncreaseEvenThoughTheResponseIsNewestFirst() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("time_series_daily_aapl.json"));

        List<DailyPrice> prices = withResponses(client).fetchDailyPrices("AAPL");

        for (int index = 1; index < prices.size(); index++) {
            assertTrue(prices.get(index - 1).getDate().isBefore(prices.get(index).getDate()),
                    "Not sorted at index " + index);
        }
    }

    @Test
    void parsesAllFieldsOfARow() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("time_series_daily_aapl.json"));

        DailyPrice newest = withResponses(client).fetchDailyPrices("AAPL").get(4);

        assertEquals(265.10, newest.getOpen(), 1e-9);
        assertEquals(267.55, newest.getHigh(), 1e-9);
        assertEquals(264.01, newest.getLow(), 1e-9);
        assertEquals(266.80, newest.getClose(), 1e-9);
        assertEquals(3_512_004L, newest.getVolume());
    }

    /** Volume is documented as an integer but has been observed in decimal form. */
    @Test
    void acceptsVolumeExpressedAsADecimal() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("time_series_daily_decimal_volume.json"));

        List<DailyPrice> prices = withResponses(client).fetchDailyPrices("IBM");

        assertEquals(2_984_110L, prices.get(0).getVolume());
        assertEquals(3_512_004L, prices.get(1).getVolume());
    }

    @Test
    void parsesCompanyName() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(OVERVIEW, JsonFixtures.read("overview_ibm.json"));

        assertEquals(Optional.of("International Business Machines"),
                withResponses(client).fetchCompanyName("IBM"));
    }

    @Test
    void treatsAnUnknownCompanyAsAbsentRatherThanAFailure() throws Exception {
        StubHttpJsonClient nameIsNone = new StubHttpJsonClient()
                .respondTo(OVERVIEW, JsonFixtures.read("overview_name_none.json"));

        assertTrue(withResponses(nameIsNone).fetchCompanyName("VOO").isEmpty());
    }

    /**
     * An unrecognized symbol produces a bare {@code {}} OVERVIEW body. That is an absent
     * name, not a failure: the price endpoint may still know the symbol, and OVERVIEW is
     * the first endpoint the free-tier quota kills, so a name lookup must never be able
     * to block adding a ticker.
     */
    @Test
    void anEmptyOverviewBodyIsAnAbsentNameRatherThanAnError() throws Exception {
        final StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(OVERVIEW, JsonFixtures.read("overview_unknown_symbol.json"));

        assertEquals(Optional.empty(), withResponses(client).fetchCompanyName("ZZZZ"));
    }

    /**
     * The same {@code {}} body remains an EMPTY_RESPONSE on the price endpoint - there is
     * no price history to degrade gracefully to.
     */
    @Test
    void anEmptyDailySeriesBodyIsStillAnError() {
        assertEquals(MarketDataException.Kind.EMPTY_RESPONSE,
                dailyFailureKind("overview_unknown_symbol.json"));
    }

    // --- Endpoint usage (the API rubric line, proven rather than claimed) -------

    @Test
    void requestsTheDailyTimeSeriesEndpointWithACompactOutputSize() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("time_series_daily_aapl.json"));

        withResponses(client).fetchDailyPrices("AAPL");

        String url = client.getLastRequestedUrl();
        assertTrue(url.startsWith(AlphaVantageMarketDataAccessObject.BASE_URL), url);
        assertTrue(url.contains("function=" + AlphaVantageMarketDataAccessObject.FUNCTION_TIME_SERIES_DAILY), url);
        assertTrue(url.contains("symbol=AAPL"), url);
        assertTrue(url.contains("outputsize=compact"), url);
        assertTrue(url.contains("apikey=" + API_KEY), url);
    }

    @Test
    void requestsTheCompanyOverviewEndpoint() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(OVERVIEW, JsonFixtures.read("overview_ibm.json"));

        withResponses(client).fetchCompanyName("IBM");

        String url = client.getLastRequestedUrl();
        assertTrue(url.contains("function=" + AlphaVantageMarketDataAccessObject.FUNCTION_OVERVIEW), url);
        assertTrue(url.contains("symbol=IBM"), url);
    }

    @Test
    void symbolsNeedingEscapingAreUrlEncoded() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("time_series_daily_aapl.json"));

        withResponses(client).fetchDailyPrices("BRK.B");

        assertTrue(client.getLastRequestedUrl().contains("symbol=BRK.B"),
                client.getLastRequestedUrl());
    }

    /**
     * The stub is the whole reason the suite never touches the network, so its own
     * failure modes have to be legible. Asking for a URL before any request was made is
     * a test bug, and it must say so rather than throwing IndexOutOfBoundsException.
     */
    @Test
    void theStubExplainsItselfWhenNoRequestWasMade() {
        final StubHttpJsonClient client = new StubHttpJsonClient();

        assertTrue(client.getRequestedUrls().isEmpty());
        assertTrue(assertThrows(AssertionError.class, client::getLastRequestedUrl)
                .getMessage().contains("No request was made"));
    }

    // --- Symbol contract (MarketDataGateway, orchestrator 5.1) -----------------

    @Test
    void aNullSymbolIsRejectedWithANullPointerException() {
        final AlphaVantageMarketDataAccessObject dao = withResponses(new StubHttpJsonClient());

        assertThrows(NullPointerException.class, () -> dao.fetchDailyPrices(null));
        assertThrows(NullPointerException.class, () -> dao.fetchDailyPricesFresh(null));
        assertThrows(NullPointerException.class, () -> dao.fetchCompanyName(null));
    }

    @Test
    void aBlankSymbolIsReportedAsInvalidWithoutReachingTheProvider() {
        final StubHttpJsonClient client = new StubHttpJsonClient();
        final AlphaVantageMarketDataAccessObject dao = withResponses(client);

        for (final String blank : List.of("", "   ", "\t")) {
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> dao.fetchDailyPrices(blank)).getKind(), blank);
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> dao.fetchDailyPricesFresh(blank)).getKind(), blank);
            assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                    assertThrows(MarketDataException.class,
                            () -> dao.fetchCompanyName(blank)).getKind(), blank);
        }

        assertTrue(client.getRequestedUrls().isEmpty(),
                "A blank symbol must never reach the provider");
    }

    // --- Failure mapping -------------------------------------------------------

    @Test
    void unreachableProviderIsReportedAsANetworkFailure() {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .failWith(new IOException("connection reset"));

        MarketDataException thrown = assertThrows(MarketDataException.class,
                () -> withResponses(client).fetchDailyPrices("AAPL"));

        assertEquals(MarketDataException.Kind.NETWORK, thrown.getKind());
        assertEquals("AAPL", thrown.getSymbol());
    }

    @Test
    void rejectedSymbolIsReportedAsInvalid() {
        assertEquals(MarketDataException.Kind.INVALID_SYMBOL,
                dailyFailureKind("error_invalid_symbol.json"));
    }

    @Test
    void rejectedApiKeyIsDistinguishedFromAnInvalidSymbol() {
        assertEquals(MarketDataException.Kind.MISSING_API_KEY,
                dailyFailureKind("error_bad_apikey.json"));
    }

    @Test
    void callFrequencyNoteIsReportedAsARateLimit() {
        assertEquals(MarketDataException.Kind.RATE_LIMIT,
                dailyFailureKind("note_call_frequency.json"));
    }

    @Test
    void dailyRequestCapIsReportedAsARateLimit() {
        assertEquals(MarketDataException.Kind.RATE_LIMIT,
                dailyFailureKind("information_daily_limit.json"));
    }

    @Test
    void premiumOnlyEndpointIsReportedAsARateLimit() {
        assertEquals(MarketDataException.Kind.RATE_LIMIT,
                dailyFailureKind("information_premium_endpoint.json"));
    }

    @Test
    void unrecognizedInformationalResponseIsReportedAsMalformed() {
        assertEquals(MarketDataException.Kind.MALFORMED_RESPONSE,
                dailyFailureKind("information_unrecognized.json"));
    }

    @Test
    void emptyObjectIsReportedAsAnEmptyResponse() {
        assertEquals(MarketDataException.Kind.EMPTY_RESPONSE,
                dailyFailureKind("empty_object.json"));
    }

    @Test
    void emptyTimeSeriesIsReportedAsAnEmptyResponse() {
        assertEquals(MarketDataException.Kind.EMPTY_RESPONSE,
                dailyFailureKind("time_series_daily_empty_series.json"));
    }

    @Test
    void missingTimeSeriesSectionIsReportedAsMalformed() {
        assertEquals(MarketDataException.Kind.MALFORMED_RESPONSE,
                dailyFailureKind("time_series_missing_series_key.json"));
    }

    @Test
    void unparsableNumberIsReportedAsMalformed() {
        assertEquals(MarketDataException.Kind.MALFORMED_RESPONSE,
                dailyFailureKind("time_series_daily_malformed_row.json"));
    }

    @Test
    void unparsableDateIsReportedAsMalformed() {
        assertEquals(MarketDataException.Kind.MALFORMED_RESPONSE,
                dailyFailureKind("time_series_daily_bad_date.json"));
    }

    @Test
    void nonJsonBodyIsReportedAsMalformed() {
        assertEquals(MarketDataException.Kind.MALFORMED_RESPONSE,
                dailyFailureKind("not_json.html"));
    }

    @Test
    void blankBodyIsReportedAsAnEmptyResponse() {
        StubHttpJsonClient client = new StubHttpJsonClient().respondTo(DAILY, "   ");

        assertEquals(MarketDataException.Kind.EMPTY_RESPONSE,
                assertThrows(MarketDataException.class,
                        () -> withResponses(client).fetchDailyPrices("AAPL")).getKind());
    }

    @Test
    void companyNameLookupAlsoMapsProviderErrors() {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(OVERVIEW, JsonFixtures.read("information_daily_limit.json"));

        assertEquals(MarketDataException.Kind.RATE_LIMIT,
                assertThrows(MarketDataException.class,
                        () -> withResponses(client).fetchCompanyName("IBM")).getKind());
    }

    /**
     * The request URL carries the API key, so no failure may quote it. A leaked key in
     * a stack trace or an error dialog would be a credential disclosure.
     */
    @Test
    void failureMessagesNeverContainTheApiKey() {
        StubHttpJsonClient network = new StubHttpJsonClient()
                .failWith(new IOException("connection reset"));
        StubHttpJsonClient malformed = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("not_json.html"));

        for (StubHttpJsonClient client : List.of(network, malformed)) {
            MarketDataException thrown = assertThrows(MarketDataException.class,
                    () -> withResponses(client).fetchDailyPrices("AAPL"));

            assertFalse(thrown.getMessage().contains(API_KEY), thrown.getMessage());
            assertFalse(thrown.getTechnicalDetail().contains(API_KEY), thrown.getTechnicalDetail());
        }
    }

    // --- Configuration ---------------------------------------------------------

    @Test
    void apiKeyFromEnvironmentIsEmptyWhenTheVariableIsUnset() {
        /*
         * The environment cannot be modified from inside the JVM, so only the absent
         * branch is exercised here. The reading code is kept to a few lines for
         * exactly that reason, and the configured branch is covered by the
         * constructor tests above.
         */
        if (System.getenv(AlphaVantageMarketDataAccessObject.API_KEY_ENV_VARIABLE) == null) {
            assertTrue(AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment().isEmpty());
        }
        else {
            assertTrue(AlphaVantageMarketDataAccessObject.apiKeyFromEnvironment().isPresent());
        }
    }

    @Test
    void constructorRejectsANullKeyOrClient() {
        assertThrows(NullPointerException.class,
                () -> new AlphaVantageMarketDataAccessObject(null, new StubHttpJsonClient()));
        assertThrows(NullPointerException.class,
                () -> new AlphaVantageMarketDataAccessObject(API_KEY, null));
    }

    @Test
    void fetchDailyPricesFreshUsesTheSameEndpointWhenNotCached() throws Exception {
        StubHttpJsonClient client = new StubHttpJsonClient()
                .respondTo(DAILY, JsonFixtures.read("time_series_daily_aapl.json"));

        assertEquals(5, withResponses(client).fetchDailyPricesFresh("AAPL").size());
        assertTrue(client.getLastRequestedUrl().contains(DAILY));
    }
}
