package com.example.optimize_layout_demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity updated to:
 * - keep references to the rendering views so we can manage GLSurfaceView lifecycle (onResume/onPause)
 * - disable the two rendering buttons while a benchmark is running to avoid re-entrancy
 * - re-enable buttons when benchmark callback returns
 * - ensure GL view is resumed after being added and paused when removed
 */
public class MainActivity extends AppCompatActivity {

    private FrameLayout renderContainer;

    // Keep references to rendering views so we can pause/resume GL correctly
    private NormalRenderingView currentCanvasView;
    private OpenGLRenderingView currentGLView;

    // Buttons for the OpenGL/Canvas pair (we'll disable/enable them while a benchmark runs)
    private Button btn4Bad;
    private Button btn4Good;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Other navigation buttons (unchanged)
        findViewById(R.id.btn1Bad).setOnClickListener(v ->
                startActivity(new Intent(this, Linear.class)));

        findViewById(R.id.btn1Good).setOnClickListener(v ->
                startActivity(new Intent(this, Constraint.class)));

        findViewById(R.id.btnBenchmark).setOnClickListener(v ->
                startActivity(new Intent(this, re_li_benchmark_activity.class)));

        findViewById(R.id.btn2Bad).setOnClickListener(v ->
                startActivity(new Intent(this, NotMerge.class)));

        findViewById(R.id.btn2Good).setOnClickListener(v ->
                startActivity(new Intent(this, Merge.class)));

        findViewById(R.id.btn3Bad).setOnClickListener(v ->
                startActivity(new Intent(this, NotViewStub.class)));

        findViewById(R.id.btn3Good).setOnClickListener(v ->
                startActivity(new Intent(this, ViewStub.class)));

        // Buttons for the rendering demo (we need references to enable/disable)
        btn4Bad = findViewById(R.id.btn4Bad);
        btn4Good = findViewById(R.id.btn4Good);

        btn4Bad.setOnClickListener(v -> showCanvas2DRendering());
        btn4Good.setOnClickListener(v -> showOpenGLRendering());

        findViewById(R.id.btn5Bad).setOnClickListener(v ->
                startActivity(new Intent(this, ImageEagerLoadActivity.class)));

        findViewById(R.id.btn5Good).setOnClickListener(v ->
                startActivity(new Intent(this, ImageLazyLoadActivity.class)));
    }

    private void disableRenderButtons() {
        if (btn4Bad != null) btn4Bad.setEnabled(false);
        if (btn4Good != null) btn4Good.setEnabled(false);
    }

    private void enableRenderButtons() {
        if (btn4Bad != null) btn4Bad.setEnabled(true);
        if (btn4Good != null) btn4Good.setEnabled(true);
    }

    private void showCanvas2DRendering() {
        // hide ActionBar before switching view
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // remove old view if any (and pause GL if needed)
        removeRenderContainerIfAny();

        // create container
        renderContainer = new FrameLayout(this);
        renderContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // create and add Canvas 2D View
        currentCanvasView = new NormalRenderingView(this);
        renderContainer.addView(currentCanvasView);

        // add to screen
        ((ViewGroup) findViewById(android.R.id.content)).addView(renderContainer);

        // disable buttons while benchmark runs
        disableRenderButtons();

        // start benchmark after view has size
        currentCanvasView.post(() -> {
            currentCanvasView.startBenchmark(result -> {
                // re-enable buttons when done
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "Canvas: " + result);
                    enableRenderButtons();
                });
            });
        });
    }

    private void showOpenGLRendering() {
        // hide ActionBar before switching view
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // remove old view if any (and pause GL if needed)
        removeRenderContainerIfAny();

        // create container
        renderContainer = new FrameLayout(this);
        renderContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // create and add OpenGL View
        currentGLView = new OpenGLRenderingView(this);
        renderContainer.addView(currentGLView);

        // add to screen
        ((ViewGroup) findViewById(android.R.id.content)).addView(renderContainer);

        // resume GL view explicitly (important)
        currentGLView.onResume();

        // disable buttons while benchmark runs
        disableRenderButtons();

        // start benchmark after a short delay to allow surface creation (startBenchmark already waits, but keep a small delay)
        currentGLView.postDelayed(() -> {
            currentGLView.startBenchmark(result -> {
                // re-enable buttons when done
                runOnUiThread(() -> {
                    android.util.Log.d("MainActivity", "OpenGL: " + result);
                    enableRenderButtons();
                });
            });
        }, 100);
    }

    private void removeRenderContainerIfAny() {
        if (renderContainer != null) {
            // if GL view is active, pause it
            if (currentGLView != null) {
                try {
                    currentGLView.onPause();
                } catch (Exception ignored) {}
                currentGLView = null;
            }
            // remove container from root
            ((ViewGroup) findViewById(android.R.id.content)).removeView(renderContainer);
            renderContainer = null;

            // show ActionBar again
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
        }
        // null out canvas ref as well
        currentCanvasView = null;
    }

    @Override
    public void onBackPressed() {
        // If showing rendering, go back to main menu
        if (renderContainer != null) {
            removeRenderContainerIfAny();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ensure GL view is resumed if present
        if (currentGLView != null) currentGLView.onResume();
    }

    @Override
    protected void onPause() {
        // ensure GL view is paused if present
        if (currentGLView != null) currentGLView.onPause();
        super.onPause();
    }
}