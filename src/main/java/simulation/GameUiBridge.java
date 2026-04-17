package simulation;

import java.io.File;
import java.io.IOException;

import positions.Player;

/**
 * Thin platform bridge used by the portable simulation layer to request UI work.
 * Android can implement this directly today, while future iPhone/desktop shells
 * can provide their own adapters without the core sim depending on activities.
 */
public interface GameUiBridge {
    GameUiBridge NO_OP = new GameUiBridge() {
        @Override
        public void crash() {}

        @Override
        public void startRecruiting(File saveFile, Team userTeam) {}

        @Override
        public void transferPlayer(Player player) {}

        @Override
        public void updateSpinners() {}

        @Override
        public void disciplineAction(Player player, String issue, int gamesA, int gamesB) {}

        @Override
        public void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent) {}

        @Override
        public void showNotification(String title, String message) {}

        @Override
        public void refreshCurrentPage() {}

        @Override
        public void showAwardsSummary(String summaryText) {}

        @Override
        public void showMidseasonSummary() {}

        @Override
        public void showSeasonSummary() {}

        @Override
        public void showContractDialog() {}

        @Override
        public void showJobOffersDialog() {}

        @Override
        public void showPromotionsDialog() {}

        @Override
        public void showRedshirtList() {}

        @Override
        public void showTransferList() {}

        @Override
        public void showRealignmentSummary() {}

        @Override
        public void startRecruitingFlow() {}
    };




    void crash();

    void startRecruiting(File saveFile, Team userTeam) throws InterruptedException, IOException;

    void transferPlayer(Player player);

    void updateSpinners();

    void disciplineAction(Player player, String issue, int gamesA, int gamesB);

    /**
     * Update the main simulation button and status indicators.
     */
    void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent);

    /**
     * Show a notification or alert to the user.
     */
    void showNotification(String title, String message);

    /**
     * Trigger a UI refresh of the current page.
     */
    void refreshCurrentPage();

    /**
     * Show the Heisman ceremony or award summary.
     */
    void showAwardsSummary(String summaryText);

    /**
     * Show the midseason recruitment and performance summary.
     */
    void showMidseasonSummary();

    /**
     * Show the final season summary and hall of fame updates.
     */
    void showSeasonSummary();

    /**
     * Show the contract extension/renewal dialog for the user.
     */
    void showContractDialog();

    /**
     * Show the job offers dialog for career mode.
     */
    void showJobOffersDialog();

    /**
     * Show the promotion/coordinator change dialog.
     */
    void showPromotionsDialog();

    /**
     * Show the redshirt list/management dialog.
     */
    void showRedshirtList();

    /**
     * Show the transfer list/management dialog.
     */
    void showTransferList();

    /**
     * Show the conference realignment summary.
     */
    void showRealignmentSummary();

    /**
     * Trigger the recruiting flow transition.
     */
    void startRecruitingFlow();



}
