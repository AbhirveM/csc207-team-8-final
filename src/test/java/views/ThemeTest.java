package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

/**
 * The design tokens carry two properties the rest of the UI depends on and no reviewer can
 * check by eye: the direction colours clear the contrast ratio the accessibility report
 * claims, and the type scale is actually a scale rather than three sizes that drifted apart.
 */
class ThemeTest {

    /** WCAG AA for body text. */
    private static final double MINIMUM_CONTRAST = 4.5;

    @Test
    void upAndDownClearAaContrastAgainstTheDataSurface() {
        assertTrue(contrastRatio(Theme.UP, Theme.BG) >= MINIMUM_CONTRAST,
                "UP on BG measured " + contrastRatio(Theme.UP, Theme.BG));
        assertTrue(contrastRatio(Theme.DOWN, Theme.BG) >= MINIMUM_CONTRAST,
                "DOWN on BG measured " + contrastRatio(Theme.DOWN, Theme.BG));
    }

    @Test
    void bodyAndMutedTextClearAaContrastAgainstBothSurfaces() {
        assertTrue(contrastRatio(Theme.FG, Theme.BG) >= MINIMUM_CONTRAST);
        assertTrue(contrastRatio(Theme.FG_MUTED, Theme.BG) >= MINIMUM_CONTRAST);
        assertTrue(contrastRatio(Theme.FG_MUTED, Theme.CHROME) >= MINIMUM_CONTRAST);
    }

    @Test
    void textOnTheAccentClearsAaContrast() {
        assertTrue(contrastRatio(Theme.ACCENT_FG, Theme.ACCENT) >= MINIMUM_CONTRAST);
    }

    @Test
    void headingIsSmallerThanBodyAndTitleIsLarger() {
        assertTrue(Theme.FONT_HEADING.getSize() < Theme.FONT_UI.getSize());
        assertTrue(Theme.FONT_TITLE.getSize() > Theme.FONT_UI.getSize());
        assertTrue(Theme.FONT_HEADING.isBold());
        assertTrue(Theme.FONT_TITLE.isBold());
    }

    @Test
    void spacingStepsIncrease() {
        assertTrue(Theme.XS < Theme.SM);
        assertTrue(Theme.SM < Theme.MD);
        assertTrue(Theme.MD < Theme.LG);
        assertTrue(Theme.LG < Theme.XL);
    }

    @Test
    void rowsAreDenserThanFieldsAndTheNavBar() {
        assertTrue(Theme.ROW_HEIGHT < Theme.HEADER_HEIGHT + Theme.HEADER_HEIGHT);
        assertTrue(Theme.ROW_HEIGHT < Theme.FIELD_HEIGHT);
        assertTrue(Theme.FIELD_HEIGHT < Theme.NAV_HEIGHT);
    }

    @Test
    void fontsResolveToSomethingInstalled() {
        assertNotNull(Theme.FONT_UI.getFamily());
        assertNotNull(Theme.FONT_MONO.getFamily());
    }

    @Test
    void theClassIsAConstantHolderThatCannotBeInstantiatedByAccident() throws Exception {
        assertTrue(Modifier.isFinal(Theme.class.getModifiers()));
        Constructor<Theme> constructor = Theme.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    /**
     * The WCAG 2.1 contrast ratio between two opaque colours.
     *
     * @param foreground the text colour
     * @param background the surface behind it
     * @return the ratio, between 1 and 21
     */
    private static double contrastRatio(Color foreground, Color background) {
        double lighter = Math.max(relativeLuminance(foreground), relativeLuminance(background));
        double darker = Math.min(relativeLuminance(foreground), relativeLuminance(background));
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * The WCAG relative luminance of a colour.
     *
     * @param colour the colour to measure
     * @return its luminance, between 0 and 1
     */
    private static double relativeLuminance(Color colour) {
        return 0.2126 * channel(colour.getRed())
                + 0.7152 * channel(colour.getGreen())
                + 0.0722 * channel(colour.getBlue());
    }

    /**
     * Linearises one 8-bit colour channel.
     *
     * @param value the channel value, 0 to 255
     * @return the linearised value
     */
    private static double channel(int value) {
        double scaled = value / 255.0;
        final double result;
        if (scaled <= 0.03928) {
            result = scaled / 12.92;
        }
        else {
            result = Math.pow((scaled + 0.055) / 1.055, 2.4);
        }
        return result;
    }

    @Test
    void surfacesAreDistinctSoTheChromeReadsAsChrome() {
        assertEquals(new Color(0x0A0A0A), Theme.BG);
        assertTrue(contrastRatio(Theme.RULE, Theme.BG) > 1.0);
        // Inverted against the light theme on purpose: on a near-black surface a region
        // boundary reads by emitting more light than the hairline grid, not less.
        assertTrue(relativeLuminance(Theme.RULE_STRONG) > relativeLuminance(Theme.RULE));
        // The field and stripe surfaces have to sit above the data surface to read at all,
        // but below the chrome, or a field would look like part of the window frame.
        assertTrue(relativeLuminance(Theme.FIELD_BG) > relativeLuminance(Theme.BG));
        assertTrue(relativeLuminance(Theme.ROW_ALT) > relativeLuminance(Theme.BG));
        assertTrue(relativeLuminance(Theme.CHROME) > relativeLuminance(Theme.FIELD_BG));
    }

    @Test
    void theAccentAndTheKeyColourBothClearAaContrastAgainstTheDataSurface() {
        assertTrue(contrastRatio(Theme.ACCENT, Theme.BG) >= MINIMUM_CONTRAST,
                "ACCENT on BG measured " + contrastRatio(Theme.ACCENT, Theme.BG));
        assertTrue(contrastRatio(Theme.KEY, Theme.BG) >= MINIMUM_CONTRAST,
                "KEY on BG measured " + contrastRatio(Theme.KEY, Theme.BG));
        // Both also appear on the chrome: the accent on panel bands and table headers, the
        // key colour on form labels beside a field.
        assertTrue(contrastRatio(Theme.ACCENT, Theme.CHROME) >= MINIMUM_CONTRAST);
        assertTrue(contrastRatio(Theme.KEY, Theme.CHROME) >= MINIMUM_CONTRAST);
    }

    @Test
    void directionColoursSurviveTheZebraStripeTheyAreDrawnOn() {
        // Half the rows in every table are ROW_ALT rather than BG, so contrast-checking
        // against BG alone would leave the other half unmeasured.
        assertTrue(contrastRatio(Theme.UP, Theme.ROW_ALT) >= MINIMUM_CONTRAST,
                "UP on ROW_ALT measured " + contrastRatio(Theme.UP, Theme.ROW_ALT));
        assertTrue(contrastRatio(Theme.DOWN, Theme.ROW_ALT) >= MINIMUM_CONTRAST,
                "DOWN on ROW_ALT measured " + contrastRatio(Theme.DOWN, Theme.ROW_ALT));
        assertTrue(contrastRatio(Theme.FG, Theme.FIELD_BG) >= MINIMUM_CONTRAST);
    }

    @Test
    void theHouseFaceIsMonospaceAndCarriesTheHeadingAndTitleScale() {
        // Mono-first typography: headings and titles are derived from the mono face, not the
        // prose face, so a heading sits on the same grid as the figures under it.
        assertEquals(Theme.FONT_MONO.getFamily(), Theme.FONT_HEADING.getFamily());
        assertEquals(Theme.FONT_MONO.getFamily(), Theme.FONT_TITLE.getFamily());
        assertEquals(Theme.FONT_MONO.getFamily(), Theme.FONT_MONO_BOLD.getFamily());
        assertEquals(Theme.FONT_MONO.getSize(), Theme.FONT_MONO_BOLD.getSize());
        assertTrue(Theme.FONT_MONO_BOLD.isBold());
        assertTrue(!Theme.FONT_MONO.isBold());
    }
}
