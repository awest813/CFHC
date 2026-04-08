package antdroid.cfbcoach;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

final class SaveFileSummary {
    private SaveFileSummary() {
    }

    static String summarize(File saveFile, String currentSaveVer) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(saveFile))) {
            String header = bufferedReader.readLine();
            if (header == null || header.length() == 0) {
                return "EMPTY";
            }

            String primary = header.endsWith("%")
                    ? header.substring(0, header.length() - 1)
                    : header;
            String title = primary.split(">")[0];

            boolean careerMode = false;
            boolean twelveTeamPlayoff = false;
            String line;
            String previousLine = null;
            while ((line = bufferedReader.readLine()) != null) {
                if ("END_CAREER_MODE".equals(line)) {
                    if (previousLine != null) {
                        careerMode = Boolean.parseBoolean(previousLine.trim());
                    }
                }
                if ("END_EXP_PLAYOFFS".equals(line)) {
                    if (previousLine != null) {
                        twelveTeamPlayoff = Boolean.parseBoolean(previousLine.trim());
                    }
                }
                previousLine = line;
            }

            String mode = careerMode ? "Head Coach Career" : "Open Dynasty";
            String playoff = twelveTeamPlayoff ? "12-Team Playoff" : "4-Team Playoff";
            String versionLine = primary.contains(currentSaveVer)
                    ? "Version: " + currentSaveVer
                    : "Legacy Save  Incompatible";

            return title + "\n" + mode + "  |  " + playoff + "\n" + versionLine;
        }
    }
}
