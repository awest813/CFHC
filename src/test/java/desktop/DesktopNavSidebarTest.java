package desktop;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DesktopNavSidebarTest {

    private static final String[] TITLES = {
            "Home", "Recruiting", "Standings", "Poll Rankings", "League History", "News"
    };
    private static final UiIcons.Glyph[] ICONS = {
            UiIcons.Glyph.HOME, UiIcons.Glyph.RECRUITING, UiIcons.Glyph.STANDINGS,
            UiIcons.Glyph.PODIUM, UiIcons.Glyph.HISTORY, UiIcons.Glyph.NEWS
    };

    @Test
    public void setSelected_movesHighlightWithoutNotifying() {
        DesktopTheme.load();
        AtomicReference<String> picked = new AtomicReference<>();
        DesktopNavSidebar nav = new DesktopNavSidebar(TITLES, ICONS, picked::set);

        assertEquals("Home", nav.selectedTitle());
        nav.setSelected("Poll Rankings");

        assertEquals("Poll Rankings", nav.selectedTitle());
        assertNull(picked.get());
    }

    @Test
    public void activate_notifiesListenerAndKeepsBadges() {
        DesktopTheme.load();
        AtomicReference<String> picked = new AtomicReference<>();
        DesktopNavSidebar nav = new DesktopNavSidebar(TITLES, ICONS, picked::set);

        nav.setBadge("Recruiting", "14");
        nav.setBadge("News", "3");
        nav.setBadge("News", "  ");
        nav.activate("Recruiting");

        assertEquals("Recruiting", picked.get());
        assertEquals("Recruiting", nav.selectedTitle());
        assertEquals("14", nav.badge("Recruiting"));
        assertNull(nav.badge("News"));
    }

    @Test
    public void setFooter_acceptsProgramSealText() {
        DesktopTheme.load();
        DesktopNavSidebar nav = new DesktopNavSidebar(TITLES, ICONS, title -> { });
        nav.setFooter("PV", "Pine Valley State", "5-2  ·  Valley");
        nav.setSelected("Missing");
        assertEquals("Home", nav.selectedTitle());
    }
}
