package view;

import java.awt.BorderLayout;
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
        setLayout(new BorderLayout(Theme.MD, Theme.MD));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(Theme.LG, Theme.LG, Theme.LG, Theme.LG));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createFormPanel(), BorderLayout.CENTER);
        add(createStatusArea(), BorderLayout.SOUTH);

        configureButton.addActionListener(event ->
                submitConfiguration());

        viewModel.addPropertyChangeListener(this);
        displayState(viewModel.getState());
    }

    private JPanel createHeaderPanel() {
        final JPanel panel = new JPanel(new BorderLayout(0, Theme.SM));
        panel.setBackground(Theme.BG);

        final JLabel titleLabel = Controls.title(
                new JLabel("Moving Average Configuration"));

        final JTextArea instructions = new JTextArea(
                "Enter positive whole-number window sizes. "
                        + "The short window must be smaller than "
                        + "the long window.");
        instructions.setEditable(false);
        instructions.setFocusable(false);
        instructions.setOpaque(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setFont(Theme.FONT_UI);
        instructions.setForeground(Theme.FG_MUTED);

        instructions.getAccessibleContext().setAccessibleName(
                "Moving Average configuration instructions");

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(instructions, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Builds a form label in the house type.
     *
     * @param text the label text
     * @return the styled label
     */
    private static JLabel label(String text) {
        final JLabel formLabel = new JLabel(text);
        formLabel.setFont(Theme.FONT_UI);
        formLabel.setForeground(Theme.FG);
        return formLabel;
    }

    /**
     * Builds a filler that takes up space without painting anything.
     *
     * @return an invisible spacer panel
     */
    private static JPanel transparentFiller() {
        final JPanel filler = new JPanel();
        filler.setOpaque(false);
        return filler;
    }

    private JPanel createFormPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG);

        final GridBagConstraints constraints =
                new GridBagConstraints();
        constraints.insets = new Insets(0, 0, Theme.SM, Theme.MD);
        constraints.anchor = GridBagConstraints.LINE_START;

        final JLabel shortWindowLabel =
                label("Short window (days):");
        shortWindowLabel.setLabelFor(shortWindowField);
        shortWindowLabel.setDisplayedMnemonic(KeyEvent.VK_S);

        shortWindowField.getAccessibleContext().setAccessibleName(
                "Short moving average window");
        shortWindowField.getAccessibleContext()
                .setAccessibleDescription(
                        "Positive whole number smaller than "
                                + "the long window.");

        final JLabel longWindowLabel =
                label("Long window (days):");
        longWindowLabel.setLabelFor(longWindowField);
        longWindowLabel.setDisplayedMnemonic(KeyEvent.VK_L);

        longWindowField.getAccessibleContext().setAccessibleName(
                "Long moving average window");
        longWindowField.getAccessibleContext()
                .setAccessibleDescription(
                        "Positive whole number larger than "
                                + "the short window.");

        Controls.styleField(shortWindowField);
        Controls.styleField(longWindowField);
        Controls.primary(configureButton);

        configureButton.setMnemonic(KeyEvent.VK_C);
        configureButton.getAccessibleContext().setAccessibleDescription(
                "Validates and applies the Moving Average settings.");

        // Labels right-aligned against their fields: the colon column is what the eye
        // follows down a form, and it only exists if the labels share a right edge.
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(shortWindowLabel, constraints);

        constraints.gridy = 1;
        panel.add(longWindowLabel, constraints);

        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(0, 0, Theme.SM, 0);
        constraints.gridx = 1;
        constraints.gridy = 0;
        panel.add(shortWindowField, constraints);

        constraints.gridy = 1;
        panel.add(longWindowField, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(Theme.MD, 0, 0, 0);
        panel.add(configureButton, constraints);

        // Two fillers, and they do different jobs. The first absorbs the spare width so the
        // form keeps its natural size on the left instead of spreading two number fields
        // across the screen. The second takes the spare height in a row of its own *below*
        // the form - give it to a form row instead and the rows space themselves out like a
        // menu, which is what the gaps between these fields were.
        final GridBagConstraints widthFiller = new GridBagConstraints();
        widthFiller.gridx = 2;
        widthFiller.gridy = 0;
        widthFiller.weightx = 1.0;
        widthFiller.fill = GridBagConstraints.HORIZONTAL;
        panel.add(transparentFiller(), widthFiller);

        final GridBagConstraints heightFiller = new GridBagConstraints();
        heightFiller.gridx = 0;
        heightFiller.gridy = 3;
        heightFiller.weighty = 1.0;
        heightFiller.fill = GridBagConstraints.VERTICAL;
        panel.add(transparentFiller(), heightFiller);

        return panel;
    }

    private JTextArea createStatusArea() {
        statusArea.setEditable(false);
        statusArea.setFocusable(false);
        statusArea.setOpaque(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);

        statusArea.setFont(Theme.FONT_UI);
        statusArea.setForeground(Theme.FG_MUTED);

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

        statusArea.setForeground(
                state.isConfigurationSuccessful() ? Theme.FG_MUTED : Theme.DOWN);
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
