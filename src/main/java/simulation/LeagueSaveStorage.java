package simulation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class LeagueSaveStorage {
    /**
     * Rolling autosave the Android app writes after each week and when it goes
     * to the background. It is not a numbered slot, so it never shows up in the
     * slot list or counts toward {@link #hasAnySave(File)}.
     */
    public static final String AUTOSAVE_FILE_NAME = "autosave.cfb";

    private LeagueSaveStorage() {
    }

    public static File getAutosaveFile(File filesDir) {
        return getNamedInternalFile(filesDir, AUTOSAVE_FILE_NAME);
    }

    /**
     * Writes {@code data} to {@code target} through a temp file, so a crash or a
     * full disk mid-write leaves the previous save intact. Safe to call off the
     * league thread: it touches only the bytes and the file system.
     */
    public static boolean writeAtomically(File target, byte[] data) {
        File tmp = new File(target.getAbsolutePath() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(data);
            fos.getFD().sync();
        } catch (IOException e) {
            PlatformLog.e("LeagueSaveStorage", "Failed to write temp save file " + tmp.getAbsolutePath(), e);
            deleteQuietly(tmp);
            return false;
        }
        try {
            // POSIX (Android, macOS, Linux): rename replaces the old save
            // atomically, so there is never a moment without a complete save.
            if (tmp.renameTo(target)) {
                return true;
            }
            // Windows cannot rename over an existing file. Move the old save
            // aside first and put it back if the new one cannot take its place.
            if (target.exists()) {
                File old = new File(target.getAbsolutePath() + ".bak");
                deleteQuietly(old);
                if (!target.renameTo(old)) {
                    PlatformLog.e("LeagueSaveStorage", "Could not move existing save file aside: "
                            + target.getAbsolutePath());
                    deleteQuietly(tmp);
                    return false;
                }
                if (tmp.renameTo(target)) {
                    deleteQuietly(old);
                    return true;
                }
                if (!old.renameTo(target)) {
                    PlatformLog.e("LeagueSaveStorage", "Could not restore previous save file "
                            + old.getAbsolutePath());
                }
                deleteQuietly(tmp);
                return false;
            }
            try (FileInputStream in = new FileInputStream(tmp);
                 FileOutputStream out = new FileOutputStream(target)) {
                IoStreams.copy(in, out);
            }
            deleteQuietly(tmp);
            return true;
        } catch (IOException e) {
            PlatformLog.e("LeagueSaveStorage", "Failed to replace save file " + target.getAbsolutePath(), e);
            deleteQuietly(tmp);
            return false;
        }
    }

    private static void deleteQuietly(File file) {
        if (file.exists() && !file.delete()) {
            PlatformLog.w("LeagueSaveStorage", "Could not delete temp save file " + file.getAbsolutePath());
        }
    }

    public static File getSlotFile(File filesDir, int slot) {
        return recoverInterruptedReplace(new File(filesDir, "saveFile" + slot + ".cfb"));
    }

    public static File getNamedInternalFile(File filesDir, String name) {
        return recoverInterruptedReplace(new File(filesDir, name));
    }

    /**
     * Puts back a save that {@link #writeAtomically} moved aside on Windows when the
     * process died before the new file took its place: the save is missing but its
     * {@code .bak} is the complete previous one. Returns {@code target} either way.
     */
    public static File recoverInterruptedReplace(File target) {
        if (target == null || target.exists()) {
            return target;
        }
        File old = new File(target.getAbsolutePath() + ".bak");
        if (old.isFile()) {
            if (old.renameTo(target)) {
                PlatformLog.w("LeagueSaveStorage", "Recovered save from " + old.getAbsolutePath());
            } else {
                PlatformLog.e("LeagueSaveStorage", "Could not recover save from " + old.getAbsolutePath());
            }
        }
        return target;
    }

    public static File getRecruitingSaveFile(File filesDir) {
        return getNamedInternalFile(filesDir, "saveLeagueRecruiting.cfb");
    }

    /** True if any internal save slot (0–19) contains a file. */
    public static boolean hasAnySave(File filesDir) {
        for (int i = 0; i < 20; ++i) {
            if (getSlotFile(filesDir, i).exists()) {
                return true;
            }
        }
        return false;
    }

    public static String[] getSaveFileInfos(File filesDir) {
        return getSaveFileInfos(filesDir, League.CURRENT_SAVE_VERSION);
    }

    public static String[] getSaveFileInfos(File filesDir, String saveVer) {
        String[] infos = new String[20];
        Arrays.fill(infos, "EMPTY");
        for (int i = 0; i < 20; ++i) {
            File saveFile = getSlotFile(filesDir, i);
            if (saveFile.exists()) {
                try {
                    infos[i] = SaveFileSummary.summarize(saveFile, saveVer);
                } catch (IOException ex) {
                    PlatformLog.e("LeagueSaveStorage", "Error reading save file: slot " + i, ex);
                    infos[i] = SaveLoadMessages.UNREADABLE;
                }
            }
        }
        return infos;
    }

    public static File getExportDir(File documentsDir, String folderName) throws IOException {
        if (documentsDir == null) {
            throw new IOException("Documents directory is unavailable");
        }
        File file = new File(documentsDir, folderName);
        if (!file.mkdirs() && !file.exists()) {
            throw new IOException("Directory not created: " + file.getAbsolutePath());
        }
        return file;
    }
}
