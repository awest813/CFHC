package desktop;

import org.junit.Test;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiIconsTest {

    @Test
    public void everyGlyphDrawsSomething() {
        for (UiIcons.Glyph glyph : UiIcons.Glyph.values()) {
            assertTrue(glyph + " painted nothing", inkedPixels(UiIcons.of(glyph, 16), new JLabel()) > 12);
        }
    }

    @Test
    public void iconsReportTheirSizeAndFollowTheHostColour() {
        Icon icon = UiIcons.of(UiIcons.Glyph.HOME, 20);
        assertEquals(20, icon.getIconWidth());
        assertEquals(20, icon.getIconHeight());

        JLabel host = new JLabel();
        host.setForeground(new Color(200, 30, 40));
        BufferedImage img = paint(UiIcons.of(UiIcons.Glyph.MAIL, 16), host);
        boolean sawHostColour = false;
        for (int y = 0; y < img.getHeight() && !sawHostColour; y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) == 0xFF && (argb & 0xFFFFFF) == 0xC81E28) {
                    sawHostColour = true;
                    break;
                }
            }
        }
        assertTrue("icon should paint in the label's foreground colour", sawHostColour);
    }

    private static int inkedPixels(Icon icon, JLabel host) {
        BufferedImage img = paint(icon, host);
        int inked = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) > 64) inked++;
            }
        }
        return inked;
    }

    private static BufferedImage paint(Icon icon, JLabel host) {
        BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        icon.paintIcon(host, g, 0, 0);
        g.dispose();
        return img;
    }
}
