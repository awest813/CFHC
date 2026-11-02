package simulation;

/**
 * Small grammar helpers for award blurbs and news copy, so generated text reads
 * "1 fumble", "an 8-5 record" and doesn't praise a high count with "just".
 */
public final class AwardText {

    private AwardText() {}

    /** "1 fumble", "3 fumbles" (regular plurals only). */
    public static String count(int n, String singular) {
        return n + " " + (n == 1 ? singular : singular + "s");
    }

    /** "1 pass", "3 passes" for irregular plurals. */
    public static String count(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /** {@link #count}, prefixed with "just" when {@code n} is at most {@code lowMax}. */
    public static String fewCount(int n, int lowMax, String singular) {
        return (n <= lowMax ? "just " : "") + count(n, singular);
    }

    /** "a"/"an" for a number read aloud: an 8, an 11, an 18, an 80. */
    public static String article(int n) {
        String s = String.valueOf(Math.abs(n));
        return s.startsWith("8") || n == 11 || n == 18 ? "an" : "a";
    }

    /** "a 4-0 record", "an 8-2 record" (wins-losses only, for prose). */
    public static String recordOf(Team team) {
        int w = team.getWins();
        return article(w) + " " + w + "-" + team.getLosses() + " record";
    }

    /** " to an 8-5 record and a #12 poll ranking." */
    public static String recordClause(Team team) {
        int w = team.getWins();
        return " to " + article(w) + " " + w + "-" + team.getLosses()
                + " record and a #" + team.getRankTeamPollScore() + " poll ranking.";
    }

    /** "once", "twice", "5 times" (also "0 times"). */
    public static String times(int n) {
        return n == 1 ? "once" : n == 2 ? "twice" : n + " times";
    }
}
