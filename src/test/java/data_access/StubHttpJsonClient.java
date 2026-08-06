package data_access;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A hand-written {@link HttpJsonClient} that returns canned bodies and records the
 * URLs it was asked for.
 *
 * <p>Bodies are keyed by a substring of the URL, so a test can answer the two Alpha
 * Vantage endpoints differently by matching on {@code function=...}. Recording the
 * URLs is what lets the tests assert which endpoints are actually being called.
 */
class StubHttpJsonClient implements HttpJsonClient {

    private final Map<String, String> bodiesByUrlFragment = new LinkedHashMap<>();
    private final List<String> requestedUrls = new ArrayList<>();
    private IOException failure;

    StubHttpJsonClient respondTo(String urlFragment, String body) {
        bodiesByUrlFragment.put(urlFragment, body);
        return this;
    }

    /** Makes every request fail, for exercising the network path. */
    StubHttpJsonClient failWith(IOException exception) {
        this.failure = exception;
        return this;
    }

    @Override
    public String get(String url) throws IOException {
        requestedUrls.add(url);

        if (failure != null) {
            throw failure;
        }

        for (Map.Entry<String, String> candidate : bodiesByUrlFragment.entrySet()) {
            if (url.contains(candidate.getKey())) {
                return candidate.getValue();
            }
        }

        throw new AssertionError("No stub response configured for URL: " + url);
    }

    List<String> getRequestedUrls() {
        return requestedUrls;
    }

    String getLastRequestedUrl() {
        return requestedUrls.get(requestedUrls.size() - 1);
    }
}
