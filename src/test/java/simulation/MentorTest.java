package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.*;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class MentorTest {

    private League league;
    private Team team;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void isEligibleMentor_eligibleWhenVeteranHighOvrHighChar() {
        PlayerQB qb = new PlayerQB("Vet QB", 1, 4, team);
        qb.year = 3;
        qb.ratOvr = 88;
        qb.character = 80;
        assertTrue(qb.isEligibleMentor());
    }

    @Test
    public void isEligibleMentor_notEligibleWhenLowYear() {
        PlayerQB qb = new PlayerQB("Young QB", 1, 4, team);
        qb.year = 1;
        qb.ratOvr = 88;
        qb.character = 80;
        assertFalse(qb.isEligibleMentor());
    }

    @Test
    public void isEligibleMentor_notEligibleWhenLowOvr() {
        PlayerQB qb = new PlayerQB("Low QB", 1, 2, team);
        qb.year = 3;
        qb.ratOvr = 70;
        qb.character = 80;
        assertFalse(qb.isEligibleMentor());
    }

    @Test
    public void isEligibleMentor_notEligibleWhenLowChar() {
        PlayerQB qb = new PlayerQB("Bad QB", 1, 4, team);
        qb.year = 3;
        qb.ratOvr = 88;
        qb.character = 40;
        assertFalse(qb.isEligibleMentor());
    }

    @Test
    public void hasMentor_returnsTrueWhenSet() {
        PlayerQB qb = new PlayerQB("QB", 1, 3, team);
        assertFalse(qb.hasMentor());
        qb.mentorName = "Vet QB";
        assertTrue(qb.hasMentor());
    }

    @Test
    public void getMentorBonus_returns3WhenMentorExists() {
        PlayerQB mentor = new PlayerQB("Mentor QB", 1, 5, team);
        mentor.year = 4;
        mentor.ratOvr = 92;
        mentor.character = 85;
        mentor.team = team;

        team.teamQBs.add(mentor);

        PlayerQB mentee = new PlayerQB("Mentee QB", 1, 3, team);
        mentee.mentorName = "Mentor QB";
        assertEquals(3, mentee.getMentorBonus());
    }

    @Test
    public void getMentorBonus_returns0WhenMentorNotFound() {
        PlayerQB mentee = new PlayerQB("Lost QB", 1, 3, team);
        mentee.mentorName = "Ghost Mentor";
        assertEquals(0, mentee.getMentorBonus());
    }

    @Test
    public void getMentorBonus_returns0WhenMentorNotEligible() {
        PlayerQB mentor = new PlayerQB("Bad Mentor", 1, 2, team);
        mentor.year = 4;
        mentor.ratOvr = 70;
        mentor.character = 50;
        mentor.team = team;

        team.teamQBs.add(mentor);

        PlayerQB mentee = new PlayerQB("Mentee QB", 1, 3, team);
        mentee.mentorName = "Bad Mentor";
        assertEquals(0, mentee.getMentorBonus());
    }

    @Test
    public void assignMentors_assignsEligibleVeterans() {
        ArrayList<PlayerQB> savedQBs = new ArrayList<>(team.teamQBs);
        team.teamQBs.clear();

        PlayerQB mentor = new PlayerQB("QB Mentor", 1, 5, team);
        mentor.year = 4; mentor.ratOvr = 90; mentor.character = 85;
        team.teamQBs.add(mentor);

        PlayerQB mentee = new PlayerQB("QB Pupil", 1, 3, team);
        mentee.year = 1;
        team.teamQBs.add(mentee);

        team.assignMentors();

        assertFalse("mentee.mentorName: [" + mentee.mentorName + "]", mentee.mentorName.isEmpty());

        team.teamQBs.clear();
        team.teamQBs.addAll(savedQBs);
    }

    @Test
    public void assignMentors_clearsMentorsEachSeason() {
        ArrayList<PlayerQB> savedQBs = new ArrayList<>(team.teamQBs);
        team.teamQBs.clear();

        PlayerQB mentor = new PlayerQB("QB Mentor", 1, 5, team);
        mentor.year = 4; mentor.ratOvr = 90; mentor.character = 85;
        team.teamQBs.add(mentor);

        PlayerQB mentee = new PlayerQB("QB Pupil", 1, 3, team);
        mentee.year = 1;
        mentee.mentorName = "Old Mentor";
        team.teamQBs.add(mentee);

        team.assignMentors();
        assertEquals("QB Mentor", mentee.mentorName);

        team.teamQBs.clear();
        team.teamQBs.addAll(savedQBs);
    }

    @Test
    public void saveLoadRoundTrip_preservesMentorName() {
        PlayerQB qb = new PlayerQB("Save QB", 1, 3, team);
        qb.mentorName = "Vet Mentor";

        var record = qb.toRecord();
        assertEquals("Vet Mentor", record.mentorName());

        PlayerQB loaded = new PlayerQB(team, record);
        assertEquals("Vet Mentor", loaded.mentorName);
    }

    @Test
    public void transferPlayer_resetsMentorName() {
        PlayerQB qb = new PlayerQB("Trans QB", 2, 4, team);
        qb.mentorName = "Old Mentor";
        PlayerQB dest = new PlayerQB(qb, team);
        assertEquals("", dest.mentorName);
    }
}
