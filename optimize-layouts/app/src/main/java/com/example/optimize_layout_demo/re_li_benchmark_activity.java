package com.example.optimize_layout_demo; // Đổi package cho đúng của bạn

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class re_li_benchmark_activity extends AppCompatActivity {

    private TextView tvResult;
    private static final int LOOP_COUNT = 2000; // Số lần lặp (Tăng lên nếu máy quá mạnh)

    // Biến lưu kết quả để so sánh
    private long timeBad = 0;
    private long timeGood = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.re_li_benchmark_activity);

        Button btnBad = findViewById(R.id.btnTestLinear);
        Button btnGood = findViewById(R.id.btnTestOptimized);
        tvResult = findViewById(R.id.tvResult);

        btnBad.setOnClickListener(v -> {
            tvResult.setText("Đang chạy Linear Test...");
            // Dùng Handler để UI cập nhật chữ "Đang chạy..." trước khi bị block
            v.postDelayed(() -> runBenchmark(true), 100);
        });

        btnGood.setOnClickListener(v -> {
            tvResult.setText("Đang chạy Optimized Test...");
            v.postDelayed(() -> runBenchmark(false), 100);
        });
    }

    private void runBenchmark(boolean isBadLayout) {
        int layoutId = isBadLayout ? R.layout.linear_layout : R.layout.relative_layout; // Thay tên file xml của bạn vào đây
        String layoutName = isBadLayout ? "Linear Nested" : "Relative/Constraint Flat";

        long startTime = System.nanoTime();

        for (int i = 0; i < LOOP_COUNT; i++) {
            // 1. Inflate: Tạo View từ XML
            View view = getLayoutInflater().inflate(layoutId, null);

            // 2. Force Measure: Bắt hệ thống tính toán kích thước
            // Đây là bước LinearLayout bị chậm nhất do tính toán weight
            view.measure(
                    View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;

        if (isBadLayout) {
            timeBad = durationMs;
        } else {
            timeGood = durationMs;
        }

        updateUI(layoutName, durationMs);
    }

    private void updateUI(String name, long time) {
        StringBuilder sb = new StringBuilder();
        sb.append("KẾT QUẢ (" + LOOP_COUNT + " lần lặp):\n");

        if (timeBad > 0) sb.append("Linear (not-optimized): " + timeBad + " ms\n");
        if (timeGood > 0) sb.append("Relative (optimized): " + timeGood + " ms\n");

        if (timeBad > 0 && timeGood > 0) {
            long diff = timeBad - timeGood;
            double percent = ((double) diff / timeBad) * 100;
            sb.append("\n-----------------\n");
            sb.append("KẾT LUẬN: Relative Layout nhanh hơn " + String.format("%.1f", percent) + "%");
        }

        tvResult.setText(sb.toString());
    }
}