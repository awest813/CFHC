package recruiting;

import java.util.ArrayList;
import java.util.List;

public final class RecruitingPresentation {
    /** Prospect letter-grade bands on the board (not identical to {@link simulation.Team}'s roster star tiers used in {@link simulation.Team#getRecruitingClassRat()}). */
    private static final String[] STATES = {"AS","AZ","CA","HI","ID","MT","NV","OR","UT","WA","CO","KS","MO","NE","NM","ND","OK","SD","TX","WY","IL","IN","IA","KY","MD","MI","MN","OH","TN","WI","CT","DE","ME","MA","NH","NJ","NY","PA","RI","VT","AL","AK","FL","GA","LA","MS","NC","SC","VA","WV"};
    private static final int FIVE = 84;
    private static final int FOUR = 78;
    private static final int THREE = 68;
    private static final int TWO = 58;

    private RecruitingPresentation() {
    }

    public static String buildOverviewSummary(RecruitingSessionData sessionData) {
        int currentRoster = sessionData.projectedRosterSize();
        // Seniors have already left by signing day, so the old "outgoing seniors"
        // count always read 0; open roster spots are what matter now.
        int openSpots = Math.max(0, simulation.RosterRules.MAX_PLAYERS - currentRoster);
        String summary = "You currently have " + currentRoster + " active players, " + openSpots
                + (openSpots == 1 ? " open roster spot" : " open roster spots")
                + ", and a class board shaped by your biggest roster needs."
                + " Head coach recruiting (" + sessionData.coachTalent + ") adds to your signing budget and reduces scouting cost.";
        if (sessionData.isSharedPool()) {
            summary += " Every program signs from the same national pool: " + sessionData.poolCommittedAtOpen()
                    + " of " + sessionData.poolSize() + " prospects committed in the early signing period."
                    + " Contested prospects commit to their rival after a set number of your signings, so sign"
                    + " your top targets first.";
            int lost = sessionData.lostToRivals().size();
            if (lost > 0) {
                summary += " Lost to rivals so far: " + lost + ".";
            }
        }
        return summary;
    }

    /**
     * Where a national-pool prospect stands, for the board: "Committed to X",
     * "Contested: X - decides after your next signing", or null when he is open
     * with no deadline (or the board is not the national pool).
     */
    public static String statusLine(RecruitingSessionData sessionData, RecruitingPlayerRecord recruit) {
        if (sessionData == null || recruit == null) {
            return null;
        }
        String committed = sessionData.committedTo(recruit);
        if (committed != null) {
            return "Committed to " + committed;
        }
        String rival = sessionData.rivalFor(recruit);
        int left = sessionData.signingsUntilDecision(recruit);
        if (rival != null && left > 0) {
            return "Contested: " + rival + " - decides after your "
                    + (left == 1 ? "next signing" : "next " + left + " signings");
        }
        return null;
    }

    /** Short board tag: "COMMITTED" (to a rival), "Deciding" (decides on the next signing), "Contested", or "". */
    public static String statusTag(RecruitingSessionData sessionData, RecruitingPlayerRecord recruit) {
        if (sessionData == null || recruit == null) {
            return "";
        }
        if (sessionData.isCommittedElsewhere(recruit)) {
            return "COMMITTED";
        }
        int left = sessionData.signingsUntilDecision(recruit);
        return left == 1 ? "Deciding" : left > 1 ? "Contested" : "";
    }

    /** Notice after a signing: who just committed to a rival, or null when nobody did. */
    public static String buildLostProspectsNotice(RecruitingSessionData sessionData) {
        String list = buildLostProspectsList(sessionData);
        return list == null ? null : "Committed elsewhere: " + list;
    }

    /**
     * Who committed to a rival on the user's last signing, e.g.
     * "QB Sam Hill (Alabama), WR Jo Lee (Auburn)"; null when nobody did.
     */
    public static String buildLostProspectsList(RecruitingSessionData sessionData) {
        if (sessionData == null || sessionData.lastDecisions().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (RecruitingPlayerRecord r : sessionData.lastDecisions()) {
            if (shown == 3) {
                sb.append(" and ").append(sessionData.lastDecisions().size() - 3).append(" more");
                break;
            }
            if (shown > 0) sb.append(", ");
            sb.append(r.position()).append(' ').append(r.name()).append(" (")
                    .append(sessionData.committedTo(r)).append(')');
            shown++;
        }
        return sb.toString();
    }

    /** Positions still below their roster minimum, e.g. "QB 1, OL 2"; "none" when every position is covered. */
    public static String shortPositionsSummary(RecruitingSessionData sessionData) {
        RecruitingSessionData.PositionNeeds n = sessionData.calculateNeeds();
        int[] needs = {n.qbs, n.rbs, n.wrs, n.tes, n.ols, n.ks, n.dls, n.lbs, n.cbs, n.ss};
        String[] names = {"QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (needs[i] <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(names[i]).append(' ').append(needs[i]);
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }

    public static String buildBoardStatus(RecruitingSessionData sessionData) {
        int n = sessionData.availAll.size();
        if (n == 0) {
            return "Board: no prospects available";
        }
        if (sessionData.isSharedPool()) {
            return "Board: " + n + " (" + sessionData.committedElsewhereCount() + " committed)";
        }
        return "Board: " + n + " prospects";
    }

    /** Empty-filter / empty-board copy shared by Android and desktop recruiting UI. */
    public static String buildEmptyBoardMessage() {
        return "No recruits match this filter. Try All Positions or clear filters.";
    }

    public static String buildRosterText(RecruitingSessionData sessionData, RecruitingSessionData.PositionNeeds needs) {
        StringBuilder sb = new StringBuilder();
        appendPositionSection(sessionData, sb, "QBs", needs.qbs, sessionData.teamQBs, 1);
        appendPositionSection(sessionData, sb, "RBs", needs.rbs, sessionData.teamRBs, 2);
        appendPositionSection(sessionData, sb, "WRs", needs.wrs, sessionData.teamWRs, 3);
        appendPositionSection(sessionData, sb, "TEs", needs.tes, sessionData.teamTEs, 1);
        appendPositionSection(sessionData, sb, "OLs", needs.ols, sessionData.teamOLs, 5);
        appendPositionSection(sessionData, sb, "Ks", needs.ks, sessionData.teamKs, 1);
        appendPositionSection(sessionData, sb, "DLs", needs.dls, sessionData.teamDLs, 4);
        appendPositionSection(sessionData, sb, "LBs", needs.lbs, sessionData.teamLBs, 3);
        appendPositionSection(sessionData, sb, "CBs", needs.cbs, sessionData.teamCBs, 3);
        appendPositionSection(sessionData, sb, "Ss", needs.ss, sessionData.teamSs, 2);
        return sb.toString();
    }

    public static ArrayList<String> buildPositionLabels(RecruitingSessionData sessionData, RecruitingSessionData.PositionNeeds needs) {
        ArrayList<String> labels = new ArrayList<>();
        labels.add("Top 50 Recruits");
        labels.add("All Players");
        labels.add("QB " + needLabel(needs.qbs));
        labels.add("RB " + needLabel(needs.rbs));
        labels.add("WR " + needLabel(needs.wrs));
        labels.add("TE " + needLabel(needs.tes));
        labels.add("OL " + needLabel(needs.ols));
        labels.add("K " + needLabel(needs.ks));
        labels.add("DL " + needLabel(needs.dls));
        labels.add("LB " + needLabel(needs.lbs));
        labels.add("CB " + needLabel(needs.cbs));
        labels.add("S " + needLabel(needs.ss));
        labels.add("West (" + sessionData.west.size() + ")");
        labels.add("Midwest (" + sessionData.midwest.size() + ")");
        labels.add("Central (" + sessionData.central.size() + ")");
        labels.add("East (" + sessionData.east.size() + ")");
        labels.add("South (" + sessionData.south.size() + ")");
        return labels;
    }

    /**
     * Human-readable roster-need label. Negative needs mean the position is
     * already overstocked — "Need: -2" read like an error in the UI.
     */
    public static String needLabel(int need) {
        if (need > 0) return "(Need: " + need + ")";
        if (need == 0) return "(Even)";
        return "(Surplus +" + (-need) + ")";
    }


    public static String buildRecruitBoardDetails(RecruitingPlayerRecord player, String pos) {
        if (pos.equals("QB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nPass Strength: " + getGrade(player.rat1()) +
                    "\nPass Accuracy: " + getGrade(player.rat2()) +
                    "\nEvasion: " + getGrade(player.rat3()) +
                    "\nSpeed: " + getGrade(player.rat4());
        } else if (pos.equals("RB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nSpeed: " + getGrade(player.rat1()) +
                    "\nEvasion: " + getGrade(player.rat2()) +
                    "\nPower: " + getGrade(player.rat3()) +
                    "\nCatching: " + getGrade(player.rat4());
        } else if (pos.equals("WR")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nSpeed: " + getGrade(player.rat1()) +
                    "\nCatching: " + getGrade(player.rat2()) +
                    "\nEvasion: " + getGrade(player.rat3()) +
                    "\nJumping: " + getGrade(player.rat4());
        } else if (pos.equals("TE")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nRun Blk: " + getGrade(player.rat1()) +
                    "\nCatching: " + getGrade(player.rat2()) +
                    "\nEvasion: " + getGrade(player.rat3()) +
                    "\nSpeed: " + getGrade(player.rat4());
        } else if (pos.equals("OL")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nRush Blk: " + getGrade(player.rat1()) +
                    "\nPass Blk: " + getGrade(player.rat2()) +
                    "\nStrength: " + getGrade(player.rat3()) +
                    "\nVision: " + getGrade(player.rat4());
        } else if (pos.equals("K")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nKick Power: " + getGrade(player.rat1()) +
                    "\nAccuracy: " + getGrade(player.rat2()) +
                    "\nPressure: " + getGrade(player.rat3()) +
                    "\nForm: " + getGrade(player.rat4());
        } else if (pos.equals("DL")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nRun Stop: " + getGrade(player.rat1()) +
                    "\nTackling: " + getGrade(player.rat2()) +
                    "\nPass Rush: " + getGrade(player.rat3()) +
                    "\nStrength: " + getGrade(player.rat4());
        } else if (pos.equals("LB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nTackle: " + getGrade(player.rat1()) +
                    "\nRun Stop: " + getGrade(player.rat2()) +
                    "\nCoverage: " + getGrade(player.rat3()) +
                    "\nSpeed: " + getGrade(player.rat4());
        } else if (pos.equals("CB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nCoverage: " + getGrade(player.rat1()) +
                    "\nSpeed: " + getGrade(player.rat2()) +
                    "\nTackling: " + getGrade(player.rat3()) +
                    "\nJumping: " + getGrade(player.rat4());
        } else if (pos.equals("S")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nTackling: " + getGrade(player.rat1()) +
                    "\nCoverage: " + getGrade(player.rat2()) +
                    "\nSpeed: " + getGrade(player.rat3()) +
                    "\nRun Stop: " + getGrade(player.rat4());
        }

        return "Home State: " + getRegion(player.regionCode());
    }

    public static String buildPotentialDetails(RecruitingPlayerRecord recruit) {
        return buildPotentialDetails(recruit, false);
    }

    /**
     * Measurables plus the recruit's potential, which only scouting reveals
     * (scouting used to cost budget without showing anything new).
     */
    public static String buildPotentialDetails(RecruitingPlayerRecord recruit, boolean scouted) {
        String potential = scouted
                ? getGrade(Integer.toString(recruit.potential()))
                : " ? (scout to reveal)";
        return "Potential: " + potential +
                "\nHeight: " + getHeight(recruit.heightInches()) +
                "\nWeight: " + getWeight(recruit.weightPounds()) +
                "\nIntelligence: " + getGrade(Integer.toString(recruit.intelligence())) +
                "\nCharacter: " + getGrade(Integer.toString(recruit.character())) +
                "\nDurability: " + getGrade(Integer.toString(recruit.durability()));
    }

    public static String buildRecruitConfirmMessage(RecruitingSessionData sessionData, int maxPlayers, RecruitingPlayerRecord recruit) {
        int currentRoster = sessionData.projectedRosterSize();
        return "Your projected roster is at " + currentRoster + " (max: " + maxPlayers + ").\n\nRecruit "
                + recruit.stars() + "-star " + recruit.position() + " " + recruit.name()
                + " for $" + recruit.cost() + "?";
    }

    /** As {@link #buildExitConfirmMessage(List)}, noting the prospects rivals signed during the session. */
    public static String buildExitConfirmMessage(List<String> positions, RecruitingSessionData sessionData) {
        String msg = buildExitConfirmMessage(positions);
        if (sessionData != null && sessionData.isSharedPool()) {
            int lost = sessionData.lostToRivals().size();
            msg += "\nProspects still open sign with other programs when you finish."
                    + (lost > 0 ? " Rivals signed " + lost + " contested prospect" + (lost == 1 ? "" : "s")
                    + " while you recruited." : "") + "\n";
        }
        return msg;
    }

    public static String buildExitConfirmMessage(List<String> positions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Finish recruiting and sign this class? Any unfilled positions will be filled by walk-ons.\n\n");
        for (int i = 2; i < positions.size() - 5; ++i) {
            sb.append("\t\t").append(positions.get(i)).append("\n");
        }
        return sb.toString();
    }

    public static String getPlayerListLeftLabel(RecruitingPlayerRecord recruit) {
        return "$" + recruit.cost() + " " + recruit.position() + " " + recruit.name();
    }

    /** As {@link #getPlayerListLeftLabel(RecruitingPlayerRecord)}, but a committed prospect shows no price. */
    public static String getPlayerListLeftLabel(RecruitingSessionData sessionData, RecruitingPlayerRecord recruit) {
        if (sessionData != null && sessionData.isCommittedElsewhere(recruit)) {
            return "[X] " + recruit.position() + " " + recruit.name();
        }
        return getPlayerListLeftLabel(recruit);
    }

    /** As {@link #getPlayerListRightLabel(RecruitingPlayerRecord)}, with the national-pool status. */
    public static String getPlayerListRightLabel(RecruitingSessionData sessionData, RecruitingPlayerRecord recruit) {
        String committed = sessionData != null ? sessionData.committedTo(recruit) : null;
        if (committed != null) {
            return "Committed: " + committed;
        }
        String tag = statusTag(sessionData, recruit);
        // "Contested \u00b7 * * *" rather than "Contested Grade: * * *".
        return tag.isEmpty() ? getPlayerListRightLabel(recruit)
                : tag + " \u00b7 " + getStarGrade(recruit.stars());
    }

    public static String getPlayerListRightLabel(RecruitingPlayerRecord recruit) {
        return "Grade: " + getStarGrade(recruit.stars());
    }

    private static void appendPositionSection(RecruitingSessionData sessionData, StringBuilder sb, String label, int need, List<RecruitingPlayerRecord> players, int numStart) {
        sb.append(label).append(" ").append(needLabel(need)).append("\n");
        for (int i = 0; i < players.size(); ++i) {
            String readable = sessionData.getReadablePlayerInfo(players.get(i));
            sb.append("\t").append(i > numStart - 1 ? "BN" : "ST").append(" ").append(readable).append("\n");
        }
        sb.append("\n");
    }


    private static String getGrade(String num) {
        int pRat = Integer.parseInt(num);
        if (pRat > FIVE) return " * * * * *";
        if (pRat > FOUR) return " * * * *";
        if (pRat > THREE) return " * * *";
        if (pRat > TWO) return " * *";
        return " *";
    }

    private static String getStarGrade(int pRat) {
        if (pRat == 5) return " * * * * *";
        if (pRat == 4) return " * * * *  ";
        if (pRat == 3) return " * * *    ";
        if (pRat == 2) return " * *      ";
        if (pRat == 1) return " *        ";
        return "??";
    }


    private static String getRegion(int region) {
        if (region < 0 || region >= STATES.length) {
            return "Unknown";
        }
        return STATES[region];
    }

    private static String getHeight(int height) {
        int feet = height / 12;
        int inches = height % 12;
        return feet + "'" + inches + "\"";
    }

    private static String getWeight(int weight) {
        return weight + " lbs";
    }
}
