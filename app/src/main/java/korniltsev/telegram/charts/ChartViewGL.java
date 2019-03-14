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

// draw path using VBO
// draw chart, learn to scale & translate
// animate
// scrollbar
public class ChartViewGL extends TextureView {
    public static final String LOG_TAG = "tg.ch.gl";

    public ChartViewGL(Context context, ColumnData c) {
        super(context);
        Render r = new Render(c);
        r.start();
        setSurfaceTextureListener(r);
    }

    public void setData(ChartData datum) {

    }


    class Render extends Thread implements TextureView.SurfaceTextureListener {


        private final ColumnData data;
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
        private GLChartProgram chart;


        public Render(ColumnData column) {
            this.data = column;

        }

        @Override
        public void run() {
            if (!waitForSurface()) {

                return;
            }
            initGL(surface);
            chart = new GLChartProgram(data, w, h);
            loop();

        }

        private boolean waitForSurface() {
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
                            return false;
                        }
                    }
                }
            }
            return true;
        }






        private void loop() {






            while (true) {


                glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);


                chart.draw();

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


//            final float ratio = (float) w / h;
//
//
//            final float left = -1.0f;
//            final float right = 1.0f;
//            final float bottom = -1.0f;
//            final float top = 1.0f;
//            final float near = 1.0f;
//            final float far = 10.0f;
//
//            Matrix.frustumM(projection, 0, left, right, bottom, top, near, far);
//
//            final float eyeX = 0.0f;
//            final float eyeY = 0.0f;
//            final float eyeZ = 1.5f;
//
//            final float lookX = 0.0f;
//            final float lookY = 0.0f;
//            final float lookZ = 0.0f;
//
//            final float upX = 0.0f;
//            final float upY = 1.0f;
//            final float upZ = 0.0f;
//
//            Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
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
