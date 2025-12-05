package com.example.optimize_layout_demo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ViewStub extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Mở ngay lập tức, không delay vì layout nhẹ
        setContentView(R.layout.viewstub_activity);
        setTitle("3. Good: ViewStub");

        // 10 ViewStub này siêu nhẹ, chưa chiếm bộ nhớ layout con
        android.view.ViewStub[] stubs = new android.view.ViewStub[]{
            findViewById(R.id.stubHeavy1),
            findViewById(R.id.stubHeavy2),
            findViewById(R.id.stubHeavy3),
            findViewById(R.id.stubHeavy4),
            findViewById(R.id.stubHeavy5),
            findViewById(R.id.stubHeavy6),
            findViewById(R.id.stubHeavy7),
            findViewById(R.id.stubHeavy8),
            findViewById(R.id.stubHeavy9),
            findViewById(R.id.stubHeavy10)
        };

        // Mỗi button inflate 1 ViewStub riêng
        findViewById(R.id.btnInflate1).setOnClickListener(v -> {
            if (stubs[0] != null) {
                stubs[0].inflate();
                stubs[0] = null; // Đánh dấu đã inflate
                Toast.makeText(this, "Đã inflate Layout 1", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 1 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate2).setOnClickListener(v -> {
            if (stubs[1] != null) {
                stubs[1].inflate();
                stubs[1] = null;
                Toast.makeText(this, "Đã inflate Layout 2", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 2 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate3).setOnClickListener(v -> {
            if (stubs[2] != null) {
                stubs[2].inflate();
                stubs[2] = null;
                Toast.makeText(this, "Đã inflate Layout 3", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 3 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate4).setOnClickListener(v -> {
            if (stubs[3] != null) {
                stubs[3].inflate();
                stubs[3] = null;
                Toast.makeText(this, "Đã inflate Layout 4", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 4 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate5).setOnClickListener(v -> {
            if (stubs[4] != null) {
                stubs[4].inflate();
                stubs[4] = null;
                Toast.makeText(this, "Đã inflate Layout 5", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 5 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate6).setOnClickListener(v -> {
            if (stubs[5] != null) {
                stubs[5].inflate();
                stubs[5] = null;
                Toast.makeText(this, "Đã inflate Layout 6", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 6 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate7).setOnClickListener(v -> {
            if (stubs[6] != null) {
                stubs[6].inflate();
                stubs[6] = null;
                Toast.makeText(this, "Đã inflate Layout 7", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 7 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate8).setOnClickListener(v -> {
            if (stubs[7] != null) {
                stubs[7].inflate();
                stubs[7] = null;
                Toast.makeText(this, "Đã inflate Layout 8", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 8 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate9).setOnClickListener(v -> {
            if (stubs[8] != null) {
                stubs[8].inflate();
                stubs[8] = null;
                Toast.makeText(this, "Đã inflate Layout 9", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 9 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnInflate10).setOnClickListener(v -> {
            if (stubs[9] != null) {
                stubs[9].inflate();
                stubs[9] = null;
                Toast.makeText(this, "Đã inflate Layout 10", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Layout 10 đã được inflate rồi!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}