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
        int graduatingCount = sessionData.playersGraduating.size();
        return "You currently have " + currentRoster + " active players, " + graduatingCount + " outgoing seniors, and a class board shaped by your biggest roster needs."
                + " Head coach recruiting (" + sessionData.coachTalent + ") adds to your signing budget and reduces scouting cost.";
    }

    public static String buildBoardStatus(RecruitingSessionData sessionData) {
        int n = sessionData.availAll.size();
        if (n == 0) {
            return "Board: no prospects available";
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
        return "Height: " + getHeight(recruit.heightInches()) +
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
