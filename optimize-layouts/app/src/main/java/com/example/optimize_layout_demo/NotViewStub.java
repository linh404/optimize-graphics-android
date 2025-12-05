package com.example.optimize_layout_demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NotViewStub extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.not_viewstub_activity);
        setTitle("3. Bad: Visibility GONE");

        // View này đã được tạo (Inflated) ngay khi mở màn hình
        // Dù visibility="gone" nhưng nó vẫn chiếm bộ nhớ RAM
        View panel = findViewById(R.id.heavyPanel);

        findViewById(R.id.btnShow).setOnClickListener(v -> {
            panel.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Chỉ đổi Visibility (RAM đã tốn từ trước)", Toast.LENGTH_SHORT).show();
        });
    }
}