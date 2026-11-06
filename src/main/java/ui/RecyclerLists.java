package ui;

import android.database.DataSetObserver;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import antdroid.cfbcoach.R;

/**
 * Lists inside dialogs are {@link RecyclerView}s (UI redesign Wave 4). They
 * still use the existing {@link ListAdapter} row adapters: {@link #setAdapter}
 * wraps one in a RecyclerView adapter that passes it each row's previous view
 * as {@code convertView}, so its own recycling keeps working, and that
 * forwards its data-change notifications. The ListView behaviour the dialogs
 * relied on is reproduced here: a vertical layout, the 8dp gap between rows,
 * and row clicks with press feedback. That lets each adapter be moved to a
 * native {@link RowAdapter} later, one at a time.
 */
public final class RecyclerLists {

    private RecyclerLists() {
    }

    /** Vertical layout plus the 8dp row gap (once per list). */
    public static void setup(RecyclerView list) {
        if (list.getLayoutManager() == null) {
            list.setLayoutManager(new LinearLayoutManager(list.getContext()));
        }
        if (list.getTag(R.id.tag_row_gap) == null) {
            final int gap = list.getResources().getDimensionPixelSize(R.dimen.cf_space_sm);
            list.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                           @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    if (parent.getChildAdapterPosition(view) > 0 && view.getVisibility() != View.GONE
                            && !isCollapsed(view)) {
                        outRect.top = gap;
                    }
                }
            });
            list.setTag(R.id.tag_row_gap, Boolean.TRUE);
        }
    }

    /** A row whose adapter collapsed it (GONE child or zero height) gets no gap. */
    private static boolean isCollapsed(View container) {
        if (container instanceof ViewGroup && ((ViewGroup) container).getChildCount() == 1) {
            View row = ((ViewGroup) container).getChildAt(0);
            ViewGroup.LayoutParams lp = row.getLayoutParams();
            return row.getVisibility() == View.GONE || (lp != null && lp.height == 0);
        }
        return false;
    }

    /** Shows {@code rows} in {@code list}, replacing any current adapter. */
    public static void setAdapter(RecyclerView list, ListAdapter rows) {
        setup(list);
        list.setAdapter(rows == null ? null : new Bridge(rows, list));
    }

    /**
     * Row taps, like {@code ListView.setOnItemClickListener}. The listener gets
     * a {@code null} AdapterView and the tapped row. Pass {@code null} to stop.
     * It survives adapter swaps.
     */
    public static void setOnItemClickListener(RecyclerView list, AdapterView.OnItemClickListener listener) {
        list.setTag(R.id.tag_item_click, listener);
        RecyclerView.Adapter<?> adapter = list.getAdapter();
        if (adapter != null) {
            adapter.notifyItemRangeChanged(0, adapter.getItemCount()); // rebind click state
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final FrameLayout container;
        View row;

        Holder(FrameLayout container) {
            super(container);
            this.container = container;
        }
    }

    private static final class Bridge extends RecyclerView.Adapter<Holder> {
        private final ListAdapter rows;
        private final RecyclerView list;
        @android.annotation.SuppressLint("NotifyDataSetChanged") // a ListAdapter only reports "changed"
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

        Bridge(ListAdapter rows, RecyclerView list) {
            this.rows = rows;
            this.list = list;
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
        public void onBindViewHolder(@NonNull final Holder holder, int position) {
            View row = rows.getView(position, holder.row, holder.container);
            if (row != holder.row) {
                holder.container.removeAllViews();
                if (row.getParent() instanceof ViewGroup) {
                    ((ViewGroup) row.getParent()).removeView(row);
                }
                holder.container.addView(row);
                holder.row = row;
            }
            final AdapterView.OnItemClickListener click =
                    (AdapterView.OnItemClickListener) list.getTag(R.id.tag_item_click);
            if (click != null && rows.isEnabled(position)) {
                holder.container.setBackground(androidx.core.content.ContextCompat.getDrawable(
                        holder.container.getContext(), R.drawable.bg_cf_list_selector));
                holder.container.setOnClickListener(v -> {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        click.onItemClick(null, holder.row, pos, rows.getItemId(pos));
                    }
                });
            } else {
                holder.container.setBackground(null);
                holder.container.setOnClickListener(null);
                holder.container.setClickable(false);
            }
        }
    }
}
