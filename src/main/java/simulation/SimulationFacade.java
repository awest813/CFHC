package simulation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import positions.Player;
import recruiting.RecruitingController;
import recruiting.RecruitingSessionData;
import staff.HeadCoach;

/**
 * Headless host API for driving the simulation without depending on Android or
 * desktop UI classes.
 */
public final class SimulationFacade {
    public static final int SEASON_START = 2022;

    public static final int MIN_QBS = 2;
    public static final int MIN_RBS = 3;
    public static final int MIN_WRS = 4;
    public static final int MIN_TES = 2;
    public static final int MIN_OLS = 6;
    public static final int MIN_KS = 1;
    public static final int MIN_DLS = 4;
    public static final int MIN_LBS = 4;
    public static final int MIN_CBS = 4;
    public static final int MIN_SS = 2;
    public static final int MIN_ROSTER_SIZE = 55;

    public static final GameFlowManager NO_OP_FLOW_MANAGER = new GameFlowManager() {
        @Override
        public void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode prestigeMode, String customUniverseUri) {
        }

        @Override
        public void loadGame(String saveFileName) {
        }

        @Override
        public void importSave(String uri) {
        }

        @Override
        public void finishRecruiting(String recruitsStr) {
        }

        @Override
        public void startRecruiting(String userTeamInfo) {
        }

        @Override
        public void showNotification(String title, String message) {
        }

        @Override
        public void returnToMainHub() {
        }
    };

    private final File filesDir;
    private final PlatformResourceProvider resources;
    private final GameUiBridge bridge;
    private final GameFlowManager flowManager;
    private final SaveLoadService saveLoadService;

    private League league;
    private Team userTeam;
    private Team currentTeam;
    private SeasonController seasonController;

    public SimulationFacade(File filesDir, PlatformResourceProvider resources) {
        this(filesDir, resources, GameUiBridge.NO_OP, NO_OP_FLOW_MANAGER);
    }

    public SimulationFacade(File filesDir, PlatformResourceProvider resources, GameUiBridge bridge, GameFlowManager flowManager) {
        if (filesDir == null) {
            throw new IllegalArgumentException("filesDir is required");
        }
        if (resources == null) {
            throw new IllegalArgumentException("resources is required");
        }
        this.filesDir = filesDir;
        this.resources = resources;
        this.bridge = bridge != null ? bridge : GameUiBridge.NO_OP;
        this.flowManager = flowManager != null ? flowManager : NO_OP_FLOW_MANAGER;
        this.saveLoadService = new SaveLoadService(filesDir);
    }

    public LeagueLaunchCoordinator.LaunchResult load(LeagueLaunchCoordinator.LaunchRequest request,
                                                     LeagueLaunchCoordinator.CustomUniverseImporter customImporter,
                                                     LeagueLaunchCoordinator.SaveImportStreamOpener saveImportStreamOpener) throws IOException {
        LeagueLaunchCoordinator.LaunchResult result = LeagueLaunchCoordinator.load(
                request,
                filesDir,
                bridge,
                SEASON_START,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                customImporter,
                saveImportStreamOpener
        );
        setLeague(result.league, result.userTeam, result.currentTeam);
        return result;
    }

    public LeagueLaunchCoordinator.LaunchResult loadDefaultLeague() throws IOException {
        return load(LeagueLaunchCoordinator.LaunchRequest.newLeague(
                LeagueLaunchCoordinator.LaunchRequest.PrestigeMode.DEFAULT), null, null);
    }

    public LeagueLaunchCoordinator.LaunchResult loadInternalSlot(int slot) throws IOException {
        File saveFile = saveLoadService.getSlotFile(slot);
        if (!saveFile.exists()) {
            throw new IOException("Save slot is empty: " + slot);
        }
        League loadedLeague = new League(saveFile,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                bridge,
                true);
        loadedLeague.rebuildScheduleIfNeeded();
        loadedLeague.updateTeamTalentRatings();
        setLeague(loadedLeague, loadedLeague.userTeam, loadedLeague.userTeam);
        return new LeagueLaunchCoordinator.LaunchResult(
                loadedLeague,
                loadedLeague.userTeam,
                loadedLeague.userTeam,
                loadedLeague.getYear(),
                true,
                false,
                loadedLeague.getYear() == SEASON_START
        );
    }

    public LeagueLaunchCoordinator.LaunchResult loadInternalSlot(int slot, String userTeamName) throws IOException {
        LeagueLaunchCoordinator.LaunchResult result = loadInternalSlot(slot);
        if (userTeamName != null && !userTeamName.trim().isEmpty()) {
            selectUserTeam(userTeamName);
        }
        return new LeagueLaunchCoordinator.LaunchResult(
                league,
                userTeam,
                currentTeam,
                league.getYear(),
                result.loadedLeague,
                result.newGame,
                result.showSeasonGoals
        );
    }

    public LeagueLaunchCoordinator.LaunchResult importSave(String uri,
                                                           LeagueLaunchCoordinator.SaveImportStreamOpener saveImportStreamOpener) throws IOException {
        return importSave(uri, null, saveImportStreamOpener);
    }

    public LeagueLaunchCoordinator.LaunchResult importSave(String uri,
                                                           String userTeamName,
                                                           LeagueLaunchCoordinator.SaveImportStreamOpener saveImportStreamOpener) throws IOException {
        if (saveImportStreamOpener == null) {
            throw new IOException("Save import stream opener is required");
        }
        try (InputStream inputStream = saveImportStreamOpener.open(uri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open imported save stream");
            }
            File tempImport = File.createTempFile("cfhc-import-", ".cfb", filesDir);
            try {
                java.nio.file.Files.copy(inputStream, tempImport.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                League importedLeague = new League(tempImport,
                        resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                        resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                        bridge,
                        true);
                importedLeague.rebuildScheduleIfNeeded();
                importedLeague.updateTeamTalentRatings();
                setLeague(importedLeague, importedLeague.userTeam, importedLeague.userTeam);
                if (userTeamName != null && !userTeamName.trim().isEmpty()) {
                    selectUserTeam(userTeamName);
                }
                return new LeagueLaunchCoordinator.LaunchResult(
                        importedLeague,
                        userTeam,
                        currentTeam,
                        importedLeague.getYear(),
                        true,
                        false,
                        false
                );
            } finally {
                if (!tempImport.delete()) {
                    tempImport.deleteOnExit();
                }
            }
        }
    }

    public LeagueLaunchCoordinator.LaunchResult loadCustomLeague(String uri,
                                                                 LeagueLaunchCoordinator.LaunchRequest.PrestigeMode prestigeMode,
                                                                 LeagueLaunchCoordinator.CustomUniverseImporter customImporter) throws IOException {
        if (customImporter == null) {
            throw new IOException("Custom universe importer is required");
        }
        return load(LeagueLaunchCoordinator.LaunchRequest.newCustomLeague(uri, prestigeMode), customImporter, null);
    }

    public void setLeague(League league, Team userTeam, Team currentTeam) {
        if (league == null) {
            throw new IllegalArgumentException("league is required");
        }
        this.league = league;
        this.league.setPlatformResourceProvider(resources);
        this.userTeam = userTeam != null ? userTeam : league.userTeam;
        this.currentTeam = currentTeam != null ? currentTeam : this.userTeam;
        if (this.userTeam != null) {
            this.league.userTeam = this.userTeam;
            this.userTeam.setUserControlled(true);
        }
        this.seasonController = new SeasonController(league, bridge, flowManager);
    }

    public League getLeague() {
        return league;
    }

    public Team getUserTeam() {
        return userTeam;
    }

    public Team getCurrentTeam() {
        return currentTeam;
    }

    public void selectUserTeam(String teamName) {
        requireLeague();
        if (teamName == null || teamName.trim().isEmpty()) {
            throw new IllegalArgumentException("teamName is required");
        }
        for (Team team : league.getTeamList()) {
            if (teamName.equals(team.getName())) {
                selectUserTeam(team);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown team: " + teamName);
    }

    public void selectUserTeam(Team team) {
        requireLeague();
        if (team == null) {
            throw new IllegalArgumentException("team is required");
        }
        if (!league.getTeamList().contains(team)) {
            throw new IllegalArgumentException("Team does not belong to the loaded league: " + team.getName());
        }
        if (this.userTeam != null && this.userTeam != team) {
            this.userTeam.setUserControlled(false);
        }
        this.userTeam = team;
        this.currentTeam = team;
        this.league.userTeam = team;
        team.setUserControlled(true);
    }

    public SaveLoadService getSaveLoadService() {
        return saveLoadService;
    }

    public String[] getSaveFileSummaries() {
        return saveLoadService.getSaveFileSummaries();
    }

    public boolean saveToSlot(int slot) {
        requireLeague();
        return saveLoadService.saveToSlot(league, slot);
    }

    public boolean saveForRecruiting() {
        requireLeague();
        return saveLoadService.saveForRecruiting(league);
    }

    public SeasonAdvanceResult advanceWeek() {
        requireLeague();
        return seasonController.advanceWeek();
    }

    public RecruitingSessionData prepareRecruitingSession() {
        requireLeague();
        if (userTeam == null) {
            throw new IllegalStateException("League has no user team for recruiting");
        }
        return prepareRecruitingSession(userTeam);
    }

    public RecruitingController createRecruitingController() {
        return new RecruitingController(prepareRecruitingSession(), flowManager);
    }

    public void completeRecruiting(String recruitsData) {
        requireLeague();
        if (userTeam == null) {
            throw new IllegalStateException("League has no user team for recruiting");
        }
        if (recruitsData != null && !recruitsData.isEmpty()) {
            userTeam.recruitPlayersFromStr(recruitsData);
        }
        league.rebuildScheduleIfNeeded();
        league.updateTeamTalentRatings();
    }

    public void importCoaches(InputStream inputStream) throws IOException {
        requireLeague();
        LeagueCustomDataImporter.importCoaches(inputStream, league);
    }

    public void importRoster(InputStream inputStream) throws IOException {
        requireLeague();
        LeagueCustomDataImporter.importRoster(inputStream, league);
    }

    public static RecruitingSessionData prepareRecruitingSession(Team userTeam) {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(buildRecruitingPayload(userTeam));
        session.applyBudgetBonuses(MIN_ROSTER_SIZE);
        return session;
    }

    public static String buildRecruitingPayload(Team userTeam) {
        if (userTeam == null) {
            throw new IllegalArgumentException("userTeam is required");
        }
        StringBuilder sb = new StringBuilder();
        userTeam.sortPlayers();
        HeadCoach hc = userTeam.getHeadCoach();
        int recruitSkill = hc != null ? hc.ratTalent : 70;
        sb.append(userTeam.getConference()).append(",")
                .append(userTeam.getName()).append(",")
                .append(userTeam.getAbbr()).append(",")
                .append(userTeam.getUserRecruitBudget()).append(",")
                .append(recruitSkill).append("%\n");
        for (Player player : userTeam.getAllPlayers()) {
            sb.append(Persistence.toCsv(player.toRecord())).append("%\n");
        }
        sb.append("END_TEAM_INFO%\n");
        sb.append(userTeam.getRecruitsInfoSaveFile());
        return sb.toString();
    }

    private void requireLeague() {
        if (league == null) {
            throw new IllegalStateException("No league has been loaded");
        }
    }
}
