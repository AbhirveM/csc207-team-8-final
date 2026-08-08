package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import interface_adapter.momentum.MomentumController;
import interface_adapter.momentum.MomentumState;
import interface_adapter.momentum.MomentumViewModel;

/**
 * Swing panel for configuring the RSI Momentum strategy.
 */
public final class MomentumConfigurationView extends JPanel {

    private static final String BLANK_LINE = " ";
    private static final String ERROR_PREFIX = "Error: ";

    private final MomentumViewModel viewModel;
    private final MomentumController controller;

    private final JTextField periodField =
            new JTextField("14", 10);

    private final JTextField oversoldField =
            new JTextField("30", 10);

    private final JTextField overboughtField =
            new JTextField("70", 10);

    private final JButton configureButton =
            new JButton(MomentumViewModel.CONFIGURE_BUTTON_LABEL);

    private final JLabel statusLabel =
            new JLabel(BLANK_LINE);

    private final JLabel errorLabel =
            new JLabel(BLANK_LINE);

    /**
     * Builds the Momentum configuration panel.
     *
     * @param viewModel the observable Momentum view model
     * @param controller the Momentum configuration controller
     */
    public MomentumConfigurationView(
            MomentumViewModel viewModel,
            MomentumController controller) {

        this.viewModel = Objects.requireNonNull(
                viewModel,
                "View model cannot be null");

        this.controller = Objects.requireNonNull(
                controller,
                "Controller cannot be null");

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(
                16, 16, 16, 16));

        add(buildTitle(), BorderLayout.NORTH);
        add(buildConfigurationPanel(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        configureButton.addActionListener(event -> onConfigure());

        viewModel.addPropertyChangeListener(
                this::onViewModelChanged);

        render(viewModel.getState());
    }

    /**
     * Builds the title area.
     *
     * @return the title panel
     */
    private JPanel buildTitle() {
        final JPanel panel = new JPanel();

        final JLabel title =
                new JLabel(MomentumViewModel.TITLE_LABEL);

        panel.add(title);

        return panel;
    }

    /**
     * Builds the Momentum parameter input area.
     *
     * @return the configuration panel
     */
    private JPanel buildConfigurationPanel() {
        final JPanel panel = new JPanel(new GridLayout(
                4, 2, 8, 8));

        final JLabel periodLabel =
                new JLabel("RSI Period:");

        final JLabel oversoldLabel =
                new JLabel("Oversold Threshold:");

        final JLabel overboughtLabel =
                new JLabel("Overbought Threshold:");

        periodLabel.setLabelFor(periodField);
        oversoldLabel.setLabelFor(oversoldField);
        overboughtLabel.setLabelFor(overboughtField);

        periodField.setToolTipText(
                "Number of periods used to calculate RSI.");

        oversoldField.setToolTipText(
                "RSI value at or below which a BUY signal is generated.");

        overboughtField.setToolTipText(
                "RSI value at or above which a SELL signal is generated.");

        configureButton.setToolTipText(
                "Configure the Momentum strategy.");

        panel.add(periodLabel);
        panel.add(periodField);

        panel.add(oversoldLabel);
        panel.add(oversoldField);

        panel.add(overboughtLabel);
        panel.add(overboughtField);

        panel.add(new JLabel());
        panel.add(configureButton);

        return panel;
    }

    /**
     * Builds the status and error area.
     *
     * @return the footer panel
     */
    private JPanel buildFooter() {
        final JPanel footer = new JPanel(
                new GridLayout(2, 1));

        errorLabel.setForeground(Color.RED.darker());

        statusLabel.getAccessibleContext()
                .setAccessibleName("Status");

        errorLabel.getAccessibleContext()
                .setAccessibleName("Error");

        footer.add(statusLabel);
        footer.add(errorLabel);

        return footer;
    }

    /**
     * Sends the values currently entered by the user
     * to the Momentum controller.
     */
    private void onConfigure() {
        controller.execute(
                periodField.getText(),
                oversoldField.getText(),
                overboughtField.getText());
    }

    /**
     * Handles updates from the Momentum view model.
     *
     * @param event the property change event
     */
    private void onViewModelChanged(
            PropertyChangeEvent event) {

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> onViewModelChanged(event));
            return;
        }

        render(viewModel.getState());
    }

    /**
     * Renders the current Momentum state.
     *
     * @param state the state to display
     */
    private void render(MomentumState state) {

        if (state.getErrorMessage() != null) {
            statusLabel.setText(BLANK_LINE);
            errorLabel.setText(
                    ERROR_PREFIX + state.getErrorMessage());
        }
        else if (state.getConfiguration() != null) {
            statusLabel.setText(
                    "Momentum configuration saved.");
            errorLabel.setText(BLANK_LINE);
        }
        else {
            statusLabel.setText(BLANK_LINE);
            errorLabel.setText(BLANK_LINE);
        }
    }
}