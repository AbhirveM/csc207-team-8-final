package view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The single source of visual constants for the Swing layer.
 *
 * <p>The look is "terminal restraint" in its Bloomberg register: near-black surfaces
 * carry the design, one amber accent marks selection, focus, and section titles, cyan
 * names the fields and symbols the user reads values off, numbers are monospace and
 * right-aligned, and structure comes from alignment and one-pixel rules rather than
 * cards or shadows.
 *
 * <p>No class in {@code view} may hardcode a {@link Color}, a {@link Font}, or a
 * pixel gap. If a value is missing here, add it here. {@code UP} and {@code DOWN}
 * are contrast-checked against {@code BG} but are never load-bearing: every signed
 * number also carries an explicit {@code +} or {@code -}, so the meaning survives
 * for a user who cannot distinguish the two colours.
 */
public final class Theme {

    /** Reports a font that could not be loaded; the app still runs on the fallback family. */
    private static final Logger LOGGER = Logger.getLogger(Theme.class.getName());

    // --- Surfaces ---

    /** The data surface. Near-black, so figures read as light on dark. */
    public static final Color BG = new Color(0x0A0A0A);

    /** Input fields, one step above {@link #BG} so a field reads as a well rather than a void. */
    public static final Color FIELD_BG = new Color(0x121212);

    /** Zebra striping on odd table rows. */
    public static final Color ROW_ALT = new Color(0x101013);

    /** Nav bar, status bar, table headers, and panel header bands. */
    public static final Color CHROME = new Color(0x141414);

    /** 1px separators, table grid lines, and field borders. */
    public static final Color RULE = new Color(0x2A2A2A);

    /**
     * Boundaries between major regions. Brighter than {@link #RULE} rather than darker:
     * on a dark surface a region boundary reads by emitting more light, not less.
     */
    public static final Color RULE_STRONG = new Color(0x3D3D3D);

    // --- Text ---

    /** Primary text and values. */
    public static final Color FG = new Color(0xE8E8E8);

    /** Secondary text, units, and metadata. */
    public static final Color FG_MUTED = new Color(0x9AA0A6);

    /** Disabled text and placeholders. */
    public static final Color FG_FAINT = new Color(0x6B7075);

    // --- State ---

    /** The one accent: selection, focus, the active screen, and panel titles. Never decoration. */
    public static final Color ACCENT = new Color(0xFF9E1B);

    /** Text drawn on top of {@link #ACCENT}. Near-black, because the accent is bright. */
    public static final Color ACCENT_FG = new Color(0x0A0A0A);

    /**
     * Cyan for field labels and ticker symbols: it names what a value is, where
     * {@link #ACCENT} marks state. It is a second hue but not a second accent - it never
     * marks selection, focus, or the active screen.
     */
    public static final Color KEY = new Color(0x4FC3F7);

    /** Positive change. Always paired with an explicit {@code +}. */
    public static final Color UP = new Color(0x26A65B);

    /** Negative change. Always paired with an explicit {@code -}. */
    public static final Color DOWN = new Color(0xE5484D);

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

    /** Classpath location of the bundled monospace family. */
    private static final String FONT_DIRECTORY = "/fonts/";

    // The bundled family has to be registered before the FONT_* initialisers below run,
    // because they resolve a family by name against the graphics environment. Static
    // initialisers run in source order, so this block must stay above them.
    static {
        registerBundledFont("JetBrainsMono-Regular.ttf");
        registerBundledFont("JetBrainsMono-Bold.ttf");
    }

    /** Prose labels: form labels, instructions, and words substituted into a figures column. */
    public static final Font FONT_UI = firstAvailable(
            Font.PLAIN, BASE_SIZE, Font.SANS_SERIF, "Segoe UI", "SF Pro Text", "Inter");

    /** The house face. Table cells, buttons, headings, titles, and every readout. */
    public static final Font FONT_MONO = firstAvailable(
            Font.PLAIN, BASE_SIZE, Font.MONOSPACED, "JetBrains Mono", "Consolas", "Menlo");

    /** The house face at body size, in bold. Marks the active screen. */
    public static final Font FONT_MONO_BOLD = FONT_MONO.deriveFont(Font.BOLD);

    /** Section headings and panel bands. Used with uppercase text and {@link #ACCENT}. */
    public static final Font FONT_HEADING = FONT_MONO.deriveFont(Font.BOLD, HEADING_SIZE);

    /** Screen titles and the wordmark. */
    public static final Font FONT_TITLE = FONT_MONO.deriveFont(Font.BOLD, TITLE_SIZE);

    private Theme() {
    }

    /**
     * Registers one bundled TrueType face with the graphics environment so
     * {@link #firstAvailable} can find it by family name.
     *
     * <p>The font ships as a resource rather than as an installed system family so the
     * texture is identical on the development machine, the demo machine, and the marker's.
     * A failure here is not fatal and must not be: the family lookup below simply falls
     * through to the next candidate, which is what happened before the font was bundled.
     *
     * @param fileName the file name under {@code /fonts/} on the classpath
     */
    private static void registerBundledFont(String fileName) {
        try (InputStream stream = Theme.class.getResourceAsStream(FONT_DIRECTORY + fileName)) {
            if (stream == null) {
                LOGGER.log(Level.WARNING, "Bundled font missing from the classpath: {0}", fileName);
            }
            else {
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .registerFont(Font.createFont(Font.TRUETYPE_FONT, stream));
            }
        }
        catch (IOException | FontFormatException | HeadlessException failure) {
            LOGGER.log(Level.WARNING, "Could not register the bundled font " + fileName, failure);
        }
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
