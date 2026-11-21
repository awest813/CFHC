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
    /**
     * {@code key,value,holder,year}. The holder (names, with {@code %} between
     * player and team) is everything between the second and the last comma, so a
     * comma in a name no longer shifts the year out of place.
     */
    public static DataRecord fromCsv(String csv) {
        int a = csv.indexOf(',');
        int b = a < 0 ? -1 : csv.indexOf(',', a + 1);
        int c = csv.lastIndexOf(',');
        if (b < 0 || c <= b) return null;
        return new DataRecord(
            csv.substring(0, a),
            Float.parseFloat(csv.substring(a + 1, b)),
            csv.substring(b + 1, c),
            Integer.parseInt(csv.substring(c + 1))
        );
    }

    /** One save line: a line break in the holder would end it, so it becomes a space. */
    public String toCsv(java.text.DecimalFormat df) {
        String h = holder == null ? "" : holder.replace('\n', ' ').replace('\r', ' ');
        return key + "," + df.format(value) + "," + h + "," + year;
    }
}
