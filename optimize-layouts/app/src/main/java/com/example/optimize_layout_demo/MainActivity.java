package com.example.optimize_layout_demo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.optimize_layout_demo.Constraint;
import com.example.optimize_layout_demo.Linear;
import com.example.optimize_layout_demo.Merge;
import com.example.optimize_layout_demo.NotMerge;
import com.example.optimize_layout_demo.NotViewStub;
import com.example.optimize_layout_demo.ViewStub;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // --- CẶP 1: HIERARCHY ---
        findViewById(R.id.btn1Bad).setOnClickListener(v ->
                startActivity(new Intent(this, Linear.class)));

        findViewById(R.id.btn1Good).setOnClickListener(v ->
                startActivity(new Intent(this, Constraint.class)));

        // --- THÊM MỚI: BENCHMARK TOOL ---
        // Nút này sẽ mở màn hình đo đạc tốc độ
        findViewById(R.id.btnBenchmark).setOnClickListener(v ->
                startActivity(new Intent(this, re_li_benchmark_activity.class)));

        // --- CẶP 2: REUSE ---
        findViewById(R.id.btn2Bad).setOnClickListener(v ->
                startActivity(new Intent(this, NotMerge.class)));

        findViewById(R.id.btn2Good).setOnClickListener(v ->
                startActivity(new Intent(this, Merge.class)));

        // --- CẶP 3: LOADING ---
        findViewById(R.id.btn3Bad).setOnClickListener(v ->
                startActivity(new Intent(this, NotViewStub.class)));

        findViewById(R.id.btn3Good).setOnClickListener(v ->
                startActivity(new Intent(this, ViewStub.class)));
    }
}