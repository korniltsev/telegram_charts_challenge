package korniltsev.telegram.charts.gl;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.ViewConfiguration;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import korniltsev.telegram.charts.MainActivity;
import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColor;

import static android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION;
import static android.opengl.EGL14.EGL_OPENGL_ES2_BIT;
import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;
import static korniltsev.telegram.charts.MainActivity.LOGGING;
import static korniltsev.telegram.charts.MainActivity.TAG;

/*



    - 2. A line chart with 2 lines and 2 Y axes (Screenshot 3).
        - fix bug with wrong ruler position
    - 3. A stacked bar chart with 7 data types (Screenshots 5-6).
    - 4. A daily bar chart with single data type (Screenshot 7).
    - 5. A percentage stacked area chart with 6 data types (Screenshots 9, 10).

    - A long tap on any data filter should uncheck all other filters.

    [ ? ] Matrix -> MyMatrix for inlining? test if it does something


    разобраться что с цветами, 0xff222222 не рисуется
    4.1 крашится на выходе


    сократить цифры на лейблах
    fix touch (laggy, hard to select)
    tooltip animation
    alpha animation blending - render to fbo
    [?] optimize minmax animation, introduce step?, do not cancel animations or try to continue?
    - don't draw x lablel if the label is offscreen - lazy textures are required





//todo testing
//     requestLayaout during drag
//     requestLayaout during animation?
//     test on old device                      <<<<<<
//     monkey test   + screenshots


after contest replace GL_LINE with triangles & dithering
ACHTUNG
    chart line dithering
    https://blog.mapbox.com/drawing-antialiased-lines-with-opengl-8766f34192dc
    calculating normal in vertex shader
    https://github.com/learnopengles/Learn-OpenGLES-Tutorials/blob/641fcc25158dc30f45a7b2faaab165ec61ebb54b/android/AndroidOpenGLESLessonsCpp/app/src/main/assets/vertex/per_pixel_vertex_shader_tex_and_light.glsl#L22
    render to msa fbo https://stackoverflow.com/a/8338881/1321940



*/
public class ChartViewGL extends TextureView {
    public static final int CHECKBOX_HEIGHT_DPI = 50;
    public static final int CHECKBOX_DIVIDER_HIEIGHT = 1;
    public static final int CHART_BOTTOM_DIP = 80;//relative to checkboxes
    public final Dimen dimen;

    public final int dimen_v_padding8;
    public final int dimen_chart_height;
    public final int dimen_scrollbar_height;
    public final int h;
    public final Render r;
    public final int initial_scroller_dith;
    public final int resize_touch_area2;
    public final int touchSlop;
//    public final int rulerColor;
    public final ColorSet init_colors;
    public final ChartData data;
    public final ColumnData xColumn;
    public final int legend_height;
    public final int checkboxesHeight;
    //    public final long initTime;
    public int bgColor;
    public MyAnimation.Color bgAnim = null;
    public int chartBottom;
    public int chartTop;
    public int hpadding;
    private long viewportMin;
    private long viewportMax;
    //    public long currentMax;
//    public ColorSet currentColorsSet;


    @Override
    public boolean isOpaque() {
        return true;
    }

    public ChartViewGL(Context context, ChartData c, Dimen dimen, ColorSet currentColorsSet) {
        super(context);
        xColumn = c.data[0];
//        initTime = SystemClock.elapsedRealtimeNanos();
        this.init_colors = currentColorsSet;
        currentColors = currentColorsSet;
        this.dimen = dimen;
        this.data = c;
        legend_height = dimen.dpi(46);
        dimen_v_padding8 = dimen.dpi(8);
        dimen_chart_height = dimen.dpi(300);
        dimen_scrollbar_height = dimen.dpi(38);

        r = new Render(c);
        this.bgColor = currentColorsSet.lightBackground;
//        this.rulerColor = currentColorsSet.ruler;
//        r.start();
        setSurfaceTextureListener(r);

        int checkboxesCount = c.data.length - 1;
        int dividersCount = checkboxesCount - 1;
        checkboxesHeight = checkboxesCount * dimen.dpi(CHECKBOX_HEIGHT_DPI) + dividersCount * dimen.dpi(CHECKBOX_DIVIDER_HIEIGHT);

        hpadding = dimen.dpi(16);
        h = dimen_v_padding8
                + legend_height
                + dimen_chart_height
                + dimen_v_padding8
                + dimen_v_padding8
                + dimen_scrollbar_height
                + dimen_v_padding8 + checkboxesHeight;

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
        scrollbar.bottom = h - dimen_v_padding8 - checkboxesHeight;
        scrollbar.top = scrollbar.bottom - dimen_scrollbar_height;

        setMeasuredDimension(w, h);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (scroller_left == -1) {
            scroller__right = scrollbar.right;
            scroller_left = scrollbar.right - initial_scroller_dith;
            setOverlayPos(true);
        }

        chartBottom =  bottom - dimen.dpi(CHART_BOTTOM_DIP)-checkboxesHeight;
        chartTop =  chartBottom - dimen.dpi(280);
//        chartTop = dimen.dpi(80);
    }

    public void setChecked(String id, boolean isChecked) {
        //todo maybe need to save to ChartViewGl if we will need RenderRestart
        this.r.setChecked(id, isChecked);
    }

    public void invalidateRender() {
        r.postToRender(new Runnable() {
            @Override
            public void run() {
                r.invalidateRender();
            }
        });

    }

    public static int counter;
    class Render extends HandlerThread implements TextureView.SurfaceTextureListener {

        public int id = ++counter;
        public final float[] PROJ = new float[16];
        public final ChartData data;
        public volatile float initleft;
        public volatile float initRight;
        public volatile float initSacle;
        public EGL10 mEgl;
        public EGLDisplay mEglDisplay;
        public EGLConfig mEglConfig;
        public EGLContext mEglContext;
        public EGLSurface mEglSurface;

        SurfaceTexture surface;
        final Object lock = new Object();//todo fuck locking
        public int w;
        public int h;

        public long prevMax;
        public long prevMin;

        public boolean rulerInitDone;
        public boolean[] checked;


        Handler renderHandler2;

        public Tooltip tooltip;
        public GLChartProgram[] scrollbar;
        public GLChartProgram[] chart;
        public GLScrollbarOverlayProgram overlay;
        public GLRulersProgram ruler;

        public SimpleShader simple;
//        public GLChartProgram.Shader chartShader;
        public MyCircles.Shader joiningShader;
        private ArrayList<MyRect> debugRects;

        public Render(ChartData column) {
            super("ChartViewGLRender", Process.getThreadPriority(Process.myTid()));
            this.data = column;
            start();
            Looper l = getLooper();

            rendererInvalidated = true;
            renderHandler2 = new Handler(l);
            renderHandler2.post(new Init());
//            invalidate();
//
            renderHandler2.post(drawFrame_);

//            renderHandler.post(drawFrame);
        }

        public void postToRender(final Runnable target) {
            renderHandler2.post(new Runnable() {
                @Override
                public void run() {
                    if (released) {
                        return;
                    }
                    target.run();
                }
            });
        }

        public void release() {
            Runnable release = new Runnable() {
                @Override
                public void run() {
                    if (released) {
                        return;
                    }
                    released = true;
                    try {
                        if (tooltip != null) {
                            tooltip.rlease();
                        }
                    } catch (Throwable e) {
                        if (LOGGING) Log.e(TAG, "release err", e);
                    }
                    for (GLChartProgram c : chart) {
                        try {
                            c.release();
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                    }
                    for (GLChartProgram c : scrollbar) {
                        try {
                            c.release();
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                    }
                    try {
                        overlay.release();
                    } catch (Throwable e) {
                        if (LOGGING) Log.e(TAG, "release err", e);
                    }
                    try {
                        ruler.release();
                    } catch (Throwable e) {
                        if (LOGGING) Log.e(TAG, "release err", e);

                    }

                    try {
                        simple.release();
                    } catch (Throwable e) {
                        if (LOGGING) Log.e(TAG, "release err", e);

                    }
                    try {
                        joiningShader.release();
                    } catch (Throwable e) {
                        if (LOGGING) Log.e(TAG, "release err", e);
                    }
//                    try {
//                        chartShader.release();
//                    } catch (Throwable e) {
//                        if (LOGGING) Log.e(TAG, "release err", e);
//                    }

                    try {
                        mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
                        mEgl.eglDestroyContext(mEglDisplay, mEglContext);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    surface.release();

                    quit();
                }
            };
            renderHandler2.post(release);
        }

        class Init implements Runnable {

            @Override
            public void run() {
                int threadPriority = Process.getThreadPriority(Process.myTid());
//            long t1= SystemClock.elapsedRealtimeNanos();
                if (!waitForSurface()) {

                    return;
                }
//            File filesDir = getContext().getFilesDir();
//            File trace = new File(filesDir, "trace");
//            if (MainActivity.TRACE) {
//                Debug.startMethodTracing(trace.getAbsolutePath(), 1024  * 1024 * 10);
//            }
//            long t2 = SystemClock.elapsedRealtimeNanos();

                initGL(surface);
//            long t3 = SystemClock.elapsedRealtimeNanos();
                initPrograms();
                setLeftRightImpl(initleft, initRight, initSacle);
//            long t5 = SystemClock.elapsedRealtimeNanos();
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("init time1  %10d", t2 - t1));
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("init time2  %10d", t3 - t2));
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("init time3  %10d", t5 - t3));
            }
        }

//        class Loop implements Runnable {//todo tmp, remove
//
//            @Override
//            public void run() {
//                loop();
//            }
//        }

//        @Override
//        public void run() {
//
//
//
//        }
        public boolean released = false;
        public void initPrograms() {
//            chartShader = new GLChartProgram.Shader();
            joiningShader = new MyCircles.Shader(6);
            simple = new SimpleShader();
//            long t2 = SystemClock.elapsedRealtimeNanos();



            ColumnData[] data = this.data.data;
            scrollbar = new GLChartProgram[data.length - 1];
            long max = -1;
            long min = Long.MAX_VALUE;


            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                ColumnData datum = data[i];
                scrollbar[i - 1] = new GLChartProgram(data[i], w, h, dimen, ChartViewGL.this, true, init_colors.lightBackground, simple, joiningShader);
                max = Math.max(max, datum.max);
                min = Math.min(min, datum.min);
            }
            for (GLChartProgram it : scrollbar) {
                it.maxValue = max;
                it.minValue = min;
            }
//            long scrollbar = SystemClock.elapsedRealtimeNanos();

            chart = new GLChartProgram[data.length - 1];
            checked = new boolean[data.length - 1];
            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                chart[i - 1] = new GLChartProgram(data[i], w, h, dimen, ChartViewGL.this, false, init_colors.lightBackground, simple, joiningShader);
                checked[i - 1] = true;
            }
//            long chart = SystemClock.elapsedRealtimeNanos();


            overlay = new GLScrollbarOverlayProgram(w, h, dimen, ChartViewGL.this, init_colors.scrollbarBorder, init_colors.scrollbarOverlay, simple);
//            long overlay = SystemClock.elapsedRealtimeNanos();
            ruler = new GLRulersProgram(w, h, dimen, ChartViewGL.this, init_colors, simple, xColumn);
//            long ruler = SystemClock.elapsedRealtimeNanos();


            Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);

//            long tlast = SystemClock.elapsedRealtimeNanos();
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("-----------------------"));
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("%20s   %10d","shader init", t2 - t1));
////            if (LOGGING) Log.d(MainActivity.TAG, String.format("program init  %10d", tlast- t2));
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("%20s   %10d"  ,"scrollbar init", scrollbar- t2));
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("%20s   %10d","chart init", chart- scrollbar));
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("%20s   %10d", "overlay init", overlay- chart));
//            if (LOGGING) Log.d(MainActivity.TAG, String.format("%20s   %10d", "ruler init", ruler- overlay));

            debugRects = new ArrayList<>();
//            debugRects.add(new MyRect(w, chartBottom-chartTop, 0, h-chartBottom, Color.BLUE, w, h));
        }


        public boolean waitForSurface() {
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

            Runnable setCheckedOnRenderThread = new Runnable() {
                @Override
                public void run() {
                    if (scrollbar == null || chart == null) {
                        return;
                    }
                    int prevCheckedCOunt = 0;
                    for (GLChartProgram glChartProgram : chart) {
                        if (glChartProgram.checked) {
                            prevCheckedCOunt++;
                        }
                    }
                    // scrollbar
                    GLChartProgram found = null;
                    long max = -1;
                    long min = Long.MAX_VALUE;
                    int checkedCount = 0;

                    for (int i = 0; i < scrollbar.length; i++) {
                        GLChartProgram c = scrollbar[i];
                        if (c.column.id.equals(id)) {
                            checked[i] = isChecked;
                            found = c;
                            c.animateAlpha(isChecked);
                        }
                        if (c.checked) {
                            checkedCount++;
                            max = Math.max(c.column.max, max);
                            min = Math.min(c.column.min, min);
                        }
                    }
                    for (GLChartProgram c : scrollbar) {
                        if (found == c && !isChecked) {
                        } else {
                            c.animateMinMax(min, max, true, 208);
                        }
                    }

                    for (GLChartProgram c : chart) {
                        c.setTooltipIndex(-1);

                        if (c.column.id.equals(id)) {
                            c.animateAlpha(isChecked);
                        }
                    }

                    calculateMax(r.overlay.left, r.overlay.right);
                    //todo
                    float ratio = prevMax / (float) viewportMax;
//                    if (MainActivity.LOGGING) Log.d(MainActivity.TAG, "anim ratio " + ratio);

                    // chart
                    for (GLChartProgram c : chart) {
                        if (checkedCount != 0) {
                            c.animateMinMax(viewportMin, viewportMax, true, 208);
                        }
                    }
                    if (prevMax != viewportMax || prevMin != viewportMin) {
                        ruler.animateScale(ratio,viewportMin, viewportMax, checkedCount, prevCheckedCOunt, 208);
                        prevMax = viewportMax;
                        prevMin = viewportMin;
                    }
//                    drawAndSwap();
                    invalidateRender();
                }
            };
            postToRender(setCheckedOnRenderThread);
        }

//        public final BlockingQueue<Runnable> actionQueue = new ArrayBlockingQueue<Runnable>(100);
        DrawFrame drawFrame_ = new DrawFrame();
        class DrawFrame implements Runnable {

            @Override
            public void run() {
//                long t1 = System.nanoTime();
                drawAndSwap2();
//                long t2 = System.nanoTime();
//                Log.d(MainActivity.TAG, String.format("trace [ %d ] %20s %10d ", id, "draw frame", t2 - t1));
            }
        }

        public void drawAndSwap2() {
            if (released) {
                return;
            }
            long t = SystemClock.uptimeMillis();
            boolean invalidated = false;
            if (!rulerInitDone) {
                calculateMax(r.overlay.left, r.overlay.right);
                prevMax = viewportMax;
                prevMin = viewportMin;
                ruler.init(viewportMin, prevMax);
                for (GLChartProgram glChartProgram : chart) {
                    glChartProgram.animateMinMax(viewportMin, viewportMax, false, 0);
                }
                rulerInitDone = true;
            }

            if (bgAnim != null) {
                bgColor = bgAnim.tick(t);
                if (bgAnim.ended) {
                    bgAnim = null;
                } else {
                    invalidated = true;
                }
            }
//            long t1 = System.nanoTime();
            glClearColor(
                    MyColor.red(bgColor) / 255f,
                    MyColor.green(bgColor) / 255f,
                    MyColor.blue(bgColor) / 255f,
                    1.0f
            );
            glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
//                for (MyRect r : debugRects) {
//                    r.draw();
//                }
//            long t2 = System.nanoTime();

            invalidated = drawScrollbar(invalidated, t);
//                long t3 = System.nanoTime();
            overlay.draw(t);
//                long t4 = System.nanoTime();
            boolean rulerInvalidated = ruler.animationTick(t);
            invalidated = rulerInvalidated | invalidated;
            GLES20.glScissor(0,h-chartBottom- dimen.dpi(1), w, chartBottom-chartTop);
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            MyGL.checkGlError2();
            ruler.draw(t);
//                long t5 = System.nanoTime();
//                circle.draw();
            invalidated = drawChart(invalidated, t);
//                long t6 = System.nanoTime();
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            MyGL.checkGlError2();

            if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                throw new RuntimeException("Cannot swap buffers");
            }
//            long t7 = System.nanoTime();
//
//            Log.d(MainActivity.TAG, String.format("      [ %d ] -> %20s %10d ", id, "2", t2 - t1));
//            Log.d(MainActivity.TAG, String.format("      [ %d ] -> %20s %10d ", id, "3", t3 - t2));
//            Log.d(MainActivity.TAG, String.format("      [ %d ] -> %20s %10d ", id, "4", t4 - t3));
//            Log.d(MainActivity.TAG, String.format("      [ %d ] -> %20s %10d ", id, "5", t5 - t4));
//            Log.d(MainActivity.TAG, String.format("      [ %d ] -> %20s %10d ", id, "6", t6 - t5));
//            Log.d(MainActivity.TAG, String.format("      [ %d ] -> %20s %10d ", id, "7", t7 - t6));
            rendererInvalidated = false;
//            invalidated = true;
            if (invalidated) {
                invalidateRender();
            }
        }

        boolean rendererInvalidated = false;
        public final void invalidateRender() {
            if (rendererInvalidated) {
                return;
            }
            rendererInvalidated = true;
            //assert render thread
            Render r = this;
            if (Thread.currentThread() != r) {
                throw new AssertionError();
            }
            renderHandler2.post(drawFrame_);
        }

        public boolean drawScrollbar(boolean invalidated, long t) {
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

        public boolean drawChart(boolean invalidated, long t) {
            int tooltipIndex = chart[0].getTooltipIndex();
            if (tooltipIndex != -1 ) {
                if (this.tooltip == null) {
                    this.tooltip = new Tooltip(dimen, w, h, currentColors, data, simple, ChartViewGL.this);
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
                this.tooltip.animationTick(t, tooltipIndex, checked);
                this.tooltip.drawVLine(PROJ, chart[0].MVP, tooltipIndex);
            }

            MyGL.checkGlError2();
            for (GLChartProgram chartProgram : chart) {
                chartProgram.shader.use();//todo use only once!
                chartProgram.step2();

                chartProgram.lineJoining.shader.use();
                chartProgram.step3();
            }
            MyGL.checkGlError2();
            for (GLChartProgram c : chart) {
                if (c.goodCircle != null) {
//                    c.goodCircle.shader.use();
//
                    c.step4();
                }
            }
            MyGL.checkGlError2();


            if (tooltipIndex != -1) {
                this.tooltip.drawTooltip(PROJ);
            }
            return invalidated;
        }

        public void log_trace(String name, long t5, long t4) {
            if (LOGGING) Log.d(MainActivity.TAG, String.format("trace %20s %10d", name, t5 - t4));
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
            //todo
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

        public void initGL(SurfaceTexture surface) {
//            this.surface = surface;
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


        public EGLConfig chooseEglConfig() {
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
                if (LOGGING) {
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

        public int[] getConfig(boolean sampleBuffers) {
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
            calculateMax(left, right);

            int checkedCount = 0;
            for (GLChartProgram glChartProgram : r.chart) {
                glChartProgram.zoom = scale;
                glChartProgram.left = left;
                if (glChartProgram.checked) {
                    checkedCount++;
                }
                if (prevMax != viewportMax || prevMin != viewportMin) {
                    glChartProgram.animateMinMax(viewportMin, viewportMax, !firstLeftRightUpdate, 256);
                }
            }
            ruler.setLeftRight(left, right, scale);
            firstLeftRightUpdate = false;


            //todo
            float ratio = prevMax / (float) viewportMax;
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
            if (prevMax != viewportMax || prevMin != viewportMin) {
                if (rulerInitDone) {
                    ruler.animateScale(ratio, viewportMin, viewportMax, checkedCount,checkedCount, 256);
                }
                prevMax = viewportMax;
                prevMin = viewportMin;
            }
        }
    }

    //    public int scroller_width;//todo replace with scroller_left/rightd
//    public int scroller_pos = -1;
    public Rect scrollbar = new Rect();
    //    public int scroller_move_down_x;
    static final int DOWN_MOVE = 0;
    static final int DOWN_RESIZE_LEFT = 1;
    static final int DOWN_RESIZE_RIGHT = 2;
    static final int DOWN_TOOLTIP = 3;
    float last_x = -1f;
//    int resze_scroller_right = -1;
    int down_target = -1;
    boolean dragging;


    public int scroller__right = -1;
    public int scroller_left = -1;
    //    public Rect scrollbar = new Rect();
    public int scroller_move_down_x;
    public int scroller_move_down_width;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
//        if (LOGGING) Log.d("tg.chart", "ScrollBug touchevent " + event);
//        if (action != MotionEvent.ACTION_MOVE) {
//        }
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                boolean scrollbar = y >= this.scrollbar.top && y <= this.scrollbar.bottom;
                if (scrollbar) {
                    if (Math.abs(x - scroller_left) <= resize_touch_area2) {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN resize left");
                        last_x = x;
                        down_target = DOWN_RESIZE_LEFT;
//                        resze_scroller_right = scroller_left + scroller_width;
                        return true;
                    } else if (Math.abs(x - (scroller__right)) < resize_touch_area2) {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN resize right");
                        last_x = x;
                        down_target = DOWN_RESIZE_RIGHT;
                        return true;
                    } else if (x >= scroller_left && x <= scroller__right) {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN inside scrollbar");
                        last_x = x;
                        scroller_move_down_x = (int) (x - scroller_left);
                        scroller_move_down_width = scroller__right - scroller_left;
                        down_target = DOWN_MOVE;
                        return true;
                    } else {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN miss");
                    }
                } else {
                    boolean chart = y >= this.chartTop && y <= this.chartBottom;
                    if (chart) {
                        last_x = x;
                        down_target = DOWN_TOOLTIP;
                        return true;
//                        dispatchTouchDownChart(x);
                    } else {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN miss");
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
                            setOverlayPos(false);
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
                            setOverlayPos(false);
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
                            setOverlayPos(false);
//                            invalidate();
                        }

//                        invalidate();
                        return true;
                    } else {
                        float move = x - last_x;
                        if (Math.abs(move) > touchSlop) {
//                            disall
//                            Log.d("ScrollBug", "request disasllow ");
                            getParent().getParent().requestDisallowInterceptTouchEvent(true);
                            dragging = true;
                            last_x = x;
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (action == MotionEvent.ACTION_UP && down_target == DOWN_TOOLTIP) {
                    if (Math.abs(last_x - x) < touchSlop) {
                        dispatchTouchDownChart(x);
                    }
                }
                if (LOGGING) Log.d("tg.chart", "dragging = false " );
                last_x = -1;
                dragging = false;
                break;
            default:
                break;
        }
        return false;
    }

    public void dispatchTouchDownChart(float x) {
        if (x < scrollbar.left || x > scrollbar.right) {
            if (LOGGING) Log.d(MainActivity.TAG, "chart down miss");
        } else {
            float swindow = (x - scrollbar.left) / scrollbar.width();
            if (swindow > 0.97) {
                swindow = 1f;
            }else  if (swindow < 0.03) {
                swindow = 0;
            }
            final float finalswindow = swindow;
            Runnable dispatchTouchdown = new Runnable() {
                @Override
                public void run() {
                    float sdataset = r.overlay.left + finalswindow * (r.overlay.right - r.overlay.left);
                    int n = r.data.data[0].values.length;
                    int i = (int) (Math.round(n-1) * sdataset);
                    if (i < 0) {
                        i = 0;
                    }
                    if (i >= n) {
                        i = n-1;
                    }
                    final int finali = i;
                    int checkedCount = 0;
                    for (GLChartProgram glChartProgram : r.chart) {
                        if (glChartProgram.checked) {
                            checkedCount++;
                        }
                    }
                    if (checkedCount > 0) {
                        for (GLChartProgram glChartProgram : r.chart) {
                            glChartProgram.setTooltipIndex(finali);
                        }
                    }
//                    r.drawAndSwap();
                    r.invalidateRender();
                }
            };
            r.postToRender(dispatchTouchdown);
//            r.renderHandler.post(draw);

            if (LOGGING) Log.d(MainActivity.TAG, "chart touch down");
        }
    }

//    public static final BlockingQueue<MyMotionEvent> motionEvents = new ArrayBlockingQueue<MyMotionEvent>(100);

    public final void setOverlayPos(boolean init) {
        final float left = (float) (scroller_left - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float right = (float) (scroller__right - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float scale = (right - left);
//        motionEvents.poll()
        if (init) {
            r.initleft = left;
            r.initRight = right;
            r.initSacle = scale;
        } else {
            Runnable updateLeftRight = new Runnable() {//todo do not allocate
                @Override
                public void run() {
                    setLeftRightImpl(left, right, scale);
                }
            };
            r.postToRender(updateLeftRight);
        }
    }

    public void setLeftRightImpl(float left, float right, float scale) {
        r.updateLeftRight(left, right, scale);

        for (GLChartProgram glChartProgram : r.chart) {
            glChartProgram.setTooltipIndex(-1);
        }
//                r.drawAndSwap();
        r.invalidateRender();
    }

    public final void calculateMax(float left, float right) {
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        int len = r.chart[0].column.values.length;
        int from = Math.max(0, (int)Math.ceil(len * (left-0.02f)));
        int to = Math.min(len, (int)Math.ceil(len * (right+0.02f)));
        for (GLChartProgram glChartProgram : r.chart) {
            if (glChartProgram.checked) {
                long[] values = glChartProgram.column.values;
                for (int i = from; i < to; i++) {
                    long value = values[i];
                    max = (max >= value) ? max : value;
                    min = Math.min(min, value);
                }
            }
        }
        this.viewportMin = min;
        this.viewportMax = max;
    }

    public ColorSet currentColors;

    public void animateToColors(final ColorSet colors, final long duration) {
        Runnable switchTheme = new Runnable() {


            @Override
            public void run() {
                currentColors = colors;
                bgAnim = new MyAnimation.Color(duration, bgColor, colors.lightBackground);
                r.ruler.animate(colors, duration);
                r.overlay.animate(colors, duration);
                for (GLChartProgram glChartProgram : r.chart) {
                    glChartProgram.animateColors(colors, duration);
                }
                if (r.tooltip != null) {
                    r.tooltip.animateTo(colors, duration);
                }
//                r.drawAndSwap();
                r.invalidateRender();
            }
        };
        r.postToRender(switchTheme);
    }

    public void release() {
        r.release();

    }

//    @Override
//    public void invalidate() {
//        super.invalidate();
//    }

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
