package com.example.optimize_layout_demo;

import android.graphics.Bitmap;

/**
 * Model class for image items.
 * For eager load: bitmap is pre-decoded and stored.
 * For lazy load: only resId is stored, bitmap is decoded on-demand.
 */
public class ImageItem {
    public final int resId;
    public final int index; // 1..TOTAL_IMAGES
    public final Bitmap bitmap; // null for lazy load, pre-decoded for eager load

    public ImageItem(int resId, int index) {
        this.resId = resId;
        this.index = index;
        this.bitmap = null; // Lazy load: decode later
    }

    public ImageItem(int resId, int index, Bitmap bitmap) {
        this.resId = resId;
        this.index = index;
        this.bitmap = bitmap; // Eager load: pre-decoded
    }
}

