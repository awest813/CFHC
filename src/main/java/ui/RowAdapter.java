package ui;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

/**
 * Base for the main-page {@link RecyclerView} adapters (UI redesign Wave 3).
 * Each subclass binds one row layout. {@link RowHolder#view(int)} looks a child
 * up once per holder and then reuses it, which is the point of a ViewHolder.
 * Rows still parse the engine's positional strings in {@code bind}, so the
 * engine-to-UI protocol is unchanged.
 */
public abstract class RowAdapter<T> extends RecyclerView.Adapter<RowAdapter.RowHolder> {

    /** A row and a cache of the child views {@code bind} has asked for. */
    public static final class RowHolder extends RecyclerView.ViewHolder {
        private final SparseArray<View> views = new SparseArray<>();

        RowHolder(View row) {
            super(row);
        }

        @SuppressWarnings("unchecked")
        public <V extends View> V view(@IdRes int id) {
            View v = views.get(id);
            if (v == null) {
                v = itemView.findViewById(id);
                views.put(id, v);
            }
            return (V) v;
        }
    }

    protected final Context context;
    private final int layout;
    private final List<T> items;

    protected RowAdapter(Context context, @LayoutRes int layout, List<T> items) {
        this.context = context;
        this.layout = layout;
        this.items = items;
    }

    protected RowAdapter(Context context, @LayoutRes int layout, T[] items) {
        this(context, layout, Arrays.asList(items));
    }

    public Context getContext() {
        return context;
    }

    public T getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RowHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        bind(holder, position);
    }

    /** Fill {@code holder.itemView} for the item at {@code position}. */
    protected abstract void bind(RowHolder holder, int position);

    /**
     * Collapses or restores a row; the page adapters hide filtered-out rows this way.
     * The list's gap decoration skips collapsed rows.
     */
    protected static void setRowHeight(View row, int height) {
        ViewGroup.LayoutParams lp = row.getLayoutParams();
        if (lp == null) {
            row.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        } else if (lp.height != height) {
            lp.height = height;
            row.setLayoutParams(lp);
        }
    }
}
