package simulation;

import java.util.ArrayList;

public class Division {

    public String divName;

    public final ArrayList<Team> divTeams;

    private final League league;

    public Division(String name, League league){
        divName = name;
        divTeams = new ArrayList<Team>();
        this.league = league;
    }

    /**
     * Division round robin over {@code divWeeks} rounds. Uses the conference's
     * circle method, which handles odd divisions with a phantom opponent; the old
     * parity check was inverted and added the phantom to even divisions instead.
     * A team sitting out a round gets a bye that week.
     */
    public void setUpDivisionSchedule(int divWeeks) {
        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);
        java.util.List<java.util.List<int[]>> rounds = Conference.roundRobinRounds(divTeams.size(), divWeeks);
        for (java.util.List<int[]> round : rounds) {
            boolean[] plays = new boolean[divTeams.size()];
            for (int[] pair : round) {
                Team home = divTeams.get(pair[0]);
                Team away = divTeams.get(pair[1]);
                Game gm = new Game(home, away, "Division");
                home.addGameToSchedule(gm);
                away.addGameToSchedule(gm);
                plays[pair[0]] = true;
                plays[pair[1]] = true;
            }
            for (int t = 0; t < divTeams.size(); t++) {
                if (!plays[t]) divTeams.get(t).addGameToSchedule(new Game(divTeams.get(t), bye, "BYE WEEK"));
            }
        }
    }

}
