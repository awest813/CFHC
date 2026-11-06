package ui;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import antdroid.cfbcoach.R;

/**
 * A grouped list in a {@link RecyclerView} (UI redesign Wave 5: the recruiting
 * board). It flattens an existing {@link ExpandableListAdapter} into group rows
 * plus the child rows of expanded groups. Tapping a group toggles it, as
 * ExpandableListView did, and {@link #expandGroup} / {@link #collapseGroup} /
 * {@link #isGroupExpanded} mirror its API. Row views are recycled through the
 * adapter's own {@code convertView} handling.
 */
public final class ExpandableRows extends RecyclerView.Adapter<ExpandableRows.Holder> {

    private static final int GROUP = 0;
    private static final int CHILD = 1;

    static final class Holder extends RecyclerView.ViewHolder {
        final FrameLayout container;
        View row;

        Holder(FrameLayout container) {
            super(container);
            this.container = container;
        }
    }

    private final ExpandableListAdapter source;
    private final TreeSet<Integer> expanded = new TreeSet<>();
    /** Flattened rows: {group, child} with child = -1 for the group row itself. */
    private final List<int[]> flat = new ArrayList<>();
    @android.annotation.SuppressLint("NotifyDataSetChanged") // the source only reports "changed"
    private final DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            refresh();
        }

        @Override
        public void onInvalidated() {
            refresh();
        }
    };

    /** Puts {@code source} on screen in {@code list} and returns the controller. */
    public static ExpandableRows attach(RecyclerView list, ExpandableListAdapter source) {
        if (list.getLayoutManager() == null) {
            list.setLayoutManager(new LinearLayoutManager(list.getContext()));
        }
        ExpandableRows rows = new ExpandableRows(source);
        list.setAdapter(rows);
        return rows;
    }

    public ExpandableRows(ExpandableListAdapter source) {
        this.source = source;
        rebuild();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        source.registerDataSetObserver(observer);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        source.unregisterDataSetObserver(observer);
    }

    public boolean isGroupExpanded(int group) {
        return expanded.contains(group);
    }

    public void expandGroup(int group) {
        expandGroup(group, false);
    }

    /** {@code animate} is accepted for ExpandableListView parity; changes apply at once. */
    @android.annotation.SuppressLint("NotifyDataSetChanged")
    public void expandGroup(int group, boolean animate) {
        if (group >= 0 && group < source.getGroupCount() && expanded.add(group)) {
            rebuild();
            notifyDataSetChanged();
        }
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    public void collapseGroup(int group) {
        if (expanded.remove(group)) {
            rebuild();
            notifyDataSetChanged();
        }
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    private void refresh() {
        // Groups are keyed by position (as before): drop expansions past the end.
        expanded.removeIf(g -> g >= source.getGroupCount());
        rebuild();
        notifyDataSetChanged();
    }

    private void rebuild() {
        flat.clear();
        for (int g = 0; g < source.getGroupCount(); g++) {
            flat.add(new int[]{g, -1});
            if (expanded.contains(g)) {
                for (int c = 0; c < source.getChildrenCount(g); c++) {
                    flat.add(new int[]{g, c});
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return flat.size();
    }

    @Override
    public int getItemViewType(int position) {
        return flat.get(position)[1] < 0 ? GROUP : CHILD;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout container = new FrameLayout(parent.getContext());
        container.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new Holder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final int group = flat.get(position)[0];
        final int child = flat.get(position)[1];
        View row = child < 0
                ? source.getGroupView(group, expanded.contains(group), holder.row, holder.container)
                : source.getChildView(group, child, child == source.getChildrenCount(group) - 1, holder.row, holder.container);
        if (row != holder.row) {
            holder.container.removeAllViews();
            if (row.getParent() instanceof ViewGroup) {
                ((ViewGroup) row.getParent()).removeView(row);
            }
            // The recruiting rows inflate with a null parent, so give them row-shaped params.
            holder.container.addView(row, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            holder.row = row;
        }
        if (child < 0) {
            holder.container.setBackground(ContextCompat.getDrawable(
                    holder.container.getContext(), R.drawable.bg_cf_list_selector));
            holder.container.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                int g = flat.get(pos)[0];
                if (expanded.contains(g)) collapseGroup(g);
                else expandGroup(g);
            });
        } else {
            holder.container.setBackground(null);
            holder.container.setOnClickListener(null);
            holder.container.setClickable(false);
        }
    }
}
