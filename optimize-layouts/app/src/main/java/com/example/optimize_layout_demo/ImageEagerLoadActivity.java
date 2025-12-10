package com.example.optimize_layout_demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * EAGER LOAD (BAD) Strategy:
 * - Decodes ALL bitmaps upfront in onCreate
 * - Measures time including bitmap decoding
 * - High memory usage and slow startup
 */
public class ImageEagerLoadActivity extends AppCompatActivity {

    private static final String TAG = "ImageEagerLoad";
    private static final int TOTAL_IMAGES = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_eager_load);

        TextView tvInfo = findViewById(R.id.tvInfo);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        // Setup UI
        LinearLayoutManager lm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(lm);

        Log.i(TAG, "=== EAGER LOAD START ===");
        Log.i(TAG, "Strategy: Decode all " + TOTAL_IMAGES + " bitmaps upfront");
        Log.i(TAG, "Starting bitmap decoding...");

        // ============================================
        // MEASUREMENT START: Decode all bitmaps here
        // ============================================
        long startTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "Start time: " + startTime + " ms");

        List<ImageItem> data = new ArrayList<>(TOTAL_IMAGES);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;

        // Decode ALL bitmaps upfront (this is the expensive operation)
        for (int i = 0; i < TOTAL_IMAGES; i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(),
                    R.drawable.img,
                    options
            );
            data.add(new ImageItem(R.drawable.img, i + 1, bitmap));
        }

        // Create adapter with pre-decoded bitmaps
        ImageListAdapter adapter = new ImageListAdapter(data, true); // true = eager mode
        recyclerView.setAdapter(adapter);

        long endTime = SystemClock.elapsedRealtime();
        long duration = endTime - startTime;
        // ============================================
        // MEASUREMENT END
        // ============================================

        Log.d(TAG, "End time: " + endTime + " ms");
        Log.i(TAG, "Setup time: " + duration + " ms");
        Log.i(TAG, "Decoded " + TOTAL_IMAGES + " bitmaps");
        Log.i(TAG, "=== EAGER LOAD COMPLETE ===");

        // Update UI
        String info = "Total images: " + TOTAL_IMAGES + System.lineSeparator() +
                "Load strategy: decode all bitmaps on start" + System.lineSeparator() +
                "Setup time: " + duration + " ms";
        tvInfo.setText(info);
    }
}

