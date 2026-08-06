package use_case.watchlist;

/**
 * Input for the Add Ticker use case.
 *
 * <p>Carries the symbol exactly as typed. Normalization is the interactor's job, not
 * the view's, so the same rules apply however the use case is invoked.
 */
public final class AddTickerInputData {

    private final String rawSymbol;

    public AddTickerInputData(String rawSymbol) {
        this.rawSymbol = rawSymbol;
    }

    public String getRawSymbol() {
        return rawSymbol;
    }
}
