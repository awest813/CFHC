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

    public final ArrayList<String> playersRecruited = new ArrayList<>();
    public final ArrayList<String> playersGraduating = new ArrayList<>();
    public final ArrayList<String> teamPlayers = new ArrayList<>();

    public final ArrayList<String> teamQBs = new ArrayList<>();
    public final ArrayList<String> teamRBs = new ArrayList<>();
    public final ArrayList<String> teamWRs = new ArrayList<>();
    public final ArrayList<String> teamTEs = new ArrayList<>();
    public final ArrayList<String> teamOLs = new ArrayList<>();
    public final ArrayList<String> teamKs = new ArrayList<>();
    public final ArrayList<String> teamDLs = new ArrayList<>();
    public final ArrayList<String> teamLBs = new ArrayList<>();
    public final ArrayList<String> teamCBs = new ArrayList<>();
    public final ArrayList<String> teamSs = new ArrayList<>();

    public final ArrayList<String> availQBs = new ArrayList<>();
    public final ArrayList<String> availRBs = new ArrayList<>();
    public final ArrayList<String> availWRs = new ArrayList<>();
    public final ArrayList<String> availTEs = new ArrayList<>();
    public final ArrayList<String> availOLs = new ArrayList<>();
    public final ArrayList<String> availKs = new ArrayList<>();
    public final ArrayList<String> availDLs = new ArrayList<>();
    public final ArrayList<String> availLBs = new ArrayList<>();
    public final ArrayList<String> availCBs = new ArrayList<>();
    public final ArrayList<String> availSs = new ArrayList<>();
    public final ArrayList<String> availAll = new ArrayList<>();
    public final ArrayList<String> avail50 = new ArrayList<>();

    public final ArrayList<String> west = new ArrayList<>();
    public final ArrayList<String> midwest = new ArrayList<>();
    public final ArrayList<String> central = new ArrayList<>();
    public final ArrayList<String> east = new ArrayList<>();
    public final ArrayList<String> south = new ArrayList<>();

    private RecruitingSessionData(String teamName, int recruitingBudget, int coachTalent) {
        this.teamName = teamName;
        this.recruitingBudget = recruitingBudget;
        this.coachTalent = coachTalent;
    }

    public static RecruitingSessionData fromUserTeamInfo(String userTeamStr) {
        String[] lines = userTeamStr.split("%\n");
        String[] teamInfo = lines[0].split(",");
        int coachTalent = teamInfo[4].isEmpty() ? 70 : Integer.parseInt(teamInfo[4]);
        RecruitingSessionData session = new RecruitingSessionData(
                teamInfo[1],
                Integer.parseInt(teamInfo[3]) * 15,
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

    public String getReadablePlayerInfo(String playerCsv) {
        RecruitingPlayerRecord player = RecruitingPlayerRecord.fromCsv(playerCsv);
        String transfer = player.isTransfer() ? " (Transfer)" : "";
        if (!playersRecruited.contains(playerCsv)) {
            return player.name() + " " + getYearLabel(player.year()) + "  Ovr: " + player.currentOverall() + " (+" + player.improvement() + ")" + transfer;
        }
        return player.name() + " " + getYearLabel(player.year()) + "  Ovr: " + player.recruitOverall() + "  (Recruit)" + transfer;
    }

    public String buildRecruitsSaveData() {
        StringBuilder sb = new StringBuilder();
        for (String player : playersRecruited) {
            sb.append(player).append("%\n");
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

    public int getRecruitCost(String playerCsv) {
        return RecruitingPlayerRecord.fromCsv(playerCsv).cost();
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

    public void recruitPlayer(String playerCsv, boolean autoFilter, double recruitOffBoardChance, Random random) {
        recruitingBudget -= getRecruitCost(playerCsv);
        removeRecruitFromBoards(playerCsv);
        playersRecruited.add(playerCsv);

        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(playerCsv);
        addToTeamPositionList(recruit.position(), getReadablePlayerInfo(playerCsv));
        sortTeamByOverall();

        if (autoFilter) {
            removeUnaffordableRecruits();
        }
        removeRandomRecruits(recruitOffBoardChance, random);
    }

    public boolean scoutPlayer(String playerCsv) {
        int scoutCost = getRecruitCost(playerCsv) / 10;
        if (scoutCost < 10) {
            scoutCost = 10;
        }
        if (recruitingBudget < scoutCost) {
            return false;
        }

        recruitingBudget -= scoutCost;
        markScouted(availAll, playerCsv);
        markScouted(avail50, playerCsv);

        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(playerCsv);
        if (recruit.position().equals("QB")) {
            markScouted(availQBs, playerCsv);
        } else if (recruit.position().equals("RB")) {
            markScouted(availRBs, playerCsv);
        } else if (recruit.position().equals("WR")) {
            markScouted(availWRs, playerCsv);
        } else if (recruit.position().equals("TE")) {
            markScouted(availTEs, playerCsv);
        } else if (recruit.position().equals("OL")) {
            markScouted(availOLs, playerCsv);
        } else if (recruit.position().equals("K")) {
            markScouted(availKs, playerCsv);
        } else if (recruit.position().equals("DL")) {
            markScouted(availDLs, playerCsv);
        } else if (recruit.position().equals("LB")) {
            markScouted(availLBs, playerCsv);
        } else if (recruit.position().equals("CB")) {
            markScouted(availCBs, playerCsv);
        } else if (recruit.position().equals("S")) {
            markScouted(availSs, playerCsv);
        }
        return true;
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

    public void removeRandomRecruits(double recruitOffBoardChance, Random random) {
        ArrayList<String> removeList = new ArrayList<>();
        int i = 0;
        while (i < availAll.size()) {
            if (random.nextDouble() > recruitOffBoardChance) {
                removeList.add(availAll.get(i));
                i++;
            } else {
                i++;
            }
        }
        for (String recruit : removeList) {
            removeRecruitFromBoards(recruit);
        }
    }

    private void addExistingPlayer(String playerCsv) {
        RecruitingPlayerRecord player = RecruitingPlayerRecord.fromCsv(playerCsv);
        String readable = getReadablePlayerInfo(playerCsv);
        if (player.isGraduating()) {
            playersGraduating.add(readable);
            return;
        }
        teamPlayers.add(readable);
        addToTeamPositionList(player.position(), readable);
    }

    private void addRecruit(String recruitCsv) {
        RecruitingPlayerRecord player = RecruitingPlayerRecord.fromCsv(recruitCsv);
        availAll.add(recruitCsv);
        addToRegionList(player.regionBucket(), recruitCsv);
        addToAvailablePositionList(player.position(), recruitCsv);
    }

    private void addToRegionList(int regionBucket, String recruitCsv) {
        if (regionBucket == 0) {
            west.add(recruitCsv);
        } else if (regionBucket == 1) {
            midwest.add(recruitCsv);
        } else if (regionBucket == 2) {
            central.add(recruitCsv);
        } else if (regionBucket == 3) {
            east.add(recruitCsv);
        } else if (regionBucket == 4) {
            south.add(recruitCsv);
        }
    }

    private void addToAvailablePositionList(String position, String recruitCsv) {
        if (position.equals("QB")) {
            availQBs.add(recruitCsv);
        } else if (position.equals("RB")) {
            availRBs.add(recruitCsv);
        } else if (position.equals("WR")) {
            availWRs.add(recruitCsv);
        } else if (position.equals("TE")) {
            availTEs.add(recruitCsv);
        } else if (position.equals("K")) {
            availKs.add(recruitCsv);
        } else if (position.equals("OL")) {
            availOLs.add(recruitCsv);
        } else if (position.equals("DL")) {
            availDLs.add(recruitCsv);
        } else if (position.equals("LB")) {
            availLBs.add(recruitCsv);
        } else if (position.equals("CB")) {
            availCBs.add(recruitCsv);
        } else if (position.equals("S")) {
            availSs.add(recruitCsv);
        }
    }

    private void addToTeamPositionList(String position, String readablePlayer) {
        if (position.equals("QB")) {
            teamQBs.add(readablePlayer);
        } else if (position.equals("RB")) {
            teamRBs.add(readablePlayer);
        } else if (position.equals("WR")) {
            teamWRs.add(readablePlayer);
        } else if (position.equals("TE")) {
            teamTEs.add(readablePlayer);
        } else if (position.equals("OL")) {
            teamOLs.add(readablePlayer);
        } else if (position.equals("K")) {
            teamKs.add(readablePlayer);
        } else if (position.equals("DL")) {
            teamDLs.add(readablePlayer);
        } else if (position.equals("LB")) {
            teamLBs.add(readablePlayer);
        } else if (position.equals("CB")) {
            teamCBs.add(readablePlayer);
        } else if (position.equals("S")) {
            teamSs.add(readablePlayer);
        }
    }

    private void removeUnaffordable(List<String> list) {
        int i = 0;
        while (i < list.size()) {
            if (getRecruitCost(list.get(i)) > recruitingBudget) {
                list.remove(i);
            } else {
                ++i;
            }
        }
    }

    private void removeRecruitFromBoards(String playerCsv) {
        avail50.remove(playerCsv);
        availAll.remove(playerCsv);
        west.remove(playerCsv);
        midwest.remove(playerCsv);
        central.remove(playerCsv);
        east.remove(playerCsv);
        south.remove(playerCsv);

        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromCsv(playerCsv);
        if (recruit.position().equals("QB")) {
            availQBs.remove(playerCsv);
        } else if (recruit.position().equals("RB")) {
            availRBs.remove(playerCsv);
        } else if (recruit.position().equals("WR")) {
            availWRs.remove(playerCsv);
        } else if (recruit.position().equals("TE")) {
            availTEs.remove(playerCsv);
        } else if (recruit.position().equals("OL")) {
            availOLs.remove(playerCsv);
        } else if (recruit.position().equals("K")) {
            availKs.remove(playerCsv);
        } else if (recruit.position().equals("DL")) {
            availDLs.remove(playerCsv);
        } else if (recruit.position().equals("LB")) {
            availLBs.remove(playerCsv);
        } else if (recruit.position().equals("CB")) {
            availCBs.remove(playerCsv);
        } else if (recruit.position().equals("S")) {
            availSs.remove(playerCsv);
        }
    }

    private void markScouted(List<String> recruits, String playerCsv) {
        int index = recruits.indexOf(playerCsv);
        if (index >= 0) {
            recruits.set(index, playerCsv.substring(0, playerCsv.length() - 1) + "1");
        }
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
        for (String recruit : availAll) {
            if (!RecruitingPlayerRecord.fromCsv(recruit).position().equals("K")) {
                avail50.add(recruit);
                added++;
            }
            if (added > 50) {
                break;
            }
        }
    }
}
