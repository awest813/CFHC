package ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import antdroid.cfbcoach.R;

/**
 * Created by Achi on 3/1/2016.
 */
public class NewsStories extends RowAdapter<String> {
    private final ArrayList<String> values;

    public NewsStories(Context context, ArrayList<String> values) {
        super(context, R.layout.news_story_list_item, values);
        this.values = values;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;
        TextView newsTitle = holder.view(R.id.textNewsTitle);
        TextView newsContent = holder.view(R.id.textNewsContent);

        String[] story = values.get(position).split(">", 2);
        newsTitle.setText(valueAt(story, 0));
        newsContent.setText(valueAt(story, 1));

        return;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
