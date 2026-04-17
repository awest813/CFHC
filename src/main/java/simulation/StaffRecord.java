package simulation;

/**
 * Portable data record for coaching staff members.
 */
public record StaffRecord(
    String position,
    String name,
    int age,
    int year,
    int ratOff,
    int ratDef,
    int ratTalent,
    int ratDiscipline,
    int offStrat,
    int defStrat,
    int contractYear,
    int contractLength,
    int baselinePrestige,
    boolean retired,
    int ratOvr,
    int ratImprovement,
    boolean user,
    int[] stats,
    int[] awards
) {
    public static StaffRecord fromCsv(String csv) {
        String[] parts = csv.split("&");
        String[] basic = parts[0].split(",");
        
        int[] statsList = new int[0];
        if (parts.length > 1) {
            String[] sStrings = parts[1].split(",");
            statsList = new int[sStrings.length];
            for (int i = 0; i < sStrings.length; i++) {
                statsList[i] = Integer.parseInt(sStrings[i].trim());
            }
        }

        int[] awardsList = new int[0];
        if (parts.length > 2) {
            String[] aStrings = parts[2].split(",");
            awardsList = new int[aStrings.length];
            for (int i = 0; i < aStrings.length; i++) {
                awardsList[i] = Integer.parseInt(aStrings[i].trim());
            }
        }

        return new StaffRecord(
            basic[0],
            basic[1],
            Integer.parseInt(basic[2]),
            Integer.parseInt(basic[3]),
            Integer.parseInt(basic[4]),
            Integer.parseInt(basic[5]),
            Integer.parseInt(basic[6]),
            Integer.parseInt(basic[7]),
            Integer.parseInt(basic[8]),
            Integer.parseInt(basic[9]),
            Integer.parseInt(basic[10]),
            Integer.parseInt(basic[11]),
            Integer.parseInt(basic[12]),
            Boolean.parseBoolean(basic[13]),
            Integer.parseInt(basic[14]),
            Integer.parseInt(basic[15]),
            basic.length > 16 && Boolean.parseBoolean(basic[16]),
            statsList,
            awardsList
        );
    }
}
