package korniltsev.telegram.charts.gl;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import korniltsev.telegram.charts.MainActivity;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;

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

    + checkbox alpha animation for scroller & chart
    + checkbox min max animation for scroller & chart




    + scrollbar pointer response for scroller
    + scrollbar pointer response for charts (scale + scroll)

    + animation bug on dataset 0



    + rules
    + rules vertical labels
    + actionbar + night mode animation
    ---------------------------------------- 19 march

    + rules vertical animation
    + stop drawing when nothing changes and draw only animation / changes

    + chart pointer response for max animation

    + do not animate first show
    + animation double tap bug (jump)


    + scrollbar night mode,
    + toolbar shadow
    + format digits on rulers




    ---------------------------------------- 20 march
    + animate statusbar day night
    + [ ! ] round joining + alpha animation

    + Circles
        + wrong touch calc
        + draw over lines
        + seems oval


    ---------------------------------------- 21 march

    [ ! ] toooltip by touching + fix touch
        - render tooltip as view to canvas
        - upload as texture
        - draw texture

    [ ! ] implement empty chart
    [ ! ] horizontal lables + animations
    [ ! ] alpha animation blending - render to fbo


    MUST HAVE
    -------------
    NICE TO HAVE

    look for aliased line

    [ ! ] tweak inital zoom for smal datasets
    [ ! ] rulers text night mode + animation
    [ ! ] move "folorwers" to gl +   text night mode + animation
    [ ! ] checkbox + animations + divider width

    [?] tooltip alpha nimation
    [?] optimize minmax animation, introduce step?, do not cancel animations or try to continue?
    [?] why DIRTY slow?
    [?] replace actionQueue.add with something better?
    [?] try to play with thread priority?
    [?] enabled blending only for overlay
    [?] reuse shaders between objects for faster start?
    [?] don't draw if vertes is far behind the screen

    [ ? ] Color-> MyColor, Matrix?

    [ * ] move left-right alot, fps goes down, do not animate-out rulers who already animating-out
    [ * ] scrollbar animation bug when last value is zero
    [ * ] optimize ruler rendering


    ACHTUNG
    chart line dithering
    https://blog.mapbox.com/drawing-antialiased-lines-with-opengl-8766f34192dc
    calculating normal in vertex shader
    https://github.com/learnopengles/Learn-OpenGLES-Tutorials/blob/641fcc25158dc30f45a7b2faaab165ec61ebb54b/android/AndroidOpenGLESLessonsCpp/app/src/main/assets/vertex/per_pixel_vertex_shader_tex_and_light.glsl#L22



----------------------------------------------------------------------
----------------------------------------------------------------------
----------------------------------------------------------------------
// todo

//      boolean dirtyCheck = true;/
//     implement y=0 y=x chart
//     scrolling/caling allocations & perf
//     initial zoom for small charts is terrible
//    mb scale linewidth when drawing 365 points



// todo design
//    check colors & paddings with collor picker
//    add 1dp padding to the scrollbar charts
//    compare label fonts with design

// todo nice to have
//     checkbox animations
//      https://github.com/facebook/redex
//      try to iptimize bytecode, check for accessor methods etc


//todo testing
//     requestLayaout during drag
//     requestLayaout during animation?
//     test on old device                      <<<<<<
//     monkey test   + screenshots
//     fuzz test     + screenshots
//     nagative values




*/
public class ChartViewGL extends TextureView {
    private final Dimen dimen;

    public final int dimen_v_padding8;
    public final int dimen_chart_height;
    public final int dimen_scrollbar_height;
    private final int h;
    private final Render r;
    private final int initial_scroller_dith;
    private final int resize_touch_area2;
    private final int touchSlop;
    private final int rulerColor;
    private final ColorSet init_colors;
    private final ColumnData[] data;
    public int bgColor;
    public MyAnimation.Color bgAnim = null;
    private int chartBottom;
    private int chartTop;
    private int hpadding;
//    private long currentMax;
//    private ColorSet currentColorsSet;

    public ChartViewGL(Context context, ColumnData[] c, Dimen dimen, ColorSet currentColorsSet) {
        super(context);
        this.init_colors = currentColorsSet;
        currentColors = currentColorsSet;
        this.dimen = dimen;
        this.data = c;
        dimen_v_padding8 = dimen.dpi(8);
        dimen_chart_height = dimen.dpi(300);
        dimen_scrollbar_height = dimen.dpi(38);
        r = new Render(c);
        this.bgColor = currentColorsSet.lightBackground;
        this.rulerColor = currentColorsSet.ruler;
        r.start();
        setSurfaceTextureListener(r);

        hpadding = dimen.dpi(16);
        h = dimen_v_padding8
                + dimen_chart_height
                + dimen_v_padding8
                + dimen_v_padding8
                + dimen_scrollbar_height
                + dimen_v_padding8;

        initial_scroller_dith = dimen.dpi(86);
        resize_touch_area2 = dimen.dpi(20);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
//
//        int scrollbar_top = chart_bottom + this.scroll_bar_v_padding;

        scrollbar.left = hpadding;
        scrollbar.right = w - hpadding;
        scrollbar.bottom = h - dimen_v_padding8;
        scrollbar.top = scrollbar.bottom - dimen_scrollbar_height;

        setMeasuredDimension(w, h);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (scroller_left == -1) {
            scroller__right = scrollbar.right;
            scroller_left = scrollbar.right - initial_scroller_dith;
            setOverlayPos();
        }

        chartBottom =  bottom - dimen.dpi(80);
        chartTop =  chartBottom - dimen.dpi(280);
//        chartTop = dimen.dpi(80);
    }

    public void setChecked(String id, boolean isChecked) {
        //todo maybe need to save to ChartViewGl if we will need RenderRestart
        this.r.setChecked(id, isChecked);
    }


    class Render extends Thread implements TextureView.SurfaceTextureListener {


        private final float[] PROJ = new float[16];
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
        private GLRulersProgram ruler;
        private long prevMax;
        private Tooltip tooltip;


        public Render(ColumnData[] column) {
            this.data = column;


        }

        @Override
        public void run() {
            if (!waitForSurface()) {

                return;
            }
//            File filesDir = getContext().getFilesDir();
//            File trace = new File(filesDir, "trace");
//            if (MainActivity.TRACE) {
//                Debug.startMethodTracing(trace.getAbsolutePath(), 1024  * 1024 * 10);
//            }

            initGL(surface);
            initPrograms();
            loop();

        }

        private void initPrograms() {
            scrollbar = new GLChartProgram[data.length - 1];
            long max = -1;
            long min = Long.MAX_VALUE;
            GLChartProgram.Shader chartShader = new GLChartProgram.Shader();
            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                ColumnData datum = data[i];
                scrollbar[i - 1] = new GLChartProgram(data[i], w, h, dimen, ChartViewGL.this, true, init_colors.lightBackground, chartShader);
                max = Math.max(max, datum.maxValue);
                min = Math.min(min, datum.minValue);
            }
            for (GLChartProgram it : scrollbar) {
                it.maxValue = max;
                it.minValue = min;
            }

            chart = new GLChartProgram[data.length - 1];
            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                chart[i - 1] = new GLChartProgram(data[i], w, h, dimen, ChartViewGL.this, false, init_colors.lightBackground, chartShader);
            }
//            for (GLChartProgram it : chart) {
//                it.maxValue = max;
//                it.minValue = 0;
//            }
//            prevMax = max;

            overlay = new GLScrollbarOverlayProgram(w, h, dimen, ChartViewGL.this, init_colors.scrollbarBorder, init_colors.scrollbarOverlay);
            ruler = new GLRulersProgram(w, h, dimen, ChartViewGL.this, rulerColor);


            Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);

//            float scalex = 2.0f / w;
//            float scaley = 2.0f / h;
//            Matrix.setIdentityM(pxMat, 0);
//            Matrix.translateM(pxMat, 0, -1.0f, -1.0f, 0);
//            Matrix.scaleM(pxMat, 0, scalex, scaley, 1.0f);
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
                    if (scrollbar == null || chart == null) {
                        return;
                    }
                    // scrollbar
                    GLChartProgram found = null;
                    long max = -1;
                    long min = Long.MAX_VALUE;
                    int checkedCount = 0;
                    for (GLChartProgram c : scrollbar) {
                        if (c.column.id.equals(id)) {
                            found = c;
                            c.animateAlpha(isChecked);
                        }
                        if (c.checked) {
                            checkedCount++;
                            max = Math.max(c.column.maxValue, max);
                            min = Math.min(c.column.minValue, min);
                        }
                    }
                    for (GLChartProgram c : scrollbar) {
                        if (found == c && !isChecked) {
                        } else {
                            c.animateMinMax(min, max, true);
                        }
                    }

                    for (GLChartProgram c : chart) {
                        if (c.column.id.equals(id)) {
                            c.animateAlpha(isChecked);
                        }
                    }

                    long scaledMax = calculateMax(r.overlay.left, r.overlay.right);
                    //todo
                    float ratio = prevMax / (float) scaledMax;
//                    if (MainActivity.LOGGING) Log.d(MainActivity.TAG, "anim ratio " + ratio);

                    // chart
                    for (GLChartProgram c : chart) {
                        if (checkedCount != 0) {
                            c.animateMinMax(0, scaledMax, true);
                        }
                    }
                    if (prevMax != scaledMax) {
                        ruler.animateScale(ratio, scaledMax);
                        prevMax = scaledMax;
                    }

                }
            });
        }

        private final BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(100);


        private void loop() {
            List<MyRect> debugRects = new ArrayList<>();
            debugRects.add(new MyRect(w, h, 0, 0, Color.RED, w, h));
            debugRects.add(new MyRect(w, dimen_v_padding8 * 2 + dimen_scrollbar_height, 0, 0, Color.GREEN, w, h));
            debugRects.add(new MyRect(w, dimen.dpi(280), 0, dimen.dpi(80), Color.BLUE, w, h));
            boolean invalidated = true;

//            MyCircle circle = new MyCircle(dimen,  w, h);

            long frameCount = 0;
            long prevReportTime = SystemClock.uptimeMillis();
            int ccc = 0;
            boolean rulerInitDone = false;
            out:
            while (true) {
                ccc++;
                int cnt = 0;
                while (true) {

                    Runnable peek = actionQueue.poll();
                    if (peek == null) {
                        if (invalidated || cnt != 0) {
                            break;
                        } else {
                            if (MainActivity.DIRTY_CHECK) {
                                continue;
                            } else {
                                break;
                            }
                        }
                    } else {
                        cnt++;
                        peek.run();
                    }
                }
                if (!rulerInitDone) {
                    prevMax = calculateMax(r.overlay.left, r.overlay.right);
                    ruler.init(prevMax);
                    for (GLChartProgram glChartProgram : chart) {
                        glChartProgram.maxValue = prevMax;
                    }
                    rulerInitDone = true;
                }
                invalidated = false;

                long t = SystemClock.uptimeMillis();

                if (bgAnim != null) {
                    bgColor = bgAnim.tick(t);
                    if (bgAnim.ended) {
                        bgAnim = null;
                    } else {
                        invalidated = true;
                    }
                }
                glClearColor(
                        Color.red(bgColor) / 255f,
                        Color.green(bgColor) / 255f,
                        Color.blue(bgColor) / 255f,
                        1.0f
                );
                glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
//                for (MyRect r : debugRects) {
//                    r.draw();
//                }
                long t2 = SystemClock.uptimeMillis();

                invalidated = drawScrollbar(invalidated, t);
                long t3 = SystemClock.uptimeMillis();
                overlay.draw(t);
                long t4 = SystemClock.uptimeMillis();

                ruler.draw(t);
                long t5 = SystemClock.uptimeMillis();
//                circle.draw();
                invalidated = drawChart(invalidated, t);
                long t6 = SystemClock.uptimeMillis();

                if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                    throw new RuntimeException("Cannot swap buffers");
                }
                if (MainActivity.LOGGING) {

                    long t7 = SystemClock.uptimeMillis();
//                frameCount++;
                    long timeSinceLastReport = t - prevReportTime;
                    if (MainActivity.LOG_FPS && timeSinceLastReport > 1000) {
                        float fps = (float) frameCount * 1000 / timeSinceLastReport;
                        Log.d(MainActivity.TAG, "fps " + fps);
                        prevReportTime = t;
                        frameCount = 0;
                        log_trace("swap", t7, t6);
                        log_trace("chart", t6, t5);
                        log_trace("ruler", t5, t4);
                        log_trace("overlay", t4, t3);
                        log_trace("scrollbar", t3, t2);
                        log_trace("f1", t2, t);
                    } else {
                        frameCount++;
                    }
                }
//                prevReportTime;
//                break;
//                if (ccc == 1) {
//                    Debug.stopMethodTracing();
//                }
            }
        }

        private boolean drawScrollbar(boolean invalidated, long t) {
            for (GLChartProgram chartProgram : scrollbar) {
                boolean it_invalid = chartProgram.animateionTick(t);
                invalidated = invalidated || it_invalid;
            }
            for (GLChartProgram chartProgram : scrollbar) {
                chartProgram.step1(PROJ);
            }
            MyGL.checkGlError2();
            for (GLChartProgram chartProgram : scrollbar) {
                chartProgram.shader.use();//todo use only once!
                chartProgram.step2();

                chartProgram.lineJoining.shader.use();
                chartProgram.step3();
            }
            MyGL.checkGlError2();
            return invalidated;
        }

        private boolean drawChart(boolean invalidated, long t) {
            int tooltipIndex = chart[0].getTooltipIndex();
            if (tooltipIndex != -1 ) {
                if (this.tooltip == null) {
                    this.tooltip = new Tooltip(new Tooltip.Shader(), dimen, w, currentColors);
                }
            }
            for (GLChartProgram chartProgram : chart) {
                boolean it_invalid = chartProgram.animateionTick(t);
                invalidated = invalidated || it_invalid;
            }

            for (GLChartProgram chartProgram : chart) {
                chartProgram.step1(PROJ);
            }
            if (tooltipIndex != -1) {
                this.tooltip.animationTick(t);
                this.tooltip.draw(PROJ, chart[0].MVP, tooltipIndex);
            }


            MyGL.checkGlError2();
            for (GLChartProgram chartProgram : chart) {
                chartProgram.shader.use();//todo use only once!
                chartProgram.step2();

                chartProgram.lineJoining.shader.use();
                chartProgram.step3();
            }
            MyGL.checkGlError2();
            if (chart[0].goodCircle != null) {
                chart[0].goodCircle.shader.use();
                for (GLChartProgram chartProgram : chart) {
                    chartProgram.step4();
                }
            }
            MyGL.checkGlError2();
            return invalidated;
        }

        private void log_trace(String name, long t5, long t4) {
            if (MainActivity.LOGGING) Log.d(MainActivity.TAG, "trace " + name + " " + (t5 - t4));
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
                    Log.e(MainActivity.TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
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

            // with sample buffers
            boolean b = mEgl.eglChooseConfig(mEglDisplay, getConfig(true), configs, 1, configsCount);
            if (b) {
                return configs[0];
            } else {
                String eglErrorString = GLUtils.getEGLErrorString(mEgl.eglGetError());
//                throw new IllegalArgumentException("eglChooseConfig failed" +
//                        eglErrorString);
                if (MainActivity.LOGGING) {
                    Log.e(MainActivity.TAG, "err" + eglErrorString);
                }
            }
            // without sample buffers
            b = mEgl.eglChooseConfig(mEglDisplay, getConfig(false), configs, 1, configsCount);
            if (b) {
                return configs[0];
            } else {
                String eglErrorString = GLUtils.getEGLErrorString(mEgl.eglGetError());
                throw new IllegalArgumentException("eglChooseConfig failed" +
                        eglErrorString);
//                if (LOGGING) {
//                    Log.e(LOG_TAG, "err" + eglErrorString);
//                }

            }
//            return null;
        }

        private int[] getConfig(boolean sampleBuffers) {
            if (sampleBuffers) {

                return new int[]{
                        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                        EGL10.EGL_RED_SIZE, 8,
                        EGL10.EGL_GREEN_SIZE, 8,
                        EGL10.EGL_BLUE_SIZE, 8,
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_DEPTH_SIZE, 0,
                        EGL10.EGL_STENCIL_SIZE, 0,

                        EGL10.EGL_SAMPLE_BUFFERS, 1,
//                    EGL10.EGL_SAMPLES, 2,

                        EGL10.EGL_NONE
                };
            } else {
                return new int[]{
                        EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                        EGL10.EGL_RED_SIZE, 8,
                        EGL10.EGL_GREEN_SIZE, 8,
                        EGL10.EGL_BLUE_SIZE, 8,
                        EGL10.EGL_ALPHA_SIZE, 8,
                        EGL10.EGL_DEPTH_SIZE, 0,
                        EGL10.EGL_STENCIL_SIZE, 0,

//                        EGL10.EGL_SAMPLE_BUFFERS, 1,
//                    EGL10.EGL_SAMPLES, 2,
//                    EGL10.EGL_SAMPLES, 2,

                        EGL10.EGL_NONE
                };
            }
        }

        boolean firstLeftRightUpdate = true;
        public void updateLeftRight(float left, float right, float scale) {
            overlay.setLeftRight(left, right);
//                long t = SystemClock.elapsedRealtimeNanos();
//            long newMax = calculateMax(left, right);
            long scaledMax = calculateMax(left, right);

            for (GLChartProgram glChartProgram : r.chart) {
                glChartProgram.zoom = scale;
                glChartProgram.left = left;
                if (prevMax != scaledMax) {
                    glChartProgram.animateMinMax(0, scaledMax, !firstLeftRightUpdate);
                }
            }
            firstLeftRightUpdate = false;


            //todo
            float ratio = prevMax / (float) scaledMax;
//            if (MainActivity.LOGGING) Log.d(MainActivity.TAG, "anim ratio " + ratio);

            // chart
//            for (GLChartProgram c : chart) {
//                if (c.column.id.equals(id)) {
//                    c.animateAlpha(isChecked);
//                }
//                if (checkedCount != 0) {
//
//                }
//            }
            if (prevMax != scaledMax) {
                ruler.animateScale(ratio, scaledMax);
                prevMax = scaledMax;
            }
        }
    }

    //    private int scroller_width;//todo replace with scroller_left/right
//    private int scroller_pos = -1;
    private Rect scrollbar = new Rect();
    //    private int scroller_move_down_x;
    static final int DOWN_MOVE = 0;
    static final int DOWN_RESIZE_LEFT = 1;
    static final int DOWN_RESIZE_RIGHT = 2;
    float last_x = -1f;
//    int resze_scroller_right = -1;
    int down_target = -1;
    boolean dragging;


    private int scroller__right = -1;
    private int scroller_left = -1;
    //    private Rect scrollbar = new Rect();
    private int scroller_move_down_x;
    private int scroller_move_down_width;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
//        if (action != MotionEvent.ACTION_MOVE) {
//            if (LOGGING) Log.d("tg.chart", "touchevent " + event);
//        }
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                boolean scrollbar = y >= this.scrollbar.top && y <= this.scrollbar.bottom;
                if (scrollbar) {
                    if (Math.abs(x - scroller_left) <= resize_touch_area2) {
                        if (MainActivity.LOGGING) Log.d("tg.chart", "touchevent DOWN resize left");
                        last_x = x;
                        down_target = DOWN_RESIZE_LEFT;
//                        resze_scroller_right = scroller_left + scroller_width;
                        return true;
                    } else if (Math.abs(x - (scroller__right)) < resize_touch_area2) {
                        if (MainActivity.LOGGING) Log.d("tg.chart", "touchevent DOWN resize right");
                        last_x = x;
                        down_target = DOWN_RESIZE_RIGHT;
                        return true;
                    } else if (x >= scroller_left && x <= scroller__right) {
                        if (MainActivity.LOGGING) Log.d("tg.chart", "touchevent DOWN inside scrollbar");
                        last_x = x;
                        scroller_move_down_x = (int) (x - scroller_left);
                        scroller_move_down_width = scroller__right - scroller_left;
                        down_target = DOWN_MOVE;
                        return true;
                    } else {
                        if (MainActivity.LOGGING) Log.d("tg.chart", "touchevent DOWN miss");
                    }
                } else {
                    boolean chart = y >= this.chartTop && y <= this.chartBottom;
                    if (chart) {
                        dispatchTouchDownChart(x);
                    } else {
                        if (MainActivity.LOGGING) Log.d("tg.chart", "touchevent DOWN miss");
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //todo check pointer index
                if (last_x != -1f) {
                    if (dragging) {
                        last_x = x;
                        if (down_target == DOWN_MOVE) {

                            scroller_left = (int) (x - scroller_move_down_x);
                            scroller__right = scroller_left + scroller_move_down_width;
                            if (scroller_left < this.scrollbar.left) {
                                scroller_left = this.scrollbar.left;
                                scroller__right = this.scrollbar.left + scroller_move_down_width;
                            }
                            if (scroller__right > this.scrollbar.right) {
                                scroller__right = this.scrollbar.right;
                                scroller_left = this.scrollbar.right - scroller_move_down_width;
                            }
                            setOverlayPos();
//                            invalidate();
//                            int scroller_width = scroller__right - scroller_left;
//                            scroller_left = Math.min(Math.max(scroller_left, scrollbar.left), scrollbar.right - scroller_width);
                        } else if (down_target == DOWN_RESIZE_RIGHT) {
                            scroller__right = (int) x;
                            if (scroller__right > this.scrollbar.right) {
                                scroller__right = this.scrollbar.right;
                            }
                            // check the scrollbar is not too small
                            if (scroller__right - scroller_left < initial_scroller_dith) {
                                scroller__right = scroller_left + initial_scroller_dith;
                            }
                            setOverlayPos();
//                            invalidate();
                        } else if (down_target == DOWN_RESIZE_LEFT) {
                            scroller_left = (int) x;
                            if (scroller_left < this.scrollbar.left) {
                                scroller_left = this.scrollbar.left;
                            }
//                            scroller_width = resze_scroller_right - scroller_left;
                            if (scroller__right - scroller_left < initial_scroller_dith) {
//                                scroller_left = initial_scroller_dith;
                                scroller_left = scroller__right - initial_scroller_dith;
                            }
                            setOverlayPos();
//                            invalidate();
                        }

//                        invalidate();
                        return true;
                    } else {
                        float move = x - last_x;
                        if (Math.abs(move) > touchSlop) {
                            dragging = true;
                            last_x = x;
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                last_x = -1;
                dragging = false;
                break;
            default:
                break;
        }
        return false;
    }

    private void dispatchTouchDownChart(float x) {
        if (x < scrollbar.left || x > scrollbar.right) {
            if (MainActivity.LOGGING) Log.d(MainActivity.TAG, "chart down miss");
        } else {
            float swindow = (x - scrollbar.left) / scrollbar.width();
             float sdataset = r.overlay.left + swindow * (r.overlay.right - r.overlay.left);
            int n = r.data[0].values.length;
            int i = (int) (n * sdataset);
            if (i < 0) {
                i = 0;
            }
            if (i >= n) {
                i = n-1;
            }
            final int finali = i;
            r.actionQueue.add(new Runnable() {
                @Override
                public void run() {
                    for (GLChartProgram glChartProgram : r.chart) {
                        glChartProgram.setTooltipIndex(finali);
                    }
                }
            });

            if (MainActivity.LOGGING) Log.d(MainActivity.TAG, "chart touch down");
        }
    }

//    private static final BlockingQueue<MyMotionEvent> motionEvents = new ArrayBlockingQueue<MyMotionEvent>(100);

    public final void setOverlayPos() {
        final float left = (float) (scroller_left - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float right = (float) (scroller__right - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float scale = (right - left);
//        motionEvents.poll()
        r.actionQueue.add(new Runnable() {//todo do not allocate
            @Override
            public void run() {

                r.updateLeftRight(left, right, scale);

                        for (GLChartProgram glChartProgram : r.chart) {
                            glChartProgram.setTooltipIndex(-1);
                        }

            }
        });
    }

    public final long calculateMax(float left, float right) {
        long max = -1;
        int len = r.chart[0].column.values.length;
        int from = Math.max(0, (int)Math.ceil(len * (left-0.02f)));
        int to = Math.min(len, (int)Math.ceil(len * (right+0.02f)));
        for (GLChartProgram glChartProgram : r.chart) {
            if (glChartProgram.checked) {
                long[] values = glChartProgram.column.values;
                for (int i = from; i < to; i++) {
                    max = (max >= values[i]) ? max : values[i];
                }
            }
        }
        return max;
    }

    private ColorSet currentColors;

    public void animateToColors(final ColorSet colors) {
        r.actionQueue.add(new Runnable() {


            @Override
            public void run() {
                currentColors = colors;
                bgAnim = new MyAnimation.Color(MyAnimation.ANIM_DRATION, bgColor, colors.lightBackground);
                r.ruler.animate(colors.ruler);
                r.overlay.animate(colors.scrollbarBorder, colors.scrollbarOverlay);
                for (GLChartProgram glChartProgram : r.chart) {
                    glChartProgram.animateColors(colors);
                }
                if (r.tooltip != null) {
                    r.tooltip.animateTo(colors);
                }
            }
        });
    }

//    class MyMotionEvent implements Runnable {
//        float left;
//        float scale;
//        float right;
//
//        @Override
//        public void run() {
//            r.overlay.setLeftRight(left, right);
//            for (GLChartProgram glChartProgram : r.chart) {
//                glChartProgram.zoom = scale;
//                glChartProgram.left = left;
//            }
//            motionEvents.offer(this);
//        }
//    }
}
