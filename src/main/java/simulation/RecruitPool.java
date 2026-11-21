package simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import positions.Player;

/**
 * One national class of high-school prospects per recruiting cycle, signed by
 * every program from the same list, so the user can lose a target to a rival.
 *
 * <p><b>When.</b> {@link League#recruitPlayers()} builds the pool when the
 * recruiting gate opens ({@link SeasonFlowOrder#isRecruitingGate}) and runs the
 * <em>early wave</em>; {@link League#applyRecruitingSignings} applies the user's
 * class and runs the <em>late wave</em>. {@link League#startNextSeason()} drops it.
 *
 * <p><b>Size and talent.</b> Each program contributes the class it needs this
 * year ({@link Team#cpuRecruitNeeds()}; the user's program a nominal class),
 * drawn with that program's own star logic (the one CPU classes were generated
 * with before the pool), so the national talent spread matches the old per-team
 * classes; plus {@link #SURPLUS} unranked depth at every position so no position
 * runs dry. About 135 programs x ~20 make ~3,600 prospects. Generation runs on a
 * side stream keyed to the league seed and season ({@link SimRandom#onSideStream}),
 * so the pool is the same whenever it is built and never moves the season's stream.
 *
 * <p><b>Choosing a school</b> ({@link #interest}): prestige, an open spot at his
 * position, playing time (how he compares with the program's last rotation player
 * there), distance from home, the head coach's recruiting, the NIL collective, and
 * a fixed per-prospect, per-school taste (a hash, not the RNG). Prospects decide
 * best-first: each takes the program he likes most among those that still need
 * his position, so the order is fair to every school and set by interest, not by
 * a prestige pecking order. The weights were tuned so talent by prestige tier and
 * the share of top prospects at top-25 programs stay where per-team classes had them.
 *
 * <p><b>Early wave.</b> Programs fill up to about half of each position's need.
 * A prospect commits early with a chance that rises with his rank (blue-chippers
 * decide early), and only if he prefers that program to the user's.
 *
 * <p><b>User's board.</b> Prospects of the user's caliber (see
 * {@link #boardFor}), committed ones marked and unsignable. Each top prospect
 * still open has a <em>rival</em> (his favourite program with room for him) and a
 * <em>decision point</em>: after that many user signings he commits to the rival
 * unless the user signed him first (one to three signings when he prefers the
 * rival, four to nine when he prefers the user). The board's cost for a prospect
 * above the program's caliber carries a premium ({@link #userCostFactor}).
 *
 * <p><b>Late wave.</b> After the user signs: decided prospects join their rivals,
 * then everyone still open picks among programs with needs left. A program whose
 * need the pool cannot fill signs a generated freshman as before
 * ({@link Team#recruitPlayersFreshman}), then walk-ons.
 *
 * <p><b>Saves</b> (v1.5e): the pool, commitments, decision points and remaining
 * needs are {@code LL:recruitPool*} lists; open prospects are detached players.
 * A save from before v1.5e at the gate after CPU classes were signed gets a pool
 * built on load with no CPU needs ({@link #generate} with {@code live == false}).
 */
public final class RecruitPool {

    /** Extra prospects beyond the nation's needs, as a fraction. */
    public static final double SURPLUS = 0.30;
    /**
     * Surplus prospects are unranked depth: generated at {@code SURPLUS_MAX_STARS}
     * down to {@code SURPLUS_MAX_STARS - SURPLUS_STAR_SPREAD + 1} stars, below
     * almost every prospect of a real class.
     */
    static final int SURPLUS_MAX_STARS = 0;
    static final int SURPLUS_STAR_SPREAD = 4;
    /** Fraction of each position's need a program may fill in the early wave (rounded up). */
    static final double EARLY_SHARE = 0.5;
    /** Top fraction of the pool that gets a rival and a decision point. */
    static final double CONTESTED_SHARE = 0.35;

    static final double W_PRESTIGE = 0.40;
    static final double W_NEED = 0.15;
    static final double W_HOME = 0.15;
    static final double W_PLAYING_TIME = 0.20;
    static final double W_COACH = 0.10;
    static final double W_NIL = 0.05;
    static final double W_TASTE = 0.70;

    /** The class sizes a program without live needs contributes, in {@link Team#RECRUIT_POSITIONS} order. */
    static final int[] NOMINAL_CLASS = {2, 2, 3, 1, 4, 1, 3, 2, 2, 2};
    /** How many prospects per position the user's board lists (the old per-position board sizes). */
    static final int[] BOARD_SIZE = {70, 70, 110, 70, 110, 70, 110, 110, 110, 70};

    static final String LIST_INFO = "recruitPool";
    static final String LIST_PLAYERS = "recruitPool.players";
    static final String LIST_META = "recruitPool.meta";
    static final String LIST_NEEDS = "recruitPool.needs";

    public enum Stage {
        /** Early wave done; the user's board is open. */
        OPEN,
        /** Late wave done; every class is signed. */
        CLOSED
    }

    /** One prospect and where he stands. */
    public static final class Prospect {
        final int id;
        final Player player;
        final String key;
        Team committedTo;
        boolean early;
        boolean signedByUser;
        Team rival;
        int decisionPoint;
        int expectedPrestige;

        Prospect(int id, Player player) {
            this.id = id;
            this.player = player;
            this.key = keyOf(Persistence.toCsv(player.toRecord()));
        }

        public Player player() { return player; }
        public Team committedTo() { return committedTo; }
        public boolean isEarlyCommit() { return early; }
        public boolean isSignedByUser() { return signedByUser; }
        public Team rival() { return rival; }
        /** User signings after which an open prospect commits to his {@link #rival()}; 0 for none. */
        public int decisionPoint() { return decisionPoint; }
        /** Prestige of the program a prospect of his rank usually signs with. */
        public int expectedPrestige() { return expectedPrestige; }
        public boolean isOpen() { return committedTo == null; }
    }

    final int year;
    final long salt;
    final boolean live;
    Stage stage = Stage.OPEN;
    /** The user's program when the pool was built (null for an observer career). */
    final Team user;
    final List<Prospect> prospects;
    /** Remaining CPU needs, {@link Team#RECRUIT_POSITIONS} order (live pools only). */
    final IdentityHashMap<Team, int[]> needs = new IdentityHashMap<>();

    private RecruitPool(int year, long salt, boolean live, Team user, List<Prospect> prospects) {
        this.year = year;
        this.salt = salt;
        this.live = live;
        this.user = user;
        this.prospects = prospects;
    }

    public int year() { return year; }
    public Stage stage() { return stage; }
    public boolean isLive() { return live; }
    public Team user() { return user; }
    public List<Prospect> prospects() { return Collections.unmodifiableList(prospects); }

    /** Remaining need of a CPU program at {@code pos} (0 when it has none or the pool is not live). */
    public int remainingNeed(Team t, String pos) {
        int[] n = needs.get(t);
        int i = Team.recruitPositionIndex(pos);
        return n == null || i < 0 ? 0 : n[i];
    }

    public int committedCount() {
        int n = 0;
        for (Prospect p : prospects) {
            if (!p.isOpen()) n++;
        }
        return n;
    }

    // ------------------------------------------------------------------
    // Generation
    // ------------------------------------------------------------------

    static Team userOf(League league) {
        Team u = league.userTeam;
        return u != null && u.isUserControlled() ? u : null;
    }

    /**
     * Builds this season's pool. {@code live}: CPU programs sign from it (their needs
     * are computed now, drawing the class top-up from the league stream); otherwise
     * (a legacy save whose CPU classes are already signed) it only feeds the user's board.
     */
    static RecruitPool generate(League league, boolean live) {
        Team user = userOf(league);
        List<Team> teams = league.getTeamList();
        int[][] slots = new int[teams.size()][];
        IdentityHashMap<Team, int[]> cpuNeeds = new IdentityHashMap<>();
        for (int ti = 0; ti < teams.size(); ti++) {
            Team t = teams.get(ti);
            if (live && t != user && !t.isUserControlled()) {
                int[] n = t.cpuRecruitNeeds();
                cpuNeeds.put(t, n);
                slots[ti] = n.clone();
            } else {
                slots[ti] = NOMINAL_CLASS.clone();
            }
        }
        int year = league.getYear();
        long salt = mix(SimRandom.currentSeed() ^ (0x7EC0175L * (year + 1)));
        long streamKey = 0x52454352L * 1_000_003L + year; // "RECR"
        List<Player> made = SimRandom.onSideStream(streamKey, () -> {
            List<Player> out = new ArrayList<>();
            int[] bases = new int[teams.size()];
            int[] perPosition = new int[Team.RECRUIT_POSITIONS.length];
            for (int ti = 0; ti < teams.size(); ti++) {
                Team t = teams.get(ti);
                Team priceFor = user != null ? user : t;
                bases[ti] = t.recruitStarBase();
                for (int i = 0; i < Team.RECRUIT_POSITIONS.length; i++) {
                    perPosition[i] += slots[ti][i];
                    for (int k = 0; k < slots[ti][i]; k++) {
                        int stars = t.nudgedRecruitStars(bases[ti]);
                        out.add(Team.newFreshman(Team.RECRUIT_POSITIONS[i], league.getRandName(), stars, priceFor));
                    }
                }
            }
            // The surplus: unranked depth at every position, so a position never runs
            // dry while the talent the nation signs stays what the programs' own
            // classes would have been. Surplus drawn at class level (or even at
            // walk-on level) lifted the whole league 1-2 OVR: programs signed the
            // best of a bigger class, leaving their real low-rated recruits unsigned.
            for (int i = 0; i < Team.RECRUIT_POSITIONS.length; i++) {
                int extra = (int) Math.ceil(perPosition[i] * SURPLUS);
                for (int k = 0; k < extra; k++) {
                    Team t = teams.get(SimRandom.nextInt(teams.size()));
                    int stars = SURPLUS_MAX_STARS - SimRandom.nextInt(SURPLUS_STAR_SPREAD);
                    out.add(Team.newFreshman(Team.RECRUIT_POSITIONS[i], league.getRandName(), stars,
                            user != null ? user : t));
                }
            }
            return out;
        });
        // Best first; ties keep generation order.
        List<Player> sorted = new ArrayList<>(made);
        sorted.sort((a, b) -> Integer.compare(quality(b), quality(a)));
        List<Prospect> prospects = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            prospects.add(new Prospect(i, sorted.get(i)));
        }
        RecruitPool pool = new RecruitPool(year, salt, live, user, prospects);
        pool.needs.putAll(cpuNeeds);
        pool.assignExpectedPrestige(teams, slots);
        if (user != null) {
            for (Prospect p : prospects) {
                p.player.cost = Math.max(1, (int) Math.round(p.player.cost * userCostFactor(p.expectedPrestige, user.getTeamPrestige())));
            }
        }
        return pool;
    }

    /** Board sort key: four parts current rating, one part potential (as the board's grade sort). */
    static int quality(Player p) {
        return 4 * p.ratOvr + p.ratPot;
    }

    private void assignExpectedPrestige(List<Team> teams, int[][] slots) {
        List<Team> byPrestige = new ArrayList<>(teams);
        byPrestige.sort((a, b) -> Integer.compare(b.getTeamPrestige(), a.getTeamPrestige()));
        long classTotal = 0;
        for (int[] s : slots) {
            for (int n : s) classTotal += n;
        }
        int n = byPrestige.size();
        for (Prospect p : prospects) {
            int idx = classTotal <= 0 ? n - 1 : (int) Math.min(n - 1, (long) p.id * n / classTotal);
            p.expectedPrestige = n == 0 ? 50 : byPrestige.get(idx).getTeamPrestige();
        }
    }

    /**
     * The user's price multiplier for a prospect who usually signs with a program of
     * {@code expectedPrestige}: reaching more than 5 points above the program's
     * prestige costs up to 2.5x; one well below its caliber is up to 15% cheaper.
     */
    public static double userCostFactor(int expectedPrestige, int userPrestige) {
        int gap = expectedPrestige - userPrestige;
        if (gap > 5) {
            return Math.min(2.5, 1 + (gap - 5) / 25.0);
        }
        if (gap < -10) {
            return Math.max(0.85, 1 + (gap + 10) / 100.0);
        }
        return 1.0;
    }

    // ------------------------------------------------------------------
    // Interest
    // ------------------------------------------------------------------

    /**
     * How much prospect {@code p} wants to sign with {@code t} when it still needs
     * {@code need} at his position: larger is better. Rises with prestige, need,
     * playing time (how he compares with the last rotation player at his position),
     * closeness to home, coach recruiting and NIL tier; plus a fixed taste term.
     */
    double interest(Prospect p, Team t, int need) {
        int pos = Team.recruitPositionIndex(p.player.position);
        int[] depth = depthOvr.get(t);
        return interest(t.getTeamPrestige(), need, p.player.ratOvr, depth != null ? depth[pos] : 0,
                p.player.homeState / 10, t.getLocation(), coachRecruiting(t), t.getNilCollectiveLevel(),
                taste(p.id, t));
    }

    /** {@link #interest(Prospect, Team, int)} from its parts (exposed for the monotonicity tests). */
    static double interest(int prestige, int need, int prospectOvr, int depthOvr, int homeRegion, int schoolRegion,
                           int coachRecruiting, int nilTier, double taste) {
        double pres = Math.max(0, Math.min(100, prestige)) / 100.0;
        double playingTime = Math.max(0, Math.min(1, 0.5 + (prospectOvr - depthOvr) / 20.0));
        double home = 1 - Math.min(4, Math.abs(homeRegion - schoolRegion)) / 4.0;
        double coach = Math.max(0, Math.min(100, coachRecruiting)) / 100.0;
        double nil = Math.max(0, Math.min(League.NIL_MAX_TIER, nilTier)) / (double) League.NIL_MAX_TIER;
        return W_PRESTIGE * pres + W_NEED * needScore(need) + W_PLAYING_TIME * playingTime + W_HOME * home
                + W_COACH * coach + W_NIL * nil + W_TASTE * taste;
    }

    /** Per program, the OVR of the last rotation player at each position (0 when the rotation has a gap). */
    private final IdentityHashMap<Team, int[]> depthOvr = new IdentityHashMap<>();

    private void refreshDepth(List<Team> teams) {
        depthOvr.clear();
        for (Team t : teams) {
            if (t == null) continue;
            int[] d = new int[Team.RECRUIT_POSITIONS.length];
            for (int i = 0; i < d.length; i++) {
                d[i] = t.rotationOvr(i);
            }
            depthOvr.put(t, d);
        }
    }

    /** Playing time: a school with an open spot at his position beats one without; more open spots, more. */
    static double needScore(int need) {
        return need <= 0 ? 0.25 : Math.min(1.0, 0.5 + 0.25 * need);
    }

    static int coachRecruiting(Team t) {
        if (t.getHeadCoach() == null) {
            return 50;
        }
        return Math.min(100, t.getHeadCoach().ratTalent + t.getHeadCoach().recruitingPitchBonus());
    }

    /** A prospect's fixed taste for a school, uniform in [0, 1). */
    double taste(int prospectId, Team t) {
        return unit(mix(salt ^ (prospectId * 0x9E3779B97F4A7C15L) ^ (t.getName().hashCode() * 0xC2B2AE3D27D4EB4FL)));
    }

    private double draw(int prospectId, long tag) {
        return unit(mix(salt ^ (prospectId * 0xD6E8FEB86659FD93L) ^ tag));
    }

    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double unit(long bits) {
        return (bits >>> 11) * 0x1.0p-53;
    }

    /** The CPU program with need left at the prospect's position he likes most, or null. */
    private Team favourite(Prospect p, List<Team> cpu, IdentityHashMap<Team, int[]> room, int posIdx) {
        Team best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Team t : cpu) {
            int[] r = room.get(t);
            if (r == null || r[posIdx] <= 0) continue;
            double s = interest(p, t, remainingNeedAt(t, posIdx));
            if (s > bestScore) {
                bestScore = s;
                best = t;
            }
        }
        return best;
    }

    private int remainingNeedAt(Team t, int posIdx) {
        int[] n = needs.get(t);
        return n == null ? 0 : n[posIdx];
    }

    /** CPU programs signing from this pool, in league order (iteration never depends on hashing). */
    private List<Team> cpuTeams() {
        List<Team> out = new ArrayList<>();
        for (Team t : leagueOrder) {
            if (needs.containsKey(t)) out.add(t);
        }
        return out;
    }

    private List<Team> leagueOrder = List.of();

    // ------------------------------------------------------------------
    // Waves
    // ------------------------------------------------------------------

    /**
     * Early signing wave, then rivals and decision points for the top open
     * prospects. Uses no randomness.
     */
    void runEarlyWave(List<Team> leagueTeams) {
        leagueOrder = new ArrayList<>(leagueTeams);
        List<Team> cpu = cpuTeams();
        refreshDepth(leagueTeams);
        IdentityHashMap<Team, int[]> early = new IdentityHashMap<>();
        for (Team t : cpu) {
            int[] n = needs.get(t);
            int[] cap = new int[n.length];
            for (int i = 0; i < n.length; i++) {
                cap[i] = (int) Math.ceil(n[i] * EARLY_SHARE);
            }
            early.put(t, cap);
        }
        int[] userNeeds = user != null ? user.recruitNeedsBeforeTopUp() : null;
        int total = prospects.size();
        for (Prospect p : prospects) {
            int pos = Team.recruitPositionIndex(p.player.position);
            double rankShare = total <= 1 ? 0 : p.id / (double) (total - 1);
            if (draw(p.id, 0x51L) >= 0.25 + 0.5 * (1 - rankShare)) {
                continue; // waits for signing day
            }
            Team best = favourite(p, cpu, early, pos);
            if (best == null) continue;
            if (user != null && interest(p, user, userNeeds[pos]) > interest(p, best, remainingNeedAt(best, pos))) {
                continue; // holding out for the user's offer
            }
            commit(p, best);
            p.early = true;
            early.get(best)[pos]--;
        }
        if (user != null) {
            IdentityHashMap<Team, int[]> reserve = new IdentityHashMap<>();
            for (Team t : cpu) reserve.put(t, needs.get(t).clone());
            int contested = (int) Math.ceil(total * CONTESTED_SHARE);
            for (Prospect p : prospects) {
                if (p.id >= contested) break;
                if (!p.isOpen()) continue;
                int pos = Team.recruitPositionIndex(p.player.position);
                Team rival = favourite(p, cpu, reserve, pos);
                if (rival == null) continue;
                reserve.get(rival)[pos]--;
                p.rival = rival;
                boolean prefersRival = interest(p, rival, remainingNeedAt(rival, pos))
                        >= interest(p, user, userNeeds[pos]);
                int roll = (int) (draw(p.id, 0xDEC1DEL) * (prefersRival ? 3 : 6));
                p.decisionPoint = prefersRival ? 1 + roll : 4 + roll;
            }
        }
        stage = Stage.OPEN;
    }

    private void commit(Prospect p, Team t) {
        p.committedTo = t;
        int[] n = needs.get(t);
        if (n != null) {
            int pos = Team.recruitPositionIndex(p.player.position);
            n[pos] = Math.max(0, n[pos] - 1);
        }
        t.signRecruit(p.player);
    }

    /**
     * Drops signings the user may not make (a prospect committed elsewhere, or one
     * signed after his decision point) and marks the rest signed by the user.
     *
     * @return the signings to apply, in the payload format
     */
    String acceptUserSignings(String recruitsData) {
        if (recruitsData == null || recruitsData.isEmpty() || user == null) {
            return recruitsData;
        }
        Map<String, Prospect> byKey = new HashMap<>();
        for (Prospect p : prospects) {
            byKey.putIfAbsent(p.key, p);
        }
        StringBuilder kept = new StringBuilder();
        int signings = 0;
        for (String line : recruitsData.split("%\n")) {
            String l = line.trim();
            if (l.isEmpty() || l.equals("END_RECRUITS")) continue;
            Prospect p = byKey.get(keyOf(l));
            if (p != null) {
                boolean taken = p.committedTo != null && p.committedTo != user;
                boolean decided = p.decisionPoint > 0 && signings >= p.decisionPoint;
                if (taken || decided) {
                    PlatformLog.w("RecruitPool", "Ignoring signing of " + p.player.name + ": committed elsewhere");
                    continue;
                }
                p.committedTo = user;
                p.signedByUser = true;
            }
            signings++;
            kept.append(line).append("%\n");
        }
        kept.append("END_RECRUITS%\n");
        userSignings = signings;
        return kept.toString();
    }

    private int userSignings;

    /** Late wave: decided prospects join their rivals, the rest pick among programs with needs left. */
    void runLateWave(List<Team> leagueTeams) {
        if (stage == Stage.CLOSED) return;
        leagueOrder = new ArrayList<>(leagueTeams);
        List<Team> cpu = cpuTeams();
        refreshDepth(cpu);
        for (Prospect p : prospects) {
            if (p.isOpen() && p.rival != null && p.decisionPoint > 0 && p.decisionPoint <= userSignings
                    && remainingNeed(p.rival, p.player.position) > 0) {
                commit(p, p.rival);
            }
        }
        for (Prospect p : prospects) {
            if (!p.isOpen()) continue;
            int pos = Team.recruitPositionIndex(p.player.position);
            Team best = favourite(p, cpu, needs, pos);
            if (best != null) {
                commit(p, best);
            }
        }
        for (Team t : cpu) {
            int[] n = needs.get(t);
            // The pool ran dry at a position: generate the freshman as before, then walk-ons.
            t.recruitPlayersFreshman(n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8], n[9]);
            java.util.Arrays.fill(n, 0);
        }
        stage = Stage.CLOSED;
    }

    /**
     * One line for the Signing Day story once the late wave has run: how many
     * committed early, and how many contested prospects chose a rival over the user.
     */
    String competitionSummary() {
        int early = 0;
        int lost = 0;
        for (Prospect p : prospects) {
            if (p.early) early++;
            if (user != null && p.rival != null && !p.signedByUser && p.decisionPoint > 0
                    && p.decisionPoint <= userSignings && p.committedTo == p.rival) {
                lost++;
            }
        }
        String line = early + " of " + prospects.size() + " prospects in the national pool committed in the early"
                + " signing period.";
        if (user != null && lost > 0) {
            line += " " + lost + (lost == 1 ? " contested prospect" : " contested prospects")
                    + " chose a rival over " + user.getName() + ".";
        }
        return line;
    }

    // ------------------------------------------------------------------
    // The user's board
    // ------------------------------------------------------------------

    /**
     * The prospects on the user's board: per position the {@link #BOARD_SIZE} closest
     * to the program's caliber (a prospect expecting a stronger program counts double
     * the distance), committed ones included, and more open ones when fewer than
     * half the listed are open. Reads only.
     */
    public List<Prospect> boardFor(Team team) {
        int userPrestige = team != null ? team.getTeamPrestige() : 50;
        List<Prospect> out = new ArrayList<>();
        for (int i = 0; i < Team.RECRUIT_POSITIONS.length; i++) {
            String pos = Team.RECRUIT_POSITIONS[i];
            List<Prospect> atPos = new ArrayList<>();
            for (Prospect p : prospects) {
                if (p.player.position.equals(pos) && !p.signedByUser) atPos.add(p);
            }
            atPos.sort((a, b) -> {
                int c = Integer.compare(fit(a, userPrestige), fit(b, userPrestige));
                return c != 0 ? c : Integer.compare(a.id, b.id);
            });
            int open = 0;
            int listed = 0;
            for (Prospect p : atPos) {
                boolean within = listed < BOARD_SIZE[i];
                if (!within && (!p.isOpen() || open >= BOARD_SIZE[i] / 2)) continue;
                out.add(p);
                listed++;
                if (p.isOpen()) open++;
            }
        }
        out.sort((a, b) -> Integer.compare(a.id, b.id));
        return out;
    }

    private static int fit(Prospect p, int userPrestige) {
        int d = p.expectedPrestige - userPrestige;
        return d > 0 ? 2 * d : -d;
    }

    // ------------------------------------------------------------------
    // Save / load
    // ------------------------------------------------------------------

    /** Prospect identity in a payload line: the record minus team, cost and scouting fields. */
    static String keyOf(String csvLine) {
        String line = csvLine;
        int amp = line.indexOf('&');
        String basic = amp >= 0 ? line.substring(0, amp) : line;
        String[] f = basic.split(",", -1);
        if (f.length < 19) {
            return basic;
        }
        String pos = f[0];
        int colon = pos.lastIndexOf(':');
        if (colon >= 0) pos = pos.substring(colon + 1).trim();
        StringBuilder sb = new StringBuilder(pos);
        int[] keep = {1, 2, 3, 4, 5, 9, 10, 11, 13, 14, 15, 16, 17, 18};
        for (int k : keep) {
            sb.append('|').append(f[k]);
        }
        return sb.toString();
    }

    static void writeNone(SaveStateCodec.Writer codec) {
        codec.putList(LIST_INFO, List.of("none"));
    }

    void write(SaveStateCodec.Writer codec, List<Team> teams) {
        codec.putList(LIST_INFO, List.of("year=" + year, "stage=" + stage.name(), "live=" + (live ? 1 : 0),
                "salt=" + Long.toHexString(salt), "user=" + teams.indexOf(user), "signings=" + userSignings));
        List<String> players = new ArrayList<>(prospects.size());
        List<String> meta = new ArrayList<>(prospects.size());
        for (Prospect p : prospects) {
            boolean onRoster = p.committedTo != null && !p.signedByUser && p.player.team == p.committedTo;
            // A signed prospect is a roster player (its id); an open one is written compactly.
            players.add(onRoster ? "R" + codec.player(p.player) : "D" + compactCsv(p.player));
            meta.add(teams.indexOf(p.committedTo) + "|" + (p.early ? 1 : 0) + "|" + (p.signedByUser ? 1 : 0)
                    + "|" + teams.indexOf(p.rival) + "|" + p.decisionPoint + "|" + p.expectedPrestige);
        }
        codec.putList(LIST_PLAYERS, players);
        codec.putList(LIST_META, meta);
        List<String> needLines = new ArrayList<>();
        for (Team t : teams) {
            int[] n = needs.get(t);
            if (n == null) continue;
            StringBuilder sb = new StringBuilder().append(teams.indexOf(t)).append('=');
            for (int i = 0; i < n.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(n[i]);
            }
            needLines.add(sb.toString());
        }
        codec.putList(LIST_NEEDS, needLines);
    }

    /** Whether a save carries pool state at all (v1.5e on); false for older saves. */
    static boolean savedIn(SaveStateCodec.Reader codec) {
        return codec != null && codec.hasList(LIST_INFO);
    }

    /** The saved pool, or null when the save had none (or it is unreadable). */
    static RecruitPool read(SaveStateCodec.Reader codec, List<Team> teams) {
        List<String> info = codec.listOf(LIST_INFO);
        if (info.isEmpty() || "none".equals(info.get(0))) {
            return null;
        }
        try {
            Map<String, String> kv = new HashMap<>();
            for (String it : info) {
                int eq = it.indexOf('=');
                if (eq > 0) kv.put(it.substring(0, eq), it.substring(eq + 1));
            }
            int year = Integer.parseInt(kv.get("year"));
            long salt = Long.parseUnsignedLong(kv.get("salt"), 16);
            boolean live = "1".equals(kv.get("live"));
            Team user = teamAt(teams, Integer.parseInt(kv.get("user")));
            List<String> ids = codec.listOf(LIST_PLAYERS);
            List<String> meta = codec.listOf(LIST_META);
            if (ids.size() != meta.size()) {
                return null;
            }
            List<Prospect> prospects = new ArrayList<>(ids.size());
            for (int i = 0; i < ids.size(); i++) {
                String item = ids.get(i);
                Player pl = item.startsWith("R") ? codec.player(item.substring(1))
                        : item.startsWith("D") ? fromCompactCsv(item.substring(1), teams) : null;
                if (pl == null) {
                    return null;
                }
                Prospect p = new Prospect(i, pl);
                String[] m = meta.get(i).split("\\|", -1);
                p.committedTo = teamAt(teams, Integer.parseInt(m[0]));
                p.early = "1".equals(m[1]);
                p.signedByUser = "1".equals(m[2]);
                p.rival = teamAt(teams, Integer.parseInt(m[3]));
                p.decisionPoint = Integer.parseInt(m[4]);
                p.expectedPrestige = Integer.parseInt(m[5]);
                prospects.add(p);
            }
            RecruitPool pool = new RecruitPool(year, salt, live, user, prospects);
            pool.stage = Stage.valueOf(kv.getOrDefault("stage", "OPEN"));
            pool.userSignings = Integer.parseInt(kv.getOrDefault("signings", "0"));
            for (String line : codec.listOf(LIST_NEEDS)) {
                int eq = line.indexOf('=');
                Team t = teamAt(teams, Integer.parseInt(line.substring(0, eq)));
                String[] parts = line.substring(eq + 1).split(",");
                int[] n = new int[Team.RECRUIT_POSITIONS.length];
                for (int i = 0; i < n.length && i < parts.length; i++) {
                    n[i] = Integer.parseInt(parts[i]);
                }
                if (t != null) pool.needs.put(t, n);
            }
            pool.leagueOrder = new ArrayList<>(teams);
            return pool;
        } catch (RuntimeException e) {
            PlatformLog.w("RecruitPool", "Unreadable recruiting pool in save; rebuilding: " + e.getMessage());
            return null;
        }
    }

    /**
     * An unsigned prospect without his career-stat lines: a prospect has never
     * played, so those are the five empty seasons every new player starts with
     * (saves at the gate were half again as big when open prospects were written whole).
     */
    static String compactCsv(Player p) {
        String[] parts = Persistence.toCsv(p.toRecord()).split("&", -1);
        return parts[0] + "&" + parts[1] + "&&" + (parts.length > 3 ? parts[3] : "");
    }

    static Player fromCompactCsv(String csv, List<Team> teams) {
        PlayerRecord rec = PlayerRecord.fromCsv(csv);
        Team team = null;
        for (Team t : teams) {
            if (t.getName().equals(rec.teamName())) {
                team = t;
                break;
            }
        }
        Player p = Player.fromRecord(rec, team);
        if (p.careerStats == null || p.careerStats.isEmpty()) {
            p.careerStats = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                p.careerStats.add(new int[p.stats.length]);
            }
        }
        return p;
    }

    private static Team teamAt(List<Team> teams, int index) {
        return index >= 0 && index < teams.size() ? teams.get(index) : null;
    }
}
