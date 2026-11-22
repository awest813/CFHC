package antdroid.cfbcoach;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import antdroid.cfbcoach.recruiting.RecruitingActivity;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingSessionData;
import simulation.FileSystemResourceProvider;
import simulation.GameUiBridge;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;
import simulation.SeasonFlowOrder;
import simulation.SimRandom;
import simulation.SimulationFacade;
import simulation.Team;

import static org.junit.Assert.*;

/**
 * The recruiting board on the shared national pool: prospects who committed to
 * another program in the early signing wave are listed, marked with their new
 * school, and cannot be signed; open prospects still can.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class RecruitingScreenTest {

    /** A dynasty played headlessly to the recruiting gate, early wave run: the board payload. */
    private static String gatePayload() {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0xCF4CL);
        League league = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        league.setPlatformResourceProvider(r);
        Team user = league.getTeamList().get(0);
        user.setupUserCoach("Test Coach");
        league.userTeam = user;
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        for (int guard = 0; guard < 80; guard++) {
            if (league.recruitingPhaseActive
                    && SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks)) {
                break;
            }
            controller.advanceWeek();
        }
        assertTrue("reached the recruiting gate", league.recruitingPhaseActive);
        SimulationFacade.prepareCpuRecruiting(league);
        return SimulationFacade.buildRecruitingPayload(user);
    }

    @Test
    public void board_rendersInBothThemes() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        String payload = gatePayload();
        for (int theme : new int[]{0, 1}) {
            Intent intent = GameNavigation.createRecruitingIntent(context, payload, theme);
            try (ActivityScenario<RecruitingActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> {
                    try {
                        ShadowLooper.idleMainLooper();
                        String tag = "recruiting_" + (theme == 1 ? "light" : "dark");
                        Screenshots.capture(activity.getWindow().getDecorView(), tag + "_board");
                        // One prospect's expanded card (ratings, offer button), drawn on its own.
                        RecruitingActivity.ExpandableListAdapterRecruiting adapter = field(activity, "expListAdapter");
                        ViewGroup parent = activity.findViewById(R.id.recruitExpandList);
                        View card = adapter.getChildView(0, 0, true, null, parent);
                        card.measure(View.MeasureSpec.makeMeasureSpec(parent.getWidth() > 0 ? parent.getWidth() : 1233,
                                View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        card.layout(0, 0, card.getMeasuredWidth(), card.getMeasuredHeight());
                        Screenshots.capture(card, tag + "_card");
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                });
            }
        }
    }

    @Test
    public void committedProspects_areMarkedAndCannotBeSigned() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = GameNavigation.createRecruitingIntent(context, gatePayload(), 0);
        try (ActivityScenario<RecruitingActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    ShadowLooper.idleMainLooper();
                    RecruitingSessionData session = field(activity, "sessionData");
                    assertTrue("the board is the national pool", session.isSharedPool());
                    TextView status = activity.findViewById(R.id.textRecruitBoardStatus);
                    assertTrue(status.getText().toString(), status.getText().toString().contains(" committed"));

                    int committed = indexWhere(activity.players, session, true);
                    if (committed < 0) {
                        // Top 50 had none: look at every prospect.
                        Method set = RecruitingActivity.class.getDeclaredMethod("setPlayerList", int.class, String.class);
                        set.setAccessible(true);
                        set.invoke(activity, 1, "All Players");
                        committed = indexWhere(activity.players, session, true);
                    }
                    assertTrue("the board lists prospects committed elsewhere", committed >= 0);
                    RecruitingPlayerRecord gone = activity.players.get(committed);
                    String school = session.committedTo(gone);

                    RecruitingActivity.ExpandableListAdapterRecruiting adapter = field(activity, "expListAdapter");
                    ViewGroup parent = activity.findViewById(R.id.recruitExpandList);
                    View row = adapter.getGroupView(committed, true, null, parent);
                    TextView right = row.findViewById(R.id.textRecruitRight);
                    assertEquals("Committed: " + school, right.getText().toString());
                    View card = adapter.getChildView(committed, 0, true, null, parent);
                    Button sign = card.findViewById(R.id.buttonRecruitPlayer);
                    assertFalse("a committed prospect can't be signed", sign.isEnabled());
                    assertEquals(activity.getString(R.string.recruiting_button_committed_format, school),
                            sign.getText().toString());
                    TextView details = card.findViewById(R.id.textRecruitDetails);
                    assertTrue(details.getText().toString().startsWith("Committed to " + school));

                    // Even if asked directly, the session refuses.
                    session.recruitingBudget = 1_000_000;
                    activity.recruitPlayer(gone);
                    assertTrue("nobody signed", session.playersRecruited.isEmpty());

                    // An open prospect still signs.
                    int open = indexWhere(activity.players, session, false);
                    assertTrue(open >= 0);
                    View openCard = adapter.getChildView(open, 0, true, null, parent);
                    assertTrue(((Button) openCard.findViewById(R.id.buttonRecruitPlayer)).isEnabled());
                    RecruitingPlayerRecord target = activity.players.get(open);
                    if (session.signingBlockReason(target) == null) {
                        activity.recruitPlayer(target);
                        assertTrue(session.playersRecruited.contains(target));
                    }
                    ShadowLooper.idleMainLooper();
                    Screenshots.capture(activity.getWindow().getDecorView(), "recruiting_shared_pool");
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static int indexWhere(List<RecruitingPlayerRecord> players, RecruitingSessionData s, boolean committed) {
        for (int i = 0; i < players.size(); i++) {
            if (s.isCommittedElsewhere(players.get(i)) == committed) return i;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(owner);
    }
}
