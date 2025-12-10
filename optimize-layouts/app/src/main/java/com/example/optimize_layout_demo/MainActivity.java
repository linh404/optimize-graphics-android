package com.example.optimize_layout_demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FrameLayout renderContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // --- CẶP 1: HIERARCHY ---
        findViewById(R.id.btn1Bad).setOnClickListener(v ->
                startActivity(new Intent(this, Linear.class)));

        findViewById(R.id.btn1Good).setOnClickListener(v ->
                startActivity(new Intent(this, Constraint.class)));

        // --- BENCHMARK TOOL ---
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

        // --- CẶP 4: OPENGL ES RENDERING ---
        findViewById(R.id.btn4Bad).setOnClickListener(v -> showCanvas2DRendering());
        findViewById(R.id.btn4Good).setOnClickListener(v -> showOpenGLRendering());

        // --- CẶP 5: IMAGE LAZY LOADING ---
        findViewById(R.id.btn5Bad).setOnClickListener(v ->
                startActivity(new Intent(this, ImageEagerLoadActivity.class)));

        findViewById(R.id.btn5Good).setOnClickListener(v ->
                startActivity(new Intent(this, ImageLazyLoadActivity.class)));
    }

    private void showCanvas2DRendering() {
        // ẨN ActionBar trước khi chuyển view
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Xóa view cũ nếu có
        if (renderContainer != null) {
            ((ViewGroup) findViewById(android.R.id.content)).removeView(renderContainer);
        }

        // Tạo container
        renderContainer = new FrameLayout(this);
        renderContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Thêm Canvas 2D View
        NormalRenderingView canvas2DView = new NormalRenderingView(this);
        renderContainer.addView(canvas2DView);

        // Thêm vào màn hình
        ((ViewGroup) findViewById(android.R.id.content)).addView(renderContainer);
    }

    private void showOpenGLRendering() {
        // ẨN ActionBar trước khi chuyển view
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Xóa view cũ nếu có
        if (renderContainer != null) {
            ((ViewGroup) findViewById(android.R.id.content)).removeView(renderContainer);
        }

        // Tạo container
        renderContainer = new FrameLayout(this);
        renderContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Thêm OpenGL View
        OpenGLRenderingView openGLView = new OpenGLRenderingView(this);
        renderContainer.addView(openGLView);

        // Thêm vào màn hình
        ((ViewGroup) findViewById(android.R.id.content)).addView(renderContainer);
    }

    @Override
    public void onBackPressed() {
        // Nếu đang show rendering, back về menu chính
        if (renderContainer != null) {
            // HIỆN lại ActionBar khi back về menu
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }

            ((ViewGroup) findViewById(android.R.id.content)).removeView(renderContainer);
            renderContainer = null;
        } else {
            super.onBackPressed();
        }
    }
}