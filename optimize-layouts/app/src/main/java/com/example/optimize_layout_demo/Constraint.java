package com.example.optimize_layout_demo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import androidx.appcompat.app.AppCompatActivity;

public class Constraint extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Bắt đầu bấm giờ
        long startTime = System.nanoTime();

        // 2. Thực hiện nạp Layout Tốt
        setContentView(R.layout.relative_layout);

        // 3. Đo thời gian Inflate xong
        long inflateTime = System.nanoTime() - startTime;
        Log.e("BENCHMARK", "CONSTRAINT - Thời gian Inflate: " + (inflateTime / 1000000.0) + " ms");

        // 4. Đo thời gian khi giao diện đã vẽ xong
        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Xóa listener
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                long endTime = System.nanoTime();
                long totalTime = endTime - startTime;

                Log.e("BENCHMARK", "CONSTRAINT - Tổng thời gian hiển thị: " + (totalTime / 1000000.0) + " ms");
            }
        });

        setTitle("1. Good: Constraint");
    }
}