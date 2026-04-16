package antdroid.cfbcoach;

import java.util.ArrayList;
import java.util.List;
import simulation.PlatformResourceProvider;

/**
 * Controller for managing tutorial content and navigation.
 */
public final class TutorialController {

    public static final class TutorialChapter {
        public final String title;
        public final String content;

        public TutorialChapter(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    private final List<TutorialChapter> chapters = new ArrayList<>();
    private final simulation.GameFlowManager flowManager;

    public TutorialController(PlatformResourceProvider resProvider, simulation.GameFlowManager flowManager) {
        this.flowManager = flowManager;
        loadChapters(resProvider);
    }

    private void loadChapters(PlatformResourceProvider resProvider) {
        addChapter("Basics", resProvider.getString("tutBasics"));
        addChapter("Playing a Season", resProvider.getString("tutPlayingSeason"));
        addChapter("Rankings", resProvider.getString("tutRankings"));
        addChapter("Roster", resProvider.getString("tutRoster"));
        addChapter("Team Strategies", resProvider.getString("tutTeamStrategy"));
        addChapter("Settings Menu & Built-In Game Mods", resProvider.getString("tutSettings"));
        addChapter("User Customization", resProvider.getString("tutCustomizations"));
        addChapter("Customization Example", resProvider.getString("tutExampleCustom"));
    }

    private void addChapter(String title, String content) {
        chapters.add(new TutorialChapter(title, content));
    }

    public List<String> getChapterTitles() {
        List<String> titles = new ArrayList<>();
        for (TutorialChapter chapter : chapters) {
            titles.add(chapter.title);
        }
        return titles;
    }

    public TutorialChapter getChapter(int index) {
        if (index >= 0 && index < chapters.size()) {
            return chapters.get(index);
        }
        return null;
    }

    public void returnToMainHub() {
        flowManager.returnToMainHub();
    }
}
