package korniltsev.telegram.charts.gl;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
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
import java.util.Arrays;
import java.util.List;

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
import static korniltsev.telegram.charts.MainActivity.DEBUG;
import static korniltsev.telegram.charts.MainActivity.LOGGING;
import static korniltsev.telegram.charts.MainActivity.TAG;

/*


high prio


    - бонус зум для 1 & 2
        - зум ин графика
        - зум ин скроллбара
        - заблокировать скролл/зум
        - санимировать линейку
        - санимировать x



    - дизайн тултипа
        - дизайн
        - стрелочка
        - правильная позиция + анимирование позиции + анимирование ширины
        - анимирование смены значений как на видосах
        - анимировать добавление элементов при setChecked? ( low prio )
    ------------------------



    - дизайн скролбара
        - кропнуть со скруглениями?
    - дизайн чекбоксов
        - A long tap on any data filter should uncheck all other filters.
    - придумать как исправить лаг когда скролишь и мин/макс меняется каждые пару фреймов

    - сокращалка
        норм сокращалка
        в бар7: выбрать apricots, pears - показывает 3k несколько раз
        если сокращенные значения не меняются то ничего не делать с линейкой и надписями

    - бонус зум для 3
    - бонус зум для 4
    ------------------------
    - бонус зум для 5 ?


low prio
    убрать touch slop
    цвета
        - bar chart - неправильный цвет при выбранной колонке - заюзать из гайдлайнов
        - в тултипе цвет текста не такой как на графике линия
    - в баре тултип показывать около значения а не вверху
    - в линейных графика кажется неправильно считается максимум или неправильно кропаются значения справа
    - когда скейлишь x левую надпись пидорасит
    [ ? ] Matrix -> MyMatrix for inlining? test if it does something


    line alpha animation blending - render to fbo
    - проверить цвета в колоночных графиках day/night

---------------------------------------
оптимизации
    - не рисовать треугольники в баре за пределами экрана
    - left right - сразуже рисовать
    - не рисовать линии в линиях за пределами экрана
    - ленивые x надписи
    - глобальный кеш static layout, локальный кеш текстур надписей
    - когда скроллбар не меняется - отрендерить его во фреймбуффер
    - попробовать отрендерить все на SurfaceView для Android N
    - мб сначала создать один график, потом остальные
    - почему когда водишь пальцем то не лагает, а когда анимируется лагает? попробовать postDelayed для анимации сделать?, Choreographer Callback
---------


// todo testing
// write ui test monkey



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
    //    public static final int CHART_HEIGHT = 280;
    public static final int CHART_BOTTOM_DPI = 80;
    public final Dimen dimen;

    public final int dimen_v_padding8;
    public final int dimen_chart_height_with_top_padding;// with top padding
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
    public final int dimen_chart_usefull_height;
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
//        legend_height = dimen.dpi(46);
        legend_height = 0;
        dimen_v_padding8 = dimen.dpi(8);
        dimen_chart_height_with_top_padding = dimen.dpi(300);
        dimen_chart_usefull_height = dimen.dpi(280);
        dimen_scrollbar_height = dimen.dpi(38);

        r = new Render(c);
        this.bgColor = currentColorsSet.lightBackground;
//        this.rulerColor = currentColorsSet.ruler;
//        r.start();
        setSurfaceTextureListener(r);

        checkboxesHeight = 0;
//        if (data.type == ColumnData.Type.bar && data.data.length == 2) {
//        } else {
//            int checkboxesCount = c.data.length - 1;
//            int dividersCount = checkboxesCount - 1;
//            checkboxesHeight = checkboxesCount * dimen.dpi(CHECKBOX_HEIGHT_DPI) + dividersCount * dimen.dpi(CHECKBOX_DIVIDER_HIEIGHT);
//        }

        hpadding = dimen.dpi(16);
        h = dimen_v_padding8
                + legend_height
                + dimen_chart_height_with_top_padding
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

        scrollbarPos.left = hpadding;
        scrollbarPos.right = w - hpadding;
        scrollbarPos.bottom = h - dimen_v_padding8 - checkboxesHeight;
        scrollbarPos.top = scrollbarPos.bottom - dimen_scrollbar_height;

        setMeasuredDimension(w, h);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (scroller_left == -1) {
            scroller__right = scrollbarPos.right;
            scroller_left = scrollbarPos.right - initial_scroller_dith;
            setOverlayPos(true);
        }

        chartBottom = bottom - dimen.dpi(CHART_BOTTOM_DIP) - checkboxesHeight;
        chartTop = chartBottom - dimen_chart_usefull_height;
//        chartTop = dimen.dpi(80);
    }

    public boolean setChecked(String id, boolean isChecked) {
        boolean[] checked = r.checked;
        int checkedCount = 0;
        ColumnData[] data1 = data.data;
        int foundI = -1;
        for (int i = 1; i < data1.length; i++) {
            ColumnData datum = data1[i];
            int ci = i - 1;
            if (r.checked[ci]) {
                checkedCount++;
            }
            if (datum.id.equals(id)) {
                foundI = ci;
            }
        }
        if (foundI == -1) {
            return false;
        }
        if (checkedCount == 1 && r.checked[foundI] && !isChecked) {
            return false;
        }
        this.r.setChecked(id, isChecked);
        return true;
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
        public int tooltipIndex;

        SurfaceTexture surface;
        final Object lock = new Object();//todo fuck locking
        public int w;
        public int h;

//        public long prevMax;
//        public long prevMin;

        public boolean rulerInitDone;
        public boolean[] checked;


        Handler renderHandler2;

        public Tooltip tooltip;
        public LinesChartProgram[] scrollbar_lines;
        private BarChartProgram scrollbar_bars;
        private Bar7ChartProgram scrollbar_bar7;
        private PercentStackedChartProgram scrollbar_stacked_percent;

        public LinesChartProgram[] chartLines;
        private BarChartProgram chartBar;
        private Bar7ChartProgram chartBar7;
        private PercentStackedChartProgram chartStackedPercent;

        public GLScrollbarOverlayProgram overlay;
        public GLRulersProgram ruler;

        public SimpleShader simple;
        BarChartProgram.MyShader barShader = null;
        Bar7ChartProgram.MyShader bar7Shader = null;
        PercentStackedChartProgram.MyShader stackPercentShader = null;

        //        public LinesChartProgram.Shader chartShader;
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
                    if (chartLines != null) {
                        for (LinesChartProgram c : chartLines) {
                            try {
                                c.release();
                            } catch (Throwable e) {
                                if (LOGGING) Log.e(TAG, "release err", e);
                            }
                        }
                    }
                    if (scrollbar_lines != null) {

                        for (LinesChartProgram c : scrollbar_lines) {
                            try {
                                c.release();
                            } catch (Throwable e) {
                                if (LOGGING) Log.e(TAG, "release err", e);
                            }
                        }
                    }
                    {
                        try {
                            if (chartBar != null) {
                                chartBar.release();
                            }
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                        try {
                            if (scrollbar_bars != null) {
                                scrollbar_bars.release();
                            }
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                        try {
                            if (barShader != null) {
                                barShader.release();
                            }
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                    }
                    {
                        try {
                            if (chartBar7 != null) {
                                chartBar7.release();
                            }
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                        try {
                            if (scrollbar_bar7 != null) {
                                scrollbar_bar7.release();
                            }
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                        try {
                            if (bar7Shader != null) {
                                bar7Shader.release();
                            }
                        } catch (Throwable e) {
                            if (LOGGING) Log.e(TAG, "release err", e);
                        }
                    }
                    {
                        try {
                            if (chartStackedPercent != null) {
                                chartStackedPercent.release();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        try {
                            if (stackPercentShader != null) {
                                stackPercentShader.release();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
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

                    if (Build.VERSION.SDK_INT <= 19) {

                    } else {
                        surface.release();
                    }

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
            joiningShader = new MyCircles.Shader(6);
            simple = new SimpleShader();

            ColumnData[] data = this.data.data;
            checked = new boolean[data.length - 1];
            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                checked[i - 1] = true;
            }


            boolean barSingle = this.data.type == ColumnData.Type.bar && data.length == 2;
            boolean bar7 = this.data.type == ColumnData.Type.bar && data.length == 8;
            boolean stacked_percent = this.data.stacked && this.data.percentage;
            if (this.data.type == ColumnData.Type.line) {

                scrollbar_lines = new LinesChartProgram[data.length - 1];

                if (this.data.y_scaled) {
                    for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                        ColumnData datum = data[i];


                        LinesChartProgram it = new LinesChartProgram(data[i], w, h, dimen, ChartViewGL.this, true, init_colors.lightBackground, simple, joiningShader);
                        scrollbar_lines[i - 1] = it;
                        calculateChartLinesMaxScaled(it, 0f, 1f);
                        it.animateMinMax(it.scaledViewporMin, it.scaledViewporMax, false, 0);
                    }
                } else {
                    long max = -1;
                    long min = Long.MAX_VALUE;
                    for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                        ColumnData datum = data[i];
                        scrollbar_lines[i - 1] = new LinesChartProgram(data[i], w, h, dimen, ChartViewGL.this, true, init_colors.lightBackground, simple, joiningShader);
                        max = Math.max(max, datum.max);
                        min = Math.min(min, datum.min);
                    }
                    for (LinesChartProgram it : scrollbar_lines) {
                        it.maxValue = max;
                        it.minValue = min;
                    }
                }
            } else if (barSingle) {
                barShader = new BarChartProgram.MyShader();
                scrollbar_bars = new BarChartProgram(data[1], w, h, dimen, ChartViewGL.this, true, barShader);
            } else if (bar7) {
                bar7Shader = new Bar7ChartProgram.MyShader();
                List<ColumnData> cs = Arrays.asList(data).subList(1, 8);
                scrollbar_bar7 = new Bar7ChartProgram(cs, w, h, dimen, ChartViewGL.this, true, bar7Shader);
                long m = calculateBar7Max(data, 0, 1);
                scrollbar_bar7.animateMinMax(m, false, 0);
            } else if (stacked_percent) {
                stackPercentShader = new PercentStackedChartProgram.MyShader();
                List<ColumnData> cs = Arrays.asList(data).subList(1, 7);
                scrollbar_stacked_percent = new PercentStackedChartProgram(cs, w, h, dimen, ChartViewGL.this, true, stackPercentShader);
            }

            if (barSingle) {
                chartBar = new BarChartProgram(data[1], w, h, dimen, ChartViewGL.this, false, barShader);
            } else if (bar7) {
                List<ColumnData> cs = Arrays.asList(data).subList(1, 8);
                chartBar7 = new Bar7ChartProgram(cs, w, h, dimen, ChartViewGL.this, false, bar7Shader);
            } else if (stacked_percent) {
                List<ColumnData> cs = Arrays.asList(data).subList(1, 7);
                chartStackedPercent = new PercentStackedChartProgram(cs, w, h, dimen, ChartViewGL.this, false, stackPercentShader);
            } else {
                chartLines = new LinesChartProgram[data.length - 1];
                for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                    chartLines[i - 1] = new LinesChartProgram(data[i], w, h, dimen, ChartViewGL.this, false, init_colors.lightBackground, simple, joiningShader);
                }
            }


            overlay = new GLScrollbarOverlayProgram(w, h, dimen, ChartViewGL.this, init_colors.scrollbarBorder, init_colors.scrollbarOverlay, simple);
            boolean differentXYAxisColors = barSingle || bar7 || stacked_percent;//todo
            ruler = new GLRulersProgram(w, h, dimen, ChartViewGL.this, init_colors, simple, xColumn, differentXYAxisColors, stacked_percent, this.data.y_scaled);

            Matrix.orthoM(PROJ, 0, 0, w, 0, h, -1.0f, 1.0f);


            debugRects = new ArrayList<>();
//            debugRects.add(new MyRect(w, dimen_chart_usefull_height, 0, dimen.dpf(CHART_BOTTOM_DPI), 0xffffffff, w, h));
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
                    ColumnData[] data1 = data.data;
                    int foundIndex = -1;
                    for (int i = 1; i < data1.length; i++) {
                        ColumnData datum = data1[i];
                        if (datum.id.equals(id)) {
                            checked[i - 1] = isChecked;
                            foundIndex = i - 1;
                        }
                    }
                    if (tooltip != null) {
                        tooltip.setChecked(id, isChecked);
                    }
                    // scrollbar
                    if (scrollbar_lines != null) {
                        if (data.y_scaled) {
                            for (int i = 0; i < scrollbar_lines.length; i++) {
                                LinesChartProgram c = scrollbar_lines[i];
                                if (c.column.id.equals(id)) {
                                    c.animateAlpha(isChecked);
                                    break;
                                }
                            }
                        } else {

                            LinesChartProgram found = null;
                            long max = -1;
                            long min = Long.MAX_VALUE;


                            for (int i = 0; i < scrollbar_lines.length; i++) {
                                LinesChartProgram c = scrollbar_lines[i];
                                if (c.column.id.equals(id)) {
                                    found = c;
                                    c.animateAlpha(isChecked);
                                }
                                if (c.checked) {
                                    max = Math.max(c.column.max, max);
                                    min = Math.min(c.column.min, min);
                                }
                            }
                            for (LinesChartProgram c : scrollbar_lines) {
                                if (found == c && !isChecked) {
                                } else {
                                    c.animateMinMax(min, max, true, 208);
                                }
                            }
                        }
                    }
                    if (scrollbar_bar7 != null) {
                        long max = calculateBar7Max(data1, 0, 1);
                        scrollbar_bar7.animateMinMax(max, true, 208);
                        if (foundIndex != -1) {
                            scrollbar_bar7.animateFade(foundIndex, isChecked, 208);
                        }
                    }
                    if (scrollbar_stacked_percent != null) {
                        if (foundIndex != -1) {
                            scrollbar_stacked_percent.animateFade(foundIndex, isChecked, 208);
                        }
                    }

                    if (chartLines != null) {
                        for (LinesChartProgram c : chartLines) {
//                            c.setTooltipIndex(-1);
                            if (c.column.id.equals(id)) {
                                c.animateAlpha(isChecked);
                            }
                        }
                        if (data.y_scaled) {
                            for (LinesChartProgram c : r.chartLines) {
                                calculateChartLinesMaxScaled(c, r.overlay.left, r.overlay.right);
                                c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, !firstLeftRightUpdate, 256);
                            }
                            ruler.animateScale(
                                    r.chartLines[0].scaledViewporMin, r.chartLines[0].scaledViewporMax,
                                    r.chartLines[1].scaledViewporMin, r.chartLines[1].scaledViewporMax,
                                    208);
                        } else {
                            calculateChartLinesMax(r.overlay.left, r.overlay.right);// set checked
                            for (LinesChartProgram c : chartLines) {
                                c.animateMinMax(viewportMin, viewportMax, true, 208);
                            }
                            ruler.animateScale(viewportMin, viewportMax, 208);
                        }
                    }
                    if (chartBar7 != null) {
//                        chartBar7.setTooltipIndex(-1);
                        viewportMax = calculateBar7Max(data1, r.overlay.left, r.overlay.right);
                        chartBar7.animateMinMax(viewportMax, true, 208);
                        if (foundIndex != -1) {
                            chartBar7.animateFade(foundIndex, isChecked, 208);
                        }
                        ruler.animateScale(0, viewportMax, 208);
                    }
                    if (chartStackedPercent != null) {
//                        chartStackedPercent.setTooltipIndex(-1);
//                        viewportMax = calculateBar7Max(data1, r.overlay.left, r.overlay.right);
//                        chartStackedPercent.animateMinMax(viewportMax, true, 208);
                        if (foundIndex != -1) {
                            chartStackedPercent.animateFade(foundIndex, isChecked, 208);
                        }
//                        float ratio = prevMax / (float) viewportMax;
//                        if (prevMax != viewportMax) {
//                            ruler.animateScale(ratio, 0, viewportMax, checkedCount, prevCheckedCOunt, 208);
//                            prevMax = viewportMax;
//                        }
                    }

//                    drawAndSwap();
                    invalidateRender();
                }
            };
            postToRender(setCheckedOnRenderThread);
        }

        private long calculateBar7Max(ColumnData[] data1, float left, float right) {

            int len = data1[0].values.length;
            int from = Math.max(0, (int) Math.ceil(len * (left - 0.02f)));
            int to = Math.min(len, (int) Math.ceil(len * (right + 0.02f)));

            long max;
            max = Long.MIN_VALUE;
            for (int i = from; i < to; i++) {
                int sum = 0;
                for (int i1 = 0; i1 < checked.length; i1++) {
                    if (checked[i1]) {
                        ColumnData d = data1[i1 + 1];
                        long v = d.values[i];
                        sum += v;
                    }
                }
                max = Math.max(max, sum);
            }
            return max;
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
                if (chartLines != null) {
                    if (data.y_scaled) {
                        for (LinesChartProgram c : r.chartLines) {
                            calculateChartLinesMaxScaled(c, r.overlay.left, r.overlay.right);
                            c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, false, 0);
                        }
                        ruler.init(
                                r.chartLines[0].scaledViewporMin, r.chartLines[0].scaledViewporMax,
                                r.chartLines[1].scaledViewporMin, r.chartLines[1].scaledViewporMax,
                                r.chartLines[0].column.color,
                                r.chartLines[1].column.color
                        );
                    } else {
                        calculateChartLinesMax(r.overlay.left, r.overlay.right); // draw ( init)
                        ruler.init(viewportMin, viewportMax);
                        for (LinesChartProgram glChartProgram : chartLines) {
                            glChartProgram.animateMinMax(viewportMin, viewportMax, false, 0);
                        }
                    }
                }
                if (chartBar != null) {
                    calculateChartBarMax(r.overlay.left, r.overlay.right);
                    chartBar.animateMinMax(viewportMax, false, 0);
                    ruler.init(0, viewportMax);
                }
                if (chartBar7 != null) {
                    viewportMax = calculateBar7Max(data.data, r.overlay.left, r.overlay.right);
                    chartBar7.animateMinMax(viewportMax, false, 0);
                    ruler.init(0, viewportMax);
                }
                if (chartStackedPercent != null) {
                    ruler.init(0, 100);
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
            GLES20.glScissor(0, dimen.dpi(CHART_BOTTOM_DPI) - dimen.dpi(1), w, dimen_chart_usefull_height);
//            GLES20.glScissor(0, chartTop, w, Math.abs(chartTop - chartBottom));

            MyGL.checkGlError2();
            invalidated = drawChart(invalidated, t);

//                long t6 = System.nanoTime();
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
            if (scrollbar_lines != null) {
                for (LinesChartProgram chartProgram : scrollbar_lines) {
                    boolean it_invalid = chartProgram.animateionTick(t);
                    invalidated = invalidated || it_invalid;
                }
                for (LinesChartProgram chartProgram : scrollbar_lines) {
                    chartProgram.step1(PROJ);
                }
                MyGL.checkGlError2();
                for (LinesChartProgram chartProgram : scrollbar_lines) {
                    chartProgram.shader.use();//todo use only once!
                    chartProgram.step2();

                    chartProgram.lineJoining.shader.use();
                    chartProgram.step3();
                }
                MyGL.checkGlError2();
            } else if (scrollbar_bars != null) {
                boolean it_invalidated = scrollbar_bars.animate(t);
                scrollbar_bars.prepare(PROJ);
                scrollbar_bars.draw(t);
                invalidated = invalidated || it_invalidated;
            } else if (scrollbar_bar7 != null) {
                boolean it_inv = scrollbar_bar7.animate(t);
                scrollbar_bar7.prepare(PROJ);
                scrollbar_bar7.draw(t, PROJ);
                invalidated = it_inv || invalidated;
            } else if (scrollbar_stacked_percent != null) {
                boolean it_inv = scrollbar_stacked_percent.animate(t);
                scrollbar_stacked_percent.prepare(PROJ);
                scrollbar_stacked_percent.draw(t, PROJ);
                invalidated = it_inv || invalidated;
            }
            return invalidated;
        }

        public boolean drawChart(boolean invalidated, long t) {
            if (chartLines != null) {
                ruler.draw(t);
                int tooltipIndex = chartLines[0].getTooltipIndex();
                if (tooltipIndex != -1) {
                    if (this.tooltip == null) {
                        this.tooltip = new Tooltip(dimen, w, h, currentColors, data, simple, ChartViewGL.this);
                    }
                }


                for (LinesChartProgram chartProgram : chartLines) {
                    boolean it_invalid = chartProgram.animateionTick(t);
                    invalidated = invalidated || it_invalid;
                }

                for (LinesChartProgram chartProgram : chartLines) {
                    chartProgram.step1(PROJ);
                }

                if (tooltipIndex != -1) {
                    this.tooltip.animationTick(t, tooltipIndex, checked);
                    this.tooltip.calcPos(chartLines[0].MVP, tooltipIndex);
                    this.tooltip.drawVLine(PROJ, chartLines[0].MVP, tooltipIndex);
                }

                MyGL.checkGlError2();
                GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

                for (LinesChartProgram chartProgram : chartLines) {
                    chartProgram.shader.use();//todo use only once!
                    chartProgram.step2();

                    chartProgram.lineJoining.shader.use();
                    chartProgram.step3();
                }
                MyGL.checkGlError2();
                for (LinesChartProgram c : chartLines) {
                    if (c.goodCircle != null) {
                        //                    c.goodCircle.shader.use();
                        //
                        c.step4();
                    }
                }
                MyGL.checkGlError2();

                GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

                if (tooltipIndex != -1) {
                    this.tooltip.drawTooltip(PROJ);
                }
            } else if (chartBar != null) {
                int tooltipIndex = chartBar.getTooltipIndex();
                if (tooltipIndex != -1) {
                    if (this.tooltip == null) {
                        this.tooltip = new Tooltip(dimen, w, h, currentColors, data, simple, ChartViewGL.this);
                    }
                }
                boolean it_invalidated = chartBar.animate(t);
                chartBar.prepare(PROJ);
                chartBar.draw(t);
                ruler.draw(t);
                if (tooltipIndex != -1) {
                    this.tooltip.animationTick(t, tooltipIndex, checked);
                    this.tooltip.calcPos(chartBar.MVP, tooltipIndex);
                    this.tooltip.drawTooltip(PROJ);
                }
                invalidated = it_invalidated || invalidated;
            } else if (chartBar7 != null) {
                int tooltipIndex = chartBar7.getTooltipIndex();
                if (tooltipIndex != -1) {
                    if (this.tooltip == null) {
                        this.tooltip = new Tooltip(dimen, w, h, currentColors, data, simple, ChartViewGL.this);
                    }
                }
                boolean it_invalidated = chartBar7.animate(t);
                chartBar7.prepare(PROJ);
                chartBar7.draw(t, PROJ);

                ruler.draw(t);

                if (tooltipIndex != -1) {
                    this.tooltip.animationTick(t, tooltipIndex, checked);
                    this.tooltip.calcPos(chartBar7.MVP, tooltipIndex);
                    this.tooltip.drawTooltip(PROJ);
                }
                invalidated = it_invalidated || invalidated;

            } else if (chartStackedPercent != null) {
                int tooltipIndex = chartStackedPercent.getTooltipIndex();
                if (tooltipIndex != -1) {
                    if (this.tooltip == null) {
                        this.tooltip = new Tooltip(dimen, w, h, currentColors, data, simple, ChartViewGL.this);
                    }
                }
                boolean it_invalidated = chartStackedPercent.animate(t);
                chartStackedPercent.prepare(PROJ);
                chartStackedPercent.draw(t, PROJ);

                ruler.draw(t);

                if (tooltipIndex != -1) {
                    this.tooltip.animationTick(t, tooltipIndex, checked);
                    this.tooltip.calcPos(chartStackedPercent.MVP, tooltipIndex);
                    this.tooltip.drawVLine(PROJ, chartStackedPercent.MVP, tooltipIndex);
                    this.tooltip.drawTooltip(PROJ);
                }
                invalidated = it_invalidated || invalidated;

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
            if (Build.VERSION.SDK_INT <= 19) {
                try {
                    r.join(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            } else {
                return false;
            }
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
            if (chartLines != null) {
                if (data.y_scaled) {
                    for (LinesChartProgram c : r.chartLines) {
                        calculateChartLinesMaxScaled(c, left, right);
                        c.zoom = scale;
                        c.left = left;
                        c.animateMinMax(c.scaledViewporMin, c.scaledViewporMax, !firstLeftRightUpdate, 256);
                    }
                    ruler.setLeftRight(left, right, scale);
                    if (rulerInitDone) {
                        ruler.animateScale(
                                r.chartLines[0].scaledViewporMin, r.chartLines[0].scaledViewporMax,
                                r.chartLines[1].scaledViewporMin, r.chartLines[1].scaledViewporMax,
                                256);
                    }

                } else {
                    calculateChartLinesMax(left, right);// updateLeftRight

                    for (LinesChartProgram glChartProgram : r.chartLines) {
                        glChartProgram.zoom = scale;
                        glChartProgram.left = left;
                        glChartProgram.animateMinMax(viewportMin, viewportMax, !firstLeftRightUpdate, 256);
                    }
                    ruler.setLeftRight(left, right, scale);

                    if (rulerInitDone) {
                        ruler.animateScale(viewportMin, viewportMax, 256);
                    }
                }
            }
            if (chartBar != null) {
                ruler.setLeftRight(left, right, scale);
                chartBar.zoom = scale;
                chartBar.left = left;
                calculateChartBarMax(left, right);
                if (rulerInitDone) {
                    chartBar.animateMinMax(viewportMax, !firstLeftRightUpdate, 256);
                    ruler.animateScale(0, viewportMax, 256);
                }
            }
            if (chartBar7 != null) {
                ruler.setLeftRight(left, right, scale);
                chartBar7.zoom = scale;
                chartBar7.left = left;
                viewportMax = calculateBar7Max(data.data, r.overlay.left, r.overlay.right);
                if (rulerInitDone) {
                    chartBar7.animateMinMax(viewportMax, !firstLeftRightUpdate, 256);
                    ruler.animateScale(0, viewportMax, 256);
                }
            }
            if (chartStackedPercent != null) {
                ruler.setLeftRight(left, right, scale);
                chartStackedPercent.zoom = scale;
                chartStackedPercent.left = left;
            }
            firstLeftRightUpdate = false;
        }
    }

    public Rect scrollbarPos = new Rect();
    static final int DOWN_MOVE = 0;
    static final int DOWN_RESIZE_LEFT = 1;
    static final int DOWN_RESIZE_RIGHT = 2;
    static final int DOWN_TOOLTIP = 3;
    float last_x = -1f;
    int down_target = -1;
    boolean dragging;


    public int scroller__right = -1;
    public int scroller_left = -1;
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
                boolean scrollbar = y >= this.scrollbarPos.top && y <= this.scrollbarPos.bottom;
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
                    boolean chart = y <= this.chartBottom;
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
                            if (scroller_left < this.scrollbarPos.left) {
                                scroller_left = this.scrollbarPos.left;
                                scroller__right = this.scrollbarPos.left + scroller_move_down_width;
                            }
                            if (scroller__right > this.scrollbarPos.right) {
                                scroller__right = this.scrollbarPos.right;
                                scroller_left = this.scrollbarPos.right - scroller_move_down_width;
                            }
                            setOverlayPos(false);
//                            invalidate();
//                            int scroller_width = scroller__right - scroller_left;
//                            scroller_left = Math.min(Math.max(scroller_left, scrollbar.left), scrollbar.right - scroller_width);
                        } else if (down_target == DOWN_RESIZE_RIGHT) {
                            scroller__right = (int) x;
                            if (scroller__right > this.scrollbarPos.right) {
                                scroller__right = this.scrollbarPos.right;
                            }
                            // check the scrollbar is not too small
                            if (scroller__right - scroller_left < initial_scroller_dith / 2) {
                                scroller__right = scroller_left + initial_scroller_dith / 2;
                            }
                            setOverlayPos(false);
//                            invalidate();
                        } else if (down_target == DOWN_RESIZE_LEFT) {
                            scroller_left = (int) x;
                            if (scroller_left < this.scrollbarPos.left) {
                                scroller_left = this.scrollbarPos.left;
                            }
//                            scroller_width = resze_scroller_right - scroller_left;
                            if (scroller__right - scroller_left < initial_scroller_dith / 2) {
//                                scroller_left = initial_scroller_dith;
                                scroller_left = scroller__right - initial_scroller_dith / 2;
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
                        dispatchTouchDownChart(x, y);
                    }
                }
                if (LOGGING) Log.d("tg.chart", "dragging = false ");
                last_x = -1;
                dragging = false;
                break;
            default:
                break;
        }
        return false;
    }

    public void dispatchTouchDownChart(final float x, final float y) {
        if (x < scrollbarPos.left || x > scrollbarPos.right) {
            if (LOGGING) Log.d(MainActivity.TAG, "chart down miss");
        } else {
            float swindow = (x - scrollbarPos.left) / scrollbarPos.width();
            if (swindow > 0.97) {
                swindow = 1f;
            } else if (swindow < 0.03) {
                swindow = 0;
            }
            final float finalswindow = swindow;
            Runnable dispatchTouchdown = new Runnable() {
                @Override
                public void run() {
                    if (r.tooltip != null && r.tooltipIndex != -1) {
                        float xpos = r.tooltip.xpos;
                        float ypos = r.tooltip.ypos;
                        float tw = r.tooltip.framebuffer.realW;
                        float th = r.tooltip.framebuffer.realH;
                        float invy = h - y;
                        if (x >= xpos && x <= xpos + tw && invy >= ypos && invy <= ypos + th) {
                            onZoom(r.tooltipIndex);
                            return;
                        }
                    }
                    float sdataset = r.overlay.left + finalswindow * (r.overlay.right - r.overlay.left);
                    int n = r.data.data[0].values.length;
                    int i = (int) (Math.round(n - 1) * sdataset);
                    if (i < 0) {
                        i = 0;
                    }
                    if (i >= n) {
                        i = n - 1;
                    }
                    final int finali = i;
                    int checkedCount = 0;
                    if (r.chartLines != null) {
                        for (LinesChartProgram glChartProgram : r.chartLines) {
                            if (glChartProgram.checked) {
                                checkedCount++;
                            }
                        }
                        if (checkedCount > 0) {
                            for (LinesChartProgram glChartProgram : r.chartLines) {
                                glChartProgram.setTooltipIndex(finali);
                            }
                        }
                    }
                    if (r.scrollbar_lines != null) {
                        for (LinesChartProgram glChartProgram : r.scrollbar_lines ) {
                            glChartProgram.setTooltipIndex(finali);
                        }
                    }
                    if (r.chartBar != null) {
                        r.chartBar.setTooltipIndex(finali);
                    }
                    if (r.chartBar7 != null) {
                        r.chartBar7.setTooltipIndex(finali);
                    }
                    if (r.chartStackedPercent != null) {
                        r.chartStackedPercent.setTooltipIndex(finali);
                    }
                    r.tooltipIndex = finali;
//                    r.drawAndSwap();
                    r.invalidateRender();
                }
            };
            r.postToRender(dispatchTouchdown);
//            r.renderHandler.post(draw);

            if (LOGGING) Log.d(MainActivity.TAG, "chart touch down");
        }
    }

    boolean zoomedIn = false;

    private void onZoom(int tooltipIndex) {
        zoomedIn = !zoomedIn;
        if (r.chartLines != null) {
            for (LinesChartProgram c : r.chartLines) {
                c.animateOut(256 + 128, zoomedIn);
            }
        }
        if (r.scrollbar_lines != null) {
            for (LinesChartProgram c : r.scrollbar_lines) {
                c.animateOut(256 + 128, zoomedIn);
            }
        }

        r.invalidateRender();
    }

//    public static final BlockingQueue<MyMotionEvent> motionEvents = new ArrayBlockingQueue<MyMotionEvent>(100);

    public final void setOverlayPos(boolean init) {
        final float left = (float) (scroller_left - scrollbarPos.left) / (scrollbarPos.right - scrollbarPos.left);
        final float right = (float) (scroller__right - scrollbarPos.left) / (scrollbarPos.right - scrollbarPos.left);
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
        if (r.chartLines != null) {

            for (LinesChartProgram glChartProgram : r.chartLines) {
                glChartProgram.setTooltipIndex(-1);
            }
        }
        if (r.scrollbar_lines != null) {
            for (LinesChartProgram glChartProgram : r.scrollbar_lines ) {
                glChartProgram.setTooltipIndex(-1);
            }
        }
        if (r.chartBar != null) {
            r.chartBar.setTooltipIndex(-1);
        }
        if (r.chartBar7 != null) {
            r.chartBar7.setTooltipIndex(-1);
        }
        if (r.chartStackedPercent != null) {
            r.chartStackedPercent.setTooltipIndex(-1);
        }
        r.tooltipIndex = -1;
//                r.drawAndSwap();
        r.invalidateRender();
    }

    public final void calculateChartLinesMax(float left, float right) {
        if (r.chartLines != null) {

            long max = Long.MIN_VALUE;
            long min = Long.MAX_VALUE;
            int len = r.chartLines[0].column.values.length;
            int from = Math.max(0, (int) Math.ceil(len * (left - 0.02f)));
            int to = Math.min(len, (int) Math.ceil(len * (right + 0.02f)));
            for (LinesChartProgram glChartProgram : r.chartLines) {
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
    }

    static {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (DEBUG) {
                    Log.e(TAG, "err", e);
                    System.exit(0);
                }
            }
        });
    }

    public static void calculateChartLinesMaxScaled(LinesChartProgram p, float left, float right) {
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        int len = p.column.values.length;
        int from = Math.max(0, (int) Math.ceil(len * (left - 0.02f)));
        int to = Math.min(len, (int) Math.ceil(len * (right + 0.02f)));
        long[] values = p.column.values;
        for (int i = from; i < to; i++) {
            long value = values[i];
            max = (max >= value) ? max : value;
            min = Math.min(min, value);
        }
        p.scaledViewporMax = max;
        p.scaledViewporMin = min;
    }

    public final void calculateChartBarMax(float left, float right) {
        if (r.chartBar != null) {

            long max = Long.MIN_VALUE;
            long min = Long.MAX_VALUE;
            int len = r.chartBar.column.values.length;
            int from = Math.max(0, (int) Math.ceil(len * (left - 0.02f)));
            int to = Math.min(len, (int) Math.ceil(len * (right + 0.02f)));
//            for (LinesChartProgram glChartProgram : r.chartLines) {
//                if (glChartProgram.checked) {
            long[] values = r.chartBar.column.values;
            for (int i = from; i < to; i++) {
                long value = values[i];
                max = (max >= value) ? max : value;
                min = Math.min(min, value);
            }
//                }
//            }
            this.viewportMin = min;
            this.viewportMax = max;
        }
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
                if (r.chartLines != null) {
                    for (LinesChartProgram glChartProgram : r.chartLines) {
                        glChartProgram.animateColors(colors, duration);
                    }
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

    @Override
    protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        } catch (Exception e) {
            if (DEBUG) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    throw new AssertionError();
                } else {
                    Log.e(TAG, "detach error", e);
                }
            }

        }
    }

}
