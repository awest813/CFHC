package simulation;

import java.util.List;

/**
 * A complete, portable snapshot of a League's state.
 * This can be serialized to any format (JSON, XML, Custom)
 * and is entirely platform-agnostic.
 */
public record LeagueRecord(
    String leagueName,
    int year,
    int currentWeek,
    List<ConferenceRecord> conferences,
    List<PlayerRecord> leagueHoF,
    List<DataRecord> leagueRecords,
    String heismanWinnerName,
    String nationalChampName
) {
    // Nested records for structured hierarchy
    public record ConferenceRecord(
        String name,
        List<TeamRecord> teams
    ) {}

    public record TeamRecord(
        String name,
        String abbr,
        int prestige,
        StaffRecord headCoach,
        StaffRecord offenseCoach,
        StaffRecord defenseCoach,
        List<PlayerRecord> roster,
        List<TeamHistoryRecord> history,
        List<DataRecord> records
    ) {}
}
