package com.blogspace.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspace.app.R;

/**
 * Swipe a card left to delete (red, bin icon) or right to edit (indigo, pencil icon).
 * Draws a rounded coloured background with an icon behind the card while it is dragged.
 */
public class SwipeActions extends ItemTouchHelper.SimpleCallback {

    public interface Listener {
        void onSwipeDelete(int position);

        void onSwipeEdit(int position);

        /** Swipes are disabled while multi-selecting. */
        boolean isSwipeEnabled();
    }

    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable deleteIcon;
    private final Drawable editIcon;
    private final int deleteColor;
    private final int editColor;
    private final float radius;
    private final int margin;

    public SwipeActions(@NonNull Context context, @NonNull Listener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete).mutate();
        editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit).mutate();
        deleteIcon.setTint(0xFFFFFFFF);
        editIcon.setTint(0xFFFFFFFF);
        deleteColor = ContextCompat.getColor(context, R.color.status_error);
        editColor = ContextCompat.getColor(context, R.color.brand_indigo);
        radius = context.getResources().getDimension(R.dimen.card_radius);
        margin = context.getResources().getDimensionPixelSize(R.dimen.screen_padding);
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return listener.isSwipeEnabled();
    }

    @Override
    public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
        int position = vh.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipeDelete(position);
        } else {
            listener.onSwipeEdit(position);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.35f;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                            float dX, float dY, int actionState, boolean isActive) {
        View item = vh.itemView;
        if (dX != 0) {
            boolean left = dX < 0;
            paint.setColor(left ? deleteColor : editColor);
            RectF bg = new RectF(item.getLeft(), item.getTop(), item.getRight(), item.getBottom());
            c.drawRoundRect(bg, radius, radius, paint);

            Drawable icon = left ? deleteIcon : editIcon;
            int size = icon.getIntrinsicWidth();
            int top = item.getTop() + (item.getHeight() - size) / 2;
            int iconLeft = left ? item.getRight() - margin - size : item.getLeft() + margin;
            icon.setBounds(iconLeft, top, iconLeft + size, top + size);
            // Icon grows as the swipe nears the threshold.
            float progress = Math.min(1f, Math.abs(dX) / (item.getWidth() * 0.35f));
            icon.setAlpha((int) (255 * progress));
            icon.draw(c);
        }
        super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive);
    }
}
