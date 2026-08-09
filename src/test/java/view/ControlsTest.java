package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

/**
 * The control helpers exist so five screens cannot each invent their own button. These tests
 * pin the two properties that would otherwise drift: a control keeps the house height, and
 * the primary and secondary tiers stay visibly different from each other.
 */
class ControlsTest {

    @Test
    void aFieldTakesTheHouseHeightAndKeepsItUnderABoxLayout() {
        JTextField field = Controls.styleField(new JTextField(10));
        assertEquals(Theme.FIELD_HEIGHT, field.getPreferredSize().height);
        // A box layout honours the maximum size; without a capped width the field absorbs
        // the whole row and pushes the buttons after it off the window.
        assertEquals(Theme.FIELD_HEIGHT, field.getMaximumSize().height);
        assertEquals(field.getPreferredSize().width, field.getMaximumSize().width);
    }

    @Test
    void aFieldHasInteriorPaddingSoItsTextClearsTheRule() {
        JTextField field = Controls.styleField(new JTextField(10));
        Insets insets = field.getBorder().getBorderInsets(field);
        assertTrue(insets.left >= Theme.SM);
        assertTrue(insets.right >= Theme.SM);
    }

    @Test
    void aComboBoxMatchesTheFieldsBesideIt() {
        JComboBox<String> comboBox = new JComboBox<>(new String[] {"one", "two"});
        Controls.styleComboBox(comboBox);
        assertEquals(Theme.FIELD_HEIGHT, comboBox.getPreferredSize().height);
        assertEquals(Theme.FONT_UI, comboBox.getFont());
    }

    @Test
    void thePrimaryAndSecondaryTiersAreToldApartByFillNotJustBorder() {
        JButton primary = Controls.primary(new JButton("Run"));
        JButton secondary = Controls.secondary(new JButton("Cancel"));
        assertEquals(Theme.ACCENT, primary.getBackground());
        assertEquals(Theme.ACCENT_FG, primary.getForeground());
        assertEquals(Theme.BG, secondary.getBackground());
        assertEquals(Theme.FG, secondary.getForeground());
        assertNotEquals(primary.getBackground(), secondary.getBackground());
    }

    @Test
    void stylingAButtonReturnsTheSameInstanceSoListenersAndMnemonicsSurvive() {
        JButton button = new JButton("Add");
        button.setMnemonic('A');
        assertSame(button, Controls.primary(button));
        assertEquals('A', button.getMnemonic());
    }

    @Test
    void aHeadingIsUppercasedForDisplayOnly() {
        JLabel label = new JLabel("Watchlist");
        label.getAccessibleContext().setAccessibleName("Watchlist");
        Controls.heading(label);
        assertEquals("WATCHLIST", label.getText());
        assertEquals(Theme.FONT_HEADING, label.getFont());
        assertEquals(Theme.FG_MUTED, label.getForeground());
        // Shouting is a display choice; the spoken name keeps its original casing.
        assertEquals("Watchlist", label.getAccessibleContext().getAccessibleName());
    }

    @Test
    void aTitleTakesTitleType() {
        JLabel label = Controls.title(new JLabel("Momentum Strategy Configuration"));
        assertEquals(Theme.FONT_TITLE, label.getFont());
        assertEquals(Theme.FG, label.getForeground());
    }

    @Test
    void anOutputAreaIsReadOnlyMonospaceWithNoBlinkingCaret() {
        JTextArea area = Controls.styleOutput(new JTextArea(3, 20));
        assertEquals(Theme.FONT_MONO, area.getFont());
        assertEquals(Theme.BG, area.getBackground());
        assertTrue(!area.isEditable());
        assertTrue(!area.getCaret().isVisible());
    }
}
