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
import android.view.Window;
import android.widget.Toast;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OpenGLRenderingView extends GLSurfaceView {

    private final RectanglesRenderer renderer;

    public OpenGLRenderingView(Context context) {
        this(context, null);
    }

    public OpenGLRenderingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new RectanglesRenderer(context, this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public RectanglesRenderer getRendererInstance() {
        return renderer;
    }

    public interface OnMeasureDone {
        void onDone(double ms, int pixelCopyResult);
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

        requestRender();

        PixelCopy.request(window, bmp, copyResult -> {
            long endNanos = System.nanoTime();
            double ms = (endNanos - startNanos) / 1_000_000.0;
            Log.d("PixelCopyMeasure", "OpenGL presented in " + ms + " ms, result=" + copyResult);
            cb.onDone(ms, copyResult);
        }, handler);
    }

    public static class RectanglesRenderer implements GLSurfaceView.Renderer {

        private static final int RECT_COUNT = 20_000;
        private static final float RECT_PX = 40f;

        private final Context context;
        private final Handler mainHandler;
        private final Random random = new Random(42L);
        private final GLSurfaceView glView;

        private int program = 0;
        private int vboId = 0;
        private int aPosLoc;
        private int aColorLoc;
        private int surfaceW = 0, surfaceH = 0;

        private boolean firstDrawMeasured = false;
        private double firstDrawMs = -1.0;

        // Text overlay
        private int textureProgram = 0;
        private int textureId = 0;
        private int texturePosLoc;
        private int textureTexCoordLoc;
        private int textureSamplerLoc;
        private FloatBuffer textureVertexBuffer;
        private FloatBuffer textureTexCoordBuffer;

        private final String vs =
                "attribute vec4 vPosition;\n" +
                        "attribute vec4 aColor;\n" +
                        "varying vec4 vColor;\n" +
                        "void main(){\n" +
                        "  gl_Position = vPosition;\n" +
                        "  vColor = aColor;\n" +
                        "}\n";

        private final String fs =
                "precision mediump float;\n" +
                        "varying vec4 vColor;\n" +
                        "void main(){ gl_FragColor = vColor; }\n";

        private final String textureVs =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTexCoord;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main(){\n" +
                        "  gl_Position = aPosition;\n" +
                        "  vTexCoord = aTexCoord;\n" +
                        "}\n";

        private final String textureFs =
                "precision mediump float;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "void main(){ gl_FragColor = texture2D(uTexture, vTexCoord); }\n";

        public RectanglesRenderer(Context context, GLSurfaceView glView) {
            this.context = context;
            this.glView = glView;
            this.mainHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            // Rect shader
            int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
            int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vShader);
            GLES20.glAttachShader(program, fShader);
            GLES20.glLinkProgram(program);

            aPosLoc = GLES20.glGetAttribLocation(program, "vPosition");
            aColorLoc = GLES20.glGetAttribLocation(program, "aColor");

            // Texture shader for text overlay
            int texVShader = loadShader(GLES20.GL_VERTEX_SHADER, textureVs);
            int texFShader = loadShader(GLES20.GL_FRAGMENT_SHADER, textureFs);
            textureProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(textureProgram, texVShader);
            GLES20.glAttachShader(textureProgram, texFShader);
            GLES20.glLinkProgram(textureProgram);

            texturePosLoc = GLES20.glGetAttribLocation(textureProgram, "aPosition");
            textureTexCoordLoc = GLES20.glGetAttribLocation(textureProgram, "aTexCoord");
            textureSamplerLoc = GLES20.glGetUniformLocation(textureProgram, "uTexture");

            // Text overlay quad (top-left corner)
            // Text overlay quad (top-left corner) - trong onSurfaceCreated
            float[] vertices = {
                    -1.0f, 1.0f, 0.0f,
                    -0.1f, 1.0f, 0.0f,
                    -1.0f, 0.6f, 0.0f,
                    -0.1f, 0.6f, 0.0f    
            };

            float[] texCoords = {
                    0.0f, 0.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f
            };

            textureVertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            textureVertexBuffer.put(vertices).position(0);

            textureTexCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            textureTexCoordBuffer.put(texCoords).position(0);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            surfaceW = width;
            surfaceH = height;
            buildVbo();
            glView.requestRender();
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
                int colInt = android.graphics.Color.HSVToColor((int) (0.8f * 255),
                        new float[]{hue, sat, val});
                float a = ((colInt >> 24) & 0xFF) / 255f;
                float r = ((colInt >> 16) & 0xFF) / 255f;
                float g = ((colInt >> 8) & 0xFF) / 255f;
                float b = (colInt & 0xFF) / 255f;

                float x2 = x + sizeNdcX;
                float y2 = y + sizeNdcY;

                interleaved[idx++] = x;    interleaved[idx++] = y;    interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x2;   interleaved[idx++] = y;    interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x;    interleaved[idx++] = y2;   interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

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

            Log.d("OpenGLRenderer", "VBO built for 20k rects");
        }

        private void updateTextTexture(String text) {
            // Tăng kích thước bitmap để text rõ nét hơn
            Bitmap textBitmap = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(textBitmap);
            canvas.drawColor(Color.TRANSPARENT);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setTextSize(80f);  // Tăng từ 56f lên 80f
            paint.setTypeface(android.graphics.Typeface.DEFAULT);  // Dùng font mặc định giống Canvas
            canvas.drawText("OpenGL ES (20k rect)", 60f, 140f, paint);  // Điều chỉnh vị trí
            canvas.drawText(text, 60f, 240f, paint);

            if (textureId == 0) {
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);
                textureId = textures[0];
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textBitmap, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            textBitmap.recycle();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (vboId == 0 && surfaceW > 0 && surfaceH > 0) {
                buildVbo();
            }

            Trace.beginSection("OpenGL_Render_20000_Shapes");
            long start = System.nanoTime();

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

            GLES20.glFinish();

            GLES20.glDisableVertexAttribArray(aPosLoc);
            GLES20.glDisableVertexAttribArray(aColorLoc);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            long end = System.nanoTime();
            double drawMs = (end - start) / 1_000_000.0;

            if (!firstDrawMeasured) {
                firstDrawMeasured = true;
                firstDrawMs = drawMs;

                mainHandler.post(() -> {
                    context.getSharedPreferences("results", Context.MODE_PRIVATE)
                            .edit().putFloat("opengl_render_time", (float) firstDrawMs).apply();
                    Log.d("OpenGLTest", "OpenGL ES first-frame render: " + firstDrawMs + " ms");
                    Toast.makeText(context,
                            "OpenGL ES first frame: " + String.format("%.2f ms", firstDrawMs),
                            Toast.LENGTH_LONG).show();
                });

                glView.post(() -> glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY));
            }

            // Draw text overlay
            double displayMs = (firstDrawMs >= 0) ? firstDrawMs : drawMs;
            updateTextTexture(String.format("First frame: %.2f ms", displayMs));

            GLES20.glUseProgram(textureProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(textureSamplerLoc, 0);

            GLES20.glEnableVertexAttribArray(texturePosLoc);
            GLES20.glVertexAttribPointer(texturePosLoc, 3, GLES20.GL_FLOAT, false, 0, textureVertexBuffer);

            GLES20.glEnableVertexAttribArray(textureTexCoordLoc);
            GLES20.glVertexAttribPointer(textureTexCoordLoc, 2, GLES20.GL_FLOAT, false, 0, textureTexCoordBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glDisableVertexAttribArray(texturePosLoc);
            GLES20.glDisableVertexAttribArray(textureTexCoordLoc);

            Trace.endSection();
        }

        private int loadShader(int type, String code) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, code);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }
}