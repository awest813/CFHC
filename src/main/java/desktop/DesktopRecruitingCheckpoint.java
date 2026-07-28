package desktop;

import recruiting.RecruitingSessionData;
import simulation.League;
import simulation.PlatformLog;
import simulation.SimulationFacade;
import simulation.Team;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sidecar checkpoint so docked recruiting progress survives app restarts.
 * Stored next to the league save when possible, otherwise under {@link DesktopAppPaths#savesDir()}.
 */
final class DesktopRecruitingCheckpoint {
    private static final String TAG = "DesktopRecruitingCheckpoint";
    private static final String MAGIC = "CFHC_RECRUITING_CHECKPOINT_v1";

    final int year;
    final String teamName;
    final int week;
    final int budget;
    final String boardPayload;
    final List<String> recruitedRaws;

    DesktopRecruitingCheckpoint(int year, String teamName, int week, int budget,
                                String boardPayload, List<String> recruitedRaws) {
        this.year = year;
        this.teamName = teamName != null ? teamName : "";
        this.week = week;
        this.budget = budget;
        this.boardPayload = boardPayload != null ? boardPayload : "";
        this.recruitedRaws = recruitedRaws != null ? recruitedRaws : new ArrayList<>();
    }

    static File pathFor(File leagueSave, League league) {
        if (leagueSave != null) {
            return new File(leagueSave.getAbsolutePath() + ".recruiting");
        }
        String team = league != null && league.userTeam != null
                ? league.userTeam.getName().replaceAll("\\s+", "_")
                : "team";
        int year = league != null ? league.getYear() : 0;
        return new File(DesktopAppPaths.chooserStartDir(),
                "recruiting-" + team + "-" + year + ".chk");
    }

    static DesktopRecruitingCheckpoint capture(League league, String boardPayload, RecruitingSessionData session) {
        if (league == null || session == null || boardPayload == null || boardPayload.isEmpty()) {
            return null;
        }
        String team = league.userTeam != null ? league.userTeam.getName() : session.teamName;
        List<String> recruited = new ArrayList<>();
        for (recruiting.RecruitingPlayerRecord p : session.playersRecruited) {
            if (p != null && p.raw() != null) {
                recruited.add(p.raw());
            }
        }
        return new DesktopRecruitingCheckpoint(
                league.getYear(),
                team,
                league.currentWeek,
                session.recruitingBudget,
                boardPayload,
                recruited);
    }

    static void write(File target, DesktopRecruitingCheckpoint checkpoint) throws IOException {
        if (target == null || checkpoint == null) {
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(target))) {
            w.write(MAGIC);
            w.newLine();
            w.write("year=" + checkpoint.year);
            w.newLine();
            w.write("team=" + checkpoint.teamName);
            w.newLine();
            w.write("week=" + checkpoint.week);
            w.newLine();
            w.write("budget=" + checkpoint.budget);
            w.newLine();
            w.write("---BOARD---");
            w.newLine();
            w.write(checkpoint.boardPayload);
            if (!checkpoint.boardPayload.endsWith("\n")) {
                w.newLine();
            }
            w.write("---RECRUITED---");
            w.newLine();
            for (String raw : checkpoint.recruitedRaws) {
                w.write(raw);
                w.newLine();
            }
            w.write("---END---");
            w.newLine();
        }
    }

    static DesktopRecruitingCheckpoint read(File source) throws IOException {
        if (source == null || !source.isFile()) {
            return null;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(source))) {
            String magic = r.readLine();
            if (magic == null || !MAGIC.equals(magic.trim())) {
                throw new IOException("Unrecognized recruiting checkpoint");
            }
            int year = 0;
            String team = "";
            int week = 0;
            int budget = 0;
            String line;
            while ((line = r.readLine()) != null) {
                if ("---BOARD---".equals(line.trim())) {
                    break;
                }
                if (line.startsWith("year=")) year = Integer.parseInt(line.substring(5).trim());
                else if (line.startsWith("team=")) team = line.substring(5);
                else if (line.startsWith("week=")) week = Integer.parseInt(line.substring(5).trim());
                else if (line.startsWith("budget=")) budget = Integer.parseInt(line.substring(7).trim());
            }
            StringBuilder board = new StringBuilder();
            while ((line = r.readLine()) != null) {
                if ("---RECRUITED---".equals(line.trim())) {
                    break;
                }
                board.append(line).append('\n');
            }
            List<String> recruited = new ArrayList<>();
            while ((line = r.readLine()) != null) {
                if ("---END---".equals(line.trim())) {
                    break;
                }
                if (!line.trim().isEmpty()) {
                    recruited.add(line.trim());
                }
            }
            return new DesktopRecruitingCheckpoint(year, team, week, budget, board.toString(), recruited);
        }
    }

    static void clear(File target) {
        if (target != null && target.isFile() && !target.delete()) {
            PlatformLog.w(TAG, "Could not delete recruiting checkpoint: " + target.getAbsolutePath());
        }
    }

    boolean matches(League league) {
        if (league == null) return false;
        if (league.getYear() != year) return false;
        Team user = league.userTeam;
        if (user == null) return false;
        return teamName.equals(user.getName());
    }

    RecruitingSessionData restoreSession() {
        RecruitingSessionData session = SimulationFacade.prepareRecruitingSessionFromPayload(boardPayload);
        session.applyCheckpoint(budget, recruitedRaws);
        return session;
    }
}
