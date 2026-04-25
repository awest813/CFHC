package simulation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import simulation.GameUiBridge;
import simulation.League;
import simulation.Team;

public final class LeagueLaunchCoordinator {
    public static final class LaunchRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        public enum Action {
            NEW_LEAGUE,
            NEW_CUSTOM_LEAGUE,
            LOAD_INTERNAL,
            IMPORT_SAVE,
            DONE_RECRUITING
        }

        public enum PrestigeMode {
            DEFAULT,
            RANDOMIZE,
            EQUALIZE
        }

        public final Action action;
        public final PrestigeMode prestigeMode;
        public final String saveFileName;
        public final String importUri;
        public final String recruits;

        private LaunchRequest(Action action, PrestigeMode prestigeMode, String saveFileName, String importUri, String recruits) {
            this.action = action;
            this.prestigeMode = prestigeMode;
            this.saveFileName = saveFileName;
            this.importUri = importUri;
            this.recruits = recruits;
        }

        public static LaunchRequest newLeague(PrestigeMode prestigeMode) {
            return new LaunchRequest(Action.NEW_LEAGUE, prestigeMode, null, null, null);
        }

        public static LaunchRequest newCustomLeague(String importUri, PrestigeMode prestigeMode) {
            return new LaunchRequest(Action.NEW_CUSTOM_LEAGUE, prestigeMode, null, importUri, null);
        }

        public static LaunchRequest loadInternal(String saveFileName) {
            return new LaunchRequest(Action.LOAD_INTERNAL, PrestigeMode.DEFAULT, saveFileName, null, null);
        }

        public static LaunchRequest importSave(String importUri) {
            return new LaunchRequest(Action.IMPORT_SAVE, PrestigeMode.DEFAULT, null, importUri, null);
        }

        public static LaunchRequest doneRecruiting(String recruits) {
            return new LaunchRequest(Action.DONE_RECRUITING, PrestigeMode.DEFAULT, null, null, recruits);
        }

        public boolean isCustomUniverse() {
            return action == Action.NEW_CUSTOM_LEAGUE;
        }

        public static LaunchRequest fromLegacy(String saveFile, String recruits) {
            if (saveFile == null) {
                return null;
            }
            if (saveFile.startsWith("saveFile") && saveFile.endsWith(".cfb")) {
                return loadInternal(saveFile);
            }
            if (saveFile.startsWith("IMPORT_GAME,")) {
                return importSave(saveFile.substring("IMPORT_GAME,".length()));
            }
            if (saveFile.equals("DONE_RECRUITING")) {
                return doneRecruiting(recruits);
            }
            if (saveFile.startsWith("NEW_LEAGUE_CUSTOM")) {
                String[] filesSplit = saveFile.split(",", 2);
                String uri = filesSplit.length > 1 ? filesSplit[1] : null;
                return newCustomLeague(uri, parsePrestigeMode(saveFile));
            }
            if (saveFile.startsWith("NEW_LEAGUE")) {
                return newLeague(parsePrestigeMode(saveFile));
            }
            return loadInternal(saveFile);
        }

        private static PrestigeMode parsePrestigeMode(String saveFile) {
            if (saveFile.contains("RANDOM")) {
                return PrestigeMode.RANDOMIZE;
            }
            if (saveFile.contains("EQUALIZE")) {
                return PrestigeMode.EQUALIZE;
            }
            return PrestigeMode.DEFAULT;
        }
    }

    public interface CustomUniverseImporter {
        CustomUniverseFiles importFromUri(String uri) throws IOException;
    }

    public interface SaveImportStreamOpener {
        InputStream open(String uri) throws IOException;
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

    public static LaunchResult load(LaunchRequest request,
                                    File filesDir,
                                    GameUiBridge bridge,
                                    int seasonStart,
                                    String playerNames,
                                    String lastNames,
                                    String conferencesText,
                                    String teamsText,
                                    String bowlsText,
                                    CustomUniverseImporter customImporter,
                                    SaveImportStreamOpener saveImportStreamOpener) throws IOException {
        if (request != null) {
            if (request.action == LaunchRequest.Action.NEW_CUSTOM_LEAGUE) {
                CustomUniverseFiles customFiles = customImporter.importFromUri(request.importUri);
                League league = buildLeagueFromMode(request.prestigeMode, playerNames, lastNames, customFiles, bridge);
                return new LaunchResult(league, null, null, seasonStart, false, true, false);
            }

            if (request.action == LaunchRequest.Action.NEW_LEAGUE) {
                League league = buildLeagueFromMode(request.prestigeMode, playerNames, lastNames, conferencesText, teamsText, bowlsText);
                return new LaunchResult(league, null, null, seasonStart, false, false, false);
            }

            if (request.action == LaunchRequest.Action.DONE_RECRUITING) {
                File saveFile = LeagueSaveStorage.getRecruitingSaveFile(filesDir);
                if (saveFile.exists()) {
                    League league = new League(saveFile, playerNames, lastNames, bridge, false);
                    Team userTeam = league.userTeam;
                    if (userTeam == null) {
                        throw new IOException("Loaded recruiting resume save without a user team");
                    }
                    userTeam.getHeadCoach().user = true;
                    if (request.recruits != null && !request.recruits.isEmpty()) {
                        userTeam.recruitPlayersFromStr(request.recruits);
                    }
                    league.rebuildScheduleIfNeeded();
                    league.updateTeamTalentRatings();
                    return new LaunchResult(league, userTeam, userTeam, league.getYear(), true, false, false);
                }
            } else if (request.action == LaunchRequest.Action.IMPORT_SAVE) {
                try (InputStream inputStream = saveImportStreamOpener.open(request.importUri)) {
                    if (inputStream == null) {
                        throw new IOException("Unable to open imported save stream");
                    }
                    League league = new League(inputStream, playerNames, lastNames, bridge);
                    Team userTeam = league.userTeam;
                    if (userTeam == null) {
                        throw new IOException("Imported save did not contain a user team");
                    }
                    userTeam.getHeadCoach().user = true;
                    league.updateTeamTalentRatings();
                    return new LaunchResult(league, userTeam, userTeam, league.getYear(), true, false, false);
                }
            } else if (request.action == LaunchRequest.Action.LOAD_INTERNAL) {
                File saveFile = LeagueSaveStorage.getNamedInternalFile(filesDir, request.saveFileName);
                if (saveFile.exists()) {
                    League league = new League(saveFile, playerNames, lastNames, bridge, true);
                    Team userTeam = league.userTeam;
                    if (userTeam == null) {
                        throw new IOException("Loaded save did not contain a user team");
                    }
                    userTeam.getHeadCoach().user = true;
                    league.rebuildScheduleIfNeeded();
                    league.updateTeamTalentRatings();
                    return new LaunchResult(league, userTeam, userTeam, league.getYear(), true, false, league.getYear() == seasonStart);
                }
            }
        }

        League league = new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, false, false);
        return new LaunchResult(league, null, null, seasonStart, false, false, false);
    }

    private static League buildLeagueFromMode(LaunchRequest.PrestigeMode prestigeMode,
                                              String playerNames,
                                              String lastNames,
                                              String conferencesText,
                                              String teamsText,
                                              String bowlsText) {
        if (prestigeMode == LaunchRequest.PrestigeMode.RANDOMIZE) {
            return new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, true, false);
        }
        if (prestigeMode == LaunchRequest.PrestigeMode.EQUALIZE) {
            return new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, false, true);
        }
        return new League(playerNames, lastNames, conferencesText, teamsText, bowlsText, false, false);
    }

    private static League buildLeagueFromMode(LaunchRequest.PrestigeMode prestigeMode,
                                              String playerNames,
                                              String lastNames,
                                              CustomUniverseFiles customFiles,
                                              GameUiBridge bridge) {
        if (prestigeMode == LaunchRequest.PrestigeMode.RANDOMIZE) {
            return new League(playerNames, lastNames, customFiles.conferences, customFiles.teams, customFiles.bowls, true, false, bridge);
        }
        if (prestigeMode == LaunchRequest.PrestigeMode.EQUALIZE) {
            return new League(playerNames, lastNames, customFiles.conferences, customFiles.teams, customFiles.bowls, false, true, bridge);
        }
        return new League(playerNames, lastNames, customFiles.conferences, customFiles.teams, customFiles.bowls, false, false, bridge);
    }
}
