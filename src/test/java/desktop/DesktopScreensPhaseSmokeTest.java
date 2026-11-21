package desktop;

import org.junit.Test;
import simulation.GameUiBridge;
import simulation.League;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;
import simulation.Team;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Headless smoke test for the desktop shell: every sidebar screen (and the docked
 * recruiting board) builds without throwing at each season phase, from preseason
 * through the offseason ladder to the recruiting gate, and none of their visible
 * text carries debug-looking fragments ("null", "NaN", "(s)" plurals, build stamps).
 * Table headers are rendered too, which exercises the column-aligned header renderer.
 */
public class DesktopScreensPhaseSmokeTest {

    private static final Pattern DEBUG_TEXT = Pattern.compile(
            "\\bnull\\b|\\bNaN\\b|\\(s\\)|REDESIGN|TODO|FIXME", Pattern.CASE_INSENSITIVE);

    @Test
    public void everyScreenBuildsCleanlyAtEverySeasonPhase() {
        PlatformLog.setDebugEnabled(false);
        DesktopTheme.load();
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        Team user = league.getTeamList().get(0);
        user.setupUserCoach("Smoke Coach");
        league.userTeam = user;
        user.setUserControlled(true);
        league.careerMode = true;

        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        DesktopUiBridge bridge = new DesktopUiBridge(null, league);
        int r = league.regSeasonWeeks;
        // Preseason, opening week, mid-season, CCG week, bowls, NCG, every offseason
        // step, and the recruiting gate.
        int[] stops = {0, 1, 6, r - 1, r, r + 2, r + 3, r + 4, r + 5, r + 6, r + 7, r + 8,
                r + 9, r + 10, r + 11, r + 12, r + 13};
        List<String> failures = new ArrayList<>();
        int screensBuilt = 0;
        for (int stop : stops) {
            int guard = 0;
            while (league.currentWeek < stop && guard++ < 40) {
                controller.advanceWeek();
            }
            Map<String, Team> teamMap = new HashMap<>();
            for (Team t : league.getTeamList()) {
                teamMap.put(t.getName(), t);
            }
            LeagueScreenContext ctx = new LeagueScreenContext(league, league.toRecord(), teamMap,
                    null, bridge, null, null);
            for (Map.Entry<String, LeagueScreen> e : screens(league, bridge).entrySet()) {
                String where = e.getKey() + " @ week " + league.currentWeek;
                try {
                    JPanel panel = e.getValue().build(ctx);
                    layOut(panel);
                    checkText(panel, where, failures);
                    screensBuilt++;
                } catch (RuntimeException ex) {
                    failures.add(where + " threw " + ex);
                }
            }
            String where = "Recruiting board @ week " + league.currentWeek;
            try {
                RecruitingPanel board = new RecruitingPanel(league, data -> { });
                layOut(board);
                checkText(board, where, failures);
            } catch (RuntimeException ex) {
                failures.add(where + " threw " + ex);
            }
        }
        if (!failures.isEmpty()) {
            fail(failures.size() + " problems:\n" + String.join("\n", failures));
        }
        assertTrue("expected every screen at every stop", screensBuilt >= stops.length * 14);
    }

    private static Map<String, LeagueScreen> screens(League league, DesktopUiBridge bridge) {
        Runnable noop = () -> { };
        Map<String, LeagueScreen> m = new LinkedHashMap<>();
        m.put("Home", new DashboardPanel(league, bridge, new DashboardPanel.Callbacks(
                noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop, noop)));
        m.put("Scoreboard", new ScoreboardPanel());
        m.put("News", new NewsPanel());
        m.put("Player Search", new PlayerSearchPanel());
        m.put("Standings", new StandingsPanel());
        m.put("Poll Rankings", new PollRankingsPanel());
        m.put("Team Rankings", new TeamRankingsPanel());
        m.put("Player Stats", new PlayerStatsPanel());
        m.put("League History", new LeagueHistoryPanel());
        m.put("Coaches", new CoachDatabasePanel());
        m.put("Hall of Fame", new HallOfFamePanel());
        m.put("Records", new LeagueRecordsPanel());
        m.put("My Coach", new CoachProfilePanel());
        m.put("Settings", new SettingsPanel());
        return m;
    }

    private static void layOut(Container c) {
        c.setSize(1200, 800);
        c.doLayout();
        c.validate();
    }

    private static void checkText(Container root, String where, List<String> failures) {
        for (Component c : root.getComponents()) {
            String text = null;
            if (c instanceof JLabel l) {
                text = l.getText();
            } else if (c instanceof AbstractButton b) {
                text = b.getText();
            } else if (c instanceof JTable t) {
                checkTable(t, where, failures);
            }
            if (text != null && DEBUG_TEXT.matcher(text).find()) {
                failures.add(where + " shows \"" + text + "\"");
            }
            if (c instanceof Container inner) {
                checkText(inner, where, failures);
            }
        }
    }

    private static void checkTable(JTable t, String where, List<String> failures) {
        if (t.getTableHeader() != null) {
            TableCellRenderer header = t.getTableHeader().getDefaultRenderer();
            for (int col = 0; col < t.getColumnCount(); col++) {
                header.getTableCellRendererComponent(t, t.getColumnName(col), false, false, -1, col);
            }
        }
        int rows = Math.min(t.getRowCount(), 40);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < t.getColumnCount(); col++) {
                Object v = t.getValueAt(row, col);
                if (v instanceof String s && DEBUG_TEXT.matcher(s).find()) {
                    failures.add(where + " table cell [" + row + "," + col + "] = \"" + s + "\"");
                }
            }
        }
    }
}
