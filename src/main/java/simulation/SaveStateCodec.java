package simulation;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import positions.Player;
import staff.DC;
import staff.HeadCoach;
import staff.OC;
import staff.Staff;

/**
 * The v1.5b part of a save: everything a league holds between two weeks beyond the
 * rosters, staffs and schedules the older format already carried, so a save made at
 * any week reloads into the same state and plays on exactly as if it was never saved.
 *
 * <p><b>Ids.</b> Players and coaches appear in many lists (award candidates,
 * offseason pools, transfer lists, the coaching carousel), so they are written as
 * ids. A player's id is its position in a fixed walk: every league team (conference
 * order, then team order within the conference) and then every FCS opponent, each
 * roster in {@link Team#getAllPlayers()} order; players on no roster follow as
 * {@code XP:} lines. Coaches likewise: each team's HC, OC and DC (one id per
 * post, as each is its own {@code H:} line), then the saved free-agent pool
 * ({@code CFA:} lines), then {@code XS:} lines. Loading rebuilds the same walk,
 * so the ids resolve to the same objects. A pool coach who also holds a post (the
 * pool lists can briefly hold a coach a team still employs) is not written as a
 * {@code CFA:} line: the pool lists name him by his post's id, so he stays one
 * object after a reload ({@link Writer#writtenPool()}).
 *
 * <p><b>What is carried</b> (named {@code LL:} lists; {@code TL:} lists and
 * {@code TX:} keys per team, see {@link Team#saveLists}; {@code PX:} per player):
 * the team order, weekly news, scores and headlines, every award (winners,
 * candidates, All-American, All-Freshman and All-Conference teams, Coach of the
 * Year, the decided flags and ceremony text), media polls, freshman and transfer
 * pools, the coach pools and per-coach flags, the FCS opponents with their
 * rosters, player award flags and injury state, and the RNG position
 * ({@link SimRandom#exportState()}).
 *
 * <p>Since v1.5c played games also keep their box scores (play-by-play, team
 * totals, returners and per-player game lines), compressed in one
 * {@code LL:boxScores} list; see {@link BoxScoreCodec}.
 */
final class SaveStateCodec {

    /** Presence of this league list marks a v1.5b save. */
    static final String MARKER = "v1.5b";

    private SaveStateCodec() {
    }

    /** Player / coach id lookups shared by the save and load sides. */
    interface Refs {
        String player(Player p);

        Player player(String id);

        String staff(Staff s);

        Staff staff(String id);

        default List<String> players(List<? extends Player> ps) {
            List<String> out = new ArrayList<>(ps.size());
            for (Player p : ps) {
                out.add(player(p));
            }
            return out;
        }

        default List<Player> resolvePlayers(List<String> ids) {
            List<Player> out = new ArrayList<>(ids.size());
            for (String id : ids) {
                Player p = player(id);
                if (p != null) {
                    out.add(p);
                }
            }
            return out;
        }

        default List<String> staffs(List<? extends Staff> ss) {
            List<String> out = new ArrayList<>(ss.size());
            for (Staff s : ss) {
                out.add(staff(s));
            }
            return out;
        }

        default List<Staff> resolveStaff(List<String> ids) {
            List<Staff> out = new ArrayList<>(ids.size());
            for (String id : ids) {
                Staff s = staff(id);
                if (s != null) {
                    out.add(s);
                }
            }
            return out;
        }
    }

    /** A nullable string as a list: empty for null, else one item. */
    static List<String> optional(String s) {
        List<String> out = new ArrayList<>(1);
        if (s != null) {
            out.add(s);
        }
        return out;
    }

    static String optional(List<String> v) {
        return v == null || v.isEmpty() ? null : v.get(0);
    }

    /** The opponents outside the league on any league team's schedule or OOC list. */
    static List<Team> fcsOpponents(League league) {
        java.util.Set<Team> inLeague = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        inLeague.addAll(league.teamList);
        java.util.Set<Team> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        List<Team> out = new ArrayList<>();
        for (Conference c : league.conferences) {
            for (Team t : c.confTeams) {
                for (Game g : t.getGameSchedule()) {
                    if (g == null) continue;
                    addFcs(g.homeTeam, inLeague, seen, out);
                    addFcs(g.awayTeam, inLeague, seen, out);
                }
                for (Team o : t.getOocTeams()) {
                    addFcs(o, inLeague, seen, out);
                }
            }
        }
        return out;
    }

    private static void addFcs(Team t, java.util.Set<Team> inLeague, java.util.Set<Team> seen, List<Team> out) {
        if (t == null || inLeague.contains(t) || "BYE".equals(t.getConference()) || "BYE".equals(t.getName())) {
            return;
        }
        if (seen.add(t)) {
            out.add(t);
        }
    }

    private static List<Team> walk(League league, List<Team> fcs) {
        List<Team> out = new ArrayList<>();
        for (Conference c : league.conferences) {
            out.addAll(c.confTeams);
        }
        out.addAll(fcs);
        return out;
    }

    private static String cls(Staff s) {
        if (s instanceof HeadCoach) return "H";
        if (s instanceof OC) return "O";
        if (s instanceof DC) return "D";
        return "S";
    }

    // ------------------------------------------------------------------------------------------
    // Save
    // ------------------------------------------------------------------------------------------

    /** Captures a league's v1.5b state; build it before reading any of its parts. */
    static final class Writer implements Refs {
        final League league;
        final List<Team> fcsTeams;
        private final IdentityHashMap<Player, Integer> playerIds = new IdentityHashMap<>();
        private final List<Player> detachedPlayers = new ArrayList<>();
        private int playerCount;
        private final IdentityHashMap<Staff, Integer> staffIds = new IdentityHashMap<>();
        private final List<Staff> allStaff = new ArrayList<>();
        private final List<Staff> writtenPool = new ArrayList<>();
        private final List<Staff> detachedStaff = new ArrayList<>();
        private final IdentityHashMap<Staff, Team> staffOwner = new IdentityHashMap<>();
        private final IdentityHashMap<Team, Map<String, List<String>>> teamLists = new IdentityHashMap<>();
        private final Map<String, List<String>> leagueLists = new LinkedHashMap<>();

        /**
         * @param pool the coaches without a job, in order; those that got no id of
         *             their own are the {@code CFA:} lines ({@link #writtenPool()})
         */
        Writer(League league, List<Staff> pool) {
            this.league = league;
            this.fcsTeams = fcsOpponents(league);
            List<Team> teams = walk(league, fcsTeams);
            for (Team t : teams) {
                for (Player p : t.getAllPlayers()) {
                    if (!playerIds.containsKey(p)) {
                        playerIds.put(p, playerCount++);
                    }
                }
            }
            for (Team t : teams) {
                for (Staff s : new Staff[]{t.getHeadCoach(), t.getOC(), t.getDC()}) {
                    if (s != null) {
                        // Every post is its own H: line and loads as its own object, so
                        // each takes an id (the loader counts them the same way).
                        staffIds.putIfAbsent(s, allStaff.size());
                        allStaff.add(s);
                        staffOwner.putIfAbsent(s, t);
                    }
                }
            }
            for (Staff s : pool) {
                if (s != null && !staffIds.containsKey(s)) {
                    addStaff(s);
                    writtenPool.add(s);
                }
            }
            for (Team t : teams) {
                teamLists.put(t, t.saveLists(this));
            }
            captureLeague();
        }

        /** The pool coaches to write as {@code CFA:} lines: those not already on a staff. */
        List<Staff> writtenPool() {
            return writtenPool;
        }

        private void addStaff(Staff s) {
            if (s != null && !staffIds.containsKey(s)) {
                staffIds.put(s, allStaff.size());
                allStaff.add(s);
            }
        }

        @Override
        public String player(Player p) {
            if (p == null) return "";
            Integer id = playerIds.get(p);
            if (id == null) {
                id = playerCount + detachedPlayers.size();
                playerIds.put(p, id);
                detachedPlayers.add(p);
            }
            return String.valueOf(id);
        }

        @Override
        public Player player(String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String staff(Staff s) {
            if (s == null) return "";
            Integer id = staffIds.get(s);
            if (id == null) {
                id = allStaff.size();
                staffIds.put(s, id);
                allStaff.add(s);
                detachedStaff.add(s);
            }
            return String.valueOf(id);
        }

        @Override
        public Staff staff(String id) {
            throw new UnsupportedOperationException();
        }

        private void put(String key, List<String> items) {
            leagueLists.put(key, items);
        }

        /** Adds a league list the league itself builds (e.g. the FCS opponent of each game). */
        void putList(String key, List<String> items) {
            put(key, new ArrayList<>(items));
        }

        private void putNested(String key, List<? extends List<String>> lists) {
            put(key, List.of(String.valueOf(lists == null ? -1 : lists.size())));
            if (lists == null) return;
            for (int i = 0; i < lists.size(); i++) {
                put(key + "#" + i, new ArrayList<>(lists.get(i)));
            }
        }

        private static List<String> mapItems(Map<String, Integer> m) {
            List<String> out = new ArrayList<>();
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                out.add(e.getKey() + "=" + e.getValue());
            }
            return out;
        }

        private void captureLeague() {
            League l = league;
            put(MARKER, List.of());
            List<String> order = new ArrayList<>();
            for (Team t : l.teamList) order.add(t.getName());
            put("teamOrder", order);

            LeagueRecord.StateBag.Builder sc = new LeagueRecord.StateBag.Builder()
                    .put("rng", SimRandom.exportState())
                    .flag("awardsDecided", l.seasonAwardsDecided)
                    .flag("heismanDecided", l.heismanDecided)
                    .flag("defPOTYDecided", l.defPOTYDecided)
                    .flag("freshmanDecided", l.freshmanDecided)
                    .flag("coachAwardDecided", l.coachAwardDecided)
                    .flag("linemanPOTYDecided", l.linemanPOTYDecided)
                    .put("leagueOffTal", l.leagueOffTal)
                    .put("leagueDefTal", l.leagueDefTal)
                    .put("confAvg", l.confAvg)
                    .put("leagueChemistry", Double.toString(l.leagueChemistry))
                    .put("staffEvalPrestigeMean", Double.toString(l.staffEvalPrestigeMean))
                    .put("staffEvalRecordMean", Double.toString(l.staffEvalRecordMean))
                    .put("disciplineChance", Double.toString(l.disciplineChance))
                    .put("disciplineScrutiny", Double.toString(l.disciplineScrutiny))
                    .put("countRealignment", l.countRealignment)
                    .flag("updateTV", l.updateTV)
                    .put("countTeam", l.countTeam);
            List<String> scalars = new ArrayList<>();
            for (Map.Entry<String, String> e : sc.build().values().entrySet()) {
                scalars.add(e.getKey() + "=" + e.getValue());
            }
            put("scalars", scalars);

            // Win streaks: which team's streak the active record is (it is that object).
            String active = "";
            for (Team t : l.teamList) {
                if (t.getWinStreak() == l.longestActiveWinStreak) {
                    active = t.getName();
                    break;
                }
            }
            put("activeStreak", List.of(active, streakCsv(l.longestActiveWinStreak),
                    l.yearStartLongestWinStreak == l.longestWinStreak ? "same" : ""));

            // Awards.
            put("awardWinners", List.of(player(l.heisman), player(l.defPOTY), player(l.linemanPOTY),
                    player(l.freshman), staff(l.coachWinner)));
            put("awardText.heisman", optional(l.heismanWinnerStrFull));
            put("awardText.defPOTY", optional(l.defPOTYWinnerStrFull));
            put("awardText.lineman", optional(l.linemanPOTYWinnerStrFull));
            put("awardText.freshman", optional(l.freshmanWinnerStrFull));
            put("awardText.coach", optional(l.coachWinnerStrFull));
            putPlayers("heismanCandidates", l.heismanCandidates);
            putPlayers("defPOTYCandidates", l.defPOTYCandidates);
            putPlayers("freshmanCandidates", l.freshmanCandidates);
            putPlayers("linemanPOTYCandidates", l.linemanPOTYCandidates);
            putPlayers("allAmericans", l.allAmericans);
            putPlayers("allAmericans2", l.allAmericans2);
            putPlayers("allFreshman", l.allFreshman);
            for (int i = 0; i < l.conferences.size(); i++) {
                Conference c = l.conferences.get(i);
                put("conf#" + i + ".allConfPlayers", players(c.allConfPlayers));
                put("conf#" + i + ".allConfCoach", staffs(c.allConfCoach));
            }

            // Freshman and transfer pools.
            putPlayers("freshmen", l.freshmen);
            putPlayers("redshirts", l.redshirts);
            putPlayers("fQBs", l.fQBs);
            putPlayers("fRBs", l.fRBs);
            putPlayers("fWRs", l.fWRs);
            putPlayers("fTEs", l.fTEs);
            putPlayers("fKs", l.fKs);
            putPlayers("fOLs", l.fOLs);
            putPlayers("fDLs", l.fDLs);
            putPlayers("fLBs", l.fLBs);
            putPlayers("fCBs", l.fCBs);
            putPlayers("fSs", l.fSs);
            putPlayers("transferQBs", l.transferQBs);
            putPlayers("transferRBs", l.transferRBs);
            putPlayers("transferWRs", l.transferWRs);
            putPlayers("transferTEs", l.transferTEs);
            putPlayers("transferKs", l.transferKs);
            putPlayers("transferOLs", l.transferOLs);
            putPlayers("transferDLs", l.transferDLs);
            putPlayers("transferLBs", l.transferLBs);
            putPlayers("transferCBs", l.transferCBs);
            putPlayers("transferSs", l.transferSs);
            putPlayers("userTransferList", l.userTransferList);
            putStrings("transfersList", l.transfersList);
            put("text.userTransfers", optional(l.userTransfers));
            put("text.sumTransfers", optional(l.sumTransfers));
            put("text.newsRealignment", optional(l.newsRealignment));

            // News.
            putNested("newsStories", l.newsStories);
            putNested("weeklyScores", l.weeklyScores);
            putStrings("newsHeadlines", l.newsHeadlines);
            putStrings("newsTV", l.newsTV);
            putStrings("teamDiscipline", l.teamDiscipline);
            if (l.teamsFCSList != null) {
                put("teamsFCSList", new ArrayList<>(l.teamsFCSList));
            }

            // Media polls.
            put("apRanks", mapItems(l.apRanks));
            put("coachesRanks", mapItems(l.coachesRanks));
            put("prevApRanks", mapItems(l.prevApRanks));
            put("prevCoachesRanks", mapItems(l.prevCoachesRanks));
            List<String> rest = new ArrayList<>();
            for (Map.Entry<Team, Integer> e : l.queuedPostseasonRest.entrySet()) {
                rest.add(e.getKey().getName() + "=" + e.getValue());
            }
            put("postseasonRest", rest);

            // Coach pools, then each coach's class, flags and team.
            put("coachFreeAgents", staffs(l.coachFreeAgents));
            put("coachList", staffs(l.coachList));
            put("coachStarList", staffs(l.coachStarList));
            put("coachDatabase", staffs(l.coachDatabase));
        }

        /** Per-coach class, flags and team, for every coach id; call after every list is encoded. */
        private List<String> staffStates() {
            List<String> out = new ArrayList<>(allStaff.size());
            for (Staff s : allStaff) {
                // "=": the team whose staff it is (FCS names can repeat); "-": none.
                String team = s.team == null ? "-" : s.team == staffOwner.get(s) ? "=" : "@" + s.team.getName();
                out.add(cls(s) + (s.promotionCandidate ? "1" : "0") + (s.retirement ? "1" : "0")
                        + (s.wonConfHC ? "1" : "0") + (s.wonTopHC ? "1" : "0") + team);
            }
            return out;
        }

        /** A player list as ids; a null list is left out (and loads as null). */
        private void putPlayers(String key, List<? extends Player> ps) {
            if (ps != null) put(key, players(ps));
        }

        private void putStrings(String key, List<String> ss) {
            if (ss != null) put(key, new ArrayList<>(ss));
        }

        Map<String, List<String>> teamLists(Team t) {
            Map<String, List<String>> m = teamLists.get(t);
            return m == null ? Map.of() : m;
        }

        List<LeagueRecord.StateBag> rosterState(Team t) {
            List<LeagueRecord.StateBag> out = new ArrayList<>();
            for (Player p : t.getAllPlayers()) {
                out.add(playerState(p));
            }
            return out;
        }

        /** The league lists; call last, after the team records (their lists may add detached ids). */
        Map<String, List<String>> leagueLists() {
            // Detached players' own state can't add ids, so the staff list is final here.
            put("staffState", staffStates());
            return leagueLists;
        }

        List<LeagueRecord.DetachedPlayer> detachedPlayers() {
            List<LeagueRecord.DetachedPlayer> out = new ArrayList<>();
            for (Player p : detachedPlayers) {
                out.add(new LeagueRecord.DetachedPlayer(p.team != null ? p.team.getName() : "", p.toRecord(),
                        playerState(p)));
            }
            return out;
        }

        List<LeagueRecord.DetachedStaff> detachedStaff() {
            List<LeagueRecord.DetachedStaff> out = new ArrayList<>();
            for (Staff s : detachedStaff) {
                out.add(new LeagueRecord.DetachedStaff(s.team != null ? s.team.getName() : "", s.toRecord()));
            }
            return out;
        }
    }

    static String streakCsv(TeamStreak s) {
        if (s == null) {
            return "";
        }
        return s.getStreakLength() + "," + s.getStartYear() + "," + s.getEndYear() + ","
                + (s.getTeam() != null ? s.getTeam() : "");
    }

    /** Player fields the {@code P:} record lacks: award flags, progression and injury details. */
    static LeagueRecord.StateBag playerState(Player p) {
        LeagueRecord.StateBag.Builder b = new LeagueRecord.StateBag.Builder();
        String aw = (p.wonHeisman ? "1" : "0") + (p.wonAllAmerican ? "1" : "0") + (p.wonAllConference ? "1" : "0")
                + (p.wonTopFreshman ? "1" : "0") + (p.wonAllFreshman ? "1" : "0");
        if (aw.indexOf('1') >= 0) b.put("aw", aw);
        if (p.progression != 0) b.put("prog", p.progression);
        if (p.isInjured) b.flag("inj", true);
        if (p.injury != null) {
            b.put("injD", p.injury.getDuration());
            b.put("injDesc", p.injury.getDescription() == null ? "" : p.injury.getDescription());
        }
        return b.build();
    }

    static void applyPlayerState(Player p, LeagueRecord.StateBag s, boolean fullState) {
        if (!fullState) {
            return;
        }
        String aw = s.str("aw", "00000");
        p.wonHeisman = aw.length() > 0 && aw.charAt(0) == '1';
        p.wonAllAmerican = aw.length() > 1 && aw.charAt(1) == '1';
        p.wonAllConference = aw.length() > 2 && aw.charAt(2) == '1';
        p.wonTopFreshman = aw.length() > 3 && aw.charAt(3) == '1';
        p.wonAllFreshman = aw.length() > 4 && aw.charAt(4) == '1';
        p.progression = s.integer("prog", 0);
        if (s.has("injD")) {
            p.injury = new Injury(s.integer("injD", 0), s.str("injDesc", ""), p);
        } else {
            p.injury = null;
        }
        p.isInjured = s.bool("inj", false);
    }

    // ------------------------------------------------------------------------------------------
    // Load
    // ------------------------------------------------------------------------------------------

    /**
     * Resolves ids against a league just built from a v1.5b record: its conferences
     * and FCS teams must still be in save order, and {@code pool} must be the
     * {@code CFA:} coaches in file order.
     */
    static final class Reader implements Refs {
        private final League league;
        private final LeagueRecord.LeagueState state;
        private final List<Player> players = new ArrayList<>();
        private final List<Staff> staff = new ArrayList<>();
        private final List<Team> walk;
        private final List<Team> fcsTeams;

        Reader(League league, LeagueRecord record, List<Team> fcsTeams, List<Staff> pool) {
            this.league = league;
            this.state = record.state();
            this.fcsTeams = fcsTeams;
            this.walk = walk(league, fcsTeams);
            for (Team t : walk) {
                players.addAll(t.getAllPlayers());
            }
            for (Team t : walk) {
                if (t.getHeadCoach() != null) staff.add(t.getHeadCoach());
                if (t.getOC() != null) staff.add(t.getOC());
                if (t.getDC() != null) staff.add(t.getDC());
            }
            List<String> staffState = state.lists().getOrDefault("staffState", List.of());
            boolean[] alias = legacyPoolAliases(pool, staffState.size());
            for (int i = 0; i < pool.size(); i++) {
                if (alias[i]) {
                    continue;
                }
                int id = staff.size();
                String st = id < staffState.size() ? staffState.get(id) : "H";
                staff.add(retype(pool.get(i), st));
            }
            for (LeagueRecord.DetachedStaff d : state.detachedStaff()) {
                int id = staff.size();
                String st = id < staffState.size() ? staffState.get(id) : "H";
                staff.add(newStaff(st.isEmpty() ? 'H' : st.charAt(0), d.staff(), teamNamed(d.teamName())));
            }
            for (int i = 0; i < staff.size() && i < staffState.size(); i++) {
                applyStaffState(staff.get(i), staffState.get(i));
            }
            for (LeagueRecord.DetachedPlayer d : state.detachedPlayers()) {
                Player p = Player.fromRecord(d.player(), teamNamed(d.teamName()));
                applyPlayerState(p, d.state(), true);
                players.add(p);
            }
        }

        /**
         * Which {@code CFA:} coaches of a save from before v1.5d are a team's coach
         * written twice. Those saves gave such a coach no id of his own but still
         * wrote him to the pool, so every later id was off by one. Found by count
         * (the coach states list is short by exactly that many) and by an identical
         * record on some post; nothing is skipped unless both agree.
         */
        private boolean[] legacyPoolAliases(List<Staff> pool, int stateCount) {
            boolean[] alias = new boolean[pool.size()];
            int missing = staff.size() + pool.size() + state.detachedStaff().size() - stateCount;
            if (stateCount == 0 || missing <= 0 || missing > pool.size()) {
                return alias;
            }
            java.util.Set<String> posts = new java.util.HashSet<>();
            for (Staff s : staff) {
                posts.add(Persistence.toCsv(s.toRecord()));
            }
            int found = 0;
            for (int i = 0; i < pool.size(); i++) {
                if (posts.contains(Persistence.toCsv(pool.get(i).toRecord()))) {
                    alias[i] = true;
                    found++;
                }
            }
            return found == missing ? alias : new boolean[pool.size()];
        }

        /** The pool coach as the class it was saved as (the old loader made every one a HeadCoach). */
        private Staff retype(Staff loaded, String st) {
            char c = st.isEmpty() ? 'H' : st.charAt(0);
            if (c == 'H' && loaded instanceof HeadCoach) {
                return loaded;
            }
            return newStaff(c, loaded.toRecord(), loaded.team);
        }

        private static Staff newStaff(char cls, StaffRecord r, Team team) {
            switch (cls) {
                case 'O': return new OC(team, r);
                case 'D': return new DC(team, r);
                default: return new HeadCoach(r, team);
            }
        }

        private void applyStaffState(Staff s, String st) {
            if (st.length() < 6) {
                return;
            }
            s.promotionCandidate = st.charAt(1) == '1';
            s.retirement = st.charAt(2) == '1';
            s.wonConfHC = st.charAt(3) == '1';
            s.wonTopHC = st.charAt(4) == '1';
            String team = st.substring(5);
            if (team.equals("-")) {
                s.team = null;
            } else if (team.startsWith("@")) {
                Team t = teamNamed(team.substring(1));
                if (t != null) {
                    s.team = t;
                }
            }
        }

        /** A league team by name, else an FCS opponent of that name, else null. */
        Team teamNamed(String name) {
            if (name == null || name.isEmpty()) return null;
            Team t = league.findTeam(name);
            if (t != null) return t;
            for (Team f : fcsTeams) {
                if (name.equals(f.getName())) return f;
            }
            return null;
        }

        @Override
        public String player(Player p) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Player player(String id) {
            if (id == null || id.isEmpty()) return null;
            try {
                int i = Integer.parseInt(id.trim());
                return i >= 0 && i < players.size() ? players.get(i) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String staff(Staff s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Staff staff(String id) {
            if (id == null || id.isEmpty()) return null;
            try {
                int i = Integer.parseInt(id.trim());
                return i >= 0 && i < staff.size() ? staff.get(i) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /** Applies each team's {@code PX:} rows and {@code TL:} lists. */
        void applyTeams(List<LeagueRecord.TeamRecord> recordsInWalkOrder) {
            for (int i = 0; i < walk.size() && i < recordsInWalkOrder.size(); i++) {
                Team t = walk.get(i);
                LeagueRecord.TeamRecord tr = recordsInWalkOrder.get(i);
                List<Player> roster = t.getAllPlayers();
                for (int p = 0; p < roster.size() && p < tr.rosterState().size(); p++) {
                    applyPlayerState(roster.get(p), tr.rosterState().get(p), true);
                }
                t.applySaveLists(tr.lists(), this);
            }
        }

        private List<String> list(String key) {
            return state.lists().getOrDefault(key, List.of());
        }

        /** A league list for state the league restores itself (e.g. the recruiting pool). */
        List<String> listOf(String key) {
            return list(key);
        }

        boolean hasList(String key) {
            return has(key);
        }

        private boolean has(String key) {
            return state.lists().containsKey(key);
        }

        /** The saved player list, or null when the save left it out (it was null). */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private ArrayList playerList(String key) {
            return has(key) ? new ArrayList(resolvePlayers(list(key))) : null;
        }

        private ArrayList<String> stringList(String key) {
            return has(key) ? new ArrayList<>(list(key)) : null;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void fill(List target, String key) {
            if (target == null || !has(key)) return;
            target.clear();
            target.addAll(resolvePlayers(list(key)));
        }

        private void fillStaff(List<Staff> target, String key) {
            if (target == null || !has(key)) return;
            target.clear();
            target.addAll(resolveStaff(list(key)));
        }

        private static void fillStrings(List<String> target, List<String> items) {
            if (target == null) return;
            target.clear();
            target.addAll(items);
        }

        private List<ArrayList<String>> nested(String key) {
            if (!has(key)) return null;
            int n;
            try {
                n = Integer.parseInt(list(key).get(0));
            } catch (RuntimeException e) {
                return null;
            }
            if (n < 0) return null;
            List<ArrayList<String>> out = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                out.add(new ArrayList<>(list(key + "#" + i)));
            }
            return out;
        }

        private static void fillMap(Map<String, Integer> target, List<String> items) {
            target.clear();
            for (String it : items) {
                int eq = it.lastIndexOf('=');
                if (eq <= 0) continue;
                try {
                    target.put(it.substring(0, eq), Integer.parseInt(it.substring(eq + 1)));
                } catch (NumberFormatException ignored) {
                    // skip
                }
            }
        }

        /** Applies the league lists; teams must already be in their final order. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        void applyLeague() {
            League l = league;
            Map<String, String> sc = new LinkedHashMap<>();
            for (String it : list("scalars")) {
                int eq = it.indexOf('=');
                if (eq > 0) sc.put(it.substring(0, eq), it.substring(eq + 1));
            }
            LeagueRecord.StateBag s = new LeagueRecord.StateBag(sc);
            l.seasonAwardsDecided = s.bool("awardsDecided", l.seasonAwardsDecided);
            l.heismanDecided = s.bool("heismanDecided", l.heismanDecided);
            l.defPOTYDecided = s.bool("defPOTYDecided", l.defPOTYDecided);
            l.freshmanDecided = s.bool("freshmanDecided", l.freshmanDecided);
            l.coachAwardDecided = s.bool("coachAwardDecided", l.coachAwardDecided);
            l.linemanPOTYDecided = s.bool("linemanPOTYDecided", l.linemanPOTYDecided);
            l.leagueOffTal = s.integer("leagueOffTal", l.leagueOffTal);
            l.leagueDefTal = s.integer("leagueDefTal", l.leagueDefTal);
            l.confAvg = s.integer("confAvg", l.confAvg);
            l.leagueChemistry = s.dbl("leagueChemistry", l.leagueChemistry);
            l.staffEvalPrestigeMean = s.dbl("staffEvalPrestigeMean", l.staffEvalPrestigeMean);
            l.staffEvalRecordMean = s.dbl("staffEvalRecordMean", l.staffEvalRecordMean);
            l.disciplineChance = s.dbl("disciplineChance", l.disciplineChance);
            l.disciplineScrutiny = s.dbl("disciplineScrutiny", l.disciplineScrutiny);
            l.countRealignment = s.integer("countRealignment", l.countRealignment);
            l.updateTV = s.bool("updateTV", l.updateTV);
            l.countTeam = s.integer("countTeam", l.countTeam);

            List<String> streak = list("activeStreak");
            if (streak.size() >= 3) {
                Team holder = l.findTeam(streak.get(0));
                if (holder != null) {
                    l.longestActiveWinStreak = holder.getWinStreak();
                } else {
                    TeamStreak parsed = parseStreak(streak.get(1));
                    if (parsed != null) l.longestActiveWinStreak = parsed;
                }
                if ("same".equals(streak.get(2))) {
                    l.yearStartLongestWinStreak = l.longestWinStreak;
                }
            }

            List<String> winners = list("awardWinners");
            if (winners.size() >= 5) {
                l.heisman = player(winners.get(0));
                l.defPOTY = player(winners.get(1));
                l.linemanPOTY = player(winners.get(2));
                l.freshman = player(winners.get(3));
                Staff c = staff(winners.get(4));
                l.coachWinner = c instanceof HeadCoach ? (HeadCoach) c : null;
            }
            if (has("awardText.heisman")) l.heismanWinnerStrFull = optional(list("awardText.heisman"));
            if (has("awardText.defPOTY")) l.defPOTYWinnerStrFull = optional(list("awardText.defPOTY"));
            if (has("awardText.lineman")) l.linemanPOTYWinnerStrFull = optional(list("awardText.lineman"));
            if (has("awardText.freshman")) l.freshmanWinnerStrFull = optional(list("awardText.freshman"));
            if (has("awardText.coach")) l.coachWinnerStrFull = optional(list("awardText.coach"));
            l.heismanCandidates = playerList("heismanCandidates");
            l.defPOTYCandidates = playerList("defPOTYCandidates");
            l.freshmanCandidates = playerList("freshmanCandidates");
            l.linemanPOTYCandidates = playerList("linemanPOTYCandidates");
            l.allAmericans = playerList("allAmericans");
            l.allAmericans2 = playerList("allAmericans2");
            l.allFreshman = playerList("allFreshman");
            for (int i = 0; i < l.conferences.size(); i++) {
                Conference c = l.conferences.get(i);
                fill(c.allConfPlayers, "conf#" + i + ".allConfPlayers");
                if (has("conf#" + i + ".allConfCoach")) {
                    c.allConfCoach.clear();
                    for (Staff st : resolveStaff(list("conf#" + i + ".allConfCoach"))) {
                        if (st instanceof HeadCoach) c.allConfCoach.add((HeadCoach) st);
                    }
                }
            }

            l.freshmen = playerList("freshmen");
            l.redshirts = playerList("redshirts");
            l.fQBs = playerList("fQBs");
            l.fRBs = playerList("fRBs");
            l.fWRs = playerList("fWRs");
            l.fTEs = playerList("fTEs");
            l.fKs = playerList("fKs");
            l.fOLs = playerList("fOLs");
            l.fDLs = playerList("fDLs");
            l.fLBs = playerList("fLBs");
            l.fCBs = playerList("fCBs");
            l.fSs = playerList("fSs");
            l.transferQBs = playerList("transferQBs");
            l.transferRBs = playerList("transferRBs");
            l.transferWRs = playerList("transferWRs");
            l.transferTEs = playerList("transferTEs");
            l.transferKs = playerList("transferKs");
            l.transferOLs = playerList("transferOLs");
            l.transferDLs = playerList("transferDLs");
            l.transferLBs = playerList("transferLBs");
            l.transferCBs = playerList("transferCBs");
            l.transferSs = playerList("transferSs");
            l.userTransferList = playerList("userTransferList");
            l.transfersList = stringList("transfersList");
            if (has("text.userTransfers")) l.userTransfers = optional(list("text.userTransfers"));
            if (has("text.sumTransfers")) l.sumTransfers = optional(list("text.sumTransfers"));
            if (has("text.newsRealignment")) l.newsRealignment = optional(list("text.newsRealignment"));

            List<ArrayList<String>> stories = nested("newsStories");
            if (stories != null) {
                l.newsStories = new ArrayList<>(stories);
            }
            List<ArrayList<String>> scores = nested("weeklyScores");
            if (scores != null) {
                l.weeklyScores = new ArrayList<>(scores);
            }
            l.newsHeadlines = stringList("newsHeadlines");
            if (l.newsHeadlines == null) l.newsHeadlines = new ArrayList<>();
            l.newsTV = stringList("newsTV");
            l.teamDiscipline = stringList("teamDiscipline");
            l.teamsFCSList = has("teamsFCSList") ? new ArrayList<>(list("teamsFCSList")) : null;

            fillMap(l.apRanks, list("apRanks"));
            fillMap(l.coachesRanks, list("coachesRanks"));
            fillMap(l.prevApRanks, list("prevApRanks"));
            fillMap(l.prevCoachesRanks, list("prevCoachesRanks"));
            l.queuedPostseasonRest.clear();
            for (String it : list("postseasonRest")) {
                int eq = it.lastIndexOf('=');
                Team t = eq > 0 ? l.findTeam(it.substring(0, eq)) : null;
                if (t != null) {
                    try {
                        l.queuedPostseasonRest.put(t, Integer.parseInt(it.substring(eq + 1)));
                    } catch (NumberFormatException ignored) {
                        // skip
                    }
                }
            }

            fillStaff(l.coachFreeAgents, "coachFreeAgents");
            fillStaff(l.coachList, "coachList");
            fillStaff(l.coachStarList, "coachStarList");
            fillStaff(l.coachDatabase, "coachDatabase");
        }

        /** The saved RNG position, or null. */
        String rngState() {
            for (String it : list("scalars")) {
                if (it.startsWith("rng=")) return it.substring(4);
            }
            return null;
        }

        List<String> teamOrder() {
            return list("teamOrder");
        }
    }

    static TeamStreak parseStreak(String csv) {
        if (csv == null || csv.isEmpty()) {
            return null;
        }
        String[] p = csv.split(",", 4);
        try {
            return new TeamStreak(Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()),
                    Integer.parseInt(p[0].trim()), p.length > 3 ? p[3] : "");
        } catch (RuntimeException e) {
            return null;
        }
    }
}
