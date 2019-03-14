package korniltsev.telegram.charts;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

import korniltsev.telegram.charts.gl.MyGL;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;

// use vbo
// draw chart, learn to scale & translate
public class ChartViewGL extends TextureView {
    public static final String LOG_TAG = "tg.ch.gl";

    public ChartViewGL(Context context) {
        super(context);
        Render r = new Render();
        r.start();
        setSurfaceTextureListener(r);
    }

    public void setData(ChartData datum) {

    }


    class Render extends Thread implements TextureView.SurfaceTextureListener {

        private static final int BYTES_PER_FLOAT = 4;
        private static final int STRIDE_BYTES = 2 * BYTES_PER_FLOAT;
        private static final int POSITION_DATA_SIZE = 2;


        //        private final Chart[] data;
        private final FloatBuffer buf1;
        private EGL10 mEgl;
        private EGLDisplay mEglDisplay;
        private EGLConfig mEglConfig;
        private EGLContext mEglContext;
        private EGLSurface mEglSurface;
        private GL mGL;

        SurfaceTexture surface;
        final Object lock = new Object();//todo fuck locking
        private int w;
        private int h;

        private float[] model = new float[16];
        private float[] view = new float[16];
        private float[] projection = new float[16];

        private float[] MVP = new float[16];
        private int MVPHandle;
        private int positionHandle;
        private int vbo;

        public Render() {

            buf1 = ByteBuffer.allocateDirect(vertices.length * BYTES_PER_FLOAT)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            buf1.put(vertices);
            buf1.position(0);
        }

        @Override
        public void run() {
            SurfaceTexture surface = null;//todo destroying handling
            synchronized (lock) {
                while (true) {
                    surface = this.surface;
                    if (surface != null) {
                        break;
                    } else {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
            initGL(surface);
            loop();

        }







        final String vertexShader =
                          "uniform mat4 u_MVPMatrix;      \n"
                        + "attribute vec2 a_Position;     \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_Position = u_MVPMatrix * vec4(a_Position.x, a_Position.y, 0.0, 1.0);   \n"
                        + "}                              \n";

        final String fragmentShader =
                          "precision mediump float;       \n"
                        + "void main()                    \n"
                        + "{                              \n"
                        + "   gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0);     \n"
                        + "}                              \n";





        final float[] vertices = {
                -0.5f, -0.25f,
                0.5f, -0.25f,
                0.0f, 0.559016994f,
        };

        private void drawOneTriangle()
        {
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
            GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GLES20.GL_FLOAT, false , STRIDE_BYTES, 0);


            Matrix.multiplyMM(MVP, 0, view, 0, model, 0);
            Matrix.multiplyMM(MVP, 0, projection, 0, MVP, 0);

            GLES20.glUniformMatrix4fv(MVPHandle, 1, false, MVP, 0);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
        }

        private void loop() {
            int[] vbos = new int[1];
            GLES20.glGenBuffers(1, vbos, 0);
            MyGL.checkGlError2();
            vbo = vbos[0];
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
            MyGL.checkGlError2();
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * BYTES_PER_FLOAT, buf1, GLES20.GL_STATIC_DRAW);
            MyGL.checkGlError2();
            buf1.position(0);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            MyGL.checkGlError2();

            int program = MyGL.createProgram(vertexShader, fragmentShader);
            MVPHandle = GLES20.glGetUniformLocation(program, "u_MVPMatrix");
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position");
            MyGL.checkGlError2();



            while (true) {



                glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                glClear (GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);


                Matrix.setIdentityM(model, 0);
                GLES20.glUseProgram(program);

                MyGL.checkGlError2();


                drawOneTriangle();

                if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                    throw new RuntimeException("Cannot swap buffers");
                }
            }
        }



        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            synchronized (lock) {
                this.w = width;
                this.h = height;
                this.surface = surface;
                this.lock.notify();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

        private void initGL(SurfaceTexture surface) {
            mEgl = (EGL10) EGLContext.getEGL();

            mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed "
                        + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }

            int[] version = new int[2];
            if (!mEgl.eglInitialize(mEglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed " +
                        GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }

            mEglConfig = chooseEglConfig();
            if (mEglConfig == null) {
                throw new RuntimeException("eglConfig not initialized");
            }

            mEglContext = createContext(mEgl, mEglDisplay, mEglConfig);

            mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay,
                    mEglConfig, surface, null);

            if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
                int error = mEgl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e(LOG_TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                    return;
                }
                throw new RuntimeException("createWindowSurface failed "
                        + GLUtils.getEGLErrorString(error));
            }

            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface,
                    mEglContext)) {
                throw new RuntimeException("eglMakeCurrent failed "
                        + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }

            mGL = mEglContext.getGL();


            final float ratio = (float) w / h;


            final float left = -1.0f;
            final float right = 1.0f;
            final float bottom = -1.0f;
            final float top = 1.0f;
            final float near = 1.0f;
            final float far = 10.0f;

            Matrix.frustumM(projection, 0, left, right, bottom, top, near, far);

            final float eyeX = 0.0f;
            final float eyeY = 0.0f;
            final float eyeZ = 1.5f;

            final float lookX = 0.0f;
            final float lookY = 0.0f;
            final float lookZ = 0.0f;

            final float upX = 0.0f;
            final float upY = 1.0f;
            final float upZ = 0.0f;

            Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        }

        EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay,
                                 EGLConfig eglConfig) {
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL10.EGL_NONE};
            return egl.eglCreateContext(eglDisplay, eglConfig,
                    EGL10.EGL_NO_CONTEXT, attrib_list);
        }


        private EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            int[] configSpec = getConfig();
            if (!mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1,
                    configsCount)) {
                throw new IllegalArgumentException("eglChooseConfig failed" +
                        GLUtils.getEGLErrorString(mEgl.eglGetError()));
            } else if (configsCount[0] > 0) {
                return configs[0];
            }
            return null;
        }

        private int[] getConfig() {
            return new int[]{
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 0,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_NONE
            };
        }
    }
}
