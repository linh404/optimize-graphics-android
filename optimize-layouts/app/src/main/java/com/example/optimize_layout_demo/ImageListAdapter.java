package com.example.optimize_layout_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying image items.
 * - Eager mode: Uses pre-decoded bitmaps from ImageItem.bitmap
 * - Lazy mode: Decodes bitmap on-demand in onBindViewHolder
 */
public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.VH> {

    private static final String TAG = "ImageListAdapter";
    private static final int[] COLORS = {
        0x55FF0000, // đỏ nhạt
        0x5500FF00, // xanh lá nhạt
        0x550000FF, // xanh dương nhạt
        0x55FFFF00, // vàng nhạt
        0x55FF00FF, // tím nhạt
        0x5500FFFF  // cyan nhạt
    };

    private final List<ImageItem> data;
    private final boolean isEagerMode; // true = eager (pre-decoded), false = lazy (decode on-demand)

    public ImageListAdapter(List<ImageItem> data, boolean isEagerMode) {
        this.data = data;
        this.isEagerMode = isEagerMode;
        Log.d(TAG, "Adapter created. Mode: " + (isEagerMode ? "EAGER" : "LAZY") + ", Items: " + data.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_simple, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ImageItem item = data.get(position);

        // ============================================
        // BITMAP DECODING HAPPENS HERE FOR LAZY MODE
        // ============================================
        if (isEagerMode) {
            // EAGER MODE: Use pre-decoded bitmap
            if (item.bitmap != null) {
                holder.imageView.setImageBitmap(item.bitmap);
            } else {
                // Fallback (shouldn't happen in eager mode)
                holder.imageView.setImageResource(item.resId);
            }
        } else {
            // LAZY MODE: Use pre-decoded if available, otherwise decode on-demand
            if (item.bitmap != null) {
                // First page: use pre-decoded bitmap
                holder.imageView.setImageBitmap(item.bitmap);
            } else {
                // Remaining items: decode on-demand when item is displayed
                long decodeStart = System.currentTimeMillis();
                Bitmap bitmap = BitmapFactory.decodeResource(
                        holder.itemView.getContext().getResources(),
                        item.resId
                );
                long decodeEnd = System.currentTimeMillis();

                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap);
                    if (position < 5 || position % 50 == 0) { // Log first 5 and every 50th item
                        Log.v(TAG, "Decoded bitmap for position " + position + " in " + (decodeEnd - decodeStart) + " ms");
                    }
                } else {
                    Log.w(TAG, "Failed to decode bitmap for position " + position);
                    holder.imageView.setImageResource(item.resId);
                }
            }
        }

        // Set label
        holder.tvLabel.setText("Image #" + item.index);

        // Apply color filter to differentiate items visually
        int color = COLORS[position % COLORS.length];
        holder.imageView.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvLabel;

        VH(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            tvLabel = itemView.findViewById(R.id.tvLabel);
        }
    }
}

