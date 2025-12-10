package com.example.optimize_layout_demo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.view.PixelCopy;
import android.view.Window;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

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

    public RectanglesRenderer getRendererInstance() { return renderer; }

    public interface OnMeasureDone { void onDone(double ms, int pixelCopyResult); }

    public void measurePresented(Activity activity, OnMeasureDone cb) {
        if (activity == null || cb == null) return;
        if (getWidth() <= 0 || getHeight() <= 0) { cb.onDone(-1, -999); return; }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { cb.onDone(-2, -998); return; }

        final Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        final Window window = activity.getWindow();
        final Handler handler = new Handler(Looper.getMainLooper());
        final long startNanos = System.nanoTime();

        requestRender();

        PixelCopy.request(window, bmp, copyResult -> {
            long endNanos = System.nanoTime();
            double ms = (endNanos - startNanos) / 1_000_000.0;
            Log.d("PixelCopyMeasure", "OpenGL presented in " + ms + " ms, result=" + copyResult);
            cb.onDone(ms, copyResult);
        }, handler);
    }

    public static class RectanglesRenderer implements GLSurfaceView.Renderer {

        private static final int RECT_COUNT = 50_000;
        private static final float RECT_PX = 40f;
        private static final int RECT_ALPHA = 255;

        private static final boolean MEASURE_GPU_FINISH = false;
        private static final int WARMUP_FRAMES = 2;

        // Overlay constants (giống Canvas)
        private static final float OVERLAY_LEFT = 16f;
        private static final float OVERLAY_TOP = 16f;
        private static final float OVERLAY_WIDTH = 600f;
        private static final float OVERLAY_HEIGHT = 160f;

        private final Context context;
        private final Handler mainHandler;
        private final Random random = new Random(42L);
        private final GLSurfaceView glView;

        private int program = 0;
        private int vboId = 0;
        private int aPosLoc;
        private int aColorLoc;
        private int surfaceW = 0, surfaceH = 0;

        private boolean vboReady = false;
        private int frameCount = 0;
        private boolean firstMeasured = false;
        private double firstDrawMs = -1.0;

        // Overlay
        private int texProgram = 0;
        private int texId = 0;
        private int texPosLoc;
        private int texCoordLoc;
        private int texSamplerLoc;
        private FloatBuffer texVertexBuf;
        private FloatBuffer texCoordBuf;
        private boolean overlayReady = false;

        public RectanglesRenderer(Context context, GLSurfaceView glView) {
            this.context = context;
            this.glView = glView;
            this.mainHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glDisable(GLES20.GL_BLEND);

            int vShader = loadShader(GLES20.GL_VERTEX_SHADER, VS);
            int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FS);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vShader);
            GLES20.glAttachShader(program, fShader);
            GLES20.glLinkProgram(program);

            aPosLoc = GLES20.glGetAttribLocation(program, "vPosition");
            aColorLoc = GLES20.glGetAttribLocation(program, "aColor");

            // Overlay shader
            int tv = loadShader(GLES20.GL_VERTEX_SHADER, TEX_VS);
            int tf = loadShader(GLES20.GL_FRAGMENT_SHADER, TEX_FS);
            texProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(texProgram, tv);
            GLES20.glAttachShader(texProgram, tf);
            GLES20.glLinkProgram(texProgram);

            texPosLoc = GLES20.glGetAttribLocation(texProgram, "aPosition");
            texCoordLoc = GLES20.glGetAttribLocation(texProgram, "aTexCoord");
            texSamplerLoc = GLES20.glGetUniformLocation(texProgram, "uTexture");

            // Texture coordinates
            float[] uvs = {
                    0f, 0f,
                    1f, 0f,
                    0f, 1f,
                    1f, 1f
            };
            texCoordBuf = ByteBuffer.allocateDirect(uvs.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            texCoordBuf.put(uvs).position(0);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            surfaceW = width;
            surfaceH = height;
            buildVbo();
            updateOverlayQuad();
            vboReady = true;
            frameCount = 0;
            firstMeasured = false;
            overlayReady = false;
            glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }

        private void updateOverlayQuad() {
            if (surfaceW <= 0 || surfaceH <= 0) return;

            // Convert pixel coordinates to NDC
            float x1 = (OVERLAY_LEFT / surfaceW) * 2f - 1f;
            float y1 = 1f - (OVERLAY_TOP / surfaceH) * 2f;
            float x2 = ((OVERLAY_LEFT + OVERLAY_WIDTH) / surfaceW) * 2f - 1f;
            float y2 = 1f - ((OVERLAY_TOP + OVERLAY_HEIGHT) / surfaceH) * 2f;

            float[] verts = {
                    x1, y1, 0f,  // top-left
                    x2, y1, 0f,  // top-right
                    x1, y2, 0f,  // bottom-left
                    x2, y2, 0f   // bottom-right
            };

            texVertexBuf = ByteBuffer.allocateDirect(verts.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            texVertexBuf.put(verts).position(0);
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
                int colInt = android.graphics.Color.HSVToColor(RECT_ALPHA, new float[]{hue, sat, val});

                float a = ((colInt >> 24) & 0xFF) / 255f;
                float r = ((colInt >> 16) & 0xFF) / 255f;
                float g = ((colInt >> 8) & 0xFF) / 255f;
                float b = (colInt & 0xFF) / 255f;

                float x2 = x + sizeNdcX;
                float y2 = y + sizeNdcY;

                // Triangle 1
                interleaved[idx++] = x;    interleaved[idx++] = y;    interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x2;   interleaved[idx++] = y;    interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x;    interleaved[idx++] = y2;   interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                // Triangle 2
                interleaved[idx++] = x2;   interleaved[idx++] = y;    interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x2;   interleaved[idx++] = y2;   interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x;    interleaved[idx++] = y2;   interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;
            }

            if (vboId != 0) {
                int[] old = {vboId};
                GLES20.glDeleteBuffers(1, old, 0);
                vboId = 0;
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
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (!vboReady || vboId == 0) return;

            Trace.beginSection("OpenGL_Render_50000_Shapes");
            long startCpu = System.nanoTime();

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(program);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

            final int floatsPerVertex = 7;
            final int stride = floatsPerVertex * 4;

            GLES20.glEnableVertexAttribArray(aPosLoc);
            GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, 0);

            GLES20.glEnableVertexAttribArray(aColorLoc);
            GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, stride, 3 * 4);

            int vertexCount = RECT_COUNT * 6;
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

            long afterSubmit = System.nanoTime();

            double gpuFinishMs = -1;
            if (MEASURE_GPU_FINISH) {
                GLES20.glFinish();
                gpuFinishMs = (System.nanoTime() - startCpu) / 1_000_000.0;
            }

            GLES20.glDisableVertexAttribArray(aPosLoc);
            GLES20.glDisableVertexAttribArray(aColorLoc);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            double cpuSubmitMs = (afterSubmit - startCpu) / 1_000_000.0;

            frameCount++;
            if (frameCount <= WARMUP_FRAMES) {
                Trace.endSection();
                return;
            }

            if (!firstMeasured) {
                firstMeasured = true;
                firstDrawMs = MEASURE_GPU_FINISH ? gpuFinishMs : cpuSubmitMs;

                mainHandler.post(() -> {
                    context.getSharedPreferences("results", Context.MODE_PRIVATE)
                            .edit().putFloat("opengl_render_time", (float) firstDrawMs).apply();
                    Log.d("OpenGLTest", "OpenGL ES first-frame (50k), " +
                            (MEASURE_GPU_FINISH ? "CPU+GPU finish" : "CPU submit") +
                            ": " + firstDrawMs + " ms");
                    Toast.makeText(context,
                            "OpenGL ES (50k): " + String.format("%.2f ms", firstDrawMs),
                            Toast.LENGTH_LONG).show();

                    glView.queueEvent(() -> {
                        initOverlayTexture(String.format("First frame: %.2f ms", firstDrawMs));
                        glView.requestRender();
                        glView.post(() -> glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY));
                    });
                });
            }

            // Draw overlay
            if (overlayReady) {
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

                GLES20.glUseProgram(texProgram);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
                GLES20.glUniform1i(texSamplerLoc, 0);

                GLES20.glEnableVertexAttribArray(texPosLoc);
                GLES20.glVertexAttribPointer(texPosLoc, 3, GLES20.GL_FLOAT, false, 0, texVertexBuf);

                GLES20.glEnableVertexAttribArray(texCoordLoc);
                GLES20.glVertexAttribPointer(texCoordLoc, 2, GLES20.GL_FLOAT, false, 0, texCoordBuf);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                GLES20.glDisableVertexAttribArray(texPosLoc);
                GLES20.glDisableVertexAttribArray(texCoordLoc);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

                GLES20.glDisable(GLES20.GL_BLEND);
            }

            Trace.endSection();
        }

        private void initOverlayTexture(String text) {
            // Giống y hệt Canvas: 600x160, text size 42f, padding 16px
            Bitmap bmp = Bitmap.createBitmap((int)OVERLAY_WIDTH, (int)OVERLAY_HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(Color.TRANSPARENT);

            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.WHITE);
            p.setTextSize(42f);
            p.setTypeface(android.graphics.Typeface.DEFAULT);

            // Text positions (relative to bitmap, giống Canvas)
            c.drawText("OpenGL ES (50k rect)", 16f, 60f, p);
            c.drawText(text, 16f, 120f, p);

            int[] tex = new int[1];
            GLES20.glGenTextures(1, tex, 0);
            texId = tex[0];

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            bmp.recycle();
            overlayReady = true;
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
                        "void main(){\n" +
                        "  gl_Position = vPosition;\n" +
                        "  vColor = aColor;\n" +
                        "}\n";

        private static final String FS =
                "precision mediump float;\n" +
                        "varying vec4 vColor;\n" +
                        "void main(){ gl_FragColor = vColor; }\n";

        private static final String TEX_VS =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTexCoord;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main(){ gl_Position = aPosition; vTexCoord = aTexCoord; }\n";

        private static final String TEX_FS =
                "precision mediump float;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main(){ gl_FragColor = texture2D(uTexture, vTexCoord); }\n";
    }
}