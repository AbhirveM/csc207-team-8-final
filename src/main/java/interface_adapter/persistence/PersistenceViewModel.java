package interface_adapter.persistence;

import entity.Watchlist;

import java.beans.PropertyChangeSupport;

public class PersistenceViewModel {
    public static final String STATUS_PROPERTY = "status";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private Watchlist watchlist;
    private String statusMessage = "";

    public void setLoadedWatchlist(Watchlist watchlist) {
        this.watchlist = watchlist;
        this.statusMessage = "Watchlist loaded.";
        support.firePropertyChange(STATUS_PROPERTY, null, statusMessage);
    }

    public void setStatusMessage(String message) {
        this.statusMessage = message;
        support.firePropertyChange(STATUS_PROPERTY, null, message);
    }

    public Watchlist getWatchlist() { return watchlist; }
    public String getStatusMessage() { return statusMessage; }

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
