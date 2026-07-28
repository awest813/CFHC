package simulation;

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

            String schemaVersion = null;
            String primary;
            if (header.startsWith(SaveSchema.VERSION_PREFIX)) {
                schemaVersion = SaveSchema.normalize(
                        header.substring(SaveSchema.VERSION_PREFIX.length()));
                String next = bufferedReader.readLine();
                if (next == null || next.isEmpty()) {
                    return "EMPTY";
                }
                primary = stripTrailingPercent(next);
            } else {
                primary = stripTrailingPercent(header);
                if (primary.startsWith("L:")) {
                    // Pre-V: SaveManager files are the current generation.
                    schemaVersion = SaveSchema.unversionedNewFormatDefault();
                }
            }

            String title = primary.startsWith("L:")
                    ? primary.substring(2).split("\t", 2)[0].split(">", 2)[0]
                    : primary.split(">")[0];

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
            String versionLine;
            if (schemaVersion != null && SaveSchema.isSupported(schemaVersion)) {
                versionLine = "Version: " + schemaVersion;
            } else if (schemaVersion != null) {
                versionLine = "Unsupported Version: " + schemaVersion;
            } else if (primary.contains(currentSaveVer)) {
                versionLine = "Version: " + currentSaveVer;
            } else {
                versionLine = "Legacy Save  Incompatible";
            }

            return title + "\n" + mode + "  |  " + playoff + "\n" + versionLine;
        }
    }

    private static String stripTrailingPercent(String header) {
        return header.endsWith("%") ? header.substring(0, header.length() - 1) : header;
    }
}
