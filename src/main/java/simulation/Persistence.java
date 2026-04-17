package simulation;

/**
 * Utility for simulation-wide data persistence and formatting.
 */
public final class Persistence {
    private Persistence() {
    }

    public static String toCsv(PlayerRecord r) {
        StringBuilder sb = new StringBuilder();
        // Basic Part
        String pos = r.position();
        if (r.teamName() != null && !r.teamName().isEmpty()) {
            pos = r.teamName() + ": " + pos;
        }
        sb.append(pos).append(",").append(r.name()).append(",").append(r.year()).append(",")

          .append(r.homeState()).append(",").append(r.character()).append(",").append(r.ratIntelligence()).append(",")
          .append(r.recruitRating()).append(",").append(r.isTransfer()).append(",").append(r.wasRedshirt()).append(",")
          .append(r.ratPot()).append(",").append(r.ratDurability()).append(",").append(r.ratOvr()).append(",")
          .append(r.cost()).append(",")
          .append(r.attributes()[0]).append(",").append(r.attributes()[1]).append(",")
          .append(r.attributes()[2]).append(",").append(r.attributes()[3]).append(",")
          .append(r.height()).append(",").append(r.weight()).append(",")
          .append(r.isSuspended()).append(",").append(r.weeksSuspended()).append(",")
          .append(r.troubledTimes()).append(",").append(r.talentNFL()).append(",")
          .append(r.isRedshirt()).append(",").append(r.isMedicalRS()).append(",")
          .append(r.isGradTransfer()).append(",").append(r.isWalkOn());
        
        sb.append("&");

        // Stats Part
        for (int i = 0; i < r.stats().length; i++) {
            sb.append(r.stats()[i]);
            if (i < r.stats().length - 1) sb.append(",");
        }
        sb.append("&");

        // Career Stats Part
        for (int i = 0; i < r.careerStats().size(); i++) {
            int[] yrStats = r.careerStats().get(i);
            for (int j = 0; j < yrStats.length; j++) {
                sb.append(yrStats[j]);
                if (j < yrStats.length - 1) sb.append(",");
            }
            if (i < r.careerStats().size() - 1) sb.append("#");
        }
        sb.append("&");

        // Awards Part
        for (int i = 0; i < r.awards().length; i++) {
            sb.append(r.awards()[i]);
            if (i < r.awards().length - 1) sb.append(",");
        }

        return sb.toString();
    }


    public static String toCsv(StaffRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.position()).append(",").append(r.name()).append(",").append(r.age()).append(",")
          .append(r.year()).append(",").append(r.ratOff()).append(",").append(r.ratDef()).append(",")
          .append(r.ratTalent()).append(",").append(r.ratDiscipline()).append(",").append(r.offStrat())
          .append(",").append(r.defStrat()).append(",").append(r.contractYear()).append(",")
          .append(r.contractLength()).append(",").append(r.baselinePrestige()).append(",")
          .append(r.retired()).append(",").append(r.ratOvr()).append(",").append(r.ratImprovement())
          .append(",").append(r.user()).append(",&");
        
        // Stats
        for (int i = 0; i < r.stats().length; i++) {
            sb.append(r.stats()[i]);
            if (i < r.stats().length - 1) sb.append(",");
        }
        sb.append("&");

        // Awards
        for (int i = 0; i < r.awards().length; i++) {
            sb.append(r.awards()[i]);
            if (i < r.awards().length - 1) sb.append(",");
        }

        return sb.toString();
    }
}
