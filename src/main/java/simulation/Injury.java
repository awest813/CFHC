package simulation;


import positions.Player;


public class Injury {

    private static final String[] injuries = {"Knee", "Thigh", "Shoulder", "Wrist", "Ankle", "Foot", "Arm", "Back", "Head"};
    public League league;
    public int duration; // Duration of the injury (in games)
    private final String description; // What the injury is
    private final Player player; // Player that has this injury

    public Injury(int dur, String descrip, Player p) {
        duration = dur;
        description = descrip;
        player = p;
        player.isInjured = true;
    }

    public Injury(Player p) {
        // Generate an injury
        duration = Math.abs((int) (SimRandom.nextGaussian() * 3 + 1));
        if (duration == 0) duration = 1;
        if (SimRandom.nextDouble() < 0.01) duration = 15;
        description = injuries[(int) (SimRandom.nextDouble() * injuries.length)];
        player = p;
        player.isInjured = true;
        player.ratPot -= duration / 1.5;
        player.ratDurability -= duration;
        if (player.ratDurability < 0) player.ratDurability = 0;
        if (player.ratOvr > 85 && duration > 4) {
            player.team.league.addNewsStory(player.team.league.currentWeek + 1, "Injury Report>A major injury was sustained by " + player.team.getName() + "'s star " + player.position + ", " + player.name + " today. During the game, " + player.name + " suffered a " + description
                    + " injury and will be out for " + duration + " weeks.");
            player.team.league.addNewsHeadline("A major injury was sustained by " + player.team.getName() + " " + player.position + " " + player.name + " suffered a " + description
                    + " injury and will be out for " + duration + " weeks.");
        }
        // Medical redshirt: season-ending injury suffered early in the season.
        // Window derived from regSeasonWeeks so custom season lengths behave.
        League injLeague = player.team.league;
        if (duration > (injLeague.regSeasonWeeks - 1 - injLeague.currentWeek)
                && injLeague.currentWeek < SeasonFlowOrder.midseasonWeek(injLeague.regSeasonWeeks)
                && player.getGamesStarted() < 4 && !player.wasRedshirt) {
            player.isMedicalRS = true;
            duration = 26;
            if (player.team.isUserControlled()) {
                player.team.league.addNewsStory(player.team.league.currentWeek + 1, "Medical Redshirt>" + player.team.getName() + " " + player.position + " " + player.name + " sustained a major " + description + " injury and will be out for the season. A medical redshirt has been granted.");
            }
                player.team.league.addNewsHeadline( player.team.getName() + " " + player.position + " " + player.name + " sustained a major " + description + " injury and will be out for the season.");
            if (!player.team.isUserControlled() && player.ratOvr > 79) {
                player.team.league.addNewsStory(player.team.league.currentWeek + 1, "Medical Redshirt>" + player.team.getName() + " " + player.position + " " + player.name + " sustained a major " + description + " injury and will be out for the season. A medical redshirt has been granted.");
                player.team.league.addNewsHeadline( player.team.getName() + " " + player.position + " " + player.name + " sustained a major " + description + " injury and will be out for the season.");
            }
        }

    }

    public int getDuration() {
        return duration;
    }

    public String getDescription() {
        return description;
    }

    public void advanceGame(int weeks) {
        //duration--;
        for (int w=0; w < weeks; w++) {
            duration --;
        }
        if (duration <= 0) {
            // Done with injury
            player.isInjured = false;
            player.injury = null;
        }
    }

    public String toString() {
        return PlayerStatusCopy.injuryDetail(this, player != null && player.isMedicalRS);
    }
}
