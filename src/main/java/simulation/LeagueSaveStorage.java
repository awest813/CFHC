package simulation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public final class LeagueSaveStorage {
    private LeagueSaveStorage() {
    }

    public static File getSlotFile(File filesDir, int slot) {
        return new File(filesDir, "saveFile" + slot + ".cfb");
    }

    public static File getNamedInternalFile(File filesDir, String name) {
        return new File(filesDir, name);
    }

    public static File getRecruitingSaveFile(File filesDir) {
        return getNamedInternalFile(filesDir, "saveLeagueRecruiting.cfb");
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
                    System.out.println("Error reading file");
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
