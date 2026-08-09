package view;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Installs the application look and feel and pushes {@link Theme} into the
 * {@code UIManager} defaults.
 *
 * <p>Order matters: this must run on the event dispatch thread and complete
 * before the first Swing component is constructed, otherwise already-built
 * components keep Metal's bevels and gradients. Metal cannot be styled flat at
 * the component level, so every rule in the design system depends on this
 * running first.
 *
 * <p>Setting the defaults globally is what lets the individual views stay free
 * of styling code: row heights, grid colours, focus width, and corner radius are
 * decided once here rather than per component.
 */
public final class LookAndFeel {

    private static final Logger LOGGER = Logger.getLogger(LookAndFeel.class.getName());

    /** Focus ring thickness, in pixels. */
    private static final int FOCUS_WIDTH = 1;

    /** Corner radius for buttons, fields, and components. Zero removes FlatLaf's default pill shape. */
    private static final int ARC = 0;

    /** Scrollbar thickness, in pixels. */
    private static final int SCROLLBAR_WIDTH = 10;

    private LookAndFeel() {
    }

    /**
     * Installs the look and feel, blocking until it is in place. Safe to call from
     * any thread: when called off the event dispatch thread the work is marshalled
     * onto it and awaited, so the caller may construct components immediately
     * afterwards.
     *
     * <p>A look and feel that will not load must never stop the app from starting,
     * so every failure degrades to the platform default and is logged.
     */
    public static void install() {
        if (SwingUtilities.isEventDispatchThread()) {
            installOnEventThread();
        }
        else {
            try {
                SwingUtilities.invokeAndWait(LookAndFeel::installOnEventThread);
            }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interrupted while installing the look and feel.", interrupted);
            }
            catch (InvocationTargetException failure) {
                LOGGER.log(Level.WARNING, "Could not install the look and feel.", failure);
            }
        }
    }

    private static void installOnEventThread() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        }
        // LinkageError, not just the checked exception: if flatlaf.jar is absent from the
        // runtime classpath the failure is a NoClassDefFoundError at this line, which a catch
        // of UnsupportedLookAndFeelException alone would let kill the app before the window
        // ever appears. A missing look and feel must only cost us the styling.
        catch (UnsupportedLookAndFeelException | LinkageError failure) {
            LOGGER.log(Level.WARNING, "FlatLaf unavailable; falling back to the system look and feel.", failure);
            installSystemFallback();
        }
        applyThemeDefaults();
    }

    private static void installSystemFallback() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ReflectiveOperationException | UnsupportedLookAndFeelException failure) {
            LOGGER.log(Level.WARNING, "System look and feel unavailable; keeping the default.", failure);
        }
    }

    /**
     * Pushes Theme tokens into the shared defaults. Anything set here is one thing
     * the views do not have to set on every component they build.
     */
    private static void applyThemeDefaults() {
        UIManager.put("defaultFont", Theme.FONT_UI);

        UIManager.put("Table.rowHeight", Theme.ROW_HEIGHT);
        UIManager.put("Table.gridColor", Theme.RULE);
        UIManager.put("Table.background", Theme.BG);
        UIManager.put("Table.foreground", Theme.FG);
        UIManager.put("Table.selectionBackground", Theme.ACCENT);
        UIManager.put("Table.selectionForeground", Theme.ACCENT_FG);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 0));
        UIManager.put("TableHeader.background", Theme.CHROME);
        UIManager.put("TableHeader.foreground", Theme.FG_MUTED);
        UIManager.put("TableHeader.separatorColor", Theme.RULE);
        UIManager.put("TableHeader.height", Theme.HEADER_HEIGHT);

        UIManager.put("Component.focusWidth", FOCUS_WIDTH);
        UIManager.put("Component.focusColor", Theme.ACCENT);
        UIManager.put("Component.borderColor", Theme.RULE);
        UIManager.put("Component.arc", ARC);
        UIManager.put("Button.arc", ARC);
        UIManager.put("TextComponent.arc", ARC);
        UIManager.put("ProgressBar.arc", ARC);
        UIManager.put("CheckBox.arc", ARC);

        UIManager.put("ScrollBar.width", SCROLLBAR_WIDTH);
        UIManager.put("ScrollBar.showButtons", Boolean.FALSE);

        UIManager.put("Panel.background", Theme.BG);
        UIManager.put("TextField.background", Theme.BG);
        UIManager.put("TextArea.background", Theme.BG);
        UIManager.put("Label.foreground", Theme.FG);
        UIManager.put("SplitPane.background", Theme.RULE);
        UIManager.put("SplitPaneDivider.gripColor", Theme.RULE);
    }
}
