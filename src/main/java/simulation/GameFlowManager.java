package simulation;

/**
 * Interface for orchestrating the overall game state transitions.
 * This should be independent of any UI framework.
 */
public interface GameFlowManager {
    
    /**
     * Start a new game session.
     */
    void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode prestigeMode, String customUniverseUri);
    
    /**
     * Load an existing game session.
     */
    void loadGame(String saveFileName);
    
    /**
     * Import an external save file.
     */
    void importSave(String uri);

    /**
     * Finish recruiting and return to the main game hub.
     * @param recruitsStr The CSV string containing recruited players.
     */
    void finishRecruiting(String recruitsStr);

    /**
     * Transition to the recruiting phase.
     */
    void startRecruiting(String userTeamInfo);
    
    /**
     * Display a message or notification to the user.
     */
    void showNotification(String title, String message);
    
    /**
     * Return to the main hub/dashboard.
     */
    void returnToMainHub();
}
