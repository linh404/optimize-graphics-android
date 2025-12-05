package com.example.optimize_layout_demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NotViewStub extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.not_viewstub_activity);
        setTitle("3. Bad: Visibility GONE");

        // 10 View này đã được tạo (Inflated) ngay khi mở màn hình
        // Dù visibility="gone" nhưng chúng vẫn chiếm bộ nhớ RAM
        View[] panels = new View[]{
            findViewById(R.id.heavyPanel1),
            findViewById(R.id.heavyPanel2),
            findViewById(R.id.heavyPanel3),
            findViewById(R.id.heavyPanel4),
            findViewById(R.id.heavyPanel5),
            findViewById(R.id.heavyPanel6),
            findViewById(R.id.heavyPanel7),
            findViewById(R.id.heavyPanel8),
            findViewById(R.id.heavyPanel9),
            findViewById(R.id.heavyPanel10)
        };

        // Mỗi button hiện 1 layout riêng
        findViewById(R.id.btnShow1).setOnClickListener(v -> {
            panels[0].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 1 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow2).setOnClickListener(v -> {
            panels[1].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 2 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow3).setOnClickListener(v -> {
            panels[2].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 3 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow4).setOnClickListener(v -> {
            panels[3].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 4 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow5).setOnClickListener(v -> {
            panels[4].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 5 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow6).setOnClickListener(v -> {
            panels[5].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 6 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow7).setOnClickListener(v -> {
            panels[6].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 7 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow8).setOnClickListener(v -> {
            panels[7].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 8 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow9).setOnClickListener(v -> {
            panels[8].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 9 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnShow10).setOnClickListener(v -> {
            panels[9].setVisibility(View.VISIBLE);
            Toast.makeText(this, "Đã hiện Layout 10 (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });
    }
}