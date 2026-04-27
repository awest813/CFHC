package recruiting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RecruitingSessionData {
    public static final class PositionNeeds {
        public final int qbs;
        public final int rbs;
        public final int wrs;
        public final int tes;
        public final int ols;
        public final int ks;
        public final int dls;
        public final int lbs;
        public final int cbs;
        public final int ss;

        public PositionNeeds(int qbs, int rbs, int wrs, int tes, int ols, int ks, int dls, int lbs, int cbs, int ss) {
            this.qbs = qbs;
            this.rbs = rbs;
            this.wrs = wrs;
            this.tes = tes;
            this.ols = ols;
            this.ks = ks;
            this.dls = dls;
            this.lbs = lbs;
            this.cbs = cbs;
            this.ss = ss;
        }
    }

    public final String teamName;
    public int recruitingBudget;
    public final int coachTalent;
    public final double recruitOffBoard = 0.935;

    public final ArrayList<RecruitingPlayerRecord> playersRecruited = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> playersGraduating = new ArrayList<>();

    public final ArrayList<RecruitingPlayerRecord> teamPlayers = new ArrayList<>();

    public final ArrayList<RecruitingPlayerRecord> teamQBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamRBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamWRs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamTEs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamOLs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamKs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamDLs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamLBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamCBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> teamSs = new ArrayList<>();


    public final ArrayList<RecruitingPlayerRecord> availQBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availRBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availWRs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availTEs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availOLs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availKs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availDLs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availLBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availCBs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availSs = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> availAll = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> avail50 = new ArrayList<>();

    public final ArrayList<RecruitingPlayerRecord> west = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> midwest = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> central = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> east = new ArrayList<>();
    public final ArrayList<RecruitingPlayerRecord> south = new ArrayList<>();


    private RecruitingSessionData(String teamName, int recruitingBudget, int coachTalent) {
        this.teamName = teamName;
        this.recruitingBudget = recruitingBudget;
        this.coachTalent = coachTalent;
    }

    /**
     * Parses head coach recruiting skill from the first line of {@code userTeamStr}
     * (same token Android/desktop send before the {@code %\n} line break).
     * Strips non-digits so stray punctuation never breaks recruiting.
     */
    static int parseCoachTalentField(String raw) {
        if (raw == null) {
            return 70;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return 70;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return 70;
        }
        try {
            int v = Integer.parseInt(digits.toString());
            return Math.min(95, Math.max(20, v));
        } catch (NumberFormatException e) {
            return 70;
        }
    }

    static int parseRecruitBudgetUnits(String raw, int defaultUnits) {
        int units = parsePositiveIntLoose(raw, defaultUnits);
        return Math.min(20, Math.max(1, units));
    }

    private static int parsePositiveIntLoose(String raw, int defaultValue) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static RecruitingSessionData fromUserTeamInfo(String userTeamStr) {
        String[] lines = userTeamStr.split("%\n");
        String[] teamInfo = lines[0].split(",", -1);
        String teamNm = teamInfo.length > 1 && !teamInfo[1].isEmpty() ? teamInfo[1] : "Team";
        int budgetUnits = parseRecruitBudgetUnits(teamInfo.length > 3 ? teamInfo[3] : "", 5);
        int coachTalent = parseCoachTalentField(teamInfo.length > 4 ? teamInfo[4] : "");
        RecruitingSessionData session = new RecruitingSessionData(
                teamNm,
                budgetUnits * 15,
                coachTalent
        );

        int i = 1;
        while (!lines[i].equals("END_TEAM_INFO")) {
            session.addExistingPlayer(lines[i]);
            ++i;
        }

        ++i;
        while (i < lines.length) {
            session.addRecruit(lines[i]);
            ++i;
        }

        session.sortTeamByOverall();
        session.sortBoardsByGrade();
        session.rebuildTopProspects();
        return session;
    }

    public void applyBudgetBonuses(int minPlayers) {
        int recBonus = (int) ((minPlayers - teamPlayers.size()) * 27.5);
        int coachBonus = (int) (coachTalent * 3.5);
        recruitingBudget += recBonus + coachBonus;
    }

    public PositionNeeds calculateNeeds(int minQBs, int minRBs, int minWRs, int minTEs, int minOLs, int minKs, int minDLs, int minLBs, int minCBs, int minSs) {
        return new PositionNeeds(
                minQBs - teamQBs.size(),
                minRBs - teamRBs.size(),
                minWRs - teamWRs.size(),
                minTEs - teamTEs.size(),
                minOLs - teamOLs.size(),
                minKs - teamKs.size(),
                minDLs - teamDLs.size(),
                minLBs - teamLBs.size(),
                minCBs - teamCBs.size(),
                minSs - teamSs.size()
        );
    }

    public ArrayList<String> buildPositionLabels(PositionNeeds needs) {
        ArrayList<String> labels = new ArrayList<>();
        labels.add("Top 50 Recruits");
        labels.add("All Players");
        labels.add("QB (Need: " + needs.qbs + ")");
        labels.add("RB (Need: " + needs.rbs + ")");
        labels.add("WR (Need: " + needs.wrs + ")");
        labels.add("TE (Need: " + needs.tes + ")");
        labels.add("OL (Need: " + needs.ols + ")");
        labels.add("K (Need: " + needs.ks + ")");
        labels.add("DL (Need: " + needs.dls + ")");
        labels.add("LB (Need: " + needs.lbs + ")");
        labels.add("CB (Need: " + needs.cbs + ")");
        labels.add("S (Need: " + needs.ss + ")");
        labels.add("West (" + west.size() + ")");
        labels.add("Midwest (" + midwest.size() + ")");
        labels.add("Central (" + central.size() + ")");
        labels.add("East (" + east.size() + ")");
        labels.add("South (" + south.size() + ")");
        return labels;
    }

    public String getReadablePlayerInfo(RecruitingPlayerRecord player) {
        String transfer = player.isTransfer() ? " (Transfer)" : "";
        if (!playersRecruited.contains(player)) {
            return player.name() + " " + getYearLabel(player.year()) + "  Ovr: " + player.currentOverall() + " (+" + player.improvement() + ")" + transfer;
        }
        return player.name() + " " + getYearLabel(player.year()) + "  Ovr: " + player.recruitOverall() + "  (Recruit)" + transfer;
    }


    public String buildRecruitsSaveData() {
        StringBuilder sb = new StringBuilder();
        for (RecruitingPlayerRecord player : playersRecruited) {
            sb.append(player.raw()).append("%\n");
        }
        sb.append("END_RECRUITS%\n");
        return sb.toString();
    }


    public static String getYearLabel(String year) {
        if ("1".equals(year)) {
            return "[Fr]";
        } else if ("2".equals(year)) {
            return "[So]";
        } else if ("3".equals(year)) {
            return "[Jr]";
        } else if ("4".equals(year) || "5".equals(year)) {
            return "[Sr]";
        }
        return "[XX]";
    }

    public int getRecruitCost(RecruitingPlayerRecord player) {
        return player.cost();
    }


    public void sortBoardsByCost() {
        Collections.sort(availAll, new CompRecruitCost());
        Collections.sort(avail50, new CompRecruitCost());
        Collections.sort(availQBs, new CompRecruitCost());
        Collections.sort(availRBs, new CompRecruitCost());
        Collections.sort(availWRs, new CompRecruitCost());
        Collections.sort(availTEs, new CompRecruitCost());
        Collections.sort(availOLs, new CompRecruitCost());
        Collections.sort(availKs, new CompRecruitCost());
        Collections.sort(availDLs, new CompRecruitCost());
        Collections.sort(availLBs, new CompRecruitCost());
        Collections.sort(availCBs, new CompRecruitCost());
        Collections.sort(availSs, new CompRecruitCost());
        Collections.sort(west, new CompRecruitCost());
        Collections.sort(midwest, new CompRecruitCost());
        Collections.sort(central, new CompRecruitCost());
        Collections.sort(east, new CompRecruitCost());
        Collections.sort(south, new CompRecruitCost());
    }

    public void recruitPlayer(RecruitingPlayerRecord recruit, boolean autoFilter, double recruitOffBoardChance, Random random) {
        recruitingBudget -= recruit.cost();
        removeRecruitFromBoards(recruit);
        playersRecruited.add(recruit);

        addToTeamPositionList(recruit.position(), recruit);
        sortTeamByOverall();

        if (autoFilter) {
            removeUnaffordableRecruits();
        }
        removeRandomRecruits(recruitOffBoardChance, random);
    }


    public boolean scoutPlayer(RecruitingPlayerRecord recruit) {
        int base = Math.max(10, recruit.cost() / 10);
        int talentDiscount = Math.min(8, coachTalent / 12);
        int scoutCost = Math.max(5, base - talentDiscount);
        if (recruitingBudget < scoutCost) {
            return false;
        }

        recruitingBudget -= scoutCost;
        markScoutedEverywhere(recruit);
        return true;
    }

    private void markScoutedEverywhere(RecruitingPlayerRecord recruit) {
        markScouted(availAll, recruit);
        markScouted(avail50, recruit);
        markScouted(west, recruit);
        markScouted(midwest, recruit);
        markScouted(central, recruit);
        markScouted(east, recruit);
        markScouted(south, recruit);
        
        // Position specific
        String pos = recruit.position();
        if (pos.equals("QB")) markScouted(availQBs, recruit);
        else if (pos.equals("RB")) markScouted(availRBs, recruit);
        else if (pos.equals("WR")) markScouted(availWRs, recruit);
        else if (pos.equals("TE")) markScouted(availTEs, recruit);
        else if (pos.equals("OL")) markScouted(availOLs, recruit);
        else if (pos.equals("K")) markScouted(availKs, recruit);
        else if (pos.equals("DL")) markScouted(availDLs, recruit);
        else if (pos.equals("LB")) markScouted(availLBs, recruit);
        else if (pos.equals("CB")) markScouted(availCBs, recruit);
        else if (pos.equals("S")) markScouted(availSs, recruit);
    }

    private void markScouted(List<RecruitingPlayerRecord> list, RecruitingPlayerRecord recruit) {
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i).raw().equals(recruit.raw())) {
                String scoutedRaw = recruit.raw().substring(0, recruit.raw().length() - 1) + "T";
                list.set(i, RecruitingPlayerRecord.fromRecruitCsv(scoutedRaw));
                break;
            }
        }
    }


    public void removeUnaffordableRecruits() {
        removeUnaffordable(avail50);
        removeUnaffordable(availAll);
        removeUnaffordable(availQBs);
        removeUnaffordable(availRBs);
        removeUnaffordable(availWRs);
        removeUnaffordable(availTEs);
        removeUnaffordable(availOLs);
        removeUnaffordable(availKs);
        removeUnaffordable(availDLs);
        removeUnaffordable(availLBs);
        removeUnaffordable(availCBs);
        removeUnaffordable(availSs);
        removeUnaffordable(west);
        removeUnaffordable(midwest);
        removeUnaffordable(central);
        removeUnaffordable(east);
        removeUnaffordable(south);
    }

    private void removeUnaffordable(List<RecruitingPlayerRecord> list) {
        int i = 0;
        while (i < list.size()) {
            if (list.get(i).cost() > recruitingBudget) {
                list.remove(i);
            } else {
                i++;
            }
        }
    }


    public void removeRandomRecruits(double recruitOffBoardChance, Random random) {
        ArrayList<RecruitingPlayerRecord> removeList = new ArrayList<>();
        for (int i = 0; i < availAll.size(); i++) {
            if (random.nextDouble() > recruitOffBoardChance) {
                removeList.add(availAll.get(i));
            }
        }
        for (RecruitingPlayerRecord recruit : removeList) {
            removeRecruitFromBoards(recruit);
        }
    }


    private void addExistingPlayer(String playerCsv) {
        if (playerCsv.startsWith("HC,") || playerCsv.startsWith("OC,") || playerCsv.startsWith("DC,")) {
            return;
        }
        RecruitingPlayerRecord player = RecruitingPlayerRecord.fromRosterCsv(playerCsv);
        if (player.isGraduating()) {
            playersGraduating.add(player);
            return;
        }
        teamPlayers.add(player);
        addToTeamPositionList(player.position(), player);
    }



    private void addRecruit(String recruitCsv) {
        RecruitingPlayerRecord player = RecruitingPlayerRecord.fromRecruitCsv(recruitCsv);
        availAll.add(player);
        addToRegionList(player.regionBucket(), player);
        addToAvailablePositionList(player.position(), player);
    }


    private void addToRegionList(int regionBucket, RecruitingPlayerRecord recruit) {
        if (regionBucket == 0) {
            west.add(recruit);
        } else if (regionBucket == 1) {
            midwest.add(recruit);
        } else if (regionBucket == 2) {
            central.add(recruit);
        } else if (regionBucket == 3) {
            east.add(recruit);
        } else if (regionBucket == 4) {
            south.add(recruit);
        }
    }

    private void addToAvailablePositionList(String position, RecruitingPlayerRecord recruit) {
        if (position.equals("QB")) {
            availQBs.add(recruit);
        } else if (position.equals("RB")) {
            availRBs.add(recruit);
        } else if (position.equals("WR")) {
            availWRs.add(recruit);
        } else if (position.equals("TE")) {
            availTEs.add(recruit);
        } else if (position.equals("OL")) {
            availOLs.add(recruit);
        } else if (position.equals("K")) {
            availKs.add(recruit);
        } else if (position.equals("DL")) {
            availDLs.add(recruit);
        } else if (position.equals("LB")) {
            availLBs.add(recruit);
        } else if (position.equals("CB")) {
            availCBs.add(recruit);
        } else if (position.equals("S")) {
            availSs.add(recruit);
        }
    }


    private void addToTeamPositionList(String position, RecruitingPlayerRecord player) {
        if (position.equals("QB")) {
            teamQBs.add(player);
        } else if (position.equals("RB")) {
            teamRBs.add(player);
        } else if (position.equals("WR")) {
            teamWRs.add(player);
        } else if (position.equals("TE")) {
            teamTEs.add(player);
        } else if (position.equals("OL")) {
            teamOLs.add(player);
        } else if (position.equals("K")) {
            teamKs.add(player);
        } else if (position.equals("DL")) {
            teamDLs.add(player);
        } else if (position.equals("LB")) {
            teamLBs.add(player);
        } else if (position.equals("CB")) {
            teamCBs.add(player);
        } else if (position.equals("S")) {
            teamSs.add(player);
        }
    }


    private void removeRecruitFromBoards(RecruitingPlayerRecord recruit) {
        availAll.remove(recruit);
        avail50.remove(recruit);
        west.remove(recruit);
        midwest.remove(recruit);
        central.remove(recruit);
        east.remove(recruit);
        south.remove(recruit);
        availQBs.remove(recruit);
        availRBs.remove(recruit);
        availWRs.remove(recruit);
        availTEs.remove(recruit);
        availKs.remove(recruit);
        availOLs.remove(recruit);
        availDLs.remove(recruit);
        availLBs.remove(recruit);
        availCBs.remove(recruit);
        availSs.remove(recruit);
    }




    public void sortBoardsByGrade() {
        Collections.sort(availAll, new CompRecruitScoutGrade());
        Collections.sort(west, new CompRecruitScoutGrade());
        Collections.sort(midwest, new CompRecruitScoutGrade());
        Collections.sort(central, new CompRecruitScoutGrade());
        Collections.sort(east, new CompRecruitScoutGrade());
        Collections.sort(south, new CompRecruitScoutGrade());
        Collections.sort(availQBs, new CompRecruitScoutGrade());
        Collections.sort(availRBs, new CompRecruitScoutGrade());
        Collections.sort(availWRs, new CompRecruitScoutGrade());
        Collections.sort(availTEs, new CompRecruitScoutGrade());
        Collections.sort(availOLs, new CompRecruitScoutGrade());
        Collections.sort(availKs, new CompRecruitScoutGrade());
        Collections.sort(availDLs, new CompRecruitScoutGrade());
        Collections.sort(availLBs, new CompRecruitScoutGrade());
        Collections.sort(availCBs, new CompRecruitScoutGrade());
        Collections.sort(availSs, new CompRecruitScoutGrade());
    }

    public void sortTeamByOverall() {
        Collections.sort(teamQBs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamRBs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamWRs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamTEs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamOLs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamKs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamDLs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamLBs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamCBs, new CompRecruitTeamRosterOvr());
        Collections.sort(teamSs, new CompRecruitTeamRosterOvr());
    }

    public void rebuildTopProspects() {
        avail50.clear();
        int added = 0;
        for (RecruitingPlayerRecord recruit : availAll) {
            if (!recruit.position().equals("K")) {
                avail50.add(recruit);
                added++;
            }
            if (added > 50) {
                break;
            }
        }
    }

}
