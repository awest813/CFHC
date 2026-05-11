package antdroid.cfbcoach;

//Google Play Services ID: 116207837258

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.app.AlertDialog;

import simulation.LeagueLaunchCoordinator;
import simulation.LeagueSaveStorage;


public class Home extends AppCompatActivity {

    private static final String PREFS_HOME = "cfhc_home";
    private static final String KEY_THEME = "theme_choice";
    private static final String KEY_WELCOME_SEEN = "welcome_seen";

    private int theme = GameNavigation.DEFAULT_THEME;
    private SharedPreferences homePrefs;
    private simulation.GameFlowManager flowManager;

    private final ActivityResultLauncher<String[]> customUniversePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleCustomUniverseSelection);
    private final ActivityResultLauncher<String[]> importSavePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportSaveSelection);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        homePrefs = getSharedPreferences(PREFS_HOME, MODE_PRIVATE);
        theme = GameNavigation.getTheme(getIntent(), homePrefs.getInt(KEY_THEME, GameNavigation.DEFAULT_THEME));
        if (theme == 1) {
            setTheme(R.style.AppThemeLight);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);

        flowManager = new AndroidGameFlowManager(this, theme);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        hideSystemUI();

        applyHomeThemeUi();

        ImageView imageLogo = findViewById(R.id.imageLogo);
        imageLogo.setImageResource(R.drawable.main_menu_logo);

        Button newGameButton = findViewById(R.id.buttonNewGame);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HomeDialogController.showPrestigeModeDialog(Home.this, null, flowManager);
            }
        });

        Button newCustomGameButton = findViewById(R.id.buttonCustom);
        newCustomGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HomeDialogController.showCustomGamePrompt(Home.this, new Runnable() {
                    @Override
                    public void run() {
                        isExternalStorageReadable();
                        customUniversePicker.launch(new String[]{"*/*"});
                    }
                });
            }
        });

        Button loadGameButton = findViewById(R.id.buttonLoadGame);
        loadGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                loadLeague();
            }
        });

        Button importButton = findViewById(R.id.buttonImportSave);
        importButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HomeDialogController.showImportGameDialog(Home.this, new Runnable() {
                    @Override
                    public void run() {
                        isExternalStorageReadable();
                        importSavePicker.launch(new String[]{"text/plain"});
                    }
                });
            }
        });

        Button deleteGameButton = findViewById(R.id.buttonDeleteSave);
        deleteGameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                deleteSave();
            }
        });

        Button themeButton = findViewById(R.id.buttonChangeTheme);

        themeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                theme = (theme == 0) ? 1 : 0;
                homePrefs.edit().putInt(KEY_THEME, theme).apply();
                recreate();
            }
        });

        Button recentButton = findViewById(R.id.buttonUpdates);
        recentButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HomeDialogController.showMessageDialog(Home.this, "Changelog", getString(R.string.changelog), 14);
            }
        });

        Button tutorialButton = findViewById(R.id.buttonTutorial);
        tutorialButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showManualChooser();
            }
        });

        Button creditsButton = findViewById(R.id.buttonCredits);
        creditsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                HomeDialogController.showMessageDialog(Home.this, "Game Acknowledgements", getString(R.string.credits), 12);
            }
        });

        Button googleButton = findViewById(R.id.buttonDonate);
        googleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://drive.google.com/drive/folders/1hfB_lbTaMfhm4lXtMelvgWoZSshL12v0?usp=sharing"));
                startActivity(intent);
            }
        });


        Button subredditButton = findViewById(R.id.buttonSubreddit);
        subredditButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://m.reddit.com/r/FootballCoach"));
                startActivity(intent);
            }
        });


        Button antdroidButton = findViewById(R.id.buttonAntdroid);
        antdroidButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://www.Antdroid.dev"));
                startActivity(intent);
            }
        });

        Button githubButton = findViewById(R.id.buttonGitHub);
        githubButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("https://github.com/antdroidx/"));
                startActivity(intent);
            }
        });

        homePrefs.edit().putInt(KEY_THEME, theme).apply();
        updateSaveSlotHint();

        if (!homePrefs.getBoolean(KEY_WELCOME_SEEN, false)) {
            getWindow().getDecorView().post(new Runnable() {
                @Override
                public void run() {
                    showFirstRunWelcome();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSaveSlotHint();
    }

    private void showFirstRunWelcome() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.home_welcome_title)
                .setMessage(R.string.home_welcome_message)
                .setPositiveButton(R.string.home_welcome_cta, null)
                .setOnDismissListener(dialogInterface ->
                        homePrefs.edit().putBoolean(KEY_WELCOME_SEEN, true).apply())
                .show();
    }

    private void showManualChooser() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.home_manual_dialog_title)
                .setItems(new String[]{
                        getString(R.string.home_manual_option_in_app),
                        getString(R.string.home_manual_option_web)
                }, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(GameNavigation.createTutorialIntent(Home.this, theme));
                    } else {
                        openGameManualInBrowser();
                    }
                })
                .show();
    }

    private void openGameManualInBrowser() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://www.antdroid.dev/p/game-manual.html"));
        startActivity(intent);
    }

    private void updateSaveSlotHint() {
        TextView hint = findViewById(R.id.home_save_hint);
        if (hint == null) {
            return;
        }
        if (!LeagueSaveStorage.hasAnySave(getFilesDir())) {
            hint.setVisibility(View.VISIBLE);
            hint.setText(R.string.home_save_hint_empty);
        } else {
            hint.setVisibility(View.GONE);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    private void loadLeague() {
        HomeDialogController.showLoadLeagueDialog(this, getSaveFileInfos(), theme);
    }

    private void deleteSave() {
        HomeDialogController.showDeleteSaveDialog(this, getSaveFileInfos());
    }

    /**
     * Get info of the 10 save files for printing in the save file list
     */
    private String[] getSaveFileInfos() {
        return LeagueSaveStorage.getSaveFileInfos(getFilesDir());
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
    private void handleCustomUniverseSelection(Uri uri) {
        if (uri == null) {
            return;
        }
        HomeDialogController.showPrestigeModeDialog(this, uri.toString(), flowManager);
    }

    private void handleImportSaveSelection(Uri uri) {
        if (uri == null) {
            return;
        }
        flowManager.importSave(uri.toString());
        finish();
    }

    /** Updates hero badges and panel background (activity theme is set in {@link #onCreate}). */
    private void applyHomeThemeUi() {
        Button themeButton = findViewById(R.id.buttonChangeTheme);
        TextView themeStatus = findViewById(R.id.homeThemeStatus);
        View layout = findViewById(R.id.homeMain);

        if (theme == 0) {
            themeButton.setText(getString(R.string.home_switch_theme_light));
            if (themeStatus != null) {
                themeStatus.setText(getString(R.string.home_badge_theme_dark));
            }
            layout.setBackgroundResource(R.drawable.bg_home_menu);
        } else {
            themeButton.setText(getString(R.string.home_switch_theme_dark));
            if (themeStatus != null) {
                themeStatus.setText(getString(R.string.home_badge_theme_light));
            }
            layout.setBackgroundResource(R.drawable.bg_home_menu);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void showImmersive(AlertDialog alert) {
        PlatformUiHelper.showImmersive(alert);
    }





}


