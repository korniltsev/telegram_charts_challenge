package korniltsev.telegram.charts.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

import korniltsev.telegram.charts.ChartData;
import korniltsev.telegram.charts.ChartView;
import korniltsev.telegram.charts.ColumnData;
import korniltsev.telegram.charts.Dimen;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;

/*
    + scrollbar overlay
    + scrollbar scroller
    + scrollbar charts with minvalue non zero + 2dip offset

    checkbox alpha animation for scroller & chart
    checkbox min max animation for scroller & chart

    scrollbar pointer response for scroller
    scrollbar pointer response for charts (scale + scroll)

    scrollbar clip (or draw white rect over, lol)
    https://blog.mapbox.com/drawing-antialiased-lines-with-opengl-8766f34192dc

*/
public class ChartViewGL extends TextureView {
    public static final String LOG_TAG = "tg.ch.gl";
    private final Dimen dimen;

    public final int dimen_v_padding8;
    public final int dimen_chart_height;
    public final int dimen_scrollbar_height;
    private final int h;
    private final Render r;

    public ChartViewGL(Context context, ColumnData[] c, Dimen dimen) {
        super(context);
        this.dimen = dimen;
        dimen_v_padding8 = dimen.dpi(8);
        dimen_chart_height = dimen.dpi(300);
        dimen_scrollbar_height = dimen.dpi(38);
        r = new Render(c);
        r.start();
        setSurfaceTextureListener(r);

        h = dimen_v_padding8
                + dimen_chart_height
                + dimen_v_padding8
                + dimen_v_padding8
                + dimen_scrollbar_height
                + dimen_v_padding8;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(w, h);

    }


    public void setChecked(String id, boolean isChecked) {
        //todo maybe need to save to ChartViewGl if we will need RenderRestart
        this.r.setChecked(id, isChecked);
    }


    class Render extends Thread implements TextureView.SurfaceTextureListener {


        private final ColumnData[] data;
        private EGL10 mEgl;
        private EGLDisplay mEglDisplay;
        private EGLConfig mEglConfig;
        private EGLContext mEglContext;
        private EGLSurface mEglSurface;

        SurfaceTexture surface;
        final Object lock = new Object();//todo fuck locking
        private int w;
        private int h;
        private GLChartProgram[] scrollbar;
        private GLChartProgram[] chart;
        private GLScrollbarOverlayProgram overlay;


        public Render(ColumnData[] column) {
            this.data = column;


        }

        @Override
        public void run() {
            if (!waitForSurface()) {

                return;
            }
            initGL(surface);
            scrollbar = new GLChartProgram[data.length - 1];
            long max = -1;
            long min = Long.MAX_VALUE;
            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                ColumnData datum = data[i];
                scrollbar[i - 1] = new GLChartProgram(data[i], w, h, dimen, ChartViewGL.this, true);
                max = Math.max(max, datum.maxValue);
                min = Math.min(min, datum.minValue);
            }
            for (GLChartProgram it : scrollbar) {
                it.maxValue = max;
                it.minValue = min;
            }

            chart = new GLChartProgram[data.length - 1];
            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                chart[i - 1] = new GLChartProgram(data[i], w, h, dimen, ChartViewGL.this, false);
            }
            for (GLChartProgram it : chart) {
                it.maxValue = max;
                it.minValue = 0;
            }

            overlay = new GLScrollbarOverlayProgram(w, h, dimen, ChartViewGL.this);
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

        public void setChecked(final String id, final boolean isChecked) {

            actionQueue.add(new Runnable() {
                @Override
                public void run() {
                    if (scrollbar != null) {
                        long max = -1;
                        long min = Long.MAX_VALUE;
                        for (GLChartProgram c : scrollbar) {
                            if (c.column.id.equals(id)) {
                                c.setChecked(isChecked);
                            }
                            if (c.checked) {
                                max = Math.max(c.column.maxValue, max);
                                min = Math.min(c.column.minValue, min);
                            }
                        }
                        for (GLChartProgram c : scrollbar) {
                            c.animateMinMax(min, max);
                        }
                    }
                    if (chart != null) {
                        for (GLChartProgram c : chart) {
                            if (c.column.id.equals(id)) {
                                c.setChecked(isChecked);
                            }
                        }
                    }
                }
            });
        }

        private final BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(100);


        private void loop() {

            while (true) {
                while (true) {
                    Runnable peek = actionQueue.poll();
                    if (peek == null) {
                        break;
                    } else {
                        peek.run();
                    }
                }

                glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
                long t = SystemClock.uptimeMillis();
                for (GLChartProgram c : scrollbar) {
                    c.draw(t);
                }
                for (GLChartProgram chartProgram : chart) {
                    chartProgram.draw(t);
                }
                overlay.draw(t);

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



//            GLES20.glEnable(GLES10.GL_MULTISAMPLE);
//            GLES20.glEnable(GLES10.GL_DITHER);
//            GLES20.glHint(GLES10.GL_LINE_SMOOTH_HINT, GLES10.GL_NICEST);
//            GLES20.glEnable(GLES10.GL_POINT_SMOOTH);
//            GLES20.glHint(GLES10.GL_POINT_SMOOTH_HINT, GLES10.GL_NICEST);
            MyGL.checkGlError2();

            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendEquation(GLES20.GL_FUNC_ADD);

//            GLES20.glEnable(GLES10.GL_LINE_SMOOTH);
//            GLES20.glHint(GLES10.GL_LINE_SMOOTH_HINT, GLES10.GL_NICEST);
//            GLES20.glHint(GLES10.GL_POINT_SMOOTH_HINT, GLES10.GL_NICEST);
//            MyGL.checkGlError2();
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

//                    If you want to do FSAA, you need to create an EGL context with multisampling enabled. Write an EGLConfigChooser that returns a multisampling config (specify 1 for EGL_SAMPLE_BUFFERS),
                    EGL10.EGL_SAMPLE_BUFFERS, 1,
                    EGL10.EGL_NONE
            };
        }


    }
}
