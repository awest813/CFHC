package recruiting;

import java.util.List;

public final class RecruitingPresentation {
    private static final String[] STATES = {"AS","AZ","CA","HI","ID","MT","NV","OR","UT","WA","CO","KS","MO","NE","NM","ND","OK","SD","TX","WY","IL","IN","IA","KY","MD","MI","MN","OH","TN","WI","CT","DE","ME","MA","NH","NJ","NY","PA","RI","VT","AL","AK","FL","GA","LA","MS","NC","SC","VA","WV"};
    private static final int FIVE = 84;
    private static final int FOUR = 78;
    private static final int THREE = 68;
    private static final int TWO = 58;

    private RecruitingPresentation() {
    }

    public static String buildOverviewSummary(RecruitingSessionData sessionData) {
        int currentRoster = sessionData.teamPlayers.size() + sessionData.playersRecruited.size();
        int graduatingCount = sessionData.playersGraduating.size();
        return "You currently have " + currentRoster + " active players, " + graduatingCount + " outgoing seniors, and a class board shaped by your biggest roster needs.";
    }

    public static String buildBoardStatus(RecruitingSessionData sessionData) {
        return "Board: " + sessionData.availAll.size() + " prospects";
    }

    public static String buildRosterText(RecruitingSessionData sessionData, RecruitingSessionData.PositionNeeds needs) {
        StringBuilder sb = new StringBuilder();
        appendPositionSection(sb, "QBs", needs.qbs, sessionData.teamQBs, 1);
        appendPositionSection(sb, "RBs", needs.rbs, sessionData.teamRBs, 2);
        appendPositionSection(sb, "WRs", needs.wrs, sessionData.teamWRs, 3);
        appendPositionSection(sb, "TEs", needs.tes, sessionData.teamTEs, 1);
        appendPositionSection(sb, "OLs", needs.ols, sessionData.teamOLs, 5);
        appendPositionSection(sb, "Ks", needs.ks, sessionData.teamKs, 1);
        appendPositionSection(sb, "DLs", needs.dls, sessionData.teamDLs, 4);
        appendPositionSection(sb, "LBs", needs.lbs, sessionData.teamLBs, 3);
        appendPositionSection(sb, "CBs", needs.cbs, sessionData.teamCBs, 3);
        appendPositionSection(sb, "Ss", needs.ss, sessionData.teamSs, 2);
        return sb.toString();
    }

    public static String buildRecruitBoardDetails(RecruitingPlayerRecord player, String pos) {
        if (pos.equals("QB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nPass Strength: " + getGrade(player.field(13)) +
                    "\nPass Accuracy: " + getGrade(player.field(14)) +
                    "\nEvasion: " + getGrade(player.field(15)) +
                    "\nSpeed: " + getGrade(player.field(16));
        } else if (pos.equals("RB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nSpeed: " + getGrade(player.field(13)) +
                    "\nEvasion: " + getGrade(player.field(14)) +
                    "\nPower:" + getGrade(player.field(15)) +
                    "\nCatching: " + getGrade(player.field(16));
        } else if (pos.equals("WR")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nSpeed: " + getGrade(player.field(13)) +
                    "\nCatching: " + getGrade(player.field(14)) +
                    "\nEvasion: " + getGrade(player.field(15)) +
                    "\nJumping: " + getGrade(player.field(16));
        } else if (pos.equals("TE")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nRun Blk: " + getGrade(player.field(13)) +
                    "\nCatching: " + getGrade(player.field(14)) +
                    "\nEvasion: " + getGrade(player.field(15)) +
                    "\nSpeed: " + getGrade(player.field(16));
        } else if (pos.equals("OL")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nRush Blk: " + getGrade(player.field(13)) +
                    "\nPass Blk: " + getGrade(player.field(14)) +
                    "\nStrength: " + getGrade(player.field(15)) +
                    "\nVision: " + getGrade(player.field(16));
        } else if (pos.equals("K")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nKick Power: " + getGrade(player.field(13)) +
                    "\nAccuracy: " + getGrade(player.field(14)) +
                    "\nPressure: " + getGrade(player.field(15)) +
                    "\nForm: " + getGrade(player.field(16));
        } else if (pos.equals("DL")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nRun Stop: " + getGrade(player.field(13)) +
                    "\nTackling: " + getGrade(player.field(14)) +
                    "\nPass Rush: " + getGrade(player.field(15)) +
                    "\nStength: " + getGrade(player.field(16));
        } else if (pos.equals("LB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nTackle: " + getGrade(player.field(13)) +
                    "\nRun Stop: " + getGrade(player.field(14)) +
                    "\nCoverage: " + getGrade(player.field(15)) +
                    "\nSpeed: " + getGrade(player.field(16));
        } else if (pos.equals("CB")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nCoverage: " + getGrade(player.field(13)) +
                    "\nSpeed: " + getGrade(player.field(14)) +
                    "\nTackling: " + getGrade(player.field(15)) +
                    "\nJumping: " + getGrade(player.field(16));
        } else if (pos.equals("S")) {
            return "Home State: " + getRegion(player.regionCode()) +
                    "\nTackling: " + getGrade(player.field(13)) +
                    "\nCoverage: " + getGrade(player.field(14)) +
                    "\nSpeed: " + getGrade(player.field(15)) +
                    "\nRun Stop: " + getGrade(player.field(16));
        }
        return "ERROR";
    }

    public static String buildPotentialDetails(RecruitingPlayerRecord recruit) {
        return "Height: " + getHeight(recruit.heightInches()) +
                "\nWeight: " + getWeight(recruit.weightPounds()) +
                "\nIntelligence: " + getGrade(Integer.toString(recruit.intelligence())) +
                "\nCharacter: " + getGrade(Integer.toString(recruit.character())) +
                "\nDurability: " + getGrade(Integer.toString(recruit.durability()));
    }

    public static String buildRecruitConfirmMessage(RecruitingSessionData sessionData, int maxPlayers, String playerCsv) {
        RecruitingPlayerRecord player = RecruitingPlayerRecord.fromCsv(playerCsv);
        int currentRoster = sessionData.teamPlayers.size() + sessionData.playersRecruited.size();
        return "Your team roster is at " + currentRoster + " (Max: " + maxPlayers + ").\n\nAre you sure you want to recruit " +
                player.stars() + "-Star " + player.position() + " " + player.name() + " for $" + player.cost() + "?";
    }

    public static String buildExitConfirmMessage(List<String> positions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Are you sure you are done recruiting? Any unfilled positions will be filled by walk-ons.\n\n");
        for (int i = 2; i < positions.size() - 5; ++i) {
            sb.append("\t\t").append(positions.get(i)).append("\n");
        }
        return sb.toString();
    }

    public static String getPlayerListLeftLabel(String playerCsv) {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(playerCsv);
        return "$" + recruit.cost() + " " + recruit.position() + " " + recruit.name();
    }

    public static String getPlayerListRightLabel(String playerCsv) {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(playerCsv);
        return "Grade: " + getStarGrade(recruit.field(6));
    }

    private static void appendPositionSection(StringBuilder sb, String label, int need, List<String> players, int numStart) {
        sb.append(label).append(" (Need: ").append(need).append(")\n");
        for (int i = 0; i < players.size(); ++i) {
            sb.append("\t").append(i > numStart - 1 ? "BN" : "ST").append(" ").append(players.get(i)).append("\n");
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

    private static String getStarGrade(String num) {
        int pRat = Integer.parseInt(num);
        if (pRat == 5) return " * * * * *";
        if (pRat == 4) return " * * * *  ";
        if (pRat == 3) return " * * *    ";
        if (pRat == 2) return " * *      ";
        if (pRat == 1) return " *        ";
        return "??";
    }

    private static String getRegion(int region) {
        return STATES[region];
    }

    private static String getHeight(int height) {
        int feet = height / 12;
        int leftover = height % 12;
        return feet + "'' " + leftover + "\"";
    }

    private static String getWeight(int weight) {
        return weight + " lbs";
    }
}
