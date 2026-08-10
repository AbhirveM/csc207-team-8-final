package views;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.util.Locale;

/**
 * Shared styling for the everyday controls: fields, buttons, headings, and readouts.
 *
 * <p>The rules these apply are one-liners individually; centralising them is what keeps
 * five screens built by four people from each inventing their own button. Nothing here
 * changes behaviour - mnemonics, tooltips, and accessible names stay the responsibility of
 * the view that owns the control, because only that view knows what the control means.
 */
public final class Controls {

    private Controls() {
    }

    /**
     * Styles a text field: house height, a hairline border, and interior padding so the
     * text does not sit against the rule.
     *
     * @param field the field to style
     * @return the same field, for chaining into a layout call
     */
    public static JTextField styleField(JTextField field) {
        field.setFont(Theme.FONT_UI);
        field.setForeground(Theme.FG);
        field.setBackground(Theme.FIELD_BG);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.RULE),
                BorderFactory.createEmptyBorder(0, Theme.SM, 0, Theme.SM)));
        constrainHeight(field, field.getPreferredSize().width);
        return field;
    }

    /**
     * Styles a combo box to match the text fields beside it.
     *
     * @param comboBox the combo box to style
     * @return the same combo box
     */
    public static JComboBox<?> styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(Theme.FONT_UI);
        comboBox.setForeground(Theme.FG);
        comboBox.setBackground(Theme.FIELD_BG);
        constrainHeight(comboBox, comboBox.getPreferredSize().width);
        return comboBox;
    }

    /**
     * Marks a button as the one primary action on its screen: accent fill, reversed text.
     * Exactly one per screen - a second one leaves the user with no answer to "what now".
     *
     * @param button the button to style
     * @return the same button
     */
    public static JButton primary(JButton button) {
        button.setFont(Theme.FONT_MONO);
        button.setForeground(Theme.ACCENT_FG);
        button.setBackground(Theme.ACCENT);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createEmptyBorder(Theme.XS, Theme.MD, Theme.XS, Theme.MD));
        constrainHeight(button, button.getPreferredSize().width);
        return button;
    }

    /**
     * Styles a button as a secondary action: plain surface behind a hairline border. There
     * is no third tier; anything that is not the primary action looks like this.
     *
     * @param button the button to style
     * @return the same button
     */
    public static JButton secondary(JButton button) {
        button.setFont(Theme.FONT_MONO);
        button.setForeground(Theme.FG);
        button.setBackground(Theme.BG);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.RULE),
                BorderFactory.createEmptyBorder(Theme.XS, Theme.MD, Theme.XS, Theme.MD)));
        constrainHeight(button, button.getPreferredSize().width);
        return button;
    }

    /**
     * Turns a label into a section heading: small, bold, amber, and uppercased. The visible
     * text is uppercased here rather than in the caller so the accessible name the caller
     * set keeps its original casing.
     *
     * @param label the label to style
     * @return the same label
     */
    public static JLabel heading(JLabel label) {
        label.setFont(Theme.FONT_HEADING);
        label.setForeground(Theme.ACCENT);
        label.setText(label.getText().toUpperCase(Locale.ROOT));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.XS, 0));
        return label;
    }

    /**
     * Styles a form label: the cyan name of the value in the control beside it.
     *
     * <p>Deliberately neither uppercased nor restyled beyond the colour. A form label is the
     * one label that regularly carries a displayed mnemonic, and that mnemonic is stored as
     * an index into the text - rewriting the text would move the underline onto a different
     * character. The prose face rather than the house monospace, for the same reason it is
     * used in a substituted table cell: these are words, not figures.
     *
     * @param label the label to style
     * @return the same label
     */
    public static JLabel fieldLabel(JLabel label) {
        label.setFont(Theme.FONT_UI);
        label.setForeground(Theme.KEY);
        return label;
    }

    /**
     * Styles a screen title.
     *
     * @param label the label to style
     * @return the same label
     */
    public static JLabel title(JLabel label) {
        label.setFont(Theme.FONT_TITLE);
        label.setForeground(Theme.FG);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.MD, 0));
        return label;
    }

    /**
     * Styles a read-only text area used as an output surface: monospace, on the data
     * surface, inside a single rule.
     *
     * @param textArea the text area to style
     * @return the same text area
     */
    public static JTextArea styleOutput(JTextArea textArea) {
        textArea.setFont(Theme.FONT_MONO);
        textArea.setForeground(Theme.FG);
        textArea.setBackground(Theme.BG);
        textArea.setEditable(false);
        // An uneditable area still takes focus, which is what a screen reader needs to read
        // it; the caret would otherwise blink in text nobody can type into.
        textArea.getCaret().setVisible(false);
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.RULE),
                BorderFactory.createEmptyBorder(Theme.SM, Theme.SM, Theme.SM, Theme.SM)));
        return textArea;
    }

    /**
     * Pins a control to the house field height and to the width it asked for.
     *
     * <p>Both halves matter in a box row, which is the only layout that honours a maximum
     * size: without the height the control grows to match its tallest sibling, and without
     * the width it absorbs all the slack in the row and shoulders everything after it off
     * the edge of the window. {@code GridBagLayout} ignores maximum size, so a form field
     * still stretches to fill its cell.
     *
     * @param component the control to pin
     * @param width the preferred width to keep
     */
    private static void constrainHeight(JComponent component, int width) {
        component.setPreferredSize(new Dimension(width, Theme.FIELD_HEIGHT));
        component.setMaximumSize(new Dimension(width, Theme.FIELD_HEIGHT));
    }
}
