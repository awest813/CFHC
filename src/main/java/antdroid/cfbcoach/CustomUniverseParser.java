package antdroid.cfbcoach;

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
            while ((line = reader.readLine()) != null && !line.contains("[END_TEAMS]")) {
                teamsContent.append(line).append('\n');
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

    private static void writeFile(File file, String content) throws IOException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            writer.write(content);
        }
    }
}
