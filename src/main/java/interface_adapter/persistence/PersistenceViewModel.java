package interface_adapter.persistence;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * The observable state of the save/load status line.
 *
 * <p>A status message and nothing else. The loaded {@code Watchlist} used to be parked here
 * too, which made an entity reachable from anything holding this view model - including the
 * window's status bar. Whoever needs the restored watchlist takes it from the load use
 * case's output boundary instead; this holds only the sentence the user reads.
 */
public class PersistenceViewModel {
    public static final String STATUS_PROPERTY = "status";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String statusMessage = "";

    public void setStatusMessage(String message) {
        this.statusMessage = message;
        support.firePropertyChange(STATUS_PROPERTY, null, message);
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
