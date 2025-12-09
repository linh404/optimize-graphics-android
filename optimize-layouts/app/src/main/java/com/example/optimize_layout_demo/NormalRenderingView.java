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

public class NormalRenderingView extends View {

    private static final int RECT_COUNT = 20_000;
    private static final float RECT_PX = 40f; // 40 px mỗi chiều
    private static final int RECT_COLOR = Color.argb(204, 51, 179, 255); // RGBA ~ (0.2, 0.7, 1.0, 0.8)

    private final Paint paint;
    private final List<Rect> rectangles;
    private final Random random;
    private boolean hasMeasured = false;

    public static class Rect {
        public final float left;
        public final float top;
        public final float right;
        public final float bottom;

        public Rect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    public NormalRenderingView(Context context) {
        this(context, null);
    }

    public NormalRenderingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectangles = new ArrayList<>(RECT_COUNT);
        random = new Random();
    }

    private void ensureRects() {
        if (!rectangles.isEmpty()) return;
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float maxX = Math.max(0f, w - RECT_PX);
        float maxY = Math.max(0f, h - RECT_PX);

        for (int i = 0; i < RECT_COUNT; i++) {
            float x = random.nextFloat() * maxX;
            float y = random.nextFloat() * maxY;
            rectangles.add(new Rect(x, y, x + RECT_PX, y + RECT_PX));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hasMeasured) return;

        ensureRects();
        if (rectangles.isEmpty()) {
            // Chưa có kích thước view, đợi lần sau
            invalidate();
            return;
        }

        Trace.beginSection("Canvas_Render_20000_Shapes");
        long start = System.nanoTime();

        // Nền đen
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);

        // Vẽ 20.000 rect (cùng màu, không shadow/rotate)
        paint.setColor(RECT_COLOR);
        paint.setStyle(Paint.Style.FILL);
        for (Rect r : rectangles) {
            canvas.drawRect(r.left, r.top, r.right, r.bottom, paint);
        }

        long end = System.nanoTime();
        double renderTimeMs = (end - start) / 1_000_000.0;

        // Overlay kết quả
        paint.setColor(Color.WHITE);
        paint.setTextSize(60f);
        canvas.drawText("Canvas (CPU)", 50f, 120f, paint);
        canvas.drawText(String.format("First frame: %.2f ms", renderTimeMs), 50f, 200f, paint);
        paint.setTextSize(40f);
        canvas.drawText("20,000 rects, same size/color", 50f, 280f, paint);

        // Lưu + log + Toast
        getContext().getSharedPreferences("results", Context.MODE_PRIVATE)
                .edit()
                .putFloat("canvas_render_time", (float) renderTimeMs)
                .apply();
        Log.d("CanvasTest", "Canvas first-frame render: " + renderTimeMs + " ms");
        Toast.makeText(
                getContext(),
                "Canvas first frame: " + String.format("%.2f ms", renderTimeMs),
                Toast.LENGTH_LONG
        ).show();

        hasMeasured = true;
        Trace.endSection();
    }
}