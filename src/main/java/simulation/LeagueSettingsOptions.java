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
    public FcsPromotionMode fcsPromotionMode = FcsPromotionMode.UNLIMITED;
    public int fcsPromotionCap = League.DEFAULT_FCS_PROMOTION_CAP;

    /** Largest sensible cap: the size of the FCS name pool. */
    public static final int MAX_FCS_PROMOTION_CAP = 33;

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
        options.fcsPromotionMode = league.fcsPromotionMode;
        options.fcsPromotionCap = league.fcsPromotionCap;
        return options;
    }

    public void normalize() {
        if (universalProRel) {
            conferenceRealignment = false;
            advancedRealignment = false;
        } else if (advancedRealignment) {
            conferenceRealignment = true;
        }
        if (fcsPromotionMode == null) {
            fcsPromotionMode = FcsPromotionMode.UNLIMITED;
        }
        fcsPromotionCap = Math.max(1, Math.min(MAX_FCS_PROMOTION_CAP, fcsPromotionCap));
    }

    public void applyTo(League league, boolean allowExpandedPlayoffChange,
                        boolean allowUniversalProRelChange, boolean convertWhenEnablingProRel) {
        normalize();

        league.showPotential = showPotential;
        league.fullGameLog = fullGameLog;
        league.careerMode = careerMode;
        league.neverRetire = neverRetire;
        league.enableTV = enableTv;
        league.fcsPromotionMode = fcsPromotionMode;
        league.fcsPromotionCap = fcsPromotionCap;
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
