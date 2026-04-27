package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LeagueSettingsOptionsTest {
    private DesktopResourceProvider resources;

    @Before
    public void setUp() {
        resources = new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    @Test
    public void normalize_advancedRealignmentRequiresConferenceRealignment() {
        LeagueSettingsOptions options = new LeagueSettingsOptions();
        options.advancedRealignment = true;
        options.conferenceRealignment = false;

        options.normalize();

        assertTrue(options.advancedRealignment);
        assertTrue(options.conferenceRealignment);
    }

    @Test
    public void normalize_promotionRelegationDisablesRealignmentModes() {
        LeagueSettingsOptions options = new LeagueSettingsOptions();
        options.universalProRel = true;
        options.conferenceRealignment = true;
        options.advancedRealignment = true;

        options.normalize();

        assertTrue(options.universalProRel);
        assertFalse(options.conferenceRealignment);
        assertFalse(options.advancedRealignment);
    }

    @Test
    public void applyTo_updatesSharedGameplaySettings() {
        League league = newLeague();
        LeagueSettingsOptions options = LeagueSettingsOptions.fromLeague(league);
        options.showPotential = true;
        options.fullGameLog = true;
        options.careerMode = false;
        options.neverRetire = true;
        options.enableTv = false;
        options.expandedPlayoffs = true;

        options.applyTo(league, true, false, false);

        assertTrue(league.showPotential);
        assertTrue(league.fullGameLog);
        assertFalse(league.careerMode);
        assertTrue(league.neverRetire);
        assertFalse(league.enableTV);
        assertTrue(league.expPlayoffs);
    }

    @Test
    public void applyTo_preservesLockedPlayoffSettingAfterRegularSeasonStarts() {
        League league = newLeague();
        league.expPlayoffs = false;
        LeagueSettingsOptions options = LeagueSettingsOptions.fromLeague(league);
        options.expandedPlayoffs = true;

        options.applyTo(league, false, false, false);

        assertFalse(league.expPlayoffs);
    }

    @Test
    public void applyTo_existingPromotionRelegationKeepsRealignmentOff() {
        League league = newLeague();
        league.enableUnivProRel = true;
        LeagueSettingsOptions options = LeagueSettingsOptions.fromLeague(league);
        options.conferenceRealignment = true;
        options.advancedRealignment = true;

        options.applyTo(league, true, false, false);

        assertTrue(league.enableUnivProRel);
        assertFalse(league.confRealignment);
        assertFalse(league.advancedRealignment);
    }

    @Test
    public void applyTo_preservesPromotionRelegationWhenChangeIsLocked() {
        League league = newLeague();
        league.enableUnivProRel = true;
        LeagueSettingsOptions options = LeagueSettingsOptions.fromLeague(league);
        options.universalProRel = false;

        options.applyTo(league, true, false, false);

        assertTrue(league.enableUnivProRel);
        assertFalse(league.confRealignment);
        assertFalse(league.advancedRealignment);
    }

    private League newLeague() {
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
        return league;
    }
}
