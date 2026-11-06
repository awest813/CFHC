package ui;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Shows an existing row adapter ({@link ListAdapter}, e.g. the {@code ArrayAdapter}
 * page adapters) in a {@link RecyclerView}.
 *
 * <p>UI redesign Wave 3 moved the main page list to RecyclerView while keeping
 * each adapter's row binding and the engine's positional string protocol intact
 * (one change at a time). Each holder keeps a row container. On bind, the
 * wrapped adapter's {@code getView(position, convertView, parent)} gets the
 * row it built last time as {@code convertView}, so its own recycling still
 * works. View types are forwarded, so a holder is only reused for rows of the
 * same type. {@code notifyDataSetChanged()} on the wrapped adapter refreshes
 * the list.
 */
public final class ListRowsAdapter extends RecyclerView.Adapter<ListRowsAdapter.Holder> {

    static final class Holder extends RecyclerView.ViewHolder {
        final FrameLayout container;
        View row;

        Holder(FrameLayout container) {
            super(container);
            this.container = container;
        }
    }

    private final ListAdapter rows;
    // A ListAdapter only reports "something changed", so a full refresh is the
    // only faithful translation.
    @android.annotation.SuppressLint("NotifyDataSetChanged")
    private final DataSetObserver observer = new DataSetObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            notifyDataSetChanged();
        }
    };

    /** Puts {@code rows} on screen in {@code list}, replacing the current page. */
    public static void show(RecyclerView list, ListAdapter rows) {
        list.setAdapter(new ListRowsAdapter(rows));
    }

    public ListRowsAdapter(ListAdapter rows) {
        this.rows = rows;
        setHasStableIds(rows.hasStableIds());
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        rows.registerDataSetObserver(observer);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        rows.unregisterDataSetObserver(observer);
    }

    @Override
    public int getItemCount() {
        return rows.getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        return rows.getItemId(position);
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
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        View row = rows.getView(position, holder.row, holder.container);
        if (row != holder.row) {
            holder.container.removeAllViews();
            if (row.getParent() instanceof ViewGroup) {
                ((ViewGroup) row.getParent()).removeView(row);
            }
            holder.container.addView(row);
            holder.row = row;
        }
    }
}
