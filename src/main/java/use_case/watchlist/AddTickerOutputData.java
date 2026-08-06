package use_case.watchlist;

import java.util.Objects;

/** Result of a successful Add Ticker. */
public final class AddTickerOutputData {

    private final String addedSymbol;
    private final String companyName;
    private final int priceCount;
    private final WatchlistSnapshot snapshot;
    private final MarketDataException.Kind companyNameFailureKind;

    /**
     * Builds a result whose missing company name, if any, carries no diagnosis.
     *
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
        this(addedSymbol, companyName, priceCount, snapshot, null);
    }

    /**
     * @param addedSymbol            the normalized symbol that was added
     * @param companyName            the company name, or {@code ""} when none is available
     * @param priceCount             how many daily prices were stored for the symbol
     * @param snapshot               the watchlist as it stands after the add
     * @param companyNameFailureKind why the company name could not be fetched, or
     *                               {@code null} when it was fetched successfully or was
     *                               simply absent. Lets the presenter distinguish "this
     *                               symbol has no company record" from "the name lookup
     *                               was rate-limited"; the add succeeds either way.
     * @throws NullPointerException if any argument other than
     *                              {@code companyNameFailureKind} is null
     */
    public AddTickerOutputData(String addedSymbol, String companyName,
                               int priceCount, WatchlistSnapshot snapshot,
                               MarketDataException.Kind companyNameFailureKind) {
        this.addedSymbol = Objects.requireNonNull(addedSymbol, "Added symbol cannot be null");
        this.companyName = Objects.requireNonNull(companyName, "Company name cannot be null");
        this.priceCount = priceCount;
        this.snapshot = Objects.requireNonNull(snapshot, "Snapshot cannot be null");
        this.companyNameFailureKind = companyNameFailureKind;
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

    /**
     * @return why the company name is missing, or {@code null} when nothing failed.
     *         Only ever non-null when {@link #isCompanyNameAvailable()} is false.
     */
    public MarketDataException.Kind getCompanyNameFailureKind() {
        return companyNameFailureKind;
    }

    public int getPriceCount() {
        return priceCount;
    }

    public WatchlistSnapshot getSnapshot() {
        return snapshot;
    }
}
