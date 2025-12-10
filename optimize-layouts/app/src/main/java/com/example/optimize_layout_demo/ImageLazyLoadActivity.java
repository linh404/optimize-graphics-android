package com.example.optimize_layout_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * LAZY LOAD (GOOD) Strategy:
 * - Only creates ImageItem objects (no bitmap decoding)
 * - Bitmaps are decoded on-demand in onBindViewHolder
 * - Fast startup, low memory usage
 */
public class ImageLazyLoadActivity extends AppCompatActivity {

    private static final String TAG = "ImageLazyLoad";
    private static final int TOTAL_IMAGES = 40;
    private static final int PAGE_SIZE = 10;

    private List<ImageItem> fullData;
    private List<ImageItem> currentData;
    private ImageListAdapter adapter;
    private int currentLoadedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_lazy_load);

        TextView tvInfo = findViewById(R.id.tvInfo);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        // Setup UI
        LinearLayoutManager lm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(lm);

        Log.i(TAG, "=== LAZY LOAD START ===");
        Log.i(TAG, "Strategy: Decode first page bitmaps upfront, rest on-demand");
        Log.i(TAG, "Total images: " + TOTAL_IMAGES + ", Page size: " + PAGE_SIZE);

        // Create ImageItem objects for all items
        fullData = new ArrayList<>(TOTAL_IMAGES);
        for (int i = 0; i < TOTAL_IMAGES; i++) {
            fullData.add(new ImageItem(R.drawable.img, i + 1));
        }

        Log.d(TAG, "Created " + TOTAL_IMAGES + " ImageItem objects");

        // ============================================
        // MEASUREMENT START: Decode first page bitmaps only
        // ============================================
        long startTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "Start time: " + startTime + " ms");
        Log.i(TAG, "Decoding first page (" + PAGE_SIZE + " bitmaps)...");

        // Decode bitmaps for first page only (lazy load strategy)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        
        currentData = new ArrayList<>();
        int firstPageCount = Math.min(PAGE_SIZE, TOTAL_IMAGES);
        
        // Decode first page bitmaps upfront
        for (int i = 0; i < firstPageCount; i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(),
                    R.drawable.img,
                    options
            );
            // Store pre-decoded bitmap for first page
            ImageItem item = new ImageItem(R.drawable.img, i + 1, bitmap);
            currentData.add(item);
            currentLoadedCount++;
        }

        adapter = new ImageListAdapter(currentData, false); // false = lazy mode (mixed: some pre-decoded, some on-demand)
        recyclerView.setAdapter(adapter);

        long endTime = SystemClock.elapsedRealtime();
        long duration = endTime - startTime;
        // ============================================
        // MEASUREMENT END
        // ============================================

        Log.d(TAG, "End time: " + endTime + " ms");
        Log.i(TAG, "Setup time: " + duration + " ms");
        Log.i(TAG, "Decoded " + firstPageCount + " bitmaps upfront (first page)");
        Log.i(TAG, "Remaining " + (TOTAL_IMAGES - firstPageCount) + " bitmaps will decode on-demand");
        Log.i(TAG, "=== LAZY LOAD SETUP COMPLETE ===");

        // Setup scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager == null) return;

                int lastVisible = layoutManager.findLastVisibleItemPosition();
                int total = adapter.getItemCount();

                // Load next page when user scrolls near the end
                if (lastVisible >= total - 3) {
                    loadNextPage();
                }
            }
        });

        // Update UI
        String info = "Total images: " + TOTAL_IMAGES + System.lineSeparator() +
                "Page size: " + PAGE_SIZE + System.lineSeparator() +
                "Decoded upfront: " + firstPageCount + System.lineSeparator() +
                "Setup time: " + duration + " ms";
        tvInfo.setText(info);
    }

    /**
     * Loads next page of items (without decoding bitmaps).
     * Bitmaps for new items will be decoded on-demand in onBindViewHolder.
     */
    private void loadNextPage() {
        if (currentLoadedCount >= TOTAL_IMAGES) {
            Log.d(TAG, "All items already loaded");
            return;
        }

        int end = Math.min(currentLoadedCount + PAGE_SIZE, TOTAL_IMAGES);
        int startIndex = currentLoadedCount;
        int count = end - startIndex;

        Log.d(TAG, "Loading page: items " + startIndex + " to " + (end - 1) + " (" + count + " items)");
        Log.d(TAG, "Bitmaps will decode on-demand when items are displayed");

        // Add items without decoded bitmaps (will decode on-demand)
        for (int i = startIndex; i < end; i++) {
            currentData.add(fullData.get(i));
        }

        adapter.notifyItemRangeInserted(startIndex, count);
        currentLoadedCount = end;

        Log.d(TAG, "Page loaded. Total loaded: " + currentLoadedCount + "/" + TOTAL_IMAGES);
    }
}

