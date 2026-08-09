package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

/**
 * The header band is the one piece of shared layout that has two children competing for the
 * same row, and it is laid out by a {@code BorderLayout} that will happily let them overlap.
 * That is not hypothetical: the first version of the chart bands put a whole summary sentence
 * in the meta slot, and it painted straight across the region title, leaving the title present
 * in the component tree and invisible on screen. These tests lay a band out at a real width and
 * assert the two never occupy the same pixels.
 */
class PanelHeaderTest {

    /** A plausible band width - roughly half of a split pane in a normal window. */
    private static final int BAND_WIDTH = 520;

    @Test
    void aLongMetaReadoutNeverPaintsOverTheRegionTitle() {
        JLabel title = new JLabel("Close price");
        JLabel meta = new JLabel("Close price for AAPL, 120 days, low 216.74, high 262.46, "
                + "latest 249.68, +38.94 (+18.53%) over the window.");

        JPanel band = laidOut(PanelHeader.band(title, meta));

        assertTrue(title.getWidth() > 0, "the title was squeezed out of the band entirely");
        assertTrue(title.getX() + title.getWidth() <= meta.getX(),
                "the meta readout overlaps the title: title ends at "
                        + (title.getX() + title.getWidth()) + ", meta starts at " + meta.getX());
        assertTrue(meta.getX() + meta.getWidth() <= band.getWidth(),
                "the meta readout runs off the end of the band");
    }

    @Test
    void aShortMetaReadoutStillSitsHardAgainstTheRightEdge() {
        // The fix must not cost the band its right alignment: a row count has always sat at the
        // far end of the band, opposite the title, and that is what makes it scannable.
        JLabel title = new JLabel("Daily prices");
        JLabel meta = new JLabel("120 ROWS");

        JPanel band = laidOut(PanelHeader.band(title, meta));

        int rightEdge = band.getWidth() - band.getInsets().right;
        assertEquals(rightEdge, meta.getX() + meta.getWidth());
    }

    @Test
    void aBandWithNoMetaGivesTheTitleTheWholeRow() {
        JLabel title = new JLabel("Result");
        JPanel band = laidOut(PanelHeader.band(title, null));
        assertTrue(title.getWidth() > 0);
    }

    @Test
    void theTitleIsUppercasedForTheEyeButKeepsItsBindingForEverythingElse() {
        JLabel content = new JLabel();
        JLabel title = new JLabel("Close price");
        title.setLabelFor(content);

        PanelHeader.band(title, null);

        assertEquals("CLOSE PRICE", title.getText());
        assertEquals(content, title.getLabelFor());
    }

    /**
     * Sizes a band and runs its layout, so the children have real bounds to assert on.
     *
     * @param band the band to lay out
     * @return the same band, laid out
     */
    private static JPanel laidOut(JPanel band) {
        band.setSize(BAND_WIDTH, Theme.HEADER_HEIGHT);
        band.doLayout();
        return band;
    }
}
