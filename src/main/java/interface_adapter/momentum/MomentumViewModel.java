package interface_adapter.momentum;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * View model for the Momentum strategy configuration.
 */
public final class MomentumViewModel {

    public static final String VIEW_NAME = "momentum";

    public static final String TITLE_LABEL =
            "Momentum Strategy Configuration";
    public static final String CONFIGURE_BUTTON_LABEL =
            "Configure";

    private MomentumState state = new MomentumState();

    private final PropertyChangeSupport support =
            new PropertyChangeSupport(this);

    public MomentumState getState() {
        return state;
    }

    public void setState(MomentumState state) {
        this.state = state;
    }

    public void firePropertyChanged() {
        support.firePropertyChange("state", null, state);
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}