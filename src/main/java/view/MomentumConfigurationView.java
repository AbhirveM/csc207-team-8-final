package view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

        setLayout(new BorderLayout(Theme.MD, Theme.MD));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(
                Theme.LG, Theme.LG, Theme.LG, Theme.LG));

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
        // BorderLayout rather than the default centring FlowLayout: the title starts at the
        // same left edge as the labels below it, which is what makes the column read.
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG);

        final JLabel title = Controls.title(
                new JLabel(MomentumViewModel.TITLE_LABEL));

        panel.add(title, BorderLayout.WEST);

        return panel;
    }

    /**
     * Builds the Momentum parameter input area.
     *
     * @return the configuration panel
     */
    private JPanel buildConfigurationPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG);

        final JLabel periodLabel =
                Controls.fieldLabel(new JLabel("RSI Period:"));

        final JLabel oversoldLabel =
                Controls.fieldLabel(new JLabel("Oversold Threshold:"));

        final JLabel overboughtLabel =
                Controls.fieldLabel(new JLabel("Overbought Threshold:"));

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

        Controls.styleField(periodField);
        Controls.styleField(oversoldField);
        Controls.styleField(overboughtField);
        Controls.primary(configureButton);

        addRow(panel, 0, periodLabel, periodField);
        addRow(panel, 1, oversoldLabel, oversoldField);
        addRow(panel, 2, overboughtLabel, overboughtField);

        final GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 1;
        buttonConstraints.gridy = 3;
        buttonConstraints.anchor = GridBagConstraints.LINE_START;
        buttonConstraints.insets = new Insets(Theme.MD, 0, 0, 0);
        panel.add(configureButton, buttonConstraints);

        // The form keeps its natural width at the top of the screen instead of stretching to
        // fill it; a number field as wide as the window invites an essay rather than a number.
        final JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG);
        wrapper.add(panel, BorderLayout.NORTH);
        return wrapper;
    }

    /**
     * Adds one label/field row: label right-aligned in the first column, field at its
     * natural width in the second, and the slack pushed into a third column so the pair
     * stays left.
     *
     * @param panel the form panel being built
     * @param row the grid row to place this pair on
     * @param label the row's label
     * @param field the control the label describes
     */
    private static void addRow(JPanel panel, int row, JLabel label, JTextField field) {
        label.setFont(Theme.FONT_UI);
        label.setForeground(Theme.FG);

        final GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(0, 0, Theme.SM, Theme.MD);
        panel.add(label, labelConstraints);

        final GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.anchor = GridBagConstraints.LINE_START;
        fieldConstraints.insets = new Insets(0, 0, Theme.SM, 0);
        panel.add(field, fieldConstraints);

        final GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridx = 2;
        fillerConstraints.gridy = row;
        fillerConstraints.weightx = 1.0;
        fillerConstraints.fill = GridBagConstraints.HORIZONTAL;
        final JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, fillerConstraints);
    }

    /**
     * Builds the status and error area.
     *
     * @return the footer panel
     */
    private JPanel buildFooter() {
        final JPanel footer = new JPanel(
                new BorderLayout(0, Theme.XS));
        footer.setBackground(Theme.BG);

        statusLabel.setFont(Theme.FONT_UI);
        statusLabel.setForeground(Theme.FG_MUTED);
        errorLabel.setFont(Theme.FONT_UI);
        errorLabel.setForeground(Theme.DOWN);

        statusLabel.getAccessibleContext()
                .setAccessibleName("Status");

        errorLabel.getAccessibleContext()
                .setAccessibleName("Error");

        footer.add(statusLabel, BorderLayout.NORTH);
        footer.add(errorLabel, BorderLayout.SOUTH);

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
