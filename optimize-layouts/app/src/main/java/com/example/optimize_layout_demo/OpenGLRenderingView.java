package com.example.optimize_layout_demo;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Trace;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Vẽ 20.000 hình chữ nhật (40px x 40px) bằng OpenGL ES 2.0:
 * - Gom toàn bộ vào một VBO
 * - Một draw call (TRIANGLES)
 * - glFinish() để chờ GPU vẽ xong trước khi đo thời gian
 * - Đo 1 frame đầu tiên, có nhãn Trace cho System Trace
 */
public class OpenGLRenderingView extends GLSurfaceView {

    private final RectanglesRenderer renderer;

    public OpenGLRenderingView(Context context) { this(context, null); }

    public OpenGLRenderingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        renderer = new RectanglesRenderer(context);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // gọi requestRender() đúng 1 lần
    }

    public RectanglesRenderer getRendererInstance() { return renderer; }

    public static class RectanglesRenderer implements GLSurfaceView.Renderer {

        private static final int RECT_COUNT = 20_000;
        private static final float RECT_PX = 40f;
        private static final float[] COLOR = {0.2f, 0.7f, 1.0f, 0.8f};

        private final Context context;
        private final Handler mainHandler;
        private final Random random = new Random(42L);

        private int program = 0;
        private int vboId = 0;
        private int aPosLoc;
        private int uColorLoc;
        private int surfaceW = 0, surfaceH = 0;

        private boolean hasMeasured = false;
        private double lastRenderMs = 0.0;

        public RectanglesRenderer(Context context) {
            this.context = context;
            this.mainHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);

            String vs = "attribute vec4 vPosition;\n" +
                    "void main(){ gl_Position = vPosition; }\n";
            String fs = "precision mediump float;\n" +
                    "uniform vec4 uColor;\n" +
                    "void main(){ gl_FragColor = uColor; }\n";

            int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
            int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
            program = GLES20.glCreateProgram();
            GLES20.glAttachShader(program, vShader);
            GLES20.glAttachShader(program, fShader);
            GLES20.glLinkProgram(program);

            aPosLoc = GLES20.glGetAttribLocation(program, "vPosition");
            uColorLoc = GLES20.glGetUniformLocation(program, "uColor");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
            surfaceW = width;
            surfaceH = height;
            buildVbo(); // tạo VBO khi đã biết kích thước màn hình
        }

        private void buildVbo() {
            if (surfaceW <= 0 || surfaceH <= 0) return;

            float sizeNdcX = (RECT_PX / surfaceW) * 2f;
            float sizeNdcY = (RECT_PX / surfaceH) * 2f;
            float maxX = 2f - sizeNdcX;
            float maxY = 2f - sizeNdcY;

            // Mỗi rect 2 tam giác = 6 đỉnh
            float[] vertices = new float[RECT_COUNT * 6 * 3];
            int idx = 0;
            for (int i = 0; i < RECT_COUNT; i++) {
                float x = -1f + random.nextFloat() * maxX;
                float y = -1f + random.nextFloat() * maxY;

                // Tri 1
                vertices[idx++] = x;             vertices[idx++] = y;              vertices[idx++] = 0f;
                vertices[idx++] = x + sizeNdcX;  vertices[idx++] = y;              vertices[idx++] = 0f;
                vertices[idx++] = x;             vertices[idx++] = y + sizeNdcY;   vertices[idx++] = 0f;
                // Tri 2
                vertices[idx++] = x + sizeNdcX;  vertices[idx++] = y;              vertices[idx++] = 0f;
                vertices[idx++] = x + sizeNdcX;  vertices[idx++] = y + sizeNdcY;   vertices[idx++] = 0f;
                vertices[idx++] = x;             vertices[idx++] = y + sizeNdcY;   vertices[idx++] = 0f;
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

            FloatBuffer vb = ByteBuffer.allocateDirect(vertices.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            vb.put(vertices).position(0);

            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * 4, vb, GLES20.GL_STATIC_DRAW);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            Log.d("OpenGLRenderer", "VBO built for 20k rects, verts=" + (RECT_COUNT * 6));
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (hasMeasured || vboId == 0) return;

            Trace.beginSection("OpenGL_Render_20000_Shapes");
            long start = System.nanoTime();

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(program);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
            GLES20.glEnableVertexAttribArray(aPosLoc);
            GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, 0);
            GLES20.glUniform4fv(uColorLoc, 1, COLOR, 0);

            int vertexCount = RECT_COUNT * 6;
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

            // Chờ GPU hoàn tất để thời gian đo là “đã vẽ xong”
            GLES20.glFinish();

            GLES20.glDisableVertexAttribArray(aPosLoc);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            long end = System.nanoTime();
            lastRenderMs = (end - start) / 1_000_000.0;
            hasMeasured = true;
            Trace.endSection();

            mainHandler.post(() -> {
                context.getSharedPreferences("results", Context.MODE_PRIVATE)
                        .edit().putFloat("opengl_render_time", (float) lastRenderMs).apply();
                Log.d("OpenGLTest", "OpenGL ES first-frame render (glFinish): " + lastRenderMs + " ms");
                Toast.makeText(context,
                        "OpenGL ES first frame: " + String.format("%.2f ms", lastRenderMs),
                        Toast.LENGTH_LONG).show();
            });
        }

        private int loadShader(int type, String code) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, code);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }
}