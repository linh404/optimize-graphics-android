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

/**
 * Vẽ 20.000 hình chữ nhật (40px x 40px), mỗi rect có màu khác nhau, vị trí ngẫu nhiên.
 * Đo thời gian render 1 frame (CPU draw) và hiển thị ổn định (không đổi số giữa alert và overlay).
 * Có hàm đo end-to-end bằng PixelCopy (request -> frame presented).
 */
public class NormalRenderingView extends View {

    private static final int RECT_COUNT = 20_000;
    private static final float RECT_PX = 40f;
    private static final int RECT_ALPHA = 204;

    private final Paint paint;
    private final List<Rect> rects;
    private final Random random;

    // Thời gian vẽ frame đầu tiên (CPU draw), cố định sau lần đo đầu
    private boolean firstDrawMeasured = false;
    private double firstDrawMs = -1.0;

    public interface OnMeasureDone {
        /**
         * @param ms thời gian milli giây từ trigger tới khi frame được composited/presented
         * @param pixelCopyResult PixelCopy result (PixelCopy.SUCCESS = 0); âm nếu lỗi logic
         */
        void onDone(double ms, int pixelCopyResult);
    }

    public static class Rect {
        public final float left, top, right, bottom;
        public final int color;
        public Rect(float l, float t, float r, float b, int color) {
            left = l; top = t; right = r; bottom = b; this.color = color;
        }
    }

    public NormalRenderingView(Context context) {
        this(context, null);
    }

    public NormalRenderingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rects = new ArrayList<>(RECT_COUNT);
        random = new Random(42L);
        // Nếu muốn đo CPU raster thuần: bật software layer
        // setLayerType(LAYER_TYPE_SOFTWARE, null);
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
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rects.isEmpty()) {
            // Chưa có kích thước hoặc chưa build xong -> request vẽ lại
            invalidate();
            return;
        }

        Trace.beginSection("Canvas_Render_20000_Shapes");
        long start = System.nanoTime();

        // Nền đen
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);

        // Vẽ 20.000 rect với màu riêng từng rect
        paint.setStyle(Paint.Style.FILL);
        for (Rect r : rects) {
            paint.setColor(r.color);
            canvas.drawRect(r.left, r.top, r.right, r.bottom, paint);
        }

        long end = System.nanoTime();
        double drawMs = (end - start) / 1_000_000.0;

        // Chốt giá trị lần đầu, dùng chung cho overlay + toast/log để không lệch số
        if (!firstDrawMeasured) {
            firstDrawMeasured = true;
            firstDrawMs = drawMs;

            getContext().getSharedPreferences("results", Context.MODE_PRIVATE)
                    .edit().putFloat("canvas_render_time", (float) firstDrawMs).apply();
            Log.d("CanvasTest", "Canvas first-frame render: " + firstDrawMs + " ms");
            Toast.makeText(getContext(),
                    "Canvas first frame: " + String.format("%.2f ms", firstDrawMs),
                    Toast.LENGTH_LONG).show();
        }

        Trace.endSection();

        // Overlay hiển thị giá trị đã chốt (không đổi nữa)
        double displayMs = (firstDrawMs >= 0) ? firstDrawMs : drawMs;
        paint.setColor(Color.WHITE);
        paint.setTextSize(56f);
        canvas.drawText("Canvas (20k rect)", 40f, 100f, paint);
        canvas.drawText(String.format("First frame: %.2f ms", displayMs), 40f, 170f, paint);
    }

    /**
     * Đo end-to-end (request -> frame presented) bằng PixelCopy (API 26+).
     * Gọi khi view đã có kích thước hợp lệ.
     */
    public void measurePresented(Activity activity, OnMeasureDone cb) {
        if (activity == null || cb == null) return;
        if (getWidth() <= 0 || getHeight() <= 0) {
            cb.onDone(-1, -999); // view chưa layout
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            cb.onDone(-2, -998); // PixelCopy yêu cầu API 26+
            return;
        }

        final Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Window window = activity.getWindow();
        final Handler handler = new Handler(Looper.getMainLooper());
        final long startNanos = System.nanoTime();

        invalidate(); // trigger render

        PixelCopy.request(window, bmp, copyResult -> {
            long endNanos = System.nanoTime();
            double ms = (endNanos - startNanos) / 1_000_000.0;
            Log.d("PixelCopyMeasure", "Canvas presented in " + ms + " ms, result=" + copyResult);
            cb.onDone(ms, copyResult);
        }, handler);
    }
}