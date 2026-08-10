package views;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * The header band that sits over a data region.
 *
 * <p>This is the single strongest signal that a screen is an instrument rather than a form:
 * a chrome-filled strip, the region's name in amber on the left, and an optional figure on
 * the right - a row count, a timestamp, whatever the region can say about itself in four
 * words. A window built from banded regions reads as a set of panels a user can address one
 * at a time, where the same window built from floating headings reads as one long document.
 *
 * <p>It is a band and not a card: a fill and a rule, no border around the region, no corner
 * radius, and no shadow. The region below it keeps its own single-rule boundary.
 *
 * <p>The title label is built by the caller rather than from a string here, because a
 * heading over a table usually carries a {@code setLabelFor} binding and an accessible name
 * that belong to the caller's knowledge of what the region is. This class only styles and
 * places it.
 */
public final class PanelHeader {

    private PanelHeader() {
    }

    /**
     * Builds a header band.
     *
     * @param title the region's name, already carrying whatever accessible name and
     *              {@code setLabelFor} binding the caller needs; it is styled as a heading here
     * @param meta an optional right-hand readout such as a row count or a timestamp, or null
     *             when the region has nothing to say about itself
     * @return the assembled band
     */
    public static JPanel band(JLabel title, JLabel meta) {
        Controls.heading(title);
        // The heading helper adds a bottom gap meant for a floating heading with a region
        // under it. Inside a fixed-height band that gap pushes the text off centre.
        title.setBorder(null);

        final JPanel band = new JPanel(new BorderLayout());
        band.setBackground(Theme.CHROME);
        band.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.RULE_STRONG),
                BorderFactory.createEmptyBorder(0, Theme.SM, 0, Theme.SM)));
        band.setPreferredSize(new Dimension(0, Theme.HEADER_HEIGHT));
        band.add(title, BorderLayout.WEST);

        if (meta != null) {
            meta.setFont(Theme.FONT_MONO);
            meta.setForeground(Theme.FG_MUTED);
            band.add(meta, BorderLayout.EAST);
        }
        return band;
    }

    /**
     * Builds a complete banded region: the header band, and the content directly under it.
     *
     * @param title the region's name, styled as a heading here
     * @param meta an optional right-hand readout, or null
     * @param content the region the band heads
     * @return the assembled region
     */
    public static JPanel region(JLabel title, JLabel meta, JComponent content) {
        // No gap between the band and the content: the band is the region's top edge, and a
        // gap would leave it floating above something it is supposed to be attached to.
        final JPanel region = new JPanel(new BorderLayout());
        region.setBackground(Theme.BG);
        region.add(band(title, meta), BorderLayout.NORTH);
        region.add(content, BorderLayout.CENTER);
        return region;
    }

    /**
     * Builds a meta label that reports how many rows a table is holding, and keeps reporting
     * it as the model changes.
     *
     * <p>The count is worth showing because every one of these tables is filled
     * asynchronously and each has an empty state that looks exactly like a table that has
     * not loaded yet. A visible zero says the fetch finished and found nothing, which is a
     * different thing from silence.
     *
     * @param table the table to count
     * @return a label tracking the table's row count
     */
    public static JLabel rowCount(JTable table) {
        final JLabel meta = new JLabel(describeRows(table.getRowCount()));
        // The model fires on the event dispatch thread, because everything that mutates one
        // of these models is already marshalled onto it by the presenter.
        table.getModel().addTableModelListener(event -> meta.setText(describeRows(table.getRowCount())));
        return meta;
    }

    /**
     * Phrases a row count for the meta slot.
     *
     * @param rows the number of rows
     * @return the text to display
     */
    private static String describeRows(int rows) {
        final String unit;
        if (rows == 1) {
            unit = " ROW";
        }
        else {
            unit = " ROWS";
        }
        return rows + unit;
    }
}
