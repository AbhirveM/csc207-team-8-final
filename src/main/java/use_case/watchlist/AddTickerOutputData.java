package use_case.watchlist;

import java.util.Objects;

/** Result of a successful Add Ticker. */
public final class AddTickerOutputData {

    private final String addedSymbol;
    private final String companyName;
    private final int priceCount;
    private final WatchlistSnapshot snapshot;

    /**
     * @param addedSymbol the normalized symbol that was added
     * @param companyName the company name, or {@code ""} when the provider had none.
     *                    A single non-null field rather than a nullable name plus an
     *                    availability flag, so the two can never disagree.
     * @param priceCount  how many daily prices were stored for the symbol
     * @param snapshot    the watchlist as it stands after the add
     * @throws NullPointerException if any argument is null
     */
    public AddTickerOutputData(String addedSymbol, String companyName,
                               int priceCount, WatchlistSnapshot snapshot) {
        this.addedSymbol = Objects.requireNonNull(addedSymbol, "Added symbol cannot be null");
        this.companyName = Objects.requireNonNull(companyName, "Company name cannot be null");
        this.priceCount = priceCount;
        this.snapshot = Objects.requireNonNull(snapshot, "Snapshot cannot be null");
    }

    public String getAddedSymbol() {
        return addedSymbol;
    }

    /** @return the company name, or "" when the provider had none. Never null. */
    public String getCompanyName() {
        return companyName;
    }

    /**
     * @return whether a company name was found. The add still succeeds when it was
     *         not, so the presenter uses this to choose its wording rather than to
     *         report an error.
     */
    public boolean isCompanyNameAvailable() {
        return !companyName.isEmpty();
    }

    public int getPriceCount() {
        return priceCount;
    }

    public WatchlistSnapshot getSnapshot() {
        return snapshot;
    }
}
