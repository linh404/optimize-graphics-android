package com.example.optimize_layout_demo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class NotMerge extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load layout dùng include thường -> Tạo ra view container thừa
        setContentView(R.layout.not_merge_activity);

        setTitle("2. Bad: Normal Include");
    }
}