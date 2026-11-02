package antdroid.cfbcoach;

import android.content.Context;
import android.content.Intent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import antdroid.cfbcoach.recruiting.RecruitingActivity;
import simulation.LeagueLaunchCoordinator;
import simulation.IoStreams;
import simulation.LeagueSaveStorage;
import simulation.PlatformLog;

public final class GameNavigation {
    private static final String EXTRA_THEME = "Theme";
    private static final String EXTRA_LAUNCH_REQUEST = "LAUNCH_REQUEST";
    private static final String EXTRA_USER_TEAM_INFO = "USER_TEAM_INFO";
    private static final String EXTRA_USER_TEAM_INFO_FILE = "USER_TEAM_INFO_FILE";
    /**
     * The recruiting payload (roster + recruit board) grows through a dynasty.
     * Passed as an Intent extra it approached the ~1 MB Binder limit and could
     * crash with TransactionTooLargeException, so it goes through this file.
     */
    private static final String RECRUITING_PAYLOAD_FILE = "recruitingPayload.txt";
    private static final String EXTRA_SAVE_FILE = "SAVE_FILE";
    private static final String EXTRA_RECRUITS = "RECRUITS";

    /** Default theme value: 1 = light, 0 = dark. */
    public static final int DEFAULT_THEME = 1;

    private GameNavigation() {
    }

    public static Intent createHomeIntent(Context context, int theme) {
        Intent intent = new Intent(context, Home.class);
        intent.putExtra(EXTRA_THEME, theme);
        return intent;
    }

    public static Intent createTutorialIntent(Context context, int theme) {
        Intent intent = new Intent(context, TutorialActivity.class);
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
        String payload = userTeamInfo == null ? "" : userTeamInfo;
        File payloadFile = new File(context.getFilesDir(), RECRUITING_PAYLOAD_FILE);
        if (LeagueSaveStorage.writeAtomically(payloadFile, payload.getBytes(StandardCharsets.UTF_8))) {
            intent.putExtra(EXTRA_USER_TEAM_INFO_FILE, RECRUITING_PAYLOAD_FILE);
        } else {
            // Storage failed: fall back to the extra (works for small rosters).
            intent.putExtra(EXTRA_USER_TEAM_INFO, payload);
        }
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

    public static String getUserTeamInfo(Context context, Intent intent) {
        if (intent == null) {
            return "";
        }
        String fileName = intent.getStringExtra(EXTRA_USER_TEAM_INFO_FILE);
        if (fileName != null) {
            File payloadFile = new File(context.getFilesDir(), fileName);
            try (FileInputStream in = new FileInputStream(payloadFile)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IoStreams.copy(in, out); // java.nio.file needs API 26; minSdk is 24
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                PlatformLog.e("GameNavigation", "Could not read recruiting payload " + payloadFile, ex);
                return "";
            }
        }
        String userTeamInfo = intent.getStringExtra(EXTRA_USER_TEAM_INFO);
        return userTeamInfo == null ? "" : userTeamInfo;
    }
}
