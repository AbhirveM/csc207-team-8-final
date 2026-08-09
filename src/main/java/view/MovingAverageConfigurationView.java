package view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import interface_adapter.moving_average.MovingAverageController;
import interface_adapter.moving_average.MovingAverageState;
import interface_adapter.moving_average.MovingAverageViewModel;

/**
 * Swing screen for configuring the Moving Average Crossover strategy.
 */
public final class MovingAverageConfigurationView extends JPanel
        implements PropertyChangeListener {

    private static final int FIELD_COLUMNS = 10;

    private final MovingAverageViewModel viewModel;
    private final MovingAverageController controller;

    private final JTextField shortWindowField =
            new JTextField(FIELD_COLUMNS);
    private final JTextField longWindowField =
            new JTextField(FIELD_COLUMNS);
    private final JButton configureButton =
            new JButton("Apply Configuration");
    private final JTextArea statusArea =
            new JTextArea(2, 35);

    public MovingAverageConfigurationView(
            MovingAverageViewModel viewModel,
            MovingAverageController controller) {

        this.viewModel = Objects.requireNonNull(
                viewModel,
                "View model cannot be null");
        this.controller = Objects.requireNonNull(
                controller,
                "Controller cannot be null");

        setName(MovingAverageViewModel.VIEW_NAME);
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(24, 24, 24, 24));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createFormPanel(), BorderLayout.CENTER);
        add(createStatusArea(), BorderLayout.SOUTH);

        configureButton.addActionListener(event ->
                submitConfiguration());

        viewModel.addPropertyChangeListener(this);
        displayState(viewModel.getState());
    }

    private JPanel createHeaderPanel() {
        final JPanel panel = new JPanel(new BorderLayout(0, 8));

        final JLabel titleLabel =
                new JLabel("Moving Average Configuration");
        titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.BOLD, 20.0F));

        final JTextArea instructions = new JTextArea(
                "Enter positive whole-number window sizes. "
                        + "The short window must be smaller than "
                        + "the long window.");
        instructions.setEditable(false);
        instructions.setFocusable(false);
        instructions.setOpaque(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);

        instructions.getAccessibleContext().setAccessibleName(
                "Moving Average configuration instructions");

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(instructions, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFormPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        final GridBagConstraints constraints =
                new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.anchor = GridBagConstraints.LINE_START;

        final JLabel shortWindowLabel =
                new JLabel("Short window (days):");
        shortWindowLabel.setLabelFor(shortWindowField);
        shortWindowLabel.setDisplayedMnemonic(KeyEvent.VK_S);

        shortWindowField.getAccessibleContext().setAccessibleName(
                "Short moving average window");
        shortWindowField.getAccessibleContext()
                .setAccessibleDescription(
                        "Positive whole number smaller than "
                                + "the long window.");

        final JLabel longWindowLabel =
                new JLabel("Long window (days):");
        longWindowLabel.setLabelFor(longWindowField);
        longWindowLabel.setDisplayedMnemonic(KeyEvent.VK_L);

        longWindowField.getAccessibleContext().setAccessibleName(
                "Long moving average window");
        longWindowField.getAccessibleContext()
                .setAccessibleDescription(
                        "Positive whole number larger than "
                                + "the short window.");

        configureButton.setMnemonic(KeyEvent.VK_C);
        configureButton.getAccessibleContext().setAccessibleDescription(
                "Validates and applies the Moving Average settings.");

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(shortWindowLabel, constraints);

        constraints.gridx = 1;
        panel.add(shortWindowField, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(longWindowLabel, constraints);

        constraints.gridx = 1;
        panel.add(longWindowField, constraints);

        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.insets = new Insets(14, 6, 6, 6);
        panel.add(configureButton, constraints);

        return panel;
    }

    private JTextArea createStatusArea() {
        statusArea.setEditable(false);
        statusArea.setFocusable(false);
        statusArea.setOpaque(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);

        statusArea.getAccessibleContext().setAccessibleName(
                "Configuration status");

        return statusArea;
    }

    private void submitConfiguration() {
        final String shortWindow = shortWindowField.getText();
        final String longWindow = longWindowField.getText();

        /*
         * Preserve exactly what the user entered so a failed submission
         * remains visible and editable.
         */
        final MovingAverageState state = viewModel.getState();
        state.setShortWindow(shortWindow);
        state.setLongWindow(longWindow);

        controller.configure(shortWindow, longWindow);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (MovingAverageViewModel.STATE_PROPERTY.equals(
                event.getPropertyName())) {

            displayState(viewModel.getState());
        }
    }

    private void displayState(MovingAverageState state) {
        shortWindowField.setText(state.getShortWindow());
        longWindowField.setText(state.getLongWindow());

        final String message = state.getStatusMessage();

        if (message == null || message.isBlank()) {
            statusArea.setText("");
            return;
        }

        final String prefix = state.isConfigurationSuccessful()
                ? "Success: "
                : "Error: ";

        statusArea.setText(prefix + message);
        statusArea.getAccessibleContext().setAccessibleDescription(
                prefix + message);

        if (!state.isConfigurationSuccessful()) {
            SwingUtilities.invokeLater(() -> {
                shortWindowField.requestFocusInWindow();
                shortWindowField.selectAll();
            });
        }
    }
}
