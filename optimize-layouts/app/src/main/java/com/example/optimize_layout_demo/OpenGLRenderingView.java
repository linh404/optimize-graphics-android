package com.example.optimize_layout_demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderingView extends GLSurfaceView {

    private final RectanglesRenderer renderer;

    public OpenGLRenderingView(Context context) { this(context, null); }

    public OpenGLRenderingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new RectanglesRenderer(context, this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public void startBenchmark(NormalRenderingView.OnBenchmarkComplete callback) {
        // Wait briefly to ensure surface has been created
        postDelayed(() -> {
            queueEvent(() -> renderer.startBenchmark(callback));
        }, 200);
    }

    public static class RectanglesRenderer implements GLSurfaceView.Renderer {

        private static final String TAG = "OpenGLBenchmark";
        private static final int RECT_COUNT = 50_000;
        private static final float RECT_PX = 40f;
        private static final int RECT_ALPHA = 255;

        private static final int WARMUP_FRAMES = 5;
        private static final int MEASURE_FRAMES = 20;

        private final Context context;
        private final Handler mainHandler;
        private final Random random = new Random(42L);
        private final GLSurfaceView glView;

        private int program = 0;
        private int vboId = 0;
        private int aPosLoc, aColorLoc;
        private int surfaceW = 0, surfaceH = 0;

        private boolean vboReady = false;
        private int frameCount = 0;
        private boolean measuring = false;
        private final List<Double> frameTimes = new ArrayList<>();
        private long lastFrameTimeNanos = 0;

        private NormalRenderingView.OnBenchmarkComplete callback;

        // We will NOT draw overlay on the GL surface; results will be shown on a separate screen.
        public RectanglesRenderer(Context context, GLSurfaceView glView) {
            this.context = context;
            this.glView = glView;
            this.mainHandler = new Handler(Looper.getMainLooper());
        }

        public void startBenchmark(NormalRenderingView.OnBenchmarkComplete callback) {
            if (!vboReady || vboId == 0) {
                Log.e(TAG, "VBO not ready! Cannot start benchmark.");
                return;
            }

            this.callback = callback;
            this.frameCount = 0;
            this.frameTimes.clear();
            this.lastFrameTimeNanos = 0;
            this.measuring = true;

            Log.d(TAG, "OpenGL Benchmark Started");
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            int vShader = loadShader(GLES20.GL_VERTEX_SHADER, VS);
            int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FS);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vShader);
            GLES20.glAttachShader(program, fShader);
            GLES20.glLinkProgram(program);

            aPosLoc = GLES20.glGetAttribLocation(program, "vPosition");
            aColorLoc = GLES20.glGetAttribLocation(program, "aColor");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            surfaceW = width;
            surfaceH = height;
            buildVbo();
            vboReady = true;

            Log.d(TAG, "Surface ready: " + width + "x" + height);
        }

        private void buildVbo() {
            if (surfaceW <= 0 || surfaceH <= 0) return;

            float sizeNdcX = (RECT_PX / surfaceW) * 2f;
            float sizeNdcY = (RECT_PX / surfaceH) * 2f;
            float maxX = 2f - sizeNdcX;
            float maxY = 2f - sizeNdcY;

            final int floatsPerVertex = 7;
            int vertexCount = RECT_COUNT * 6;
            float[] interleaved = new float[vertexCount * floatsPerVertex];
            int idx = 0;

            for (int i = 0; i < RECT_COUNT; i++) {
                float x = -1f + random.nextFloat() * maxX;
                float y = -1f + random.nextFloat() * maxY;

                float hue = random.nextFloat() * 360f;
                float sat = 0.6f + random.nextFloat() * 0.4f;
                float val = 0.6f + random.nextFloat() * 0.4f;
                int colInt = Color.HSVToColor(RECT_ALPHA, new float[]{hue, sat, val});

                float a = ((colInt >> 24) & 0xFF) / 255f;
                float r = ((colInt >> 16) & 0xFF) / 255f;
                float g = ((colInt >> 8) & 0xFF) / 255f;
                float b = (colInt & 0xFF) / 255f;

                float x2 = x + sizeNdcX;
                float y2 = y + sizeNdcY;

                interleaved[idx++] = x; interleaved[idx++] = y; interleaved[idx++] = 0f;
                interleaved[idx++] = r; interleaved[idx++] = g; interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x2; interleaved[idx++] = y; interleaved[idx++] = 0f;
                interleaved[idx++] = r; interleaved[idx++] = g; interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x; interleaved[idx++] = y2; interleaved[idx++] = 0f;
                interleaved[idx++] = r; interleaved[idx++] = g; interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x2; interleaved[idx++] = y; interleaved[idx++] = 0f;
                interleaved[idx++] = r; interleaved[idx++] = g; interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x2; interleaved[idx++] = y2; interleaved[idx++] = 0f;
                interleaved[idx++] = r; interleaved[idx++] = g; interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x; interleaved[idx++] = y2; interleaved[idx++] = 0f;
                interleaved[idx++] = r; interleaved[idx++] = g; interleaved[idx++] = b; interleaved[idx++] = a;
            }

            if (vboId != 0) {
                int[] old = {vboId};
                GLES20.glDeleteBuffers(1, old, 0);
            }

            int[] buffers = new int[1];
            GLES20.glGenBuffers(1, buffers, 0);
            vboId = buffers[0];
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

            FloatBuffer vb = ByteBuffer.allocateDirect(interleaved.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            vb.put(interleaved).position(0);

            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, interleaved.length * 4, vb, GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            Log.d(TAG, "VBO built with " + RECT_COUNT + " rectangles");
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (vboReady && vboId != 0) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                GLES20.glDisable(GLES20.GL_BLEND);
                GLES20.glUseProgram(program);
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

                final int stride = 7 * 4;
                GLES20.glEnableVertexAttribArray(aPosLoc);
                GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, 0);

                GLES20.glEnableVertexAttribArray(aColorLoc);
                GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, stride, 3 * 4);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, RECT_COUNT * 6);

                GLES20.glDisableVertexAttribArray(aPosLoc);
                GLES20.glDisableVertexAttribArray(aColorLoc);
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

                if (measuring) {
                    frameCount++;
                    long now = System.nanoTime();
                    if (lastFrameTimeNanos != 0 && frameCount > WARMUP_FRAMES) {
                        long delta = now - lastFrameTimeNanos;
                        double frameTimeMs = delta / 1_000_000.0;
                        frameTimes.add(frameTimeMs);
                    }
                    lastFrameTimeNanos = now;

                    Log.d(TAG, "Frame " + frameCount + "/" + (WARMUP_FRAMES + MEASURE_FRAMES));

                    if (frameCount >= WARMUP_FRAMES + MEASURE_FRAMES) {
                        finalizeBenchmark();
                    }
                }
            }

            // We intentionally do NOT draw overlay on the GL surface.
        }

        private void finalizeBenchmark() {
            measuring = false;

            if (frameTimes.isEmpty()) return;

            double frameSum = 0;
            for (double t : frameTimes) frameSum += t;
            double avgFrame = frameSum / frameTimes.size();

            Log.d(TAG, "OpenGL Result: " + String.format(Locale.US, "%.2fms", avgFrame));

            // Notify callback on main thread (if provided)
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(avgFrame));
            }

            // Launch StatsActivity on main thread with the full list
            double[] arr = new double[frameTimes.size()];
            for (int i = 0; i < frameTimes.size(); i++) arr[i] = frameTimes.get(i);

            mainHandler.post(() -> {
                try {
                    Intent it = new Intent(context, StatsActivity.class);
                    it.putExtra(StatsActivity.EXTRA_MODE, "OpenGL");
                    it.putExtra(StatsActivity.EXTRA_AVG, avgFrame);
                    it.putExtra(StatsActivity.EXTRA_FRAME_TIMES, arr);
                    if (!(context instanceof android.app.Activity)) {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(it);
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to launch StatsActivity", ex);
                }
            });
        }

        private int loadShader(int type, String code) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, code);
            GLES20.glCompileShader(shader);
            return shader;
        }

        private static final String VS =
                "attribute vec4 vPosition;\n" +
                        "attribute vec4 aColor;\n" +
                        "varying vec4 vColor;\n" +
                        "void main(){ gl_Position = vPosition; vColor = aColor; }\n";

        private static final String FS =
                "precision mediump float;\n" +
                        "varying vec4 vColor;\n" +
                        "void main(){ gl_FragColor = vColor; }\n";
    }
}