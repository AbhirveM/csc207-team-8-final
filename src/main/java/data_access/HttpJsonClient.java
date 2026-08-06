package data_access;

import java.io.IOException;

/**
 * A minimal HTTP GET seam.
 *
 * <p>This exists so that {@code AlphaVantageMarketDataAccessObject} can be unit
 * tested without a network connection and without a mocking library: tests inject a
 * lambda that returns canned JSON and records the URL it was asked for. That lets
 * the tests assert both the parsing behaviour and the exact endpoints being called.
 *
 * <p>It stays inside {@code data_access} and must never appear in a use-case-layer
 * signature - it is a framework detail hidden behind {@link
 * use_case.watchlist.MarketDataGateway}.
 */
@FunctionalInterface
public interface HttpJsonClient {

    /**
     * Performs a GET request and returns the response body.
     *
     * @param url the fully-built request URL
     * @return the raw response body
     * @throws IOException if the host is unreachable, the request times out, or the
     *                     response status is not successful
     */
    String get(String url) throws IOException;
}
