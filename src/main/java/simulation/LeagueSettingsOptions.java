package simulation;

/**
 * Platform-neutral league settings snapshot.
 *
 * <p>Android and desktop can render these options differently, but the option
 * compatibility rules should stay in the simulation layer.</p>
 */
public class LeagueSettingsOptions {
    public boolean showPotential;
    public boolean fullGameLog;
    public boolean careerMode;
    public boolean neverRetire;
    public boolean enableTv;
    public boolean expandedPlayoffs;
    public boolean conferenceRealignment;
    public boolean advancedRealignment;
    public boolean universalProRel;

    public static LeagueSettingsOptions fromLeague(League league) {
        LeagueSettingsOptions options = new LeagueSettingsOptions();
        options.showPotential = league.showPotential;
        options.fullGameLog = league.fullGameLog;
        options.careerMode = league.careerMode;
        options.neverRetire = league.neverRetire;
        options.enableTv = league.enableTV;
        options.expandedPlayoffs = league.expPlayoffs;
        options.conferenceRealignment = league.confRealignment;
        options.advancedRealignment = league.advancedRealignment;
        options.universalProRel = league.enableUnivProRel;
        return options;
    }

    public void normalize() {
        if (universalProRel) {
            conferenceRealignment = false;
            advancedRealignment = false;
        } else if (advancedRealignment) {
            conferenceRealignment = true;
        }
    }

    public void applyTo(League league, boolean allowExpandedPlayoffChange,
                        boolean allowUniversalProRelChange, boolean convertWhenEnablingProRel) {
        normalize();

        league.showPotential = showPotential;
        league.fullGameLog = fullGameLog;
        league.careerMode = careerMode;
        league.neverRetire = neverRetire;
        league.enableTV = enableTv;
        if (allowExpandedPlayoffChange) {
            league.expPlayoffs = expandedPlayoffs;
        }

        if (allowUniversalProRelChange) {
            if (universalProRel && !league.enableUnivProRel && convertWhenEnablingProRel) {
                league.convertUnivProRel();
            }
            league.enableUnivProRel = universalProRel;
        }

        if (league.enableUnivProRel) {
            league.confRealignment = false;
            league.advancedRealignment = false;
        } else {
            league.confRealignment = conferenceRealignment;
            league.advancedRealignment = advancedRealignment;
        }
    }
}
