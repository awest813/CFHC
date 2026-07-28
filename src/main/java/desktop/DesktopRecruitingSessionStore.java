package desktop;

import recruiting.RecruitingSessionData;
import simulation.League;
import simulation.PlatformLog;
import simulation.SimulationFacade;

import java.io.File;

/**
 * Owns docked recruiting session state and sidecar checkpoint I/O for the desktop shell.
 */
final class DesktopRecruitingSessionStore {
    private static final String TAG = "DesktopRecruitingSessionStore";

    private RecruitingSessionData session;
    private String boardPayload;

    RecruitingSessionData session() {
        return session;
    }

    String boardPayload() {
        return boardPayload;
    }

    boolean hasSession() {
        return session != null && boardPayload != null;
    }

    void clearMemory() {
        session = null;
        boardPayload = null;
    }

    void ensureLoaded(League league, File leagueSave) {
        if (hasSession()) {
            return;
        }
        if (league == null || league.userTeam == null) {
            PlatformLog.w(TAG, "Cannot load recruiting session without a user team");
            clearMemory();
            return;
        }
        File chkFile = DesktopRecruitingCheckpoint.pathFor(leagueSave, league);
        try {
            DesktopRecruitingCheckpoint checkpoint = DesktopRecruitingCheckpoint.read(chkFile);
            if (checkpoint != null && checkpoint.matches(league)) {
                session = checkpoint.restoreSession();
                boardPayload = checkpoint.boardPayload;
                PlatformLog.i(TAG, "Restored recruiting checkpoint from " + chkFile.getAbsolutePath());
                return;
            }
        } catch (Exception ex) {
            PlatformLog.w(TAG, "Recruiting checkpoint load failed: " + ex.getMessage());
        }
        boardPayload = SimulationFacade.buildRecruitingPayload(league.userTeam);
        session = SimulationFacade.prepareRecruitingSessionFromPayload(boardPayload);
    }

    /**
     * @return null on success, or an error message suitable for a dialog
     */
    String persist(League league, File leagueSave) {
        if (!hasSession()) {
            return null;
        }
        DesktopRecruitingCheckpoint checkpoint = DesktopRecruitingCheckpoint.capture(
                league, boardPayload, session);
        File chkFile = DesktopRecruitingCheckpoint.pathFor(leagueSave, league);
        try {
            DesktopRecruitingCheckpoint.write(chkFile, checkpoint);
            PlatformLog.i(TAG, "Wrote recruiting checkpoint " + chkFile.getAbsolutePath());
            return null;
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Failed to write recruiting checkpoint", ex);
            return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
        }
    }

    void persistQuietly(League league, File leagueSave) {
        if (!hasSession()) {
            return;
        }
        try {
            DesktopRecruitingCheckpoint checkpoint = DesktopRecruitingCheckpoint.capture(
                    league, boardPayload, session);
            DesktopRecruitingCheckpoint.write(
                    DesktopRecruitingCheckpoint.pathFor(leagueSave, league), checkpoint);
        } catch (Exception ex) {
            PlatformLog.w(TAG, "Quiet recruiting checkpoint failed: " + ex.getMessage());
        }
    }

    void migrateAfterSaveAs(League league, File previousPath, File newPath) {
        if (previousPath == null || newPath == null) {
            return;
        }
        if (previousPath.getAbsoluteFile().equals(newPath.getAbsoluteFile())) {
            return;
        }
        File oldChk = DesktopRecruitingCheckpoint.pathFor(previousPath, league);
        File newChk = DesktopRecruitingCheckpoint.pathFor(newPath, league);
        if (oldChk.getAbsoluteFile().equals(newChk.getAbsoluteFile())) {
            return;
        }
        try {
            if (!newChk.isFile() && oldChk.isFile()) {
                DesktopRecruitingCheckpoint existing = DesktopRecruitingCheckpoint.read(oldChk);
                if (existing != null) {
                    DesktopRecruitingCheckpoint.write(newChk, existing);
                }
            }
            // Only remove the old sidecar after the new one is present (or there was nothing to keep).
            if (newChk.isFile() || !oldChk.isFile()) {
                DesktopRecruitingCheckpoint.clear(oldChk);
            } else {
                PlatformLog.w(TAG, "Keeping recruiting checkpoint at " + oldChk.getAbsolutePath()
                        + " because migration to " + newChk.getAbsolutePath() + " did not produce a file");
            }
        } catch (Exception ex) {
            PlatformLog.w(TAG, "Could not migrate recruiting checkpoint: " + ex.getMessage());
        }
    }

    void clearAll(League league, File leagueSave) {
        DesktopRecruitingCheckpoint.clear(DesktopRecruitingCheckpoint.pathFor(leagueSave, league));
        clearMemory();
    }
}
