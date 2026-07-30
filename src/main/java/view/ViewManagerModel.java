package view;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Same pattern as the ViewManager/ViewManagerModel in the course's homework-5
 * starter repo: fires a property change with the name of the view to switch to.
 */
public class ViewManagerModel {
    public static final String ACTIVE_VIEW_PROPERTY = "view";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String activeView;

    public void setActiveView(String viewName) {
        this.activeView = viewName;
        support.firePropertyChange(ACTIVE_VIEW_PROPERTY, null, viewName);
    }

    public String getActiveView() { return activeView; }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
