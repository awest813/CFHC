package antdroid.cfbcoach;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import simulation.GameFlowManager;
import simulation.LeagueLaunchCoordinator;

/**
 * Android implementation of GameFlowManager using Intents and Activities.
 */
public class AndroidGameFlowManager implements GameFlowManager {

    private final Context context;
    private final Activity activity;
    private final int theme;

    public AndroidGameFlowManager(Activity activity, int theme) {
        this.context = activity;
        this.activity = activity;
        this.theme = theme;
    }

    @Override
    public void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode prestigeMode, String customUniverseUri) {
        LeagueLaunchCoordinator.LaunchRequest request;
        if (customUniverseUri != null) {
            request = LeagueLaunchCoordinator.LaunchRequest.newCustomLeague(customUniverseUri, prestigeMode);
        } else {
            request = LeagueLaunchCoordinator.LaunchRequest.newLeague(prestigeMode);
        }
        context.startActivity(GameNavigation.createMainIntent(context, request, theme));
    }

    @Override
    public void loadGame(String saveFileName) {
        LeagueLaunchCoordinator.LaunchRequest request = LeagueLaunchCoordinator.LaunchRequest.loadInternal(saveFileName);
        context.startActivity(GameNavigation.createMainIntent(context, request, theme));
    }

    @Override
    public void importSave(String uri) {
        LeagueLaunchCoordinator.LaunchRequest request = LeagueLaunchCoordinator.LaunchRequest.importSave(uri);
        context.startActivity(GameNavigation.createMainIntent(context, request, theme));
    }

    @Override
    public void startRecruiting(String userTeamInfo) {
        context.startActivity(GameNavigation.createRecruitingIntent(context, userTeamInfo, theme));
    }

    @Override
    public void finishRecruiting(String recruitsStr) {
        Intent intent = GameNavigation.createMainIntent(
                activity,
                LeagueLaunchCoordinator.LaunchRequest.doneRecruiting(recruitsStr),
                theme
        );
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public void showNotification(String title, String message) {
        PlatformUiHelper.showNotification(context, title, message);
    }

    @Override
    public void returnToMainHub() {
        context.startActivity(GameNavigation.createHomeIntent(context, theme));
    }
}
