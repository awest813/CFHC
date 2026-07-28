package antdroid.cfbcoach.recruiting;

/**
 * Android-only recruiting UI flags (filter / popup / current position).
 * Simulation board state stays in {@link recruiting.RecruitingSessionData}.
 */
public final class RecruitingUiState {

    private boolean showPopUp = true;
    private boolean autoFilter = true;
    private String currentPosition = "QB";

    public boolean isShowPopUp() {
        return showPopUp;
    }

    public void setShowPopUp(boolean showPopUp) {
        this.showPopUp = showPopUp;
    }

    public boolean isAutoFilter() {
        return autoFilter;
    }

    public void setAutoFilter(boolean autoFilter) {
        this.autoFilter = autoFilter;
    }

    public String getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(String currentPosition) {
        this.currentPosition = currentPosition != null ? currentPosition : "QB";
    }
}
