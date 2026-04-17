package recruiting;

public final class RecruitingPlayerRecord {
    private final String raw;
    private final String position;
    private final String name;
    private final String year;
    private final int regionCode;
    private final boolean isTransfer;
    private final int recruitOverall;
    private final int cost;
    private final int currentOverall;
    private final String improvement;
    private final int stars;
    private final int intelligence;
    private final int character;
    private final boolean wasRedshirt;
    private final int potential;
    private final int durability;
    private final int heightInches;
    private final int weightPounds;
    private final String rat1;
    private final String rat2;
    private final String rat3;
    private final String rat4;
    private final boolean isTransfer;
    private final int recruitOverall;
    private final int cost;
    private final int currentOverall;
    private final String improvement;

    private RecruitingPlayerRecord(String raw, String[] fields, boolean isRecruitFormat) {

        this.raw = raw;
        if (isRecruitFormat) {
            this.position = fields[0];
            this.name = fields[1];
            this.year = fields[2];
            this.regionCode = Integer.parseInt(fields[3]);
            this.character = Integer.parseInt(fields[4]);
            this.intelligence = Integer.parseInt(fields[5]);
            this.stars = Integer.parseInt(fields[6]);
            this.isTransfer = "true".equals(fields[7]);
            this.wasRedshirt = "true".equals(fields[8]);
            this.potential = Integer.parseInt(fields[9]);
            this.durability = Integer.parseInt(fields[10]);
            this.recruitOverall = Integer.parseInt(fields[11]);
            this.cost = Integer.parseInt(fields[12]);
            this.rat1 = fields[13];
            this.rat2 = fields[14];
            this.rat3 = fields[15];
            this.rat4 = fields[16];
            this.heightInches = Integer.parseInt(fields[17]);
            this.weightPounds = Integer.parseInt(fields[18]);
            this.currentOverall = Integer.parseInt(fields[19]);
            this.improvement = fields[20];
        } else {
            // Roster Format
            this.position = fields[0];
            this.name = fields[1];
            this.year = fields[2];
            this.potential = Integer.parseInt(fields[3]);
            this.intelligence = Integer.parseInt(fields[4]);
            this.durability = Integer.parseInt(fields[5]);
            this.rat1 = fields[6];
            this.rat2 = fields[7];
            this.rat3 = fields[8];
            this.rat4 = fields[9];
            this.wasRedshirt = "true".equals(fields[11]);
            this.isTransfer = "true".equals(fields[12]);
            this.regionCode = Integer.parseInt(fields[14]);
            this.character = Integer.parseInt(fields[15]);
            this.stars = Integer.parseInt(fields[16]);
            this.heightInches = Integer.parseInt(fields[17]);
            this.weightPounds = Integer.parseInt(fields[18]);
            this.currentOverall = Integer.parseInt(fields[19]);
            this.improvement = fields[20];
            this.recruitOverall = this.currentOverall;
            this.cost = 0;
        }
    }

    public static RecruitingPlayerRecord fromRecruitCsv(String raw) {
        return new RecruitingPlayerRecord(raw, raw.split(","), true);
    }

    public static RecruitingPlayerRecord fromRosterCsv(String raw) {
        return new RecruitingPlayerRecord(raw, raw.split(","), false);
    }

    /**
     * @deprecated Use fromRecruitCsv or fromRosterCsv
     */
    @Deprecated
    public static RecruitingPlayerRecord fromCsv(String raw) {
        return fromRecruitCsv(raw);
    }


    public String raw() {
        return raw;
    }

    public String position() {
        return position;
    }

    public String name() {
        return name;
    }

    public String year() {
        return year;
    }

    public boolean isGraduating() {
        return "5".equals(year);
    }

    public int regionCode() {
        return regionCode;
    }

    public int regionBucket() {
        return regionCode / 10;
    }

    public boolean isTransfer() {
        return isTransfer;
    }

    public int recruitOverall() {
        return recruitOverall;
    }

    public int cost() {
        return cost;
    }

    public int currentOverall() {
        return currentOverall;
    }

    public String improvement() {
        return improvement;
    }

    public int stars() {
        return stars;
    }

    public int intelligence() {
        return intelligence;
    }

    public int character() {
        return character;
    }

    public int potential() {
        return potential;
    }

    public int durability() {

        return durability;
    }

    public int heightInches() {
        return heightInches;
    }

    public int weightPounds() {
        return weightPounds;
    }

    public int weightPounds() {
        return weightPounds;
    }

    public String rat1() {
        return rat1;
    }

    public String rat2() {
        return rat2;
    }

    public String rat3() {
        return rat3;
    }

    public String rat4() {
        return rat4;
    }

    public String listKey() {

        return raw.substring(0, raw.length() - 2);
    }
}

