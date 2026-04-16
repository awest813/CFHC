package antdroid.cfbcoach;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

import java.util.List;

public class TutorialActivity extends AppCompatActivity {

    private Spinner tutorialSpinner;
    private TextView tutorialTitle;
    private TextView tutorialContent;
    private TutorialController controller;
    private int theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = GameNavigation.getTheme(getIntent(), 1);
        if(theme == 1) setTheme(R.style.AppThemeLight);
        else setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_tutorial);

        tutorialSpinner = findViewById(R.id.tutorialSpinner);
        tutorialTitle = findViewById(R.id.tutorialTitle);
        tutorialContent = findViewById(R.id.tutorialContent);

        simulation.PlatformResourceProvider resProvider = new AndroidResourceProvider(this);
        simulation.GameFlowManager flowManager = new AndroidGameFlowManager(this, theme);
        controller = new TutorialController(resProvider, flowManager);

        List<String> titles = controller.getChapterTitles();
        ArrayAdapter<String> dataAdapterTutorial = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, titles);
        dataAdapterTutorial.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tutorialSpinner.setAdapter(dataAdapterTutorial);
        tutorialSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        TutorialController.TutorialChapter chapter = controller.getChapter(position);
                        if (chapter != null) {
                            tutorialTitle.setText(chapter.title);
                            tutorialContent.setText(chapter.content);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

    }

    @Override
    public void onBackPressed() {
        controller.returnToMainHub();
    }

}
