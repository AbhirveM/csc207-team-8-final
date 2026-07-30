package view;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Owns the CardLayout that swaps between each member's screen.
 * Register each view with a name matching the *_VIEW constant used
 * by that feature's ViewModel (see ComparisonViewModel.VIEW_NAME for example).
 */
public class ViewManager implements PropertyChangeListener {
    private final JPanel cardPanel;
    private final CardLayout cardLayout;

    public ViewManager(JPanel cardPanel, CardLayout cardLayout, ViewManagerModel viewManagerModel) {
        this.cardPanel = cardPanel;
        this.cardLayout = cardLayout;
        viewManagerModel.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ViewManagerModel.ACTIVE_VIEW_PROPERTY)) {
            cardLayout.show(cardPanel, (String) evt.getNewValue());
        }
    }
}
