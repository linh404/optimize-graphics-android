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
 * - Dùng VBO interleaved (position + color)
 * - Một draw call (TRIANGLES)
 * - glFinish() để chờ GPU vẽ xong trước khi đo thời gian
 * - Mỗi rect có màu riêng (dùng màu ngẫu nhiên có seed cố định)
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

        private final Context context;
        private final Handler mainHandler;
        private final Random random = new Random(42L);

        private int program = 0;
        private int vboId = 0;
        private int aPosLoc;
        private int aColorLoc;
        private int surfaceW = 0, surfaceH = 0;

        private boolean hasMeasured = false;
        private double lastRenderMs = 0.0;

        // vertex shader nhận position + color attribute
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

        public RectanglesRenderer(Context context) {
            this.context = context;
            this.mainHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);

            int vShader = loadShader(GLES20.GL_VERTEX_SHADER, vs);
            int fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs);
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
            buildVbo(); // tạo VBO khi đã biết kích thước màn hình
        }

        private void buildVbo() {
            if (surfaceW <= 0 || surfaceH <= 0) return;

            float sizeNdcX = (RECT_PX / surfaceW) * 2f;
            float sizeNdcY = (RECT_PX / surfaceH) * 2f;
            float maxX = 2f - sizeNdcX;
            float maxY = 2f - sizeNdcY;

            // Mỗi rect 2 tam giác = 6 đỉnh
            // Interleaved: 3 pos + 4 color = 7 floats per vertex
            final int floatsPerVertex = 7;
            int vertexCount = RECT_COUNT * 6;
            float[] interleaved = new float[vertexCount * floatsPerVertex];
            int idx = 0;
            for (int i = 0; i < RECT_COUNT; i++) {
                float x = -1f + random.nextFloat() * maxX;
                float y = -1f + random.nextFloat() * maxY;

                // generate a color per-rect (HSV distribution) with alpha ~0.8
                float hue = random.nextFloat() * 360f;
                float sat = 0.6f + random.nextFloat() * 0.4f;
                float val = 0.6f + random.nextFloat() * 0.4f;
                // Convert HSV to RGB floats 0..1
                int colInt = android.graphics.Color.HSVToColor( (int)(0.8f*255), new float[]{hue, sat, val} );
                float a = ((colInt >> 24) & 0xFF) / 255f;
                float r = ((colInt >> 16) & 0xFF) / 255f;
                float g = ((colInt >> 8) & 0xFF) / 255f;
                float b = (colInt & 0xFF) / 255f;

                float x2 = x + sizeNdcX;
                float y2 = y + sizeNdcY;

                // Tri 1
                interleaved[idx++] = x;    interleaved[idx++] = y;    interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x2;   interleaved[idx++] = y;    interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                interleaved[idx++] = x;    interleaved[idx++] = y2;   interleaved[idx++] = 0f;
                interleaved[idx++] = r;    interleaved[idx++] = g;    interleaved[idx++] = b; interleaved[idx++] = a;

                // Tri 2
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

            Log.d("OpenGLRenderer", "VBO built for 20k rects, verts=" + vertexCount + ", floats=" + interleaved.length);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (hasMeasured || vboId == 0) return;

            Trace.beginSection("OpenGL_Render_20000_Shapes");
            long start = System.nanoTime();

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(program);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

            final int floatsPerVertex = 7;
            final int stride = floatsPerVertex * 4; // bytes

            // position attribute: 3 floats, offset 0
            GLES20.glEnableVertexAttribArray(aPosLoc);
            GLES20.glVertexAttribPointer(aPosLoc, 3, GLES20.GL_FLOAT, false, stride, 0);

            // color attribute: 4 floats, offset 3 * 4 bytes
            GLES20.glEnableVertexAttribArray(aColorLoc);
            GLES20.glVertexAttribPointer(aColorLoc, 4, GLES20.GL_FLOAT, false, stride, 3 * 4);

            int vertexCount = RECT_COUNT * 6;
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

            // Chờ GPU hoàn tất để thời gian đo là “đã vẽ xong”
            GLES20.glFinish();

            GLES20.glDisableVertexAttribArray(aPosLoc);
            GLES20.glDisableVertexAttribArray(aColorLoc);
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