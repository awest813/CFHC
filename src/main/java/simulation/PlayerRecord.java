package simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Portable data record for player roster members.
 * This record captures the full state of a player for persistence.
 */
public record PlayerRecord(
    String position,
    String name,
    String teamName,
    int year,
    int homeState,

    int character,
    int ratIntelligence,
    int recruitRating,
    boolean isTransfer,
    boolean wasRedshirt,
    int ratPot,
    int ratDurability,
    int ratOvr,
    int cost,
    int[] attributes,
    int height,
    int weight,
    boolean isSuspended,
    int weeksSuspended,
    int troubledTimes,
    double talentNFL,
    int[] stats,
    List<int[]> careerStats,
    int[] awards,
    boolean isRedshirt,
    boolean isMedicalRS,
    boolean isGradTransfer,
    boolean isWalkOn
) {
    public static PlayerRecord fromCsv(String csv) {
        String[] parts = csv.split("&");
        String[] basic = parts[0].split(",");
        
        String pos = basic[0];
        String name = basic[1];
        String teamName = "";
        if (pos.contains(":")) {
            teamName = pos.split(":")[0].trim();
            pos = pos.split(":")[1].trim();
        }

        int[] attrs = new int[4];

        if (basic.length >= 17) {
            attrs[0] = Integer.parseInt(basic[13]);
            attrs[1] = Integer.parseInt(basic[14]);
            attrs[2] = Integer.parseInt(basic[15]);
            attrs[3] = Integer.parseInt(basic[16]);
        }

        int[] statsList = new int[0];
        if (parts.length > 1 && !parts[1].isEmpty()) {
            String[] sStrings = parts[1].split(",");
            statsList = new int[sStrings.length];
            for (int i = 0; i < sStrings.length; i++) {
                statsList[i] = Integer.parseInt(sStrings[i].trim());
            }
        }

        List<int[]> careerStatsList = new ArrayList<>();
        if (parts.length > 2 && !parts[2].isEmpty()) {
            String[] years = parts[2].split("#");
            for (String yearStr : years) {
                if (yearStr.isEmpty()) continue;
                String[] sStrings = yearStr.split(",");
                int[] yrStats = new int[sStrings.length];
                for (int i = 0; i < sStrings.length; i++) {
                    yrStats[i] = Integer.parseInt(sStrings[i].trim());
                }
                careerStatsList.add(yrStats);
            }
        }

        int[] awardsList = new int[0];
        if (parts.length > 3 && !parts[3].isEmpty()) {
            String[] aStrings = parts[3].split(",");
            awardsList = new int[aStrings.length];
            for (int i = 0; i < aStrings.length; i++) {
                awardsList[i] = Integer.parseInt(aStrings[i].trim());
            }
        }

        return new PlayerRecord(
            pos,
            name,
            teamName,
            Integer.parseInt(basic[2]),

            Integer.parseInt(basic[3]),
            Integer.parseInt(basic[4]),
            Integer.parseInt(basic[5]),
            Integer.parseInt(basic[6]),
            Boolean.parseBoolean(basic[7]),
            Boolean.parseBoolean(basic[8]),
            Integer.parseInt(basic[9]),
            Integer.parseInt(basic[10]),
            Integer.parseInt(basic[11]),
            Integer.parseInt(basic[12]),
            attrs,
            Integer.parseInt(basic[17]),
            Integer.parseInt(basic[18]),
            basic.length > 19 && Boolean.parseBoolean(basic[19]), // isSuspended
            basic.length > 20 ? Integer.parseInt(basic[20]) : 0, // weeksSuspended
            basic.length > 21 ? Integer.parseInt(basic[21]) : 0, // troubledTimes
            basic.length > 22 ? Double.parseDouble(basic[22]) : 0.0, // talentNFL
            statsList,
            careerStatsList,
            awardsList,
            basic.length > 23 && Boolean.parseBoolean(basic[23]), // isRedshirt
            basic.length > 24 && Boolean.parseBoolean(basic[24]), // isMedicalRS
            basic.length > 25 && Boolean.parseBoolean(basic[25]), // isGradTransfer
            basic.length > 26 && Boolean.parseBoolean(basic[26]) // isWalkOn
        );
    }
}

