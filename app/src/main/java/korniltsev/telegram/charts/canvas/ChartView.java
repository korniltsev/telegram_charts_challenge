package korniltsev.telegram.charts.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
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

/*
 [bp] chart zoom
 [bp] animate chart (alpha + min-max (global)
 [bp] animate chart (min-max viewport)


    Глобальный план:
    - срочно решить, opengl или канвас
    - реализовать все без бонусов, потом ебашить бонусы

 */
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
    private float zoom_left;
    private float zoom_right;
    private float zoom_scale;
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
        UIColumnData foundScroller = null;
        UIColumnData foundChart = null;
        for (UIColumnData c : scroller) {
            if (id.equals(c.data.id)) {
                foundScroller = c;
                c.checked = isChecked;
                break;
            }
        }
        for (UIColumnData c : chart) {
            if (id.equals(c.data.id)) {
                foundChart = c;
                c.checked = isChecked;
                break;
            }
        }
        int checkedCount = 0;
        for (UIColumnData c : chart) {
            if (c.checked) {
                checkedCount++;
            }
        }
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (UIColumnData c : chart) {
            if (c.checked) {
                min = Math.min(min, c.data.min);
                max = Math.max(max, c.data.max);
            }
        }
        for (UIColumnData c : scroller) {
            if (c == foundScroller) {
                if (c.alpha == 0f) {
                    c.animateMinMax(min, max, false);
                } else {
                    if (isChecked) {
                        c.animateMinMax(min, max, true);
                    }
                }
                c.animateAlpha(isChecked);
            } else {
                c.animateMinMax(min, max, true);
            }
        }
        for (UIColumnData c : chart) {
            if (c == foundChart) {
                if (!isChecked && checkedCount == 0) {

                } else if (c.alpha == 0f) {
                    c.animateMinMax(min, max, false);
                } else {
                    c.animateMinMax(min, max, true);
                }
                c.animateAlpha(isChecked);
            } else {
                c.animateMinMax(0, max, true);
            }
        }
        invalidate();
    }


    public static int counter;

    public Rect scrollbar = new Rect();
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
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                boolean scrollbar = y >= this.scrollbar.top && y <= this.scrollbar.bottom;
                if (scrollbar) {
                    if (Math.abs(x - scroller_left) <= resize_touch_area2) {
                        last_x = x;
                        down_target = DOWN_RESIZE_LEFT;
                        return true;
                    } else if (Math.abs(x - (scroller__right)) < resize_touch_area2) {
                        last_x = x;
                        down_target = DOWN_RESIZE_RIGHT;
                        return true;
                    } else if (x >= scroller_left && x <= scroller__right) {
                        last_x = x;
                        scroller_move_down_x = (int) (x - scroller_left);
                        scroller_move_down_width = scroller__right - scroller_left;
                        down_target = DOWN_MOVE;
                        return true;
                    } else {
                    }
                } else {
                    boolean chart = y >= this.chartTop && y <= this.chartBottom;
                    if (chart) {
                        last_x = x;
                        down_target = DOWN_TOOLTIP;
                        return true;
                    } else {
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
                        } else if (down_target == DOWN_RESIZE_LEFT) {
                            scroller_left = (int) x;
                            if (scroller_left < this.scrollbar.left) {
                                scroller_left = this.scrollbar.left;
                            }
                            if (scroller__right - scroller_left < initial_scroller_dith) {
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


    public final void setOverlayPos(boolean init) {
        final float left = (float) (scroller_left - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float right = (float) (scroller__right - scrollbar.left) / (scrollbar.right - scrollbar.left);
        final float scale = (right - left);
        this.zoom_left = left;
        this.zoom_right = right;
        this.zoom_scale = scale;
    }

    public final long calculateMax(float left, float right) {
        long max = -1;
        return max;
    }

    public ColorSet currentColors;

    public void animateToColors(final ColorSet colors, final long duration) {
        //todo
    }


    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long t = SystemClock.uptimeMillis();
        boolean dirty = false;
        for (int i = 0; i < scroller.size(); i++) {
            UIColumnData columnData = scroller.get(i);
            boolean tick = columnData.tick(t);
            dirty = dirty || tick;
        }
        for (int i = 0; i < chart.size(); i++) {
            UIColumnData columnData = chart.get(i);
            boolean tick = columnData.tick(t);
            dirty = dirty || tick;
        }
        if (dirty) {
            invalidate();
        }

//        canvas.drawRect(scrollbar.left, scrollbar.top, scrollbar.right, scrollbar.bottom, p);
        float dip2 = dimen.dpf(2);
        float dip1 = dimen.dpf(1);
        float dip4 = dimen.dpf(4);
//        canvas.drawRect(scrollbar.left, chartTop, scrollbar.right, chartBottom, p2);

        drawChart(canvas);

        drawScrollbar(canvas);

//         draw scrollbar overlay
        canvas.drawRect(scrollbar.left, scrollbar.top, scroller_left, scrollbar.bottom, pOverlay);
        canvas.drawRect(scroller__right, scrollbar.top, scrollbar.right, scrollbar.bottom, pOverlay);
        canvas.drawRect(scroller_left, scrollbar.top, scroller_left + dip4, scrollbar.bottom, pRuler);
        canvas.drawRect(scroller__right - dip4, scrollbar.top, scroller__right, scrollbar.bottom, pRuler);
        canvas.drawRect(scroller_left + dip4, scrollbar.top, scroller__right - dip4, scrollbar.top + dip1, pRuler);
        canvas.drawRect(scroller_left + dip4, scrollbar.bottom - dip1, scroller__right - dip4, scrollbar.bottom, pRuler);
        //todo

    }

    private void drawChart(Canvas canvas) {
        float vspace = chartUsefullHiehgt;
        for (int i1 = 0; i1 < chart.size(); i1++) {
            UIColumnData c = chart.get(i1);
            if (c.alpha == 0f) {
                continue;
            }
            int l = scrollbar.left;
            int w = scrollbar.width();
            int b = this.chartBottom;
            float h = vspace;
            calcChart(l, w, b, h, c, true);
            canvas.drawLines(c.pts, c.p);
        }
    }

    private static void calcChart(float left, float w, float bottom, float vspace, UIColumnData c, boolean minZero) {
        float[] pts = c.pts;
        float step = w / (float) (c.data.values.length - 1);
        float x = left;
        final float min;
        if (minZero) {
            min = 0;
        } else {
            min = c.min;
        }
        float diff = c.max - min;
        long[] values = c.data.values;
        float cur_value = (values[0] - min) / diff;
        float cur_pos = bottom - cur_value * vspace;
        int j = 0;
        for (int i = 1; i < values.length; i++) {
            float next_value = (values[i] - min) / diff;
            float next_pos = bottom - next_value * vspace;
            pts[j] = x;
            pts[j + 1] = cur_pos;
            pts[j + 2] = x + step;
            pts[j + 3] = next_pos;
            x += step;
            cur_value = next_value;
            cur_pos = next_pos;
            j += 4;
        }
    }

    private void drawScrollbar(Canvas canvas) {
        float dip2 = dimen.dpf(2);
        float dip1 = dimen.dpf(1);
        for (int i1 = 0; i1 < scroller.size(); i1++) {
            UIColumnData c = scroller.get(i1);
            if (c.alpha == 0f) {
                continue;
            }

            float l = scrollbar.left + dip1;
            float w = scrollbar.width() - dip2;
            float b = scrollbar.bottom - dip1;
            float h = scrollbar.height() - 2 * dip2;
            calcChart(l, w, b, h, c, false);
            canvas.drawLines(c.pts, c.p);
        }
    }

    public static class UIColumnData {
        public final ColumnData data;
        public float max;
        public float min;
        public final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        public boolean checked = true;
        public float alpha = 1f;
        public MyAnimation.Float minAnim;
        public MyAnimation.Float maxAnim;
        public MyAnimation.Float alphaAnim;
        float[] pts;


        public UIColumnData(ColumnData data) {
            this.data = data;
            p.setColor(data.color);
            pts = new float[4 * (data.values.length - 1)];//todo no need for multiple buffers, may use one!
        }

        public boolean tick(long t) {
            boolean invalidate = false;
            if (minAnim != null) {
                min = minAnim.tick(t);
                if (minAnim.ended) {
                    minAnim = null;
                } else {
                    invalidate = true;
                }
            }
            if (maxAnim != null) {
                max = maxAnim.tick(t);
                if (maxAnim.ended) {
                    maxAnim = null;
                } else {
                    invalidate = true;
                }
            }
            if (alphaAnim != null) {
                alpha = alphaAnim.tick(t);
                p.setAlpha((int) (alpha * 255));
                if (alphaAnim.ended) {
                    alphaAnim = null;
                } else {
                    invalidate = true;
                }
            }
            return invalidate;
        }

        public void animateMinMax(long min, long max, boolean animate) {
            if (animate) {
                minAnim = new MyAnimation.Float(192, this.min, min);
                maxAnim = new MyAnimation.Float(192, this.max, max);
            } else {
                this.min = min;
                this.max = max;
                minAnim = null;
                maxAnim = null;
            }
        }

        public void animateAlpha(boolean isChecked) {
            alphaAnim = new MyAnimation.Float(192, alpha, isChecked ? 1f : 0f);
        }
    }
}
