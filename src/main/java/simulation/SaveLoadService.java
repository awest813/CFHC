package simulation;

import java.io.File;
import java.io.IOException;

/**
 * Service for managing game persistence (saving and loading metadata).
 */
public final class SaveLoadService {

    private final File filesDir;

    public SaveLoadService(File filesDir) {
        this.filesDir = filesDir;
    }

    /**
     * Get summaries for all save slots.
     */
    public String[] getSaveFileSummaries() {
        return LeagueSaveStorage.getSaveFileInfos(filesDir);
    }

    /**
     * Save the given league to a specific slot.
     */
    public boolean saveToSlot(League league, int slot) {
        File saveFile = LeagueSaveStorage.getSlotFile(filesDir, slot);
        return league.saveLeague(saveFile);
    }

    /**
     * Save the given league for recruiting resumption.
     */
    public boolean saveForRecruiting(League league) {
        File saveFile = LeagueSaveStorage.getRecruitingSaveFile(filesDir);
        return league.saveLeague(saveFile);
    }

    /**
     * Check if a specific slot is empty.
     */
    public boolean isSlotEmpty(int slot) {
        return !LeagueSaveStorage.getSlotFile(filesDir, slot).exists();
    }
    
    /**
     * Get the file for a specific slot.
     */
    public File getSlotFile(int slot) {
        return LeagueSaveStorage.getSlotFile(filesDir, slot);
    }

    /**
     * Copy a save file to the recruiting slot.
     */
    public void copyToRecruitingSlot(File source) throws IOException {
        File dest = LeagueSaveStorage.getRecruitingSaveFile(filesDir);
        copyFile(source, dest);
    }

    private static void copyFile(File source, File dest) throws IOException {
        try (java.io.InputStream is = new java.io.FileInputStream(source);
             java.io.OutputStream os = new java.io.FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
}

