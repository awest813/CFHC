package antdroid.cfbcoach;

/**
 * Season / career UI flags previously embedded in {@link MainActivity}.
 * Keeps Android shell state out of the activity's field soup without changing behavior.
 */
public final class GameStateManager {

    private int season;
    private boolean newGame;
    private boolean loadedLeague;
    private boolean redshirtComplete;
    private boolean skipRetirementQ;
    private boolean reincarnate;
    private boolean wantUpdateConf = true;
    private int currPage;
    private boolean jobListSet;
    private int jobType;

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public boolean isNewGame() {
        return newGame;
    }

    public void setNewGame(boolean newGame) {
        this.newGame = newGame;
    }

    public boolean isLoadedLeague() {
        return loadedLeague;
    }

    public void setLoadedLeague(boolean loadedLeague) {
        this.loadedLeague = loadedLeague;
    }

    public boolean isRedshirtComplete() {
        return redshirtComplete;
    }

    public void setRedshirtComplete(boolean redshirtComplete) {
        this.redshirtComplete = redshirtComplete;
    }

    public boolean isSkipRetirementQ() {
        return skipRetirementQ;
    }

    public void setSkipRetirementQ(boolean skipRetirementQ) {
        this.skipRetirementQ = skipRetirementQ;
    }

    public boolean isReincarnate() {
        return reincarnate;
    }

    public void setReincarnate(boolean reincarnate) {
        this.reincarnate = reincarnate;
    }

    public boolean isWantUpdateConf() {
        return wantUpdateConf;
    }

    public void setWantUpdateConf(boolean wantUpdateConf) {
        this.wantUpdateConf = wantUpdateConf;
    }

    public int getCurrPage() {
        return currPage;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public boolean isJobListSet() {
        return jobListSet;
    }

    public void setJobListSet(boolean jobListSet) {
        this.jobListSet = jobListSet;
    }

    public int getJobType() {
        return jobType;
    }

    public void setJobType(int jobType) {
        this.jobType = jobType;
    }
}
