package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/**
 * The single source of visual constants for the Swing layer.
 *
 * <p>The look is "terminal restraint": neutral greys carry the design, one navy
 * accent marks selection and focus, numbers are monospace and right-aligned, and
 * structure comes from alignment and one-pixel rules rather than cards or shadows.
 *
 * <p>No class in {@code view} may hardcode a {@link Color}, a {@link Font}, or a
 * pixel gap. If a value is missing here, add it here. {@code UP} and {@code DOWN}
 * are contrast-checked against {@code BG} but are never load-bearing: every signed
 * number also carries an explicit {@code +} or {@code -}, so the meaning survives
 * for a user who cannot distinguish the two colours.
 */
public final class Theme {

    // --- Surfaces ---

    /** Data surfaces and input fields. */
    public static final Color BG = new Color(0xFFFFFF);

    /** Nav bar, status bar, and table headers. */
    public static final Color CHROME = new Color(0xF4F5F7);

    /** 1px separators, table grid lines, and field borders. */
    public static final Color RULE = new Color(0xD8DBE0);

    /** Boundaries between major regions. */
    public static final Color RULE_STRONG = new Color(0xB4B9C2);

    // --- Text ---

    /** Primary text. */
    public static final Color FG = new Color(0x1A1D21);

    /** Secondary text, section headings, and units. */
    public static final Color FG_MUTED = new Color(0x5C636E);

    /** Disabled text and placeholders. */
    public static final Color FG_FAINT = new Color(0x8A919C);

    // --- State ---

    /** The one accent: selection background and focus ring. Never decoration. */
    public static final Color ACCENT = new Color(0x1F5FAD);

    /** Text drawn on top of {@link #ACCENT}. */
    public static final Color ACCENT_FG = new Color(0xFFFFFF);

    /** Positive change. Always paired with an explicit {@code +}. */
    public static final Color UP = new Color(0x0B7A4B);

    /** Negative change. Always paired with an explicit {@code -}. */
    public static final Color DOWN = new Color(0xA81E1E);

    // --- Spacing ---

    /** Hairline spacing, 4px. */
    public static final int XS = 4;

    /** Tight spacing between related controls, 8px. */
    public static final int SM = 8;

    /** Default spacing between siblings, 12px. */
    public static final int MD = 12;

    /** Outer margin on a view root, 16px. */
    public static final int LG = 16;

    /** Separation between unrelated regions, 24px. */
    public static final int XL = 24;

    // --- Metrics ---

    /** Table row height. Dense on purpose. */
    public static final int ROW_HEIGHT = 22;

    /** Table header height. */
    public static final int HEADER_HEIGHT = 24;

    /** Height of a single-line text field or combo box. */
    public static final int FIELD_HEIGHT = 26;

    /** Height of the top navigation bar. */
    public static final int NAV_HEIGHT = 40;

    // --- Type ---

    /** Base point size for all text. */
    private static final int BASE_SIZE = 13;

    /** Point size for section headings. */
    private static final int HEADING_SIZE = 11;

    /** Point size for screen titles. */
    private static final int TITLE_SIZE = 15;

    /** Labels, buttons, and fields. */
    public static final Font FONT_UI = firstAvailable(
            Font.PLAIN, BASE_SIZE, Font.SANS_SERIF, "Segoe UI", "SF Pro Text", "Inter");

    /** Numeric columns and any monospace readout. */
    public static final Font FONT_MONO = firstAvailable(
            Font.PLAIN, BASE_SIZE, Font.MONOSPACED, "JetBrains Mono", "Consolas", "Menlo");

    /** Section headings. Used with uppercase text and {@link #FG_MUTED}. */
    public static final Font FONT_HEADING = FONT_UI.deriveFont(Font.BOLD, HEADING_SIZE);

    /** Screen titles and the wordmark. */
    public static final Font FONT_TITLE = FONT_UI.deriveFont(Font.BOLD, TITLE_SIZE);

    private Theme() {
    }

    /**
     * Builds a font from the first of {@code preferred} that is actually installed,
     * falling back to {@code fallbackFamily} when none of them are. Font choice has
     * to degrade rather than fail: the demo machine, the marking machine, and CI all
     * ship different families.
     *
     * @param style the {@link Font} style constant to apply
     * @param size the point size to apply
     * @param fallbackFamily the logical family to use when no preferred family exists
     * @param preferred candidate family names, most preferred first
     * @return a font in the best available family
     */
    private static Font firstAvailable(int style, int size, String fallbackFamily, String... preferred) {
        List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String family : preferred) {
            if (installed.contains(family)) {
                return new Font(family, style, size);
            }
        }
        return new Font(fallbackFamily, style, size);
    }
}
