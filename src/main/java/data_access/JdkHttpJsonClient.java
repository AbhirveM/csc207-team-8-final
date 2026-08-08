package data_access;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The real {@link HttpJsonClient}, backed by the JDK's {@code java.net.http.HttpClient}.
 *
 * <p>This is the only class in the project that performs network I/O.
 *
 * <p>Both the connect timeout and the per-request timeout are set explicitly. An
 * unbounded request would leave the application waiting indefinitely on an
 * unresponsive provider, which in a Swing application means a frozen window.
 */
public class JdkHttpJsonClient implements HttpJsonClient {

    /** Long enough for a slow provider, short enough that the UI recovers. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public JdkHttpJsonClient() {
        this(DEFAULT_TIMEOUT);
    }

    public JdkHttpJsonClient(Duration timeout) {
        this.requestTimeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String get(String url) throws IOException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();

        final HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (InterruptedException exception) {
            // Restore the flag so an interrupt is not silently swallowed.
            Thread.currentThread().interrupt();
            throw new IOException("Request was interrupted", exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // The URL carries the API key, so it is deliberately not included here.
            throw new IOException("Market data request failed with HTTP status "
                    + response.statusCode());
        }

        return response.body();
    }
}
