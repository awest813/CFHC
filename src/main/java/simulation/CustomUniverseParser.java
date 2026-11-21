package simulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public final class CustomUniverseParser {
    private CustomUniverseParser() {
    }

    public static LeagueLaunchCoordinator.CustomUniverseFiles parse(InputStream inputStream,
                                                                    File conferences,
                                                                    File teams,
                                                                    File bowls) throws IOException {
        if (inputStream == null) {
            throw new IOException("Unable to open custom universe stream");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = reader.readLine();

            StringBuilder conferencesContent = new StringBuilder();
            conferencesContent.append("[START_CONFERENCES]\n");
            while ((line = reader.readLine()) != null && !line.contains("[END_CONFERENCES]")) {
                conferencesContent.append(line).append('\n');
            }
            conferencesContent.append("[END_CONFERENCES]\n");
            writeFile(conferences, conferencesContent.toString());

            StringBuilder teamsContent = new StringBuilder();
            teamsContent.append("[START_TEAMS]\n");
            int lineNo = 0;
            int teamCount = 0;
            while ((line = reader.readLine()) != null && !line.contains("[END_TEAMS]")) {
                lineNo++;
                if (lineNo > 1 && !line.contains("[END_CONF]")) {
                    teamCount += validateTeamLine(line, lineNo);
                }
                teamsContent.append(line).append('\n');
            }
            if (teamCount == 0) {
                throw new IOException("Custom universe file has no teams");
            }
            teamsContent.append("[END_TEAMS]\n");
            writeFile(teams, teamsContent.toString());

            StringBuilder bowlsContent = new StringBuilder();
            line = reader.readLine();
            if (line == null) {
                throw new IOException("Custom universe file missing bowl data");
            }
            bowlsContent.append(line).append('\n');
            bowlsContent.append("[END_BOWL_NAMES]\n");
            writeFile(bowls, bowlsContent.toString());

            return new LeagueLaunchCoordinator.CustomUniverseFiles(conferences, teams, bowls);
        }
    }

    /**
     * A team row is "name, abbr, conference, prestige, division, location[, rival]".
     * The league reads it with unchecked parses, so a short row or a non-numeric
     * prestige/location used to throw out of the league constructor (an Android
     * crash, as hosts only catch IOException). The league reads every row between
     * the markers as a team, so a blank row is an error too.
     *
     * @return 1 (the row is a team)
     */
    static int validateTeamLine(String line, int lineNo) throws IOException {
        String[] f = line.split(", ").length > 1 ? line.split(", ") : line.split(",");
        if (f.length < 6) {
            throw new IOException("Custom universe team row " + lineNo
                    + " needs name, abbreviation, conference, prestige, division and location: " + line);
        }
        try {
            Integer.parseInt(f[3]);
            Integer.parseInt(f[5]);
        } catch (NumberFormatException e) {
            throw new IOException("Custom universe team row " + lineNo
                    + ": prestige and location must be whole numbers: " + line);
        }
        return 1;
    }

    private static void writeFile(File file, String content) throws IOException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            writer.write(content);
        }
    }
}
