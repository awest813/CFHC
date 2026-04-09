package antdroid.cfbcoach;

import android.content.Context;
import android.content.Intent;

import recruiting.RecruitingActivity;

public final class GameNavigation {
    private static final String EXTRA_THEME = "Theme";
    private static final String EXTRA_LAUNCH_REQUEST = "LAUNCH_REQUEST";
    private static final String EXTRA_USER_TEAM_INFO = "USER_TEAM_INFO";
    private static final String EXTRA_SAVE_FILE = "SAVE_FILE";
    private static final String EXTRA_RECRUITS = "RECRUITS";

    private GameNavigation() {
    }

    public static Intent createHomeIntent(Context context, int theme) {
        Intent intent = new Intent(context, Home.class);
        intent.putExtra(EXTRA_THEME, theme);
        return intent;
    }

    public static Intent createMainIntent(Context context,
                                          LeagueLaunchCoordinator.LaunchRequest launchRequest,
                                          int theme) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_LAUNCH_REQUEST, launchRequest);
        intent.putExtra(EXTRA_THEME, theme);
        return intent;
    }

    public static Intent createRecruitingIntent(Context context, String userTeamInfo, int theme) {
        Intent intent = new Intent(context, RecruitingActivity.class);
        intent.putExtra(EXTRA_USER_TEAM_INFO, userTeamInfo);
        intent.putExtra(EXTRA_THEME, theme);
        return intent;
    }

    public static int getTheme(Intent intent, int defaultTheme) {
        if (intent == null || !intent.hasExtra(EXTRA_THEME)) {
            return defaultTheme;
        }
        return intent.getIntExtra(EXTRA_THEME, defaultTheme);
    }

    public static LeagueLaunchCoordinator.LaunchRequest getLaunchRequest(Intent intent) {
        if (intent == null) {
            return null;
        }
        Object request = intent.getSerializableExtra(EXTRA_LAUNCH_REQUEST);
        if (request instanceof LeagueLaunchCoordinator.LaunchRequest) {
            return (LeagueLaunchCoordinator.LaunchRequest) request;
        }
        return LeagueLaunchCoordinator.LaunchRequest.fromLegacy(
                intent.getStringExtra(EXTRA_SAVE_FILE),
                intent.getStringExtra(EXTRA_RECRUITS)
        );
    }

    public static String getUserTeamInfo(Intent intent) {
        if (intent == null) {
            return "";
        }
        String userTeamInfo = intent.getStringExtra(EXTRA_USER_TEAM_INFO);
        return userTeamInfo == null ? "" : userTeamInfo;
    }
}
