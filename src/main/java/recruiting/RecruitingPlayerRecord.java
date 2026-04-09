package recruiting;

public final class RecruitingPlayerRecord {
    private final String raw;
    private final String[] fields;

    private RecruitingPlayerRecord(String raw) {
        this.raw = raw;
        this.fields = raw.split(",");
    }

    public static RecruitingPlayerRecord fromCsv(String raw) {
        return new RecruitingPlayerRecord(raw);
    }

    public String field(int index) {
        return fields[index];
    }

    public String position() {
        return fields[0];
    }

    public String name() {
        return fields[1];
    }

    public String year() {
        return fields[2];
    }

    public boolean isGraduating() {
        return "5".equals(year());
    }

    public int regionCode() {
        return Integer.parseInt(fields[3]);
    }

    public int regionBucket() {
        return regionCode() / 10;
    }

    public boolean isTransfer() {
        return "true".equals(fields[7]);
    }

    public int recruitOverall() {
        return Integer.parseInt(fields[11]);
    }

    public int cost() {
        return Integer.parseInt(fields[12]);
    }

    public int currentOverall() {
        return Integer.parseInt(fields[19]);
    }

    public String improvement() {
        return fields[20];
    }

    public int stars() {
        return Integer.parseInt(fields[6]);
    }

    public int intelligence() {
        return Integer.parseInt(fields[5]);
    }

    public int character() {
        return Integer.parseInt(fields[4]);
    }

    public int durability() {
        return Integer.parseInt(fields[10]);
    }

    public int heightInches() {
        return Integer.parseInt(fields[17]);
    }

    public int weightPounds() {
        return Integer.parseInt(fields[18]);
    }

    public String listKey() {
        return raw.substring(0, raw.length() - 2);
    }
}
