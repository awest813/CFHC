package simulation;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import positions.Player;
import staff.Staff;

import static org.junit.Assert.*;

/**
 * Save → load → continue must be bit-identical to never saving.
 *
 * <p>League A plays from a fixed seed straight through to the end of the next
 * season's national title game, serializing a save at each probe week on the way
 * (serializing must not change the league). For each probe, league B loads that
 * save and plays to the same point. Their final fingerprints (every game score,
 * standings, award winners, news, rosters with ratings, budgets, prestige, coach
 * assignments and the RNG position) must match exactly.
 *
 * <p>Set {@code -Dcfhc.determinism.debug=true} to also compare the full save text
 * after every step and report the first step and lines that differ.
 */
public class SaveDeterminismTest {

    private static final long SEED = Long.getLong("cfhc.determinism.seed", 0x5A7EDE7L);
    private static final boolean DEBUG = Boolean.getBoolean("cfhc.determinism.debug");
    /** The recruiting gate with recruiting under way (week {@code R+13}, phase active). */
    private static final String RECRUITING = "recruiting";

    private final FileSystemResourceProvider resources =
            new FileSystemResourceProvider(System.getProperty("user.dir"));

    private League newLeague() {
        SimRandom.pinNextSeed(SEED);
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        Team user = league.getTeamList().get(0);
        user.setUserControlled(true);
        user.getHeadCoach().user = true;
        league.userTeam = user;
        return league;
    }

    private League load(byte[] save) throws Exception {
        File f = File.createTempFile("cfhc-determinism", ".cfb");
        try {
            Files.write(f.toPath(), save);
            // The same steps the app's launch path runs after a load.
            League loaded = new League(f,
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    GameUiBridge.NO_OP, true);
            loaded.setPlatformResourceProvider(resources);
            loaded.rebuildScheduleIfNeeded();
            loaded.refreshAfterLoad();
            return loaded;
        } finally {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    private static String stepKey(League l) {
        return l.getYear() + ":" + l.currentWeek + (l.recruitingPhaseActive ? "R" : "");
    }

    private static String probeKey(League l) {
        if (l.recruitingPhaseActive && SeasonFlowOrder.isRecruitingGate(l.currentWeek, l.regSeasonWeeks)) {
            return RECRUITING;
        }
        return String.valueOf(l.currentWeek);
    }

    /** The next season's title game has been played (the year rolls at the season summary). */
    private static boolean done(League l, boolean rolledOver) {
        return rolledOver && l.currentWeek == SeasonFlowOrder.seasonSummaryWeek(l.regSeasonWeeks);
    }

    /** One headless step: the recruiting gate is cleared as the app does after the user signs nobody. */
    private static void step(League l, SeasonController c) {
        if (l.recruitingPhaseActive && SeasonFlowOrder.isRecruitingGate(l.currentWeek, l.regSeasonWeeks)) {
            l.recruitPlayers();
            l.finishRecruitingSeason("");
        } else {
            c.advanceWeek();
        }
    }

    private static String save(League l) throws Exception {
        return new String(l.serializeSave(), StandardCharsets.UTF_8);
    }

    @Test
    public void saveLoadContinue_matchesUnbrokenRun() throws Exception {
        List<String> probes = List.of("3", "13", "15", "17", "18", "19", "20", "21", "22", "23", "24", "25",
                "26", "27", RECRUITING);

        // League A: one unbroken run, saving at each probe.
        League a = newLeague();
        boolean rolledOver = false;
        SeasonController ca = new SeasonController(a, GameUiBridge.NO_OP);
        Map<String, String> saves = new LinkedHashMap<>();
        Map<String, byte[]> stepSaves = new LinkedHashMap<>();
        int steps = 0;
        while (!done(a, rolledOver)) {
            assertTrue("A should finish in time", steps++ < 120);
            if (!rolledOver && probes.contains(probeKey(a)) && !saves.containsKey(probeKey(a))) {
                saves.put(probeKey(a), save(a));
            }
            if (DEBUG) {
                stepSaves.put(stepKey(a), gzip(save(a)));
            }
            step(a, ca);
            rolledOver |= a.currentWeek == 0;
        }
        assertEquals("every probe week was reached", probes.size(), saves.size());
        String expected = fingerprint(a);

        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> probe : saves.entrySet()) {
            League b = load(probe.getValue().getBytes(StandardCharsets.UTF_8));
            String firstDiff = null;
            if (DEBUG) {
                firstDiff = firstDifference("loaded " + stepKey(b), gunzip(stepSaves.get(stepKey(b))), save(b));
            }
            SeasonController cb = new SeasonController(b, GameUiBridge.NO_OP);
            steps = 0;
            boolean bRolled = false;
            while (!done(b, bRolled)) {
                assertTrue("B should finish in time", steps++ < 120);
                step(b, cb);
                bRolled |= b.currentWeek == 0;
                if (DEBUG && firstDiff == null && !done(b, bRolled)) {
                    firstDiff = firstDifference("after step to " + stepKey(b), gunzip(stepSaves.get(stepKey(b))), save(b));
                }
            }
            String actual = fingerprint(b);
            if (!expected.equals(actual)) {
                failures.add("save at " + probe.getKey() + ": "
                        + firstDifference("fingerprint", expected, actual)
                        + (firstDiff != null ? "\n    first save divergence: " + firstDiff : ""));
            } else if (firstDiff != null) {
                failures.add("save at " + probe.getKey() + ": same outcome, but the save text differs: " + firstDiff);
            }
        }
        assertTrue("reloaded runs diverged:\n" + String.join("\n", failures), failures.isEmpty());
    }

    /**
     * Debug aid, off by default: {@code -Dcfhc.dump.week=W} writes a reflective dump of
     * league A at week W of the first season and of its save reloaded, as sorted
     * lines, to {@code cfhc.dump.dir} for diffing.
     */
    @Test
    public void dumpStateAroundSave() throws Exception {
        String weeks = System.getProperty("cfhc.dump.week", "");
        org.junit.Assume.assumeFalse(weeks.isEmpty());
        File dir = new File(System.getProperty("cfhc.dump.dir", System.getProperty("java.io.tmpdir")));
        List<String> skip = List.of("df2", "df3", "symbols", "resProvider", "bridge", "gameCoachListener",
                "nameList", "lastNameList");
        List<String> wanted = List.of(weeks.split(","));
        League a = newLeague();
        SeasonController ca = new SeasonController(a, GameUiBridge.NO_OP);
        int found = 0;
        for (int steps = 0; steps < 60 && found < wanted.size(); steps++) {
            String key = probeKey(a);
            if (wanted.contains(key)) {
                found++;
                String saved = save(a);
                String rngA = SimRandom.exportState();
                writeDump(new File(dir, "dumpA-" + key + ".txt"), rngA, a, skip);
                League b = load(saved.getBytes(StandardCharsets.UTF_8));
                writeDump(new File(dir, "dumpB-" + key + ".txt"), SimRandom.exportState(), b, skip);
                Files.write(new File(dir, "saveA-" + key + ".txt").toPath(), saved.getBytes(StandardCharsets.UTF_8));
                Files.write(new File(dir, "saveB-" + key + ".txt").toPath(), b.serializeSave());
                SimRandom.restoreState(rngA);
            }
            step(a, ca);
        }
    }

    private static void writeDump(File f, String rng, League l, List<String> skip) throws Exception {
        try (java.io.Writer w = Files.newBufferedWriter(f.toPath())) {
            w.write("rng=" + rng + "\n");
            StateDump.dump(l, skip, w);
        }
    }

    private static byte[] gzip(String s) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream z = new java.util.zip.GZIPOutputStream(out)) {
            z.write(s.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private static String gunzip(byte[] b) throws java.io.IOException {
        if (b == null) {
            return null;
        }
        try (java.util.zip.GZIPInputStream z = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(b))) {
            return new String(z.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String firstDifference(String label, String a, String b) {
        if (a == null) {
            return null;
        }
        if (a.equals(b)) {
            return null;
        }
        String[] x = a.split("\n", -1);
        String[] y = b.split("\n", -1);
        StringBuilder sb = new StringBuilder(label).append(" (").append(x.length).append(" vs ")
                .append(y.length).append(" lines)");
        int shown = 0;
        for (int i = 0; i < Math.max(x.length, y.length) && shown < 6; i++) {
            String l1 = i < x.length ? x[i] : "<none>";
            String l2 = i < y.length ? y[i] : "<none>";
            if (!l1.equals(l2)) {
                sb.append("\n      line ").append(i).append("\n        A: ").append(clip(l1))
                        .append("\n        B: ").append(clip(l2));
                shown++;
            }
        }
        return sb.toString();
    }

    private static String clip(String s) {
        return s.length() > 400 ? s.substring(0, 400) + "..." : s;
    }

    /** Everything a player can see or the sim reads, in a stable order. */
    static String fingerprint(League l) {
        StringBuilder sb = new StringBuilder();
        sb.append("year=").append(l.getYear()).append(" week=").append(l.currentWeek)
                .append(" rng=").append(SimRandom.exportState()).append('\n');
        sb.append("champ=").append(l.nationalChampionName).append('\n');
        for (Team t : l.getTeamList()) {
            sb.append("T ").append(t.getName()).append('|').append(t.getConference()).append('|')
                    .append(t.getDivision()).append("|pres=").append(t.getTeamPrestige())
                    .append("|budget=").append(t.teamBudget).append("|fac=").append(t.teamFacilities)
                    .append("|disc=").append(t.teamDisciplineScore)
                    .append("|wl=").append(t.getWins()).append('-').append(t.getLosses())
                    .append("|pts=").append(t.teamPoints).append("|oppPts=").append(t.teamOppPoints)
                    .append("|rank=").append(t.getRankTeamPollScore())
                    .append("|poll=").append(t.getTeamPollScore())
                    .append("|conf=").append(t.getConfWins()).append('\n');
            sb.append("  coaches ").append(coach(t.getHeadCoach())).append(" / ").append(coach(t.getOC()))
                    .append(" / ").append(coach(t.getDC())).append('\n');
            for (Game g : t.getGameSchedule()) {
                sb.append("  G ").append(g == null ? "null" : g.gameName + ":" + g.homeTeam.getName() + " "
                        + g.homeScore + "-" + g.awayScore + " " + g.awayTeam.getName() + (g.hasPlayed ? "" : " (unplayed)"))
                        .append('\n');
            }
            for (Player p : t.getAllPlayers()) {
                sb.append("  P ").append(p.position).append(' ').append(p.name).append(" yr=").append(p.year)
                        .append(" ovr=").append(p.ratOvr).append(" pot=").append(p.ratPot)
                        .append(" attrs=").append(p.ratAttr1).append(',').append(p.ratAttr2).append(',')
                        .append(p.ratAttr3).append(',').append(p.ratAttr4)
                        .append(" awards=").append(p.wonHeisman ? "H" : "").append(p.wonAllAmerican ? "A" : "")
                        .append(p.wonAllConference ? "C" : "").append(p.wonTopFreshman ? "F" : "")
                        .append(p.wonAllFreshman ? "f" : "")
                        .append(" stats=").append(java.util.Arrays.toString(p.stats)).append('\n');
            }
        }
        sb.append("awards heisman=").append(name(l.heisman)).append(" def=").append(name(l.defPOTY))
                .append(" line=").append(name(l.linemanPOTY)).append(" fr=").append(name(l.freshman))
                .append(" coach=").append(l.coachWinner == null ? "-" : l.coachWinner.name).append('\n');
        sb.append(l.getHeismanCeremonyStr()).append('\n').append(l.getDefensePOTYStr()).append('\n')
                .append(l.getLinemanPOTYStr()).append('\n').append(l.getFreshmanCeremonyStr()).append('\n')
                .append(l.getCoachAwardStr()).append('\n').append(l.getAllAmericanStr()).append('\n')
                .append(l.getAllFreshmanStr()).append('\n');
        for (int c = 0; c < l.getConferences().size(); c++) {
            sb.append(l.getAllConfStr(c)).append('\n');
        }
        int week = 0;
        for (List<String> stories : l.getNewsStories()) {
            for (String s : stories) {
                sb.append("N").append(week).append(' ').append(s).append('\n');
            }
            week++;
        }
        week = 0;
        for (List<String> scores : l.getWeeklyScores()) {
            for (String s : scores) {
                sb.append("S").append(week).append(' ').append(s).append('\n');
            }
            week++;
        }
        sb.append("history=").append(l.getLeagueHistoryStr()).append('\n');
        sb.append("records=").append(l.getLeagueRecordsStr()).append('\n');
        return sb.toString();
    }

    private static String name(Player p) {
        return p == null ? "-" : p.name + "/" + (p.team != null ? p.team.getName() : "");
    }

    private static String coach(Staff s) {
        return s == null ? "-" : s.name + " " + s.age + " " + s.ratOff + "," + s.ratDef + "," + s.ratTalent + ","
                + s.ratDiscipline + " pres=" + s.ratOvr;
    }
}
