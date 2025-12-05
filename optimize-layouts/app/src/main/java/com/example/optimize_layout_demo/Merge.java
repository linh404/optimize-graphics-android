package com.example.optimize_layout_demo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class Merge extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load layout dùng thẻ merge -> Loại bỏ view container thừa
        setContentView(R.layout.merge_activity);

        setTitle("2. Good: Merge Tag");
    }
}