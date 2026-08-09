package view;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Shared table presentation: row metrics, grid, header, and the cell renderers.
 *
 * <p>Every {@link JTable} in the app is styled through here rather than at each call site,
 * so the watchlist, the daily prices, and the strategy comparison read as one instrument
 * instead of three tables that happen to sit in the same window.
 *
 * <p>The renderers align and colour; they never reformat. Decimal places, percent signs,
 * and rounding are decided by each presenter, and a view that re-rounded them would be
 * duplicating a decision that deliberately lives in the interface-adapter layer.
 */
public final class TableStyler {

    /** Renderer for text columns: left aligned, with breathing room either side. */
    private static final DefaultTableCellRenderer TEXT = new PaddedRenderer(SwingConstants.LEADING, Theme.FONT_UI);

    /** Renderer for numeric columns: monospace and right aligned so decimal points line up. */
    private static final DefaultTableCellRenderer NUMERIC = new NumericRenderer();

    private TableStyler() {
    }

    /**
     * Applies the house table style: dense rows, a horizontal grid only, a chrome header in
     * heading type, and the padded text renderer on every column.
     *
     * @param table the table to style
     */
    public static void style(JTable table) {
        table.setRowHeight(Theme.ROW_HEIGHT);
        table.setBackground(Theme.BG);
        table.setForeground(Theme.FG);
        table.setFont(Theme.FONT_UI);
        table.setGridColor(Theme.RULE);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        // One pixel of vertical spacing and none horizontally. JTable draws its grid inside
        // the intercell spacing, so a zero row margin means the horizontal rule set above
        // has nowhere to land and the rows separate on whitespace alone. Cell padding comes
        // from the renderers' borders instead, which keeps the rule a hairline.
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(Theme.ACCENT);
        table.setSelectionForeground(Theme.ACCENT_FG);
        table.setDefaultRenderer(Object.class, TEXT);
        styleHeader(table.getTableHeader());
    }

    /**
     * Switches the given columns to the numeric renderer.
     *
     * @param table the table whose columns to convert
     * @param columns the model column indexes that hold figures
     */
    public static void numericColumns(JTable table, int... columns) {
        for (int column : columns) {
            table.getColumnModel().getColumn(column).setCellRenderer(NUMERIC);
        }
    }

    /**
     * Switches the given columns to the signed renderer, which prefixes a {@code +} and
     * colours the figure by direction.
     *
     * @param table the table whose columns to convert
     * @param columns the model column indexes that hold signed figures
     */
    public static void signedColumns(JTable table, int... columns) {
        SignedRenderer renderer = new SignedRenderer();
        for (int column : columns) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    /**
     * Sets the columns' preferred widths, in the order the model declares them.
     *
     * <p>Columns share the width equally by default, which truncates a date into
     * {@code 2026-08-...} while a two-character symbol keeps room it has no use for. These
     * are proportions rather than pixels: the table still divides the width it is given.
     *
     * @param table the table to size
     * @param widths one preferred width per column
     */
    public static void preferredWidths(JTable table, int... widths) {
        for (int column = 0; column < widths.length && column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
    }

    /**
     * Wraps a table in a scroll pane bounded by a single rule, rather than the sunken
     * bevel a scroll pane ships with.
     *
     * @param table the table to wrap
     * @return the scroll pane holding the table
     */
    public static JScrollPane wrap(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Theme.RULE));
        scrollPane.getViewport().setBackground(Theme.BG);
        scrollPane.setBackground(Theme.BG);
        return scrollPane;
    }

    /**
     * Styles a table header as chrome: heading type, muted, left aligned, over a rule.
     *
     * @param header the header to style; ignored when the table has none
     */
    private static void styleHeader(JTableHeader header) {
        if (header == null) {
            return;
        }
        header.setBackground(Theme.CHROME);
        header.setForeground(Theme.FG_MUTED);
        header.setFont(Theme.FONT_HEADING);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.RULE));
        header.setPreferredSize(new Dimension(0, Theme.HEADER_HEIGHT));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer(header.getDefaultRenderer()));
    }

    /**
     * Gives a cell its interior padding without losing the focus indicator.
     *
     * <p>The focused cell keeps the look and feel's highlight border with the padding nested
     * inside it. Replacing that border outright would be a real accessibility regression: it
     * is the only thing showing a keyboard user which cell they are on.
     *
     * @param renderer the label being rendered
     * @param hasFocus whether this cell currently has focus
     */
    private static void applyPadding(DefaultTableCellRenderer renderer, boolean hasFocus) {
        if (hasFocus) {
            renderer.setBorder(BorderFactory.createCompoundBorder(
                    renderer.getBorder(),
                    BorderFactory.createEmptyBorder(0, Theme.XS - 1, 0, Theme.XS - 1)));
        }
        else {
            renderer.setBorder(BorderFactory.createEmptyBorder(0, Theme.XS, 0, Theme.XS));
        }
    }

    /**
     * Reports whether a cell value is a figure rather than a word a presenter substituted
     * into a numeric column.
     *
     * @param value the cell value
     * @return true when the value parses as a number
     */
    private static boolean isNumeric(Object value) {
        boolean numeric = false;
        if (value != null) {
            try {
                Double.parseDouble(value.toString().trim());
                numeric = true;
            }
            catch (NumberFormatException notANumber) {
                numeric = false;
            }
        }
        return numeric;
    }

    /**
     * Left-aligns the header text and applies heading type, by decorating whatever renderer
     * the look and feel installed rather than replacing it.
     */
    private static final class HeaderRenderer implements TableCellRenderer {

        private final TableCellRenderer delegate;

        private HeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component =
                    delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.LEADING);
                label.setFont(Theme.FONT_HEADING);
                label.setForeground(Theme.FG_MUTED);
                label.setBorder(BorderFactory.createEmptyBorder(0, Theme.XS, 0, Theme.XS));
            }
            return component;
        }
    }

    /**
     * A cell renderer that keeps the house alignment, font, and padding.
     *
     * <p>All three are re-applied after {@code super}, which is not belt and braces:
     * {@link DefaultTableCellRenderer#getTableCellRendererComponent} assigns the border
     * itself on every call, so anything set once in a constructor is silently discarded the
     * first time a cell paints. That is what left every column's text jammed against the
     * grid line and clipped the longest value in each column by a pixel.
     */
    private static class PaddedRenderer extends DefaultTableCellRenderer {

        private final int alignment;
        private final Font font;

        PaddedRenderer(int alignment, Font font) {
            this.alignment = alignment;
            this.font = font;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(alignment);
            setFont(fontFor(value));
            applyPadding(this, hasFocus);
            return this;
        }

        /**
         * The font this cell should use. Overridden where the font depends on the value.
         *
         * @param value the cell value
         * @return the font to render in
         */
        protected Font fontFor(Object value) {
            return font;
        }
    }

    /**
     * Renders a figure right-aligned in monospace.
     *
     * <p>Monospace applies to figures only. The presenters substitute words into numeric
     * columns - "Not loaded" for a ticker with no history, an em dash for an absent price -
     * and setting prose in a figures font makes it both wider than its column and wrong to
     * read. The column keeps its right alignment either way, so the ragged edge that would
     * give the substitution away never appears.
     */
    private static final class NumericRenderer extends PaddedRenderer {

        private NumericRenderer() {
            super(SwingConstants.RIGHT, Theme.FONT_MONO);
        }

        @Override
        protected Font fontFor(Object value) {
            return isNumeric(value) ? Theme.FONT_MONO : Theme.FONT_UI;
        }
    }

    /**
     * Renders a signed figure: an explicit {@code +} or {@code -} always, and the direction
     * colour on top of it. The sign is what carries the meaning - the colour is redundant by
     * design, so the column still reads correctly in greyscale or to a colour-blind user.
     *
     * <p>The value is parsed only far enough to learn its direction. It is not reformatted:
     * the presenter already decided how many decimals it shows.
     */
    private static final class SignedRenderer extends PaddedRenderer {

        private SignedRenderer() {
            super(SwingConstants.RIGHT, Theme.FONT_MONO);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            String text = value == null ? "" : value.toString();
            Double parsed = parse(text);
            if (parsed != null && parsed > 0) {
                text = "+" + text;
            }
            super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
            // Selection paints the accent behind the cell; direction colour on top of it would
            // fail contrast, so a selected row keeps the look and feel's own foreground.
            if (!isSelected) {
                setForeground(directionColour(parsed));
            }
            return this;
        }

        /**
         * Reads the direction of a display string.
         *
         * @param text the cell text
         * @return the value it represents, or null when it is not a number
         */
        private static Double parse(String text) {
            try {
                return Double.valueOf(text.trim());
            }
            catch (NumberFormatException notANumber) {
                return null;
            }
        }

        /**
         * Picks the colour for a direction.
         *
         * @param parsed the parsed value, or null when the cell is not a number
         * @return the foreground to paint
         */
        private static Color directionColour(Double parsed) {
            Color colour = Theme.FG;
            if (parsed != null && parsed > 0) {
                colour = Theme.UP;
            }
            else if (parsed != null && parsed < 0) {
                colour = Theme.DOWN;
            }
            return colour;
        }
    }
}
