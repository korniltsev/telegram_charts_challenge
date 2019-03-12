package korniltsev.telegram.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.Arrays;


/*
plan
1. scroller + resizer
______
2. draw charts
3. animate charts scale
4. legend + rules + labels
 */




// todo
//     path animation
//     night mode animation
//     snap scrollbar near zeros
//     implement empty chart
//     implement y=0 chart

// todo design
//    overlay color seems wrong
//    lollipop bg gradient

// todo nice to have
//
//     support rtl since telegram supports
//     support split screen?
//     adjust theme for smooth transition
//     checkbox animations
//     static layout warmup / background init
//     nice app icon
//     spring animations?
//     animate velocity after dragging ??
//https://github.com/facebook/redex


//todo testing
//     requestLayaout during drag
//     requestLayaout during animation?
public class ChartView extends View {
    public static final String TAG = "tg.ch";
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean LOGGING = DEBUG;

    private final Paint debug_paint1;//todo static?
    private final Paint debug_paint_green;
    private final Paint debug_paint_red;
    private final Paint scroller_overlay_paint;
    private final Paint scroller_border_paint;

    private final int touchSlop;
    private final int resize_touch_area2;
    private final int initial_scroller_dith;



    private final int scrollbar_height;
    private final int scroll_bar_v_padding;
    private final int h_padding;

    private int scroller_width;
    private int scroller_pos = -1;
    private Rect scrollbar = new Rect();
    private int scroller_move_down_x;

    private UIColumnData[] data;

    public ChartView(Context context) {
        super(context);
        setWillNotDraw(false);
//        setBackgroundColor(0xffeeff00);
        scrollbar_height = dp(38);
        scroll_bar_v_padding = dp(8);
        h_padding = dp(16);

        initial_scroller_dith = scroller_width = dp(86);
        resize_touch_area2 = dp(20);

        debug_paint1 = new Paint();
        debug_paint1.setColor(0xffff0000);
        debug_paint_green = new Paint(Paint.ANTI_ALIAS_FLAG);
        debug_paint_green.setColor(0xff3CC23F);
        debug_paint_green.setStrokeWidth(dpf(1));

        debug_paint_red = new Paint(Paint.ANTI_ALIAS_FLAG);
        debug_paint_red.setColor(0xffED685F);

        scroller_overlay_paint = new Paint();
        scroller_overlay_paint.setColor(0xbff1f5f7);
        scroller_border_paint = new Paint();
        scroller_border_paint.setColor(0x334b87b4);

        //todo reread on configuration change , splitscreen
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

//        setLayerType(LAYER_TYPE_HARDWARE, null);

    }

    public void setData(ChartData data) {
        if (data.data.length <= 1) {
            //todo implement
            throw new AssertionError("data.data.length <= 1 unimplemented");
        }
        this.data = new UIColumnData[data.data.length-1];
        ColumnData[] data1 = data.data;
        int j = 0;
        for (int i = 0, data1Length = data1.length; i < data1Length; i++) {
            ColumnData datum = data1[i];
            if (datum.id.equals("x")) {
                continue;
            }
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(datum.color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dpf(1));
            this.data[j] = new UIColumnData(datum, paint);
            j++;
        }
    }

    private float dpf(int dip) {
//        todo inline & optimize for multiplication, not method call
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }

    private int dp(int dip) {
        //todo inline & optimize for multiplication, not method call
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (LOGGING) {
            Log.d("tg.chart", "onMeasure " + MeasureSpec.toString(widthMeasureSpec) + " " + MeasureSpec.toString(heightMeasureSpec));
        }

        int wsize = MeasureSpec.getSize(widthMeasureSpec);//todo inline
        int wmode = MeasureSpec.getMode(widthMeasureSpec);//todo inline
        if (wmode != MeasureSpec.EXACTLY && wmode != MeasureSpec.AT_MOST) {
            throw new AssertionError("wmode != MeasureSpec.EXACTLY || wmode != MeasureSpec.AT_MOST");
        }
        int chart_space = dp(300);
        int w;
        int h;
        w = wsize;
        int scrollbar_top = chart_space + this.scroll_bar_v_padding;
        scrollbar.left = h_padding;//todo replace with this fields
        scrollbar.right = w - h_padding;
        scrollbar.top = scrollbar_top;
        scrollbar.bottom = scrollbar_top + scrollbar_height;

        h = scrollbar_height + 2 * this.scroll_bar_v_padding + chart_space;


        setMeasuredDimension(w, h);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        if (LOGGING) {
            Log.d("tg.chart", "layout " + Arrays.asList(l, t, r, b));
        }
        //todo checks for resizing
        if (scroller_pos == -1) {
            scroller_pos = scrollbar.right - scroller_width;
        }
    }


    //<editor-fold desc="ScrollbarDragging">
    static final int DOWN_MOVE = 0;
    static final int DOWN_RESIZE_LEFT = 1;
    static final int DOWN_RESIZE_RIGHT = 2;
    float last_x = -1f;
    int resze_scroller_right = -1;
    int down_target = -1;
    boolean dragging;


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_MOVE) {
            if (LOGGING) Log.d("tg.chart", "touchevent " + event);
        }
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //todo check scroll edges
                boolean b = y >= scrollbar.top && y <= scrollbar.bottom;
                if (b) {
                    if (Math.abs(x - scroller_pos) <= resize_touch_area2) {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN resize left" );
                        last_x = x;
                        down_target = DOWN_RESIZE_LEFT;
                        resze_scroller_right = scroller_pos + scroller_width;
                        return true;
                    } else if (Math.abs(x - (scroller_pos + scroller_width) ) < resize_touch_area2) {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN resize right" );
                        last_x = x;
                        down_target = DOWN_RESIZE_RIGHT;
                        return true;
                    } else if (x >= scroller_pos && x <= scroller_pos + scroller_width) {
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN inside scrollbar" );
                        //todo check x inside scroller
                        last_x = x;
                        scroller_move_down_x = (int) (x - scroller_pos);
                        down_target = DOWN_MOVE;
                        return true;
                    } else {
                        //todo check scroll edges
                        if (LOGGING) Log.d("tg.chart", "touchevent DOWN miss" );
                    }
                } else {
                    if (LOGGING) Log.d("tg.chart", "touchevent DOWN miss" );
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //todo check pointer index
                if (last_x != -1f) {
                    if (dragging) {
                        last_x = x;
                        if (down_target == DOWN_MOVE) {

                            scroller_pos = (int) (x - scroller_move_down_x);
                            scroller_pos = Math.min(Math.max(scroller_pos, scrollbar.left), scrollbar.right - scroller_width);
                        } else if (down_target == DOWN_RESIZE_RIGHT) {
                            scroller_width = (int) (x - scroller_pos);
                            if (scroller_pos + scroller_width > scrollbar.right) {
                                scroller_width = scrollbar.right - scroller_pos;
                            }
                            // check the scrollbar is not too small
                            if (scroller_width < initial_scroller_dith  ) {
                                scroller_width = initial_scroller_dith;
                            }
                            invalidate();
                        } else if (down_target == DOWN_RESIZE_LEFT) {
                            scroller_pos = (int) x;
                            if (scroller_pos < scrollbar.left) {
                                scroller_pos = scrollbar.left;
                            }
                            scroller_width = resze_scroller_right - scroller_pos;
                            if (scroller_width < initial_scroller_dith  ) {
                                scroller_width = initial_scroller_dith;
                                scroller_pos = resze_scroller_right - scroller_width;
                            }
                            invalidate();
                        }

                        invalidate();
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
    //</editor-fold>

    @Override
    protected void onDraw(Canvas canvas) {
        long start = SystemClock.elapsedRealtimeNanos();
        drawScrollbar(canvas);
        long end = SystemClock.elapsedRealtimeNanos();
        long t = end - start;
        Log.d(TAG, "draw time " + t / 1000000f);
    }

    private void drawScrollbar(Canvas canvas) {
        int dip8 = dp(20);//todo
        int dip4 = dp(4);//todo
        int dip1 = dp(1);//todo
        int dip2 = dp(2);//todo


        // scrollbar charts
        if (data != null) {
            long max = data[0].data.maxValue;
            long min = data[0].data.minValue;
            //todo precompute min max
            //todo if max == min
            for (int i = 1, dataLength = data.length; i < dataLength; i++) {
                UIColumnData datum = data[i];
                min = Math.min(min, datum.data.minValue);
                max = Math.max(max, datum.data.maxValue);
            }
            float diff = max - min;
            int vspace = scrollbar.height() - 2 * dip2 /* 1 from top and bottom */;
            for (UIColumnData c : data) {
                ColumnData data = c.data;
                float x = scrollbar.left;
                float step = scrollbar.width() / (float)(data.values.length - 1);
                c.path.reset();
                float cur_value = (data.values[0] - min )/ diff;
                float cur_pos = scrollbar.bottom - dip2 - cur_value * vspace;
                c.path.moveTo(x, cur_pos);
                for (int i = 1; i < data.values.length - 1; ++i) {
                    x += step;
                    float next_value = (data.values[i] - min) / diff;
                    float next_pos = scrollbar.bottom - dip2 - next_value* vspace;
                    c.path.lineTo(x, next_pos);
                }
//                c.path.close();
                canvas.drawPath(c.path, c.paint);
            }

        }
        // scrollbar overlay
        if (scroller_pos != scrollbar.left) {
            canvas.drawRect(scrollbar.left, scrollbar.top, scroller_pos, scrollbar.bottom, scroller_overlay_paint);
        }
        if (scroller_pos != scrollbar.right - scroller_width) {
            canvas.drawRect(scroller_pos + scroller_width, scrollbar.top, scrollbar.right, scrollbar.bottom, scroller_overlay_paint);
        }
        // scrollbar border
        canvas.drawRect(scroller_pos, scrollbar.top, scroller_pos + dip4, scrollbar.bottom, scroller_border_paint);
        canvas.drawRect(scroller_pos + scroller_width - dip4, scrollbar.top, scroller_pos + scroller_width, scrollbar.bottom, scroller_border_paint);
        canvas.drawRect(scroller_pos + dip4, scrollbar.top, scroller_pos + scroller_width - dip4, scrollbar.top + dip1, scroller_border_paint);
        canvas.drawRect(scroller_pos + dip4, scrollbar.bottom-dip1, scroller_pos + scroller_width - dip4, scrollbar.bottom, scroller_border_paint);
    }

    public static class UIColumnData {
        final ColumnData data;
        final Paint paint;
        final Path path = new Path();//todo maybe precompute for cases when not animaing?


        public UIColumnData(ColumnData data, Paint paint) {
            this.data = data;
            this.paint = paint;
        }
    }
}
