package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The table renderers are the one part of the restyle that makes a decision per value rather
 * than once at construction, so they are the part worth testing: whether a figure is signed,
 * whether a cell is a figure at all, and whether the padding survives the base class
 * reassigning the border on every call.
 */
class TableStylerTest {

    private JTable table;

    @BeforeEach
    void setUp() {
        DefaultTableModel model = new DefaultTableModel(
                new String[] {"Symbol", "Close", "Change"}, 0);
        model.addRow(new Object[] {"AAPL", "249.68", "4.08"});
        model.addRow(new Object[] {"MSFT", "Not loaded", "-1.98"});
        model.addRow(new Object[] {"NVDA", "1204.55", "0.00"});
        table = new JTable(model);
        TableStyler.style(table);
    }

    @Test
    void styleAppliesTheDenseHouseMetrics() {
        assertEquals(Theme.ROW_HEIGHT, table.getRowHeight());
        assertEquals(Theme.RULE, table.getGridColor());
        assertEquals(Theme.ACCENT, table.getSelectionBackground());
        assertTrue(table.getShowHorizontalLines());
        assertTrue(!table.getShowVerticalLines());
    }

    @Test
    void rowsAreSeparatedByAOnePixelRuleRatherThanWhitespace() {
        // A zero row margin leaves the horizontal grid line nowhere to paint, so the rows
        // would separate on whitespace alone and the table would lose its ruling.
        assertEquals(1, table.getIntercellSpacing().height);
        assertEquals(0, table.getIntercellSpacing().width);
    }

    @Test
    void numericCellsAreRightAlignedMonospace() {
        JLabel cell = render(1, "249.68");
        assertEquals(SwingConstants.RIGHT, cell.getHorizontalAlignment());
        assertEquals(Theme.FONT_MONO, cell.getFont());
    }

    @Test
    void aWordSubstitutedIntoANumericColumnDropsBackToTheTextFont() {
        // The presenter puts "Not loaded" in a figures column; monospace makes prose wider
        // than its column and it gets clipped.
        JLabel cell = render(1, "Not loaded");
        assertEquals(Theme.FONT_UI, cell.getFont());
        assertEquals(SwingConstants.RIGHT, cell.getHorizontalAlignment());
    }

    @Test
    void everyCellKeepsItsPaddingDespiteTheBaseClassReassigningTheBorder() {
        Insets padding = render(0, "AAPL").getBorder().getBorderInsets(new JLabel());
        assertEquals(Theme.SM, padding.left);
        assertEquals(Theme.SM, padding.right);
    }

    @Test
    void aFocusedCellKeepsAVisibleFocusBorderAsWellAsItsPadding() {
        TableStyler.numericColumns(table, 1);
        TableCellRenderer renderer = table.getColumnModel().getColumn(1).getCellRenderer();
        // A renderer returns itself, so both borders have to be read before the next render
        // overwrites them - comparing the two components afterwards compares one object with
        // itself.
        Border focused = ((JLabel) renderer
                .getTableCellRendererComponent(table, "249.68", false, true, 0, 1)).getBorder();
        Border unfocused = ((JLabel) renderer
                .getTableCellRendererComponent(table, "249.68", false, false, 0, 1)).getBorder();
        assertNotNull(focused);
        // The focused cell's border is the look and feel's highlight wrapped around the
        // padding, so it must differ from the plain padding an unfocused cell gets.
        assertNotEquals(unfocused.getClass(), focused.getClass());
        assertTrue(focused instanceof CompoundBorder);
    }

    @Test
    void aPositiveChangeGainsAPlusSignAndTheUpColour() {
        JLabel cell = renderSigned("4.08", false);
        assertEquals("+4.08", cell.getText());
        assertEquals(Theme.UP, cell.getForeground());
    }

    @Test
    void aNegativeChangeKeepsItsMinusSignAndTakesTheDownColour() {
        JLabel cell = renderSigned("-1.98", false);
        assertEquals("-1.98", cell.getText());
        assertEquals(Theme.DOWN, cell.getForeground());
    }

    @Test
    void zeroIsNeitherUpNorDown() {
        JLabel cell = renderSigned("0.00", false);
        assertEquals("0.00", cell.getText());
        assertEquals(Theme.FG, cell.getForeground());
    }

    @Test
    void theSignSurvivesWithoutTheColourOnASelectedRow() {
        // Selection paints the accent behind the cell, so the direction colour is dropped for
        // contrast. The sign is what has to carry the meaning, and it still does.
        JLabel cell = renderSigned("4.08", true);
        assertEquals("+4.08", cell.getText());
        assertNotEquals(Theme.UP, cell.getForeground());
    }

    @Test
    void aNonNumericValueInASignedColumnIsLeftAlone() {
        JLabel cell = renderSigned("n/a", false);
        assertEquals("n/a", cell.getText());
        assertEquals(Theme.FG, cell.getForeground());
    }

    @Test
    void aNullValueInASignedColumnRendersEmptyRatherThanThrowing() {
        JLabel cell = renderSigned(null, false);
        assertEquals("", cell.getText());
    }

    @Test
    void headersAreLeftAlignedHeadingType() {
        Component header = table.getTableHeader().getDefaultRenderer()
                .getTableCellRendererComponent(table, "Symbol", false, false, -1, 0);
        assertEquals(Theme.FONT_HEADING, header.getFont());
        assertEquals(SwingConstants.LEADING, ((JLabel) header).getHorizontalAlignment());
        assertEquals(Theme.FG_MUTED, header.getForeground());
    }

    @Test
    void headersDoNotReorderSoTheColumnRenderersStayOnTheirColumns() {
        assertTrue(!table.getTableHeader().getReorderingAllowed());
    }

    @Test
    void preferredWidthsAreAppliedInColumnOrderAndIgnoreASurplus() {
        TableStyler.preferredWidths(table, 40, 90, 70, 999);
        assertEquals(40, table.getColumnModel().getColumn(0).getPreferredWidth());
        assertEquals(90, table.getColumnModel().getColumn(1).getPreferredWidth());
        assertEquals(70, table.getColumnModel().getColumn(2).getPreferredWidth());
    }

    @Test
    void wrapReplacesTheSunkenBevelWithASingleRule() {
        JScrollPane scrollPane = TableStyler.wrap(table);
        assertSame(table, scrollPane.getViewport().getView());
        assertEquals(Theme.BG, scrollPane.getViewport().getBackground());
        Insets border = scrollPane.getBorder().getBorderInsets(scrollPane);
        assertEquals(1, border.top);
        assertEquals(1, border.left);
    }

    /**
     * Renders one cell through whatever renderer its column carries.
     *
     * @param column the column to render through
     * @param value the value to render
     * @return the rendered label
     */
    private JLabel render(int column, Object value) {
        TableStyler.numericColumns(table, 1);
        // getCellRenderer falls back to the table's default renderer for a column that has
        // none of its own, which is how the text columns are rendered.
        return (JLabel) table.getCellRenderer(0, column)
                .getTableCellRendererComponent(table, value, false, false, 0, column);
    }

    /**
     * Renders a value through the signed renderer.
     *
     * @param value the value to render
     * @param selected whether the row is selected
     * @return the rendered label
     */
    private JLabel renderSigned(Object value, boolean selected) {
        TableStyler.signedColumns(table, 2);
        return (JLabel) table.getColumnModel().getColumn(2).getCellRenderer()
                .getTableCellRendererComponent(table, value, selected, false, 0, 2);
    }
}
