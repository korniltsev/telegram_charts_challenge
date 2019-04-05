package korniltsev.telegram.charts.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;


import java.util.ArrayList;

import korniltsev.telegram.charts.MainActivity;
import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.data.ColumnData;

import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;

import static korniltsev.telegram.charts.MainActivity.LOGGING;
import static korniltsev.telegram.charts.MainActivity.TAG;


public class ChartView extends View {
    public static final int CHECKBOX_HEIGHT_DPI = 50;
    public static final int CHECKBOX_DIVIDER_HIEIGHT = 1;
    public static final int CHART_BOTTOM_DIP = 80;//relative to checkboxes
    public final Dimen dimen;

    public final int dimen_v_padding8;
    public final int dimen_chart_height;
    public final int dimen_scrollbar_height;
    public final int h;
    //    public final Render r;
    public final int initial_scroller_dith;
    public final int resize_touch_area2;
    public final int touchSlop;
    //    public final int rulerColor;
    public final ColorSet init_colors;
    public final ChartData data;
    public final ColumnData xColumn;
    private final Paint pOverlay;
    private final Paint pRuler;
    private final ArrayList<UIColumnData> scroller = new ArrayList<>();
    private final ArrayList<UIColumnData> chart = new ArrayList<>();
    //    public final int checkboxesHeight;
    //    public final long initTime;
    public int bgColor;
    public MyAnimation.Color bgAnim = null;
    public int chartBottom;
    public int chartTop;
    public int hpadding;
    private Paint p = new Paint();
    private Paint p2 = new Paint();
    private int chartUsefullHiehgt;
    //    public long currentMax;
//    public ColorSet currentColorsSet;


    @Override
    public boolean isOpaque() {
        return true;
    }

    public ChartView(Context context, ChartData c, Dimen dimen, ColorSet currentColorsSet) {
        super(context);
        xColumn = c.data[0];
//        initTime = SystemClock.elapsedRealtimeNanos();
        this.init_colors = currentColorsSet;
        currentColors = currentColorsSet;
        this.dimen = dimen;
        this.data = c;
        dimen_v_padding8 = dimen.dpi(8);
        chartUsefullHiehgt = dimen.dpi(280);
        dimen_chart_height = dimen.dpi(300);
        dimen_scrollbar_height = dimen.dpi(38);

//        r = new Render(c);
        this.bgColor = currentColorsSet.lightBackground;
//        this.rulerColor = currentColorsSet.ruler;
//        r.start();
//        setSurfaceTextureListener(r);

        int checkboxesCount = c.data.length - 1;
        int dividersCount = checkboxesCount - 1;


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

//        setBackgroundColor(Color.RED);
        p.setColor(Color.BLUE);
        p2.setColor(Color.GREEN);
        setWillNotDraw(false);

        pOverlay = new Paint();
        pOverlay.setColor(init_colors.scrollbarOverlay);
        pRuler = new Paint();
        pRuler.setColor(init_colors.scrollbarBorder);

        ColumnData[] data1 = data.data;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        for (int i = 1; i < data1.length; i++) {
            ColumnData datum = data1[i];
            UIColumnData e = new UIColumnData(datum);
            e.p.setStrokeWidth(dimen.dpf(1));
            scroller.add(e);
            UIColumnData e2 = new UIColumnData(datum);
            e2.p.setStrokeWidth(dimen.dpf(2));
            chart.add(e2);
            max = Math.max(max, datum.max);
            min = Math.min(min, datum.min);
        }
        for (UIColumnData datum : scroller) {
            datum.max = max;
            datum.min = min;
        }
        for (UIColumnData datum : chart) {
            datum.max = max;
        }
//        setBackgroundColor(Color.RED);
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
            setOverlayPos(true);
        }

//        int top = getHeight() - dimen.dpi(80) - dimen.dpi(280);
//        int bottom = getHeight() - dimen.dpi(80);

        chartBottom = h - dimen.dpi(CHART_BOTTOM_DIP);
        chartTop = chartBottom - chartUsefullHiehgt;
//        chartTop = dimen.dpi(80);
    }

    public void setChecked(String id, boolean isChecked) {
        //todo
    }


    public static int counter;

    //    public int scroller_width;//todo replace with scroller_left/right
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

                        invalidate();
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
                if (LOGGING) Log.d("tg.chart", "dragging = false ");
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
            } else if (swindow < 0.03) {
                swindow = 0;
            }
            final float finalswindow = swindow;
//            Runnable dispatchTouchdown = new Runnable() {
//                @Override
//                public void run() {
//                    float sdataset = r.overlay.left + finalswindow * (r.overlay.right - r.overlay.left);
//                    int n = r.data.data[0].values.length;
//                    int i = (int) (Math.round(n-1) * sdataset);
//                    if (i < 0) {
//                        i = 0;
//                    }
//                    if (i >= n) {
//                        i = n-1;
//                    }
//                    final int finali = i;
//                    int checkedCount = 0;
//                    for (GLChartProgram glChartProgram : r.chart) {
//                        if (glChartProgram.checked) {
//                            checkedCount++;
//                        }
//                    }
//                    if (checkedCount > 0) {
//                        for (GLChartProgram glChartProgram : r.chart) {
//                            glChartProgram.setTooltipIndex(finali);
//                        }
//                    }
////                    r.drawAndSwap();
//                    r.invalidateRender();
//                }
//            };
//            r.postToRender(dispatchTouchdown);

            if (LOGGING) Log.d(MainActivity.TAG, "chart touch down");
        }
    }

//    public static final BlockingQueue<MyMotionEvent> motionEvents = new ArrayBlockingQueue<MyMotionEvent>(100);

    public final void setOverlayPos(boolean init) {
        final float left = (float) (scroller_left - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float right = (float) (scroller__right - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float scale = (right - left);
//        motionEvents.poll()
//        if (init) {
//            r.initleft = left;
//            r.initRight = right;
//            r.initSacle = scale;
//        } else {
//            Runnable updateLeftRight = new Runnable() {//todo do not allocate
//                @Override
//                public void run() {
//                    setLeftRightImpl(left, right, scale);
//                }
//            };
//            r.postToRender(updateLeftRight);
//        }
    }

    public void setLeftRightImpl(float left, float right, float scale) {
//        r.updateLeftRight(left, right, scale);
//
//        for (GLChartProgram glChartProgram : r.chart) {
//            glChartProgram.setTooltipIndex(-1);
//        }
////                r.drawAndSwap();
//        r.invalidateRender();
    }

    public final long calculateMax(float left, float right) {
        long max = -1;
//        int len = r.chart[0].column.values.length;
//        int from = Math.max(0, (int)Math.ceil(len * (left-0.02f)));
//        int to = Math.min(len, (int)Math.ceil(len * (right+0.02f)));
//        for (GLChartProgram glChartProgram : r.chart) {
//            if (glChartProgram.checked) {
//                long[] values = glChartProgram.column.values;
//                for (int i = from; i < to; i++) {
//                    max = (max >= values[i]) ? max : values[i];
//                }
//            }
//        }
        return max;
    }

    public ColorSet currentColors;

    public void animateToColors(final ColorSet colors, final long duration) {
//        Runnable switchTheme = new Runnable() {
//
//
//            @Override
//            public void run() {
//                currentColors = colors;
//                bgAnim = new MyAnimation.Color(duration, bgColor, colors.lightBackground);
//                r.ruler.animate(colors, duration);
//                r.overlay.animate(colors, duration);
//                for (GLChartProgram glChartProgram : r.chart) {
//                    glChartProgram.animateColors(colors, duration);
//                }
//                if (r.tooltip != null) {
//                    r.tooltip.animateTo(colors, duration);
//                }
////                r.drawAndSwap();
//                r.invalidateRender();
//            }
//        };
//        r.postToRender(switchTheme);
    }

    public void release() {
//        r.release();

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


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawRect(scrollbar.left, scrollbar.top, scrollbar.right, scrollbar.bottom, p);
        float dip2 = dimen.dpf(2);
        float dip1 = dimen.dpf(1);
        float dip4 = dimen.dpf(4);
//        canvas.drawRect(scrollbar.left, chartTop, scrollbar.right, chartBottom, p2);


        {// draw scrollbar chart
            float vspace = chartUsefullHiehgt;
            float step = scrollbar.width() / (float) (data.data[0].values.length - 1);
            for (int i1 = 0; i1 < chart.size(); i1++) {
                UIColumnData c = chart.get(i1);
                float x = scrollbar.left;
                final long min = 0;
                float diff = c.max - min;
                long[] values = c.data.values;
                float cur_value = (values[0] - min) / diff;
                float cur_pos = chartBottom - cur_value * vspace;
                for (int i = 1; i < values.length; i++) {
                    float next_value = (values[i] - min) / diff;
                    float next_pos = chartBottom - next_value * vspace;
                    canvas.drawLine(x, cur_pos, x + step, next_pos, c.p);
                    x += step;
                    cur_value = next_value;
                    cur_pos = next_pos;
                }
            }
        }


        {// draw scrollbar chart
            float vspace = scrollbar.height() - 2 * dip2;
            float step = scrollbar.width() / (float) (data.data[0].values.length - 1);
            for (int i1 = 0; i1 < scroller.size(); i1++) {
                UIColumnData c = scroller.get(i1);
                float x = scrollbar.left;
                long min = c.min;
                float diff = c.max - min;
                long[] values = c.data.values;
                float cur_value = (values[0] - min) / diff;
                float cur_pos = scrollbar.bottom - dip1 - cur_value * vspace;
                for (int i = 1; i < values.length; i++) {
                    float next_value = (values[i] - min) / diff;
                    float next_pos = scrollbar.bottom - dip1 - next_value * vspace;
                    canvas.drawLine(x, cur_pos, x + step, next_pos, c.p);
                    x += step;
                    cur_value = next_value;
                    cur_pos = next_pos;
                }
            }
        }

//         draw scrollbar overlay
        canvas.drawRect(scrollbar.left, scrollbar.top, scroller_left, scrollbar.bottom, pOverlay);
        canvas.drawRect(scroller__right, scrollbar.top, scrollbar.right, scrollbar.bottom, pOverlay);
        canvas.drawRect(scroller_left, scrollbar.top, scroller_left + dip4, scrollbar.bottom, pRuler);
        canvas.drawRect(scroller__right - dip4, scrollbar.top, scroller__right, scrollbar.bottom, pRuler);
        canvas.drawRect(scroller_left + dip4, scrollbar.top, scroller__right - dip4, scrollbar.top + dip1, pRuler);
        canvas.drawRect(scroller_left + dip4, scrollbar.bottom - dip1, scroller__right - dip4, scrollbar.bottom, pRuler);
        //todo

    }

    public static class UIColumnData {
        public final ColumnData data;
        public long max;
        public long min;
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);


        public UIColumnData(ColumnData data) {
            this.data = data;
            p.setColor(data.color);
        }
    }
}
