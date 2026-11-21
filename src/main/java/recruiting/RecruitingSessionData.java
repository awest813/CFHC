package recruiting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Set<String> scoutedRecruitKeys = new HashSet<>();

    /**
     * Where a national-pool prospect stands: committed to another program, or
     * open with a rival he commits to once the user has made {@code decisionPoint}
     * signings without him (0: no decision point).
     */
    private static final class ProspectStatus {
        final RecruitingPlayerRecord recruit;
        String committedTo;
        final String rival;
        final int decisionPoint;

        ProspectStatus(RecruitingPlayerRecord recruit, String committedTo, String rival, int decisionPoint) {
            this.recruit = recruit;
            this.committedTo = committedTo;
            this.rival = rival;
            this.decisionPoint = decisionPoint;
        }
    }

    /** True when the board is the national pool ({@code @POOL} payload): prospects can sign elsewhere. */
    private boolean sharedPool;
    private int poolCommitted;
    private int poolSize;
    private final Map<String, ProspectStatus> statusByKey = new HashMap<>();
    /** Board order of the statuses, so decisions are applied in a fixed order. */
    private final ArrayList<ProspectStatus> statuses = new ArrayList<>();
    /** Prospects who committed to a rival during this session, in order. */
    private final ArrayList<RecruitingPlayerRecord> lostToRivals = new ArrayList<>();
    private final ArrayList<RecruitingPlayerRecord> lastDecisions = new ArrayList<>();


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
        session.recruitingBudget += parsePositiveIntLoose(teamInfo.length > 5 ? teamInfo[5] : "", 0) * simulation.SimulationFacade.RECRUITING_BUDGET_PER_NIL_TIER;
        session.recruitingBudget += parsePositiveIntLoose(teamInfo.length > 6 ? teamInfo[6] : "", 0);

        int i = 1;
        while (i < lines.length && !lines[i].equals("END_TEAM_INFO")) {
            session.addExistingPlayer(lines[i]);
            ++i;
        }

        ++i;
        RecruitingPlayerRecord last = null;
        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.startsWith(simulation.SimulationFacade.RECRUITING_POOL_MARKER)) {
                String[] f = line.split("\\|", -1);
                session.sharedPool = true;
                session.poolCommitted = f.length > 1 ? parsePositiveIntLoose(f[1], 0) : 0;
                session.poolSize = f.length > 2 ? parsePositiveIntLoose(f[2], 0) : 0;
            } else if (line.startsWith(simulation.SimulationFacade.RECRUITING_STATUS_PREFIX)) {
                if (last != null) {
                    String[] f = line.split("\\|", -1);
                    String committed = f.length > 1 && !f[1].isEmpty() ? f[1] : null;
                    String rival = f.length > 2 && !f[2].isEmpty() ? f[2] : null;
                    int decision = f.length > 3 ? parsePositiveIntLoose(f[3], 0) : 0;
                    ProspectStatus st = new ProspectStatus(last, committed, rival, decision);
                    session.statusByKey.put(last.listKey(), st);
                    session.statuses.add(st);
                }
            } else if (!line.isEmpty() && !line.equals("END_RECRUITS")) {
                last = session.addRecruit(line);
            }
            ++i;
        }

        session.sortTeamByOverall();
        session.sortBoardsByGrade();
        session.rebuildTopProspects();
        return session;
    }

    public void applyBudgetBonuses(int minPlayers) {
        applyBudgetBonuses(minPlayers, teamPlayers.size());
    }

    /** As {@link #applyBudgetBonuses(int)} with an explicit roster size (budget previews carry no roster). */
    public void applyBudgetBonuses(int minPlayers, int rosterSize) {
        int recBonus = (int) ((minPlayers - rosterSize) * 27.5);
        int coachBonus = (int) (coachTalent * 3.5);
        recruitingBudget += Math.max(0, recBonus + coachBonus);
    }

    /** Needs against {@link simulation.RosterRules}, the minimums CPU teams recruit to. */
    public PositionNeeds calculateNeeds() {
        return calculateNeeds(simulation.RosterRules.MIN_QBS, simulation.RosterRules.MIN_RBS,
                simulation.RosterRules.MIN_WRS, simulation.RosterRules.MIN_TES, simulation.RosterRules.MIN_OLS,
                simulation.RosterRules.MIN_KS, simulation.RosterRules.MIN_DLS, simulation.RosterRules.MIN_LBS,
                simulation.RosterRules.MIN_CBS, simulation.RosterRules.MIN_SS);
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

    /** @deprecated Presentation labels belong in {@link RecruitingPresentation}. */
    @Deprecated
    public ArrayList<String> buildPositionLabels(PositionNeeds needs) {
        return RecruitingPresentation.buildPositionLabels(this, needs);
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

    /**
     * Re-applies committed recruits onto a freshly loaded board and sets the remaining budget.
     * Uses {@code recruitOffBoardChance = 0} so restore is deterministic.
     */
    public void applyCheckpoint(int remainingBudget, Iterable<String> recruitedRawLines) {
        if (recruitedRawLines != null) {
            for (String raw : recruitedRawLines) {
                if (raw == null) continue;
                String line = raw.trim();
                if (line.isEmpty() || "END_RECRUITS".equals(line)) continue;
                RecruitingPlayerRecord found = findAvailableByRaw(line);
                if (found != null) {
                    recruitPlayer(found, false, 0.0);
                } else {
                    // Board no longer has the prospect; still count the commit.
                    RecruitingPlayerRecord orphan = RecruitingPlayerRecord.fromRecruitCsv(line);
                    playersRecruited.add(orphan);
                    addToTeamPositionList(orphan.position(), orphan);
                }
            }
            sortTeamByOverall();
            if (sharedPool) {
                applyDecisions();
            }
        }
        recruitingBudget = Math.max(0, remainingBudget);
    }

    private RecruitingPlayerRecord findAvailableByRaw(String raw) {
        for (RecruitingPlayerRecord recruit : availAll) {
            if (recruit != null && raw.equals(recruit.raw())) {
                return recruit;
            }
        }
        // Scouted rows may append ",T"
        for (RecruitingPlayerRecord recruit : availAll) {
            if (recruit == null) continue;
            String r = recruit.raw();
            if (r != null && (r.startsWith(raw) || raw.startsWith(r.replace(",T", "")))) {
                return recruit;
            }
        }
        return null;
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
        Collections.sort(availAll, openFirst(new CompRecruitCost()));
        if (avail50 != null) Collections.sort(avail50, openFirst(new CompRecruitCost()));
        Collections.sort(availQBs, openFirst(new CompRecruitCost()));
        Collections.sort(availRBs, openFirst(new CompRecruitCost()));
        Collections.sort(availWRs, openFirst(new CompRecruitCost()));
        Collections.sort(availTEs, openFirst(new CompRecruitCost()));
        Collections.sort(availOLs, openFirst(new CompRecruitCost()));
        Collections.sort(availKs, openFirst(new CompRecruitCost()));
        Collections.sort(availDLs, openFirst(new CompRecruitCost()));
        Collections.sort(availLBs, openFirst(new CompRecruitCost()));
        Collections.sort(availCBs, openFirst(new CompRecruitCost()));
        Collections.sort(availSs, openFirst(new CompRecruitCost()));
        Collections.sort(west, openFirst(new CompRecruitCost()));
        Collections.sort(midwest, openFirst(new CompRecruitCost()));
        Collections.sort(central, openFirst(new CompRecruitCost()));
        Collections.sort(east, openFirst(new CompRecruitCost()));
        Collections.sort(south, openFirst(new CompRecruitCost()));
    }

    public int projectedRosterSize() {
        return teamPlayers.size() + playersRecruited.size();
    }

    public boolean canRecruitMore() {
        return projectedRosterSize() < simulation.RosterRules.MAX_PLAYERS;
    }

    /**
     * Roster spots that walk-ons will take after signing day if {@code recruit}
     * signs: one for each player a position is still short of its
     * {@link simulation.RosterRules} minimum. Signing day fills those spots with
     * walk-ons whatever the roster size, so they count against the cap here.
     */
    int spotsStillNeededAfter(RecruitingPlayerRecord recruit) {
        String pos = recruit != null ? recruit.position() : "";
        int[] counts = {teamQBs.size(), teamRBs.size(), teamWRs.size(), teamTEs.size(), teamOLs.size(),
                teamKs.size(), teamDLs.size(), teamLBs.size(), teamCBs.size(), teamSs.size()};
        String[] names = {"QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"};
        int short_ = 0;
        for (int i = 0; i < names.length; i++) {
            int have = counts[i] + (names[i].equals(pos) ? 1 : 0);
            short_ += Math.max(0, simulation.RosterRules.minForPosition(names[i]) - have);
        }
        return short_;
    }

    /**
     * Why {@code recruit} cannot sign, or null when they can: the roster is full,
     * the budget is short, or the last open spots are needed for positions below
     * their minimum (signing someone else there used to leave the walk-ons that
     * fill those positions pushing the roster past the cap).
     */
    public String signingBlockReason(RecruitingPlayerRecord recruit) {
        int max = simulation.RosterRules.MAX_PLAYERS;
        String elsewhere = committedTo(recruit);
        if (elsewhere != null) {
            return "Committed to " + elsewhere;
        }
        if (!canRecruitMore()) {
            return "Roster is full (" + projectedRosterSize() + "/" + max + ")";
        }
        if (recruit.cost() > recruitingBudget) {
            return "Recruit cost $" + recruit.cost() + " exceeds remaining budget $" + recruitingBudget;
        }
        int reserved = spotsStillNeededAfter(recruit);
        if (projectedRosterSize() + 1 + reserved > max) {
            return "The last " + (max - projectedRosterSize()) + " roster spot(s) are needed for positions below "
                    + "their minimum (" + RecruitingPresentation.shortPositionsSummary(this) + ")";
        }
        return null;
    }

    public void recruitPlayer(RecruitingPlayerRecord recruit, boolean autoFilter, double recruitOffBoardChance) {
        String elsewhere = committedTo(recruit);
        if (elsewhere != null) {
            throw new IllegalStateException(recruit.name() + " has committed to " + elsewhere);
        }
        if (!canRecruitMore()) {
            throw new IllegalStateException(
                    "Roster is full (" + projectedRosterSize() + "/" + simulation.RosterRules.MAX_PLAYERS + ")");
        }
        if (projectedRosterSize() + 1 + spotsStillNeededAfter(recruit) > simulation.RosterRules.MAX_PLAYERS) {
            throw new IllegalStateException(signingBlockReason(recruit));
        }
        int cost = recruit.cost();
        if (cost > recruitingBudget) {
            throw new IllegalArgumentException(
                    "Recruit cost $" + cost + " exceeds remaining budget $" + recruitingBudget);
        }
        recruitingBudget -= cost;
        removeRecruitFromBoards(recruit);
        playersRecruited.add(recruit);

        addToTeamPositionList(recruit.position(), recruit);
        sortTeamByOverall();

        if (autoFilter) {
            removeUnaffordableRecruits();
        }
        if (sharedPool) {
            // Rivals sign prospects as signing day goes on: whoever's decision point
            // this signing reached commits elsewhere (no random disappearances).
            applyDecisions();
        } else {
            removeRandomRecruits(recruitOffBoardChance);
        }
    }


    public boolean scoutPlayer(RecruitingPlayerRecord recruit) {
        if (isScouted(recruit)) {
            return true;
        }
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

    /** Raw lines of the scouted prospects still on the board (for checkpoints). */
    public List<String> scoutedRaws() {
        List<String> raws = new ArrayList<>();
        for (RecruitingPlayerRecord recruit : availAll) {
            if (recruit != null && isScouted(recruit)) {
                raws.add(recruit.raw());
            }
        }
        return raws;
    }

    /** Marks prospects from a restored checkpoint as scouted again, without charging. */
    public void restoreScouted(Iterable<String> raws) {
        if (raws == null) {
            return;
        }
        for (String raw : raws) {
            if (raw == null || raw.trim().isEmpty()) {
                continue;
            }
            RecruitingPlayerRecord key = RecruitingPlayerRecord.fromRecruitCsv(raw.trim());
            for (RecruitingPlayerRecord recruit : new ArrayList<>(availAll)) {
                if (recruit.equals(key)) {
                    markScoutedEverywhere(recruit);
                    break;
                }
            }
        }
    }

    public boolean isScouted(RecruitingPlayerRecord recruit) {
        String raw = recruit.raw();
        return raw.endsWith(",T")
                || scoutedRecruitKeys.contains(raw)
                || scoutedRecruitKeys.contains(markRawScouted(raw));
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
                String scoutedRaw = markRawScouted(recruit.raw());
                scoutedRecruitKeys.add(recruit.raw());
                scoutedRecruitKeys.add(scoutedRaw);
                if (!scoutedRaw.equals(recruit.raw())) {
                    list.set(i, RecruitingPlayerRecord.fromRecruitCsv(scoutedRaw));
                }
                break;
            }
        }
    }

    private String markRawScouted(String raw) {
        if (raw.endsWith(",F") || raw.endsWith(",T")) {
            return raw.substring(0, raw.length() - 1) + "T";
        }
        return raw;
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


    public void removeRandomRecruits(double recruitOffBoardChance) {
        ArrayList<RecruitingPlayerRecord> removeList = new ArrayList<>();
        for (int i = 0; i < availAll.size(); i++) {
            if (simulation.SimRandom.nextDouble() > recruitOffBoardChance) {
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



    private RecruitingPlayerRecord addRecruit(String recruitCsv) {
        RecruitingPlayerRecord player = RecruitingPlayerRecord.fromRecruitCsv(recruitCsv);
        availAll.add(player);
        addToRegionList(player.regionBucket(), player);
        addToAvailablePositionList(player.position(), player);
        return player;
    }

    // ------------------------------------------------------------------
    // National pool: competition for prospects
    // ------------------------------------------------------------------

    /** True when the board is the shared national pool: rival programs sign prospects too. */
    public boolean isSharedPool() {
        return sharedPool;
    }

    /** Prospects in the national pool. */
    public int poolSize() {
        return poolSize;
    }

    /** National-pool prospects already committed when the board opened (the early signing wave). */
    public int poolCommittedAtOpen() {
        return poolCommitted;
    }

    /** The program {@code recruit} has committed to (never the user's), or null while he is open. */
    public String committedTo(RecruitingPlayerRecord recruit) {
        ProspectStatus st = recruit != null ? statusByKey.get(recruit.listKey()) : null;
        return st != null ? st.committedTo : null;
    }

    public boolean isCommittedElsewhere(RecruitingPlayerRecord recruit) {
        return committedTo(recruit) != null;
    }

    /** The rival program an open prospect leans to, or null. */
    public String rivalFor(RecruitingPlayerRecord recruit) {
        ProspectStatus st = recruit != null ? statusByKey.get(recruit.listKey()) : null;
        return st != null && st.committedTo == null ? st.rival : null;
    }

    /**
     * How many more signings the user can make before an open prospect commits to
     * his rival: 1 means sign him next or lose him. -1 when he has no deadline.
     */
    public int signingsUntilDecision(RecruitingPlayerRecord recruit) {
        ProspectStatus st = recruit != null ? statusByKey.get(recruit.listKey()) : null;
        if (st == null || st.committedTo != null || st.rival == null || st.decisionPoint <= 0
                || playersRecruited.contains(recruit)) {
            return -1;
        }
        return Math.max(1, st.decisionPoint - playersRecruited.size());
    }

    /** Board prospects already committed to another program. */
    public int committedElsewhereCount() {
        int n = 0;
        for (RecruitingPlayerRecord r : availAll) {
            if (isCommittedElsewhere(r)) n++;
        }
        return n;
    }

    /** Prospects who committed to a rival while the user was signing, oldest first. */
    public List<RecruitingPlayerRecord> lostToRivals() {
        return Collections.unmodifiableList(lostToRivals);
    }

    /** Prospects who committed to a rival on the user's last signing (for a notice). */
    public List<RecruitingPlayerRecord> lastDecisions() {
        return Collections.unmodifiableList(lastDecisions);
    }

    /**
     * Open prospects before those committed elsewhere, each part in {@code order}:
     * committed prospects stay listed (the user sees who went where) without
     * pushing signable ones off the top of the board or out of the Top 50.
     */
    private java.util.Comparator<RecruitingPlayerRecord> openFirst(java.util.Comparator<RecruitingPlayerRecord> order) {
        return (a, b) -> {
            int c = Boolean.compare(isCommittedElsewhere(a), isCommittedElsewhere(b));
            return c != 0 ? c : order.compare(a, b);
        };
    }

    /** Commits every open prospect whose decision point the user's signings have reached. */
    private void applyDecisions() {
        lastDecisions.clear();
        int signed = playersRecruited.size();
        for (ProspectStatus st : statuses) {
            if (st.committedTo == null && st.rival != null && st.decisionPoint > 0 && st.decisionPoint <= signed
                    && !playersRecruited.contains(st.recruit)) {
                st.committedTo = st.rival;
                lastDecisions.add(st.recruit);
            }
        }
        lostToRivals.addAll(lastDecisions);
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
        Collections.sort(availAll, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(west, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(midwest, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(central, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(east, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(south, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availQBs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availRBs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availWRs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availTEs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availOLs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availKs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availDLs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availLBs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availCBs, openFirst(new CompRecruitScoutGrade()));
        Collections.sort(availSs, openFirst(new CompRecruitScoutGrade()));
        if (avail50 != null) Collections.sort(avail50, openFirst(new CompRecruitScoutGrade()));
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
            if (added >= 50) {
                break;
            }
        }
    }

}
