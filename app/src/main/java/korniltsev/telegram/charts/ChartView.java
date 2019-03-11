package korniltsev.telegram.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.Arrays;


/*
plan
1. scroller + resizer
2. draw charts
3. animate charts scale
4. legend + rules + labels
 */




// todo
//     path animation
//     night mode animation
//     snap scrollbar near zeros

// todo design
//    overlay color seems wrong

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

public class ChartView extends View {
    private final Paint debug_paint1;//todo static
    private final Paint debug_paint_green;
    private final Paint debug_paint_red;
    private final Paint scroller_overlay_paint;
    private final Paint scroller_border_paint;

    private final int touchSlop;


    private int w;
    private int h;

    private final int scrollbar_height;
    private final int scroll_bar_v_padding;
    private final int h_padding;

    private int scroller_width;
    private int scroller_pos = -1;
    private Rect scrollbar = new Rect();

    public ChartView(Context context) {
        super(context);
        setWillNotDraw(false);
//        setBackgroundColor(0xffeeff00);
        scrollbar_height = dp(38);
        scroll_bar_v_padding = dp(8);
        h_padding = dp(16);

        scroller_width = dp(86);

        debug_paint1 = new Paint();
        debug_paint1.setColor(0xffff0000);
        debug_paint_green = new Paint(Paint.ANTI_ALIAS_FLAG);
        debug_paint_green.setColor(0xff3CC23F);
        debug_paint_red = new Paint(Paint.ANTI_ALIAS_FLAG);
        debug_paint_red.setColor(0xffED685F);

        scroller_overlay_paint = new Paint();
        scroller_overlay_paint.setColor(0xbff1f5f7);
        scroller_border_paint = new Paint();
        scroller_border_paint.setColor(0x334b87b4);

        //todo reread on configuration change , splitscreen
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    }

    private int dp(int dip) {
        //todo inline
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
        int chart_space = dp(100);
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

//    float scroller_down_x = -1f;
//    float scroller_down_y = -1f;
    float last_x = -1f;
//    float last_y = -1f;
    //    float lastx = -1f;
    boolean dragging;


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (LOGGING) Log.d("tg.chart", "touchevent " + event);
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //todo check scroll edges
                if (y >= scrollbar.top && y <= scrollbar.bottom
                        && x >= scroller_pos && x <= scroller_pos + scroller_width) {
                    if (LOGGING) Log.d("tg.chart", "touchevent DOWN inside scrollbar" );
                    //todo check x inside scroller
                    last_x = x;
                    return true;
//                    scroller_down_y = y;
                } else {
                    //todo check scroll edges
                    if (LOGGING) Log.d("tg.chart", "touchevent DOWN miss" );
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //todo check pointer index
                if (LOGGING) Log.d("tg.chart", "touchevent MOVE " );
                if (last_x != -1f) {
                    if (LOGGING) Log.d("tg.chart", "touchevent MOVE 2" );

                    if (dragging) {
                        if (LOGGING) Log.d("tg.chart", "touchevent MOVE dragging" );
                        float move = x - last_x;
                        last_x = x;
                        scroller_pos += move;
                        scroller_pos = Math.min(Math.max(scroller_pos, scrollbar.left), scrollbar.right - scroller_width);

                        invalidate();
                        return true;
                    } else {
                        float move = x - last_x;
                        if (Math.abs(move) > touchSlop) {
                            if (LOGGING) Log.d("tg.chart", "touchevent DMOVE start dragging" );
                            dragging = true;
                            last_x = x;
//                            last_y = y;
//                            scroller_pos += move;
//                            scroller_pos = Math.min(Math.max(scroller_pos, scrollbar.left), scrollbar.right - scroller_width);
//                            invalidate();
                            return true;
                        } else {
                            if (LOGGING) Log.d("tg.chart", "touchevent DMOVE ..." );
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
//                scroller_down_x = -1;
                last_x = -1;
//                scroller_down_y = -1;
                dragging = false;
                break;
            default:
                break;
        }
        return false;
    }



//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event) {
//        todo draw debug info over charts
//
//        return super.dispatchTouchEvent(event);
//    }

    public static final boolean LOGGING = true;

    @Override
    protected void onDraw(Canvas canvas) {
//        canvas.save();
        if (LOGGING) {
            Log.d("tg.chart", String.format("scroller (%d %d) ", scroller_pos, scroller_width));
            Log.d("tg.chart", String.format("scrollbar %s ", scrollbar));
            Log.d("tg.chart", String.format("w h %d %d", getWidth(), getHeight()));
        }

        int dip8 = dp(20);
        int dip4 = dp(4);
        int dip1 = dp(1);

        canvas.drawRect(scrollbar.left, scrollbar.top + dip8, scrollbar.left + scrollbar.width()/2, scrollbar.bottom - dip8, debug_paint_green);
        canvas.drawRect(scrollbar.left + scrollbar.width()/2, scrollbar.top + dip8, scrollbar.right, scrollbar.bottom - dip8, debug_paint_red);

        if (scroller_pos != scrollbar.left) {
            canvas.drawRect(scrollbar.left, scrollbar.top, scroller_pos, scrollbar.bottom, scroller_overlay_paint);
        }
        if (scroller_pos != scrollbar.right - scroller_width) {
            canvas.drawRect(scroller_pos + scroller_width, scrollbar.top, scrollbar.right, scrollbar.bottom, scroller_overlay_paint);
        }

        canvas.drawRect(scroller_pos, scrollbar.top, scroller_pos + dip4, scrollbar.bottom, scroller_border_paint);
        canvas.drawRect(scroller_pos + scroller_width - dip4, scrollbar.top, scroller_pos + scroller_width, scrollbar.bottom, scroller_border_paint);
        canvas.drawRect(scroller_pos + dip4, scrollbar.top, scroller_pos + scroller_width - dip4, scrollbar.top + dip1, scroller_border_paint);
        canvas.drawRect(scroller_pos + dip4, scrollbar.bottom-dip1, scroller_pos + scroller_width - dip4, scrollbar.bottom, scroller_border_paint);
//        canvas.restore();
    }
}
