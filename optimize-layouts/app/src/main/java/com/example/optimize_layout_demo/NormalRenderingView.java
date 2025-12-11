package com.example.optimize_layout_demo;

import android.content.Intent;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Locale;

public class NormalRenderingView extends View {

    private static final String TAG = "CanvasBenchmark";
    private static final int RECT_COUNT = 50_000;
    private static final float RECT_PX = 40f;
    private static final int RECT_ALPHA = 204;

    private static final int WARMUP_FRAMES = 5;
    private static final int MEASURE_FRAMES = 20;

    private final Paint paint;
    private final List<Rect> rects;
    private final Random random;

    private int frameCount = 0;
    private boolean measuring = false;
    private final List<Double> frameTimes = new ArrayList<>();
    private long lastFrameTimeNanos = 0;

    private OnBenchmarkComplete callback;

    public interface OnBenchmarkComplete {
        void onComplete(double avgFrameMs);
    }

    public static class Rect {
        public final float left, top, right, bottom;
        public final int color;
        public Rect(float l, float t, float r, float b, int color) {
            left = l; top = t; right = r; bottom = b; this.color = color;
        }
    }

    public NormalRenderingView(Context context) { this(context, null); }

    public NormalRenderingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rects = new ArrayList<>(RECT_COUNT);
        random = new Random(42L);
    }

    public void startBenchmark(OnBenchmarkComplete callback) {
        this.callback = callback;
        this.frameCount = 0;
        this.frameTimes.clear();
        this.lastFrameTimeNanos = 0;
        this.measuring = true;

        // Start frame callbacks
        Choreographer.getInstance().postFrameCallback(frameCallback);
        invalidate();

        Log.d(TAG, "Canvas Benchmark Started");
    }

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!measuring) return;

            if (lastFrameTimeNanos != 0 && frameCount > WARMUP_FRAMES) {
                long frameDeltaNanos = frameTimeNanos - lastFrameTimeNanos;
                double frameTimeMs = frameDeltaNanos / 1_000_000.0;
                frameTimes.add(frameTimeMs);
            }
            lastFrameTimeNanos = frameTimeNanos;

            if (frameCount < WARMUP_FRAMES + MEASURE_FRAMES) {
                frameCount++;
                postInvalidateOnAnimation();
                Choreographer.getInstance().postFrameCallback(this);
            } else {
                finalizeBenchmark();
            }
        }
    };

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildRects(w, h);
        invalidate();
    }

    private void buildRects(int w, int h) {
        rects.clear();
        if (w <= 0 || h <= 0) return;

        float maxX = Math.max(0f, w - RECT_PX);
        float maxY = Math.max(0f, h - RECT_PX);

        for (int i = 0; i < RECT_COUNT; i++) {
            float x = random.nextFloat() * maxX;
            float y = random.nextFloat() * maxY;

            float hue = random.nextFloat() * 360f;
            float sat = 0.6f + random.nextFloat() * 0.4f;
            float val = 0.6f + random.nextFloat() * 0.4f;
            int color = android.graphics.Color.HSVToColor(RECT_ALPHA, new float[]{hue, sat, val});

            rects.add(new Rect(x, y, x + RECT_PX, y + RECT_PX, color));
        }

        Log.d(TAG, "Built " + RECT_COUNT + " rectangles");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!rects.isEmpty()) {
            // Clear
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);

            // Draw 50k shapes
            paint.setStyle(Paint.Style.FILL);
            for (Rect r : rects) {
                paint.setColor(r.color);
                canvas.drawRect(r.left, r.top, r.right, r.bottom, paint);
            }

            if (measuring) {
                // Keep invalidating to continue frames while measuring
                postInvalidateOnAnimation();
            }
        }

        // NOTE: We intentionally do NOT draw overlay or result here.
        // The results will be shown on a separate screen (StatsActivity).
    }

    private void finalizeBenchmark() {
        measuring = false;

        if (frameTimes.isEmpty()) return;

        double frameSum = 0;
        for (double t : frameTimes) frameSum += t;
        double avgFrame = frameSum / frameTimes.size();

        Log.d(TAG, "Canvas Result: " + String.format(Locale.US, "%.2fms", avgFrame));

        // Notify callback on main thread (if used)
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onComplete(avgFrame));
        }

        // Launch StatsActivity to show full results
        try {
            double[] arr = new double[frameTimes.size()];
            for (int i = 0; i < frameTimes.size(); i++) arr[i] = frameTimes.get(i);

            Context ctx = getContext();
            Intent it = new Intent(ctx, StatsActivity.class);
            it.putExtra(StatsActivity.EXTRA_MODE, "Canvas");
            it.putExtra(StatsActivity.EXTRA_AVG, avgFrame);
            it.putExtra(StatsActivity.EXTRA_FRAME_TIMES, arr);

            // If context is not an activity, add NEW_TASK
            if (!(ctx instanceof android.app.Activity)) {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ctx.startActivity(it);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to launch StatsActivity", ex);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}