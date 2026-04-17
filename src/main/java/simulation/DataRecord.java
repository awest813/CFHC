package simulation;

/**
 * A generic data record for tracking game/league records (e.g. Pass Yards, PPG).
 */
public record DataRecord(
    String key,
    float value,
    String holder,
    int year
) {
    public static DataRecord fromCsv(String csv) {
        String[] parts = csv.split(",");
        if (parts.length < 4) return null;
        return new DataRecord(
            parts[0],
            Float.parseFloat(parts[1]),
            parts[2],
            Integer.parseInt(parts[3])
        );
    }

    public String toCsv(java.text.DecimalFormat df) {
        return key + "," + df.format(value) + "," + holder + "," + year;
    }
}
