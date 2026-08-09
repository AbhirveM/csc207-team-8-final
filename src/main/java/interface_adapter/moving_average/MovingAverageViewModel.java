package interface_adapter.moving_average;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public final class MovingAverageViewModel {

    public static final String VIEW_NAME = "moving_average_configuration";
    public static final String STATE_PROPERTY = "state";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final MovingAverageState state = new MovingAverageState();

    public MovingAverageState getState() {
        return state;
    }

    public void firePropertyChanged() {
        support.firePropertyChange(STATE_PROPERTY, null, state);
    }

    public void addPropertyChangeListener(
            PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}
