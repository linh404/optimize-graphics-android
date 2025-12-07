package com.example.optimize_layout_demo;

import android.content. Context;
import android.opengl. GLES20;
import android. opengl.GLSurfaceView;
import android.opengl. Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os. Trace;
import android.util. Log;
import android.widget.Toast;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OpenGLRenderingView extends GLSurfaceView {

    private final RectanglesRenderer renderer;

    public OpenGLRenderingView(Context context) {
        super(context);
        Log.d("OpenGLView", "Constructor called");
        setEGLContextClientVersion(2);
        renderer = new RectanglesRenderer(context);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void requestRender() {
        Log. d("OpenGLView", "requestRender() called from: " +
                android.util.Log. getStackTraceString(new Throwable()));
        super.requestRender();
    }

    public static class RectanglesRenderer implements GLSurfaceView.Renderer {

        private Context context;
        private List<GLRect> rectangles;
        private int program = 0;
        private boolean hasMeasured = false;
        private float[] mvpMatrix = new float[16];
        private float[] projectionMatrix = new float[16];
        private float[] viewMatrix = new float[16];
        private Random random;
        private Handler mainHandler;
        private int renderCount = 0;  // Counter để track số lần render

        public static class GLRect {
            public FloatBuffer vertexBuffer;
            public float[] color;
            public float rotation;
            public float centerX;
            public float centerY;

            public GLRect(FloatBuffer vertexBuffer, float[] color, float rotation, float centerX, float centerY) {
                this.vertexBuffer = vertexBuffer;
                this.color = color;
                this.rotation = rotation;
                this.centerX = centerX;
                this. centerY = centerY;
            }
        }

        public RectanglesRenderer(Context context) {
            this.context = context;
            this.mainHandler = new Handler(Looper.getMainLooper());
            rectangles = new ArrayList<>();
            random = new Random();

            Log.d("OpenGLRenderer", "Creating 20,000 rectangles.. .");

            // Tạo 20,000 hình chữ nhật
            for (int i = 0; i < 20000; i++) {
                float x = random. nextFloat() * 2 - 1;
                float y = random.nextFloat() * 2 - 1;
                float width = 0.005f + random.nextFloat() * 0.03f;
                float height = 0.005f + random.nextFloat() * 0.03f;

                float[] vertices = {
                        x, y, 0f,
                        x + width, y, 0f,
                        x, y + height, 0f,
                        x + width, y + height, 0f
                };

                ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
                bb.order(ByteOrder.nativeOrder());
                FloatBuffer buffer = bb.asFloatBuffer();
                buffer.put(vertices);
                buffer.position(0);

                float[] color = {
                        random.nextFloat(),
                        random.nextFloat(),
                        random.nextFloat(),
                        0.6f + random.nextFloat() * 0.4f
                };

                rectangles.add(new GLRect(
                        buffer,
                        color,
                        random.nextFloat() * 360f,
                        x + width / 2,
                        y + height / 2
                ));
            }

            Log.d("OpenGLRenderer", "Rectangles created successfully");
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.d("OpenGLRenderer", "onSurfaceCreated");

            GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20. glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            String vertexShaderCode =
                    "uniform mat4 uMVPMatrix;\n" +
                            "attribute vec4 vPosition;\n" +
                            "void main() {\n" +
                            "    gl_Position = uMVPMatrix * vPosition;\n" +
                            "}\n";

            String fragmentShaderCode =
                    "precision mediump float;\n" +
                            "uniform vec4 vColor;\n" +
                            "void main() {\n" +
                            "    gl_FragColor = vColor;\n" +
                            "}\n";

            int vertexShader = loadShader(GLES20. GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            program = GLES20.glCreateProgram();
            GLES20. glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            Log.d("OpenGLRenderer", "Shaders compiled and linked");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d("OpenGLRenderer", "onSurfaceChanged: " + width + "x" + height);

            GLES20.glViewport(0, 0, width, height);
            float ratio = (float) width / height;
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f);
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            renderCount++;
            Log.d("OpenGLRenderer", "==========================================");
            Log.d("OpenGLRenderer", "onDrawFrame CALLED - Render count: " + renderCount);
            Log.d("OpenGLRenderer", "Time: " + System.currentTimeMillis());
            Log.d("OpenGLRenderer", "==========================================");

            // CHỈ RENDER 1 LẦN DUY NHẤT
            if (renderCount > 1) {
                Log.w("OpenGLRenderer", "⚠️ SKIPPING redundant render call #" + renderCount);
                return;
            }

            Trace.beginSection("OpenGL_Render_20000_Shapes");
            long startRender = !hasMeasured ? System.nanoTime() : 0L;

            Trace.beginSection("OpenGL_Clear");
            GLES20. glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(program);
            Trace.endSection();

            int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
            int colorHandle = GLES20. glGetUniformLocation(program, "vColor");
            int mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");

            Trace.beginSection("OpenGL_Draw_Rectangles");
            for (GLRect rect : rectangles) {
                float[] modelMatrix = new float[16];
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.rotateM(modelMatrix, 0, rect.rotation, 0f, 0f, 1f);

                float[] tempMatrix = new float[16];
                Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);

                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
                GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, rect.vertexBuffer);
                GLES20.glEnableVertexAttribArray(positionHandle);
                GLES20.glUniform4fv(colorHandle, 1, rect.color, 0);
                GLES20.glDrawArrays(GLES20. GL_TRIANGLE_STRIP, 0, 4);
            }
            GLES20.glDisableVertexAttribArray(positionHandle);
            Trace.endSection();

            if (! hasMeasured) {
                final double renderTime = (System.nanoTime() - startRender) / 1_000_000.0;

                Log.d("OpenGLRenderer", "✅ First render completed in " + renderTime + " ms");

                // Post lên UI thread để hiển thị Toast và lưu dữ liệu
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        context.getSharedPreferences("results", Context.MODE_PRIVATE)
                                .edit()
                                .putFloat("opengl_render_time", (float) renderTime)
                                .apply();
                        Log.d("OpenGLTest", "OpenGL ES render time: " + renderTime + " ms");

                        Toast.makeText(
                                context,
                                "OpenGL ES: " + String.format("%.2f ms", renderTime),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

                hasMeasured = true;
            }

            Trace.endSection();

            Log.d("OpenGLRenderer", "✅ Render #" + renderCount + " finished");
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20. glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            return shader;
        }
    }
}