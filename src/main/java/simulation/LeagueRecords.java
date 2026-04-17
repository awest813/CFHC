package simulation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LeagueRecords {
    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    private final DecimalFormat df2 = new DecimalFormat("#.##", symbols);
    private final HashMap<String, DataRecord> records;


    private final String[] recordsList = {"TEAM", "Team PPG", "Team Opp PPG", "Team YPG", "Team Opp YPG", "Team TO Diff",
            "SEASON: OFFENSE", "Pass Yards", "Pass TDs", "Ints Thrown", "Comp Percent", "QB Rating",
            "Rush Yards", "Rush TDs", "Fumbles Lost", "Receptions",
            "Rec Yards", "Rec TDs", "SEASON: DEFENSE", "Tackles", "Sacks", "Fumbles Recovered", "Interceptions", "Passes Defended", "SEASON: ST", "Field Goals", "Kick Ret Yards", "Kick Ret TDs", "Punt Ret Yards", "Punt Ret TDs",
            "CAREER: OFFENSE", "Career Pass Yards", "Career Pass TDs", "Career Ints Thrown", "Career Comp PCT", "Career QB Rating",
            "Career Rush Yards", "Career Rush TDs", "Career Fumbles Lost", "Career Receptions",
            "Career Rec Yards", "Career Rec TDs", "CAREER: DEFENSE", "Career Tackles", "Career Sacks", "Career Fumbles Rec", "Career Interceptions", "Career Defended",
            "CAREER: SPEC TEAMS", "Career Field Goals", "Career KR Yards", "Career KR TDs", "Career PR Yards", "Career PR TDs",
            "COACHING RECORDS", "Wins", "National Championships", "Conf Championships", "Bowl Wins", "Bowl Appearances", "Coach Awards", "All-Americans", "All-Conferences", "Coach Year Score"};

    public LeagueRecords(ArrayList<String> recordStrings) {
        records = new HashMap<>();
        for (String str : recordStrings) {
            DataRecord r = DataRecord.fromCsv(str);
            if (r != null) records.put(r.key(), r);
        }

    }

    public LeagueRecords() {
        records = new HashMap<>();
        records.put("TEAM", null);
        records.put("Team PPG", new DataRecord("Team PPG", 0, "XXX%XXX", 0));
        records.put("Team Opp PPG", new DataRecord("Team Opp PPG", 1000, "XXX%XXX", 0));
        records.put("Team YPG", new DataRecord("Team YPG", 0, "XXX%XXX", 0));
        records.put("Team Opp YPG", new DataRecord("Team Opp YPG", 1000, "XXX%XXX", 0));
        records.put("Team TO Diff", new DataRecord("Team TO Diff", 0, "XXX%XXX", 0));
        records.put("SEASON: OFFENSE", null);
        records.put("Pass Yards", new DataRecord("Pass Yards", 0, "XXX%XXX", 0));
        records.put("Pass TDs", new DataRecord("Pass TDs", 0, "XXX%XXX", 0));
        records.put("Ints Thrown", new DataRecord("Ints Thrown", 0, "XXX%XXX", 0));
        records.put("Comp Percent", new DataRecord("Comp Percent", 0, "XXX%XXX", 0));
        records.put("QB Rating", new DataRecord("QB Rating", 0, "XXX%XXX", 0));
        records.put("Rush Yards", new DataRecord("Rush Yards", 0, "XXX%XXX", 0));
        records.put("Rush TDs", new DataRecord("Rush TDs", 0, "XXX%XXX", 0));
        records.put("Fumbles Lost", new DataRecord("Fumbles Lost", 0, "XXX%XXX", 0));
        records.put("Receptions", new DataRecord("Receptions", 0, "XXX%XXX", 0));
        records.put("Rec Yards", new DataRecord("Rec Yards", 0, "XXX%XXX", 0));
        records.put("Rec TDs", new DataRecord("Rec TDs", 0, "XXX%XXX", 0));
        records.put("SEASON: DEFENSE", null);
        records.put("Tackles", new DataRecord("Tackles", 0, "XXX%XXX", 0));
        records.put("Sacks", new DataRecord("Sacks", 0, "XXX%XXX", 0));
        records.put("Fumbles Recovered", new DataRecord("Fumbles Recovered", 0, "XXX%XXX", 0));
        records.put("Interceptions", new DataRecord("Interceptions", 0, "XXX%XXX", 0));
        records.put("Passes Defended", new DataRecord("Passes Defended", 0, "XXX%XXX", 0));
        records.put("SEASON: ST", null);
        records.put("Field Goals", new DataRecord("Field Goals", 0, "XXX%XXX", 0));
        records.put("Kick Ret Yards", new DataRecord("Kick Ret Yards", 0, "XXX%XXX", 0));
        records.put("Kick Ret TDs", new DataRecord("Kick Ret TDs", 0, "XXX%XXX", 0));
        records.put("Punt Ret Yards", new DataRecord("Punt Ret Yards", 0, "XXX%XXX", 0));
        records.put("Punt Ret TDs", new DataRecord("Punt Ret TDs", 0, "XXX%XXX", 0));
        records.put("CAREER: OFFENSE", null);
        records.put("Career Pass Yards", new DataRecord("Career Pass Yards", 0, "XXX%XXX", 0));
        records.put("Career Pass TDs", new DataRecord("Career Pass TDs", 0, "XXX%XXX", 0));
        records.put("Career Ints Thrown", new DataRecord("Career Ints Thrown", 0, "XXX%XXX", 0));
        records.put("Career Comp PCT", new DataRecord("Career Comp PCT", 0, "XXX%XXX", 0));
        records.put("Career QB Rating", new DataRecord("Career QB Rating", 0, "XXX%XXX", 0));
        records.put("Career Rush Yards", new DataRecord("Career Rush Yards", 0, "XXX%XXX", 0));
        records.put("Career Rush TDs", new DataRecord("Career Rush TDs", 0, "XXX%XXX", 0));
        records.put("Career Fumbles Lost", new DataRecord("Career Fumbles Lost", 0, "XXX%XXX", 0));
        records.put("Career Receptions", new DataRecord("Career Receptions", 0, "XXX%XXX", 0));
        records.put("Career Rec Yards", new DataRecord("Career Rec Yards", 0, "XXX%XXX", 0));
        records.put("Career Rec TDs", new DataRecord("Career Rec TDs", 0, "XXX%XXX", 0));
        records.put("CAREER: DEFENSE", null);
        records.put("Career Tackles", new DataRecord("Career Tackles", 0, "XXX%XXX", 0));
        records.put("Career Sacks", new DataRecord("Career Sacks", 0, "XXX%XXX", 0));
        records.put("Career Fumbles Rec", new DataRecord("Career Fumbles Rec", 0, "XXX%XXX", 0));
        records.put("Career Interceptions", new DataRecord("Career Interceptions", 0, "XXX%XXX", 0));
        records.put("Career Defended", new DataRecord("Career Defended", 0, "XXX%XXX", 0));
        records.put("CAREER: SPEC TEAMS", null);
        records.put("Career Field Goals", new DataRecord("Career Field Goals", 0, "XXX%XXX", 0));
        records.put("Career KR Yards", new DataRecord("Career KR Yards", 0, "XXX%XXX", 0));
        records.put("Career KR TDs", new DataRecord("Career KR TDs", 0, "XXX%XXX", 0));
        records.put("Career PR Yards", new DataRecord("Career PR Yards", 0, "XXX%XXX", 0));
        records.put("Career PR TDs", new DataRecord("Career PR TDs", 0, "XXX%XXX", 0));
        records.put("COACHING RECORDS", null);
        records.put("Wins", new DataRecord("Wins", 0, "XXX%XXX", 0));
        records.put("National Championships", new DataRecord("National Championships", 0, "XXX%XXX", 0));
        records.put("Conf Championships", new DataRecord("Conf Championships", 0, "XXX%XXX", 0));
        records.put("Bowl Wins", new DataRecord("Bowl Wins", 0, "XXX%XXX", 0));
        records.put("Bowl Appearances", new DataRecord("Bowl Appearances", 0, "XXX%XXX", 0));
        records.put("Coach Awards", new DataRecord("Coach Awards", 0, "XXX%XXX", 0));
        records.put("All-Americans", new DataRecord("All-Americans", 0, "XXX%XXX", 0));
        records.put("All-Conferences", new DataRecord("All-Conferences", 0, "XXX%XXX", 0));
        records.put("Coach Year Score", new DataRecord("Coach Year Score", 0, "XXX%XXX", 0));

    }

    public void checkRecord(String record, float number, String holder, int year) {
        if (holder.split("%").length < 2) holder = holder + "% ";
        if (record.equals("Team Opp PPG") || record.equals("Team Opp YPG")) {
            // Is a record where lower = better
            if ((records.containsKey(record) && number < records.get(record).value())) {
                records.remove(record);
                records.put(record, new DataRecord(record, number, holder, year));
            } else if (!records.containsKey(record)) {
                records.put(record, new DataRecord(record, number, holder, year));
            }
        } else {
            // Is a record where higher = better
            if ((records.containsKey(record) && number > records.get(record).value())) {
                records.remove(record);
                records.put(record, new DataRecord(record, number, holder, year));
            } else if (!records.containsKey(record)) {
                records.put(record, new DataRecord(record, number, holder, year));
            }
        }
    }

    public void addRecord(DataRecord r) {
        if (r != null) records.put(r.key(), r);
    }




    public void changeAbbrRecords(String oldAbbr, String newAbbr) {
        for (String s : recordsList) {
            DataRecord r = records.get(s);
            if (r != null && r.holder().split(" ")[0].equals(oldAbbr)) {
                String[] split = r.holder().split(" ");
                String newHolder = newAbbr;
                for (int i = 1; i < split.length; ++i) {
                    newHolder += " " + split[i];
                }
                records.put(s, new DataRecord(r.key(), r.value(), newHolder, r.year()));
            }
        }
    }


    public String getRecordsStr() {
        StringBuilder sb = new StringBuilder();
        for (String s : recordsList) {
            sb.append(recordStrCSV(s) + "\n");
        }
        return sb.toString();
    }

    public ArrayList<DataRecord> toRecordList() {
        ArrayList<DataRecord> list = new ArrayList<>();
        for (String s : recordsList) {
            DataRecord r = records.get(s);
            if (r != null) list.add(r);
        }
        return list;
    }


    private String recordStrCSV(String key) {
        if (records.containsKey(key)) {
            DataRecord r = records.get(key);
            if (r == null) return key + ",-1,-1,-1";
            return r.toCsv(df2);
        } else return "ERROR,ERROR,ERROR,ERROR";
    }


    /**
     * Print out string of all the records broken by a team that year
     *
     * @return string of all records broken
     */
    public String brokenRecordsStr(int year, String abbr) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, DataRecord> r : records.entrySet()) {
            if (r.getValue() != null && r.getValue().holder().split("%").length > 1) {
                if (r.getValue().holder().split("%")[1].equals(abbr) &&
                        r.getValue().year() == year) {
                    sb.append("+ " + r.getValue().holder().split("%")[0] + " League broke the record for " +
                            r.getKey() + " with " + df2.format(r.getValue().value()) + "!\n");
                }
            }
        }

        if (sb.length() == 0) sb.append("None");

        return sb.toString();
    }

}
