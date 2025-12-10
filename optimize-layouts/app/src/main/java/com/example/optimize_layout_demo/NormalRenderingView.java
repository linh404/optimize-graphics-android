package com.example.optimize_layout_demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NormalRenderingView extends View {

    private static final int RECT_COUNT = 50_000;
    private static final float RECT_PX = 40f;
    private static final int RECT_ALPHA = 204;

    private final Paint paint;
    private final List<Rect> rects;
    private final Random random;

    private boolean firstDrawMeasured = false;
    private double firstDrawMs = -1.0;

    // Text overlay cache
    private Bitmap textBitmap;
    private boolean textOverlayInitialized = false;

    public interface OnMeasureDone {
        void onDone(double ms, int pixelCopyResult);
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
            int color = Color.HSVToColor(RECT_ALPHA, new float[]{hue, sat, val});

            rects.add(new Rect(x, y, x + RECT_PX, y + RECT_PX, color));
        }

        Log.d("CanvasTest", "Built " + RECT_COUNT + " rectangles");
    }

    private void initTextOverlay(String text) {
        if (textOverlayInitialized) return;

        // nhỏ hơn, đặt ở góc trái trên
        textBitmap = Bitmap.createBitmap(600, 160, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(textBitmap);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTextSize(42f);
        tp.setTypeface(android.graphics.Typeface.DEFAULT);

        c.drawColor(Color.TRANSPARENT);
        c.drawText("Canvas (50k rect)", 16f, 60f, tp);
        c.drawText(text, 16f, 120f, tp);

        textOverlayInitialized = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rects.isEmpty()) {
            invalidate();
            return;
        }

        Trace.beginSection("Canvas_Render_50000_Shapes");
        long start = System.nanoTime();

        // Clear background
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);

        // Draw all rectangles
        paint.setStyle(Paint.Style.FILL);
        for (Rect r : rects) {
            paint.setColor(r.color);
            canvas.drawRect(r.left, r.top, r.right, r.bottom, paint);
        }

        long end = System.nanoTime();
        double drawMs = (end - start) / 1_000_000.0;

        if (!firstDrawMeasured) {
            firstDrawMeasured = true;
            firstDrawMs = drawMs;

            getContext().getSharedPreferences("results", Context.MODE_PRIVATE)
                    .edit().putFloat("canvas_render_time", (float) firstDrawMs).apply();
            Log.d("CanvasTest", "Canvas first-frame render (50k): " + firstDrawMs + " ms");
            Toast.makeText(getContext(),
                    "Canvas (50k): " + String.format("%.2f ms", firstDrawMs),
                    Toast.LENGTH_LONG).show();

            // Init text overlay ONCE
            initTextOverlay(String.format("First frame: %.2f ms", firstDrawMs));
        }

        Trace.endSection();

        // Draw text overlay (from cached bitmap), top-left
        if (textOverlayInitialized && textBitmap != null) {
            canvas.drawBitmap(textBitmap, 16f, 16f, null);
        }
    }

    public void measurePresented(Activity activity, OnMeasureDone cb) {
        if (activity == null || cb == null) return;
        if (getWidth() <= 0 || getHeight() <= 0) {
            cb.onDone(-1, -999);
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            cb.onDone(-2, -998);
            return;
        }

        final Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Window window = activity.getWindow();
        final Handler handler = new Handler(Looper.getMainLooper());
        final long startNanos = System.nanoTime();

        invalidate();

        PixelCopy.request(window, bmp, copyResult -> {
            long endNanos = System.nanoTime();
            double ms = (endNanos - startNanos) / 1_000_000.0;
            Log.d("PixelCopyMeasure", "Canvas presented in " + ms + " ms, result=" + copyResult);
            cb.onDone(ms, copyResult);
        }, handler);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (textBitmap != null && !textBitmap.isRecycled()) {
            textBitmap.recycle();
            textBitmap = null;
        }
    }
}