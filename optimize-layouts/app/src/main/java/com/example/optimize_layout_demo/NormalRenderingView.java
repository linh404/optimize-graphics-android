package com.example.optimize_layout_demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vẽ 20.000 hình chữ nhật (40px x 40px), cùng màu, vị trí ngẫu nhiên trong màn hình.
 * Đo thời gian render 1 frame (onDraw đầu tiên), có nhãn Trace cho System Trace.
 */
public class NormalRenderingView extends View {

    private static final int RECT_COUNT = 20_000;
    private static final float RECT_PX = 40f;
    private static final int RECT_COLOR = Color.argb(204, 51, 179, 255); // RGBA ~ (0.2, 0.7, 1.0, 0.8)

    private final Paint paint;
    private final List<Rect> rects;
    private final Random random;
    private boolean hasMeasured = false;
    private double lastRenderMs = 0.0;

    public static class Rect {
        public final float left, top, right, bottom;
        public Rect(float l, float t, float r, float b) { left = l; top = t; right = r; bottom = b; }
    }

    public NormalRenderingView(Context context) { this(context, null); }

    public NormalRenderingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rects = new ArrayList<>(RECT_COUNT);
        random = new Random(42L);
        // Nếu muốn đo CPU raster thuần: bật software layer
        // setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private void buildRectsIfNeeded() {
        if (!rects.isEmpty()) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        float maxX = Math.max(0f, w - RECT_PX);
        float maxY = Math.max(0f, h - RECT_PX);
        for (int i = 0; i < RECT_COUNT; i++) {
            float x = random.nextFloat() * maxX;
            float y = random.nextFloat() * maxY;
            rects.add(new Rect(x, y, x + RECT_PX, y + RECT_PX));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hasMeasured) return;

        buildRectsIfNeeded();
        if (rects.isEmpty()) { invalidate(); return; }

        Trace.beginSection("Canvas_Render_20000_Shapes");
        long start = System.nanoTime();

        // Nền đen
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);

        // Vẽ 20.000 rect
        paint.setColor(RECT_COLOR);
        paint.setStyle(Paint.Style.FILL);
        for (Rect r : rects) {
            canvas.drawRect(r.left, r.top, r.right, r.bottom, paint);
        }

        long end = System.nanoTime();
        lastRenderMs = (end - start) / 1_000_000.0;
        hasMeasured = true;
        Trace.endSection();

        // Overlay + log + lưu
        paint.setColor(Color.WHITE);
        paint.setTextSize(56f);
        canvas.drawText("Canvas (20k rect)", 40f, 100f, paint);
        canvas.drawText(String.format("First frame: %.2f ms", lastRenderMs), 40f, 170f, paint);

        getContext().getSharedPreferences("results", Context.MODE_PRIVATE)
                .edit().putFloat("canvas_render_time", (float) lastRenderMs).apply();
        Log.d("CanvasTest", "Canvas first-frame render: " + lastRenderMs + " ms");
        Toast.makeText(getContext(),
                "Canvas first frame: " + String.format("%.2f ms", lastRenderMs),
                Toast.LENGTH_LONG).show();
    }
}