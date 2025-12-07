package com.example.optimize_layout_demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Trace;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NormalRenderingView extends View {

    private Paint paint;
    private List<Rect> rectangles;
    private boolean hasMeasured = false;
    private Random random;

    public static class Rect {
        public float left;
        public float top;
        public float right;
        public float bottom;
        public int color;
        public float rotation;

        public Rect(float left, float top, float right, float bottom, int color, float rotation) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.color = color;
            this.rotation = rotation;
        }
    }

    public NormalRenderingView(Context context) {
        super(context);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectangles = new ArrayList<>();
        random = new Random();

        // Vẽ 20,000 hình ngẫu nhiên (cho test hiệu năng)
        for (int i = 0; i < 20000; i++) {
            float left = random.nextFloat() * 2000;
            float top = random.nextFloat() * 3000;
            float width = 10f + random.nextFloat() * 50;
            float height = 10f + random.nextFloat() * 50;

            int color = Color.argb(
                    150 + random.nextInt(106),
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)
            );

            rectangles.add(new Rect(
                    left,
                    top,
                    left + width,
                    top + height,
                    color,
                    random.nextFloat() * 360f
            ));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Đo hiệu năng bằng Trace
        Trace.beginSection("Canvas_Render_20000_Shapes");

        long startRender = !hasMeasured ? System.nanoTime() : 0L;

        // Background gradient
        Trace.beginSection("Canvas_Background");
        LinearGradient gradient = new LinearGradient(
                0f, 0f, getWidth(), getHeight(),
                Color.BLACK, Color.DKGRAY,
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
        paint.setShader(null);
        Trace.endSection();

        // Vẽ hình
        Trace.beginSection("Canvas_Draw_Rectangles");
        for (Rect rect : rectangles) {
            canvas.save();
            canvas.rotate(
                    rect.rotation,
                    (rect.left + rect.right) / 2,
                    (rect.top + rect.bottom) / 2
            );
            paint.setColor(rect.color);
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(5f, 2f, 2f, Color.BLACK);
            canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
            canvas.restore();
        }
        Trace.endSection();

        if (!hasMeasured) {
            double renderTime = (System.nanoTime() - startRender) / 1_000_000.0; // ms

            // Hiển thị kết quả
            Trace.beginSection("Canvas_Draw_Text");
            paint.clearShadowLayer();
            paint.setColor(Color.WHITE);
            paint.setTextSize(60f);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("Canvas (CPU)", 50f, 120f, paint);
            canvas.drawText(String.format("%.2f ms", renderTime), 50f, 200f, paint);
            paint.setTextSize(40f);
            canvas.drawText("20,000 shapes + effects", 50f, 280f, paint);
            Trace.endSection();

            getContext().getSharedPreferences("results", Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("canvas_render_time", (float) renderTime)
                    .apply();
            Log.d("CanvasTest", "Canvas render time: " + renderTime + " ms");

            Toast.makeText(
                    getContext(),
                    "Canvas: " + String.format("%.2f ms", renderTime),
                    Toast.LENGTH_LONG
            ).show();

            hasMeasured = true;
        }

        Trace.endSection();
    }
}