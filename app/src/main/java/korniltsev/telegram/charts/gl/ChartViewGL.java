package korniltsev.telegram.charts.gl;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.ViewConfiguration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

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
    alpha animation, wrong blending for charts
    rules vertical animation
    chart pointer response for max animation

    toolbar shadow, scrollbar night mode, text night mode + animation
    horizontal lables + animations

    toooltip by touching

    >>>>>>>>>>>>>>>> https://blog.mapbox.com/drawing-antialiased-lines-with-opengl-8766f34192dc
    calculating normal in vertex shader
    https://github.com/learnopengles/Learn-OpenGLES-Tutorials/blob/641fcc25158dc30f45a7b2faaab165ec61ebb54b/android/AndroidOpenGLESLessonsCpp/app/src/main/assets/vertex/per_pixel_vertex_shader_tex_and_light.glsl#L22

    checkbox + animations + divider width

----------------------------------------------------------------------
----------------------------------------------------------------------
----------------------------------------------------------------------
// todo
stop drawing when nothing changes and draw only animation / changes
//     todo first animation is SLOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!, trace with tracer emulator, if possible - warmup, if not - rewrite custom ripple or just replace with state list drawable
//     snap scrollbar near zeros
//     implement empty chart
//     implement y=0 chart
//     scrolling/caling allocations & perf
//     initial zoom for small charts is terrible
//    mb scale linewidth when drawing 365 points


// todo design
//    overlay color seems wrong
//    lollipop bg gradient
//    check colors & paddings with collor picker
//    add 1dp padding to the scrollbar charts
//    compare label fonts with design

// todo nice to have
//     support rtl since telegram supports
//     support split screen?
//     adjust theme for smooth transition
//     checkbox animations
//     static layout warmup / background init
//     nice app icon
//      https://github.com/facebook/redex
// check for accessor methods


//todo testing
//     requestLayaout during drag
//     requestLayaout during animation?
//     test on old device                      <<<<<<
//     monkey test   + screenshots
//     fuzz test     + screenshots
//     nagative values



*/
public class ChartViewGL extends TextureView {
    public static final String LOG_TAG = "tg.ch.gl";
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
    public int bgColor;
    public MyAnimation.Color bgAnim = null;
//    private ColorSet currentColorsSet;

    public ChartViewGL(Context context, ColumnData[] c, Dimen dimen, ColorSet currentColorsSet) {
        super(context);
        this.dimen = dimen;
        dimen_v_padding8 = dimen.dpi(8);
        dimen_chart_height = dimen.dpi(300);
        dimen_scrollbar_height = dimen.dpi(38);
        r = new Render(c);
        this.bgColor = currentColorsSet.lightBackground;
        this.rulerColor = currentColorsSet.ruler;
        r.start();
        setSurfaceTextureListener(r);

        h = dimen_v_padding8
                + dimen_chart_height
                + dimen_v_padding8
                + dimen_v_padding8
                + dimen_scrollbar_height
                + dimen_v_padding8;

        initial_scroller_dith =  dimen.dpi(86);
        resize_touch_area2 = dimen.dpi(20);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
//
//        int scrollbar_top = chart_bottom + this.scroll_bar_v_padding;
        scrollbar.left = dimen.dpi(16);
        scrollbar.right = w - dimen.dpi(16);
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
        private GLRulesProgram ruler;


        public Render(ColumnData[] column) {
            this.data = column;


        }

        @Override
        public void run() {
            if (!waitForSurface()) {

                return;
            }
            initGL(surface);
            initPrograms();
            loop();

        }

        private void initPrograms() {
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
            ruler = new GLRulesProgram(w, h, dimen, ChartViewGL.this, rulerColor);
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

                    // chart
                    for (GLChartProgram c : chart) {
                        if (c.column.id.equals(id)) {
                            c.animateAlpha(isChecked);
                        }
                        if (checkedCount != 0) {
                            c.animateMinMax(0, max, true);
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
//                SystemClock.sleep(8);
                long t = SystemClock.uptimeMillis();
                if (bgAnim != null) {
                    bgColor = bgAnim.tick(t);
                    if (bgAnim.ended) {
                        bgAnim = null;
                    }
                }
                glClearColor(
                        Color.red(bgColor) / 255f,
                        Color.green(bgColor) / 255f,
                        Color.blue(bgColor) / 255f,
                        1.0f
                );
                glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

                for (GLChartProgram c : scrollbar) {
                    c.draw(t);
                }
                overlay.draw(t);

                ruler.draw(t);
                for (GLChartProgram chartProgram : chart) {
                    chartProgram.draw(t);
                }

                if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
                    throw new RuntimeException("Cannot swap buffers");
                }
//                break;
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

                    //todo try catch and try without sampel buffers, at least it wont crash
                    EGL10.EGL_SAMPLE_BUFFERS, 1,
//                    EGL10.EGL_SAMPLES, 2,

                    EGL10.EGL_NONE
            };
        }


    }

    public static final boolean LOGGING = true; //todo
    private int scroller_width;//todo replace with scroller_left/right
    private int scroller_pos = -1;
    private Rect scrollbar = new Rect();
//    private int scroller_move_down_x;
    static final int DOWN_MOVE = 0;
    static final int DOWN_RESIZE_LEFT = 1;
    static final int DOWN_RESIZE_RIGHT = 2;
    float last_x = -1f;
    int resze_scroller_right = -1;
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
                //todo check scroll edges
                boolean b = y >= scrollbar.top && y <= scrollbar.bottom;
                if (b) {
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
                        //todo check x inside scroller
                        last_x = x;
                        scroller_move_down_x = (int) (x - scroller_left);
                        scroller_move_down_width = scroller__right - scroller_left;
                        down_target = DOWN_MOVE;
                        return true;
                    } else {
                        //todo check scroll edges
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN miss");
                    }
                } else {
                    if (LOGGING) Log.d("tg.chart", "touchevent DOWN miss");
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
                            if (scroller_left < scrollbar.left) {
                                scroller_left = scrollbar.left;
                                scroller__right = scrollbar.left + scroller_move_down_width;
                            }
                            if (scroller__right > scrollbar.right) {
                                scroller__right = scrollbar.right;
                                scroller_left = scrollbar.right - scroller_move_down_width;
                            }
                            setOverlayPos();
//                            invalidate();
//                            int scroller_width = scroller__right - scroller_left;
//                            scroller_left = Math.min(Math.max(scroller_left, scrollbar.left), scrollbar.right - scroller_width);
                        } else if (down_target == DOWN_RESIZE_RIGHT) {
                            scroller__right = (int) x;
                            if (scroller__right > scrollbar.right) {
                                scroller__right = scrollbar.right;
                            }
                            // check the scrollbar is not too small
                            if (scroller__right - scroller_left < initial_scroller_dith) {
                                scroller__right = scroller_left + initial_scroller_dith;
                            }
                            setOverlayPos();
//                            invalidate();
                        } else if (down_target == DOWN_RESIZE_LEFT) {
                            scroller_left = (int) x;
                            if (scroller_left < scrollbar.left) {
                                scroller_left = scrollbar.left;
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

//    private static final BlockingQueue<MyMotionEvent> motionEvents = new ArrayBlockingQueue<MyMotionEvent>(100);

    public void setOverlayPos() {
        final float left = (float)(scroller_left - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float right = (float) (scroller__right - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float scale = (right - left);
//        motionEvents.poll()
        r.actionQueue.add(new Runnable() {//todo do not allocate
            @Override
            public void run() {
                r.overlay.setLeftRight(left, right);
                for (GLChartProgram glChartProgram : r.chart) {
                    glChartProgram.zoom = scale;
                    glChartProgram.left = left;
                }
            }
        });
    }

    public void animateToColors(final ColorSet colors) {
        r.actionQueue.add(new Runnable() {
            @Override
            public void run() {
                bgAnim = new MyAnimation.Color(160, bgColor, colors.lightBackground);
                r.ruler.animate(colors.ruler);
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
