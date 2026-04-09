package antdroid.cfbcoach;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import simulation.GameUiBridge;
import simulation.League;
import simulation.Team;

public final class LeagueLaunchCoordinator {
    public interface CustomUniverseImporter {
        CustomUniverseFiles importFromUri(Uri uri) throws IOException;
    }

    public static final class CustomUniverseFiles {
        public final File conferences;
        public final File teams;
        public final File bowls;

        public CustomUniverseFiles(File conferences, File teams, File bowls) {
            this.conferences = conferences;
            this.teams = teams;
            this.bowls = bowls;
        }
    }

    public static final class LaunchResult {
        public final League league;
        public final Team userTeam;
        public final Team currentTeam;
        public final int season;
        public final boolean loadedLeague;
        public final boolean newGame;
        public final boolean showSeasonGoals;

        public LaunchResult(League league, Team userTeam, Team currentTeam, int season,
                            boolean loadedLeague, boolean newGame, boolean showSeasonGoals) {
            this.league = league;
            this.userTeam = userTeam;
            this.currentTeam = currentTeam;
            this.season = season;
            this.loadedLeague = loadedLeague;
            this.newGame = newGame;
            this.showSeasonGoals = showSeasonGoals;
        }
    }

    private LeagueLaunchCoordinator() {
    }

    public static LaunchResult load(Bundle extras,
                                    File filesDir,
                                    ContentResolver contentResolver,
                                    GameUiBridge bridge,
                                    int seasonStart,
                                    String playerNames,
                                    String lastNames,
                                    String conferencesText,
                                    String teamsText,
                                    String bowlsText,
                                    CustomUniverseImporter customImporter) throws IOException {
        if (extras != null) {
            String saveFileStr = extras.getString("SAVE_FILE");
            if (saveFileStr != null) {
                if (saveFileStr.contains("NEW_LEAGUE")) {
                    if (saveFileStr.contains("CUSTOM")) {
                        String[] filesSplit = saveFileStr.split(",");
                        CustomUniverseFiles customFiles = customImporter.importFromUri(Uri.parse(filesSplit[1]));
                        League league;
                        if (saveFileStr.contains("RANDOM")) {
                            league = new League(playerNames, lastNames, customFiles.conferences, customFiles.teams, customFiles.bowls, true, false, bridge);
                        } else if (saveFileStr.contains("EQUALIZE")) {
                            league = new League(playerNames, lastNames, customFiles.conferences, customFiles.teams, customFiles.bowls, false, true, bridge);
                        } else {
                            league = new League(playerNames, lastNames, customFiles.conferences, customFiles.teams, customFiles.bowls, false, false, bridge);
                        }
                        return new LaunchResult(league, null, null, seasonStart, false, true, false);
                    }

                    League league;
                    if (saveFileStr.contains("RANDOM")) {
                        league = new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, true, false);
                    } else if (saveFileStr.contains("EQUALIZE")) {
                        league = new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, false, true);
                    } else {
                        league = new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, false, false);
                    }
                    return new LaunchResult(league, null, null, seasonStart, false, false, false);
                }

                if (saveFileStr.equals("DONE_RECRUITING")) {
                    File saveFile = LeagueSaveStorage.getRecruitingSaveFile(filesDir);
                    if (saveFile.exists()) {
                        League league = new League(saveFile, playerNames, lastNames, bridge, false);
                        Team userTeam = league.userTeam;
                        if (userTeam == null) {
                            throw new IOException("Loaded recruiting resume save without a user team");
                        }
                        userTeam.HC.user = true;
                        String recruits = extras.getString("RECRUITS");
                        if (recruits != null && !recruits.isEmpty()) {
                            userTeam.recruitPlayersFromStr(recruits);
                        }
                        league.updateTeamTalentRatings();
                        return new LaunchResult(league, userTeam, userTeam, league.getYear(), true, false, false);
                    }
                } else if (saveFileStr.contains("IMPORT")) {
                    String[] filesSplit = saveFileStr.split(",");
                    try (InputStream inputStream = contentResolver.openInputStream(Uri.parse(filesSplit[1]))) {
                        if (inputStream == null) {
                            throw new IOException("Unable to open imported save stream");
                        }
                        League league = new League(inputStream, playerNames, lastNames, bridge);
                        Team userTeam = league.userTeam;
                        if (userTeam == null) {
                            throw new IOException("Imported save did not contain a user team");
                        }
                        userTeam.HC.user = true;
                        league.updateTeamTalentRatings();
                        return new LaunchResult(league, userTeam, userTeam, league.getYear(), true, false, false);
                    }
                } else {
                    File saveFile = LeagueSaveStorage.getNamedInternalFile(filesDir, saveFileStr);
                    if (saveFile.exists()) {
                        League league = new League(saveFile, playerNames, lastNames, bridge, true);
                        Team userTeam = league.userTeam;
                        if (userTeam == null) {
                            throw new IOException("Loaded save did not contain a user team");
                        }
                        userTeam.HC.user = true;
                        league.updateTeamTalentRatings();
                        return new LaunchResult(league, userTeam, userTeam, league.getYear(), true, false, league.getYear() == seasonStart);
                    }
                }
            }
        }

        League league = new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, false, false);
        return new LaunchResult(league, null, null, seasonStart, false, false, false);
    }
}
