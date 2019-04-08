package korniltsev.telegram.charts;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.gl.ChartViewGL;
import korniltsev.telegram.charts.ui.ColorSet;
import korniltsev.telegram.charts.ui.Dimen;
import korniltsev.telegram.charts.ui.MyAnimation;
import korniltsev.telegram.charts.ui.MyColorDrawable;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class MainActivity extends Activity {


    public static final String TAG = "tg.ch";
    public static final boolean DEBUG = false;
    public static final boolean TRACE = false;
    public static final boolean USE_RIPPLE = true;
    public static final boolean LOGGING = false;
    public static final boolean DIRTY_CHECK = true;
    public static final boolean LOG_FPS = false;


    ColorSet currentColorSet;


    private Dimen dimen;

    private LinearLayout toolbar;
    private MyColorDrawable bgToolbar;
    private TextView title;
    private ImageView imageButton;
    private List<ChartViewGL> charts = new ArrayList<>();
    private List<View> chartsRoots = new ArrayList<>();
    private MyContentRoot root;
    private FrameLayout contentFrame;
    private View sgadow;
    private View currentView;
    private List<ChartData> data;

//    private View chartList;
//    private ArrayList<TextView> buttons;
//    private boolean chartVisible;
    private int textColor;
    private MyAnimation.Color textColorAnim;
    private List<TextView> checkboxes = new ArrayList<>();
    private int dividerColor;
    private MyAnimation.Color dividerAnim;
    private List<View> dividers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
//            @Override
//            public void doFrame(long frameTimeNanos) {
//
//            }
//        });
        currentColorSet = ColorSet.DAY;
        textColor = currentColorSet.textColor;
        dividerColor = currentColorSet.ruler;
        dimen = new Dimen(this);

        data = readData();
        prepareRootView();

        ScrollView scrollView = new ScrollView(this);

        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0, dataLength = data.size(); i < dataLength; i++) {
            ChartData dataset = data.get(i);
            ChartData dataset1 = dataset;
            View chart = createChart(dataset1);
            LinearLayout.LayoutParams chartlp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
//            if (i != 0) {

//            }
            list.addView(chart, chartlp);

            View shadow = new View(this);
            shadow.setBackgroundResource(R.drawable.header_shadow);
            LinearLayout.LayoutParams shadowlp = new LinearLayout.LayoutParams(MATCH_PARENT, dimen.dpi(2));
            shadowlp.bottomMargin = dimen.dpi(30);
            list.addView(shadow, shadowlp);
        }
        scrollView.addView(list);
        mySetContentVie(scrollView);
        getWindow().getDecorView().setBackgroundDrawable(null);
    }


    private View createChart(ChartData datum) {
        FrameLayout.LayoutParams legendLP = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        final TextView legend = new TextView(this);
        legend.setPadding(dimen.dpi(16), dimen.dpi(16), 0, dimen.dpi(8));
        legend.setTextSize(16f);
        legend.setTextColor(currentColorSet.legendTitle);
        legend.setText("Followers");
        legend.setLayoutParams(legendLP);
//        MyColorDrawable d1 = new MyColorDrawable(currentColorSet.lightBackground);
//        ds.add(d1);
//        legend.setBackgroundDrawable(d1);


        final ChartViewGL newChart = new ChartViewGL(this, datum, dimen, currentColorSet);
        newChart.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        charts.add(newChart);


        ScrollView.LayoutParams listLP = new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        FrameLayout list = new FrameLayout(this){
            @Override
            public boolean isOpaque() {
                return true;
            }
        };
//        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(listLP);
        list.addView(newChart);
        list.addView(legend);



        ColumnData[] data1 = datum.data;
        LinearLayout checkboxlist = new LinearLayout(this);
        checkboxlist.setOrientation(LinearLayout.VERTICAL);
//        MyColorDrawable d = new MyColorDrawable(currentColorSet.lightBackground);
//        ds.add(d);
//        checkboxlist.setBackgroundDrawable(d);
        for (int i = 0, data1Length = data1.length; i < data1Length; i++) {
            final ColumnData c = data1[i];
            if (c.id.equals(ChartData.COLUMN_ID_X)) {
                continue;
            }
            if (i != 1) {
                View divider = new View(this){
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }


                };
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, dimen.dpi(ChartViewGL.CHECKBOX_DIVIDER_HIEIGHT));
                lp.leftMargin = dimen.dpi(56);
                lp.gravity = Gravity.BOTTOM;
                divider.setBackgroundColor(currentColorSet.ruler);
                checkboxlist.addView(divider, lp);
                dividers.add(divider);
            }
            MyCheckBox cb = new MyCheckBox(this, dimen, c.name, c.color);
            cb.setPadding(dimen.dpi(56), 0, dimen.dpi(32), 0);
            cb.setTextColor(textColor);
            cb.setText(c.name);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    newChart.setChecked(c.id, isChecked);
                }
            });
            LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(MATCH_PARENT, dimen.dpi(ChartViewGL.CHECKBOX_HEIGHT_DPI));
            cblp.gravity = Gravity.BOTTOM;
            cb.setLayoutParams(cblp);

            checkboxlist.addView(cb);

            checkboxes.add(cb);

        }

        FrameLayout.LayoutParams cblp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        cblp.gravity = Gravity.BOTTOM;
        list.addView(checkboxlist, cblp);
        chartsRoots.add(list);
        return list;
    }

    private void mySetContentVie(View v) {
        if (currentView != null) {
            contentFrame.removeView(currentView);
        }
        currentView = v;
        contentFrame.addView(v, 0);
    }

    private void prepareRootView() {
        final int toolbar_size = dimen.dpi(56);


        LinearLayout.LayoutParams titleLP = new LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT);
        titleLP.weight = 1;
        title = new TextView(this);
        title.setTextSize(20f);
        title.setText("Statistics");
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        title.setPadding(dimen.dpi(18), 0, dimen.dpi(18), 0);
        title.setLayoutParams(titleLP);

        LinearLayout.LayoutParams buttonLP = new LinearLayout.LayoutParams(toolbar_size, toolbar_size);
        imageButton = new ImageView(this);
        imageButton.setImageResource(R.drawable.ic_moon);
        imageButton.setScaleType(ImageView.ScaleType.CENTER);
        imageButton.setLayoutParams(buttonLP);
        imageButton.setClickable(true);
        imageButton.setBackgroundDrawable(createButtonBackground(currentColorSet.pressedButton, true));
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateTheme();
            }
        });


        LinearLayout.LayoutParams toolbarLP = new LinearLayout.LayoutParams(MATCH_PARENT, toolbar_size);
        bgToolbar = new MyColorDrawable(currentColorSet.toolbar);
        toolbar = new LinearLayout(this){
            @Override
            public boolean isOpaque() {
                return true;//a
            }
        };
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundDrawable(bgToolbar);
        toolbar.setLayoutParams(toolbarLP);
        toolbar.addView(title);
        toolbar.addView(imageButton);


        LinearLayout.LayoutParams frameLP = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        contentFrame = new FrameLayout(this);
        contentFrame.setLayoutParams(frameLP);
        sgadow = new View(this);
        sgadow.setBackgroundDrawable(getResources().getDrawable(R.drawable.header_shadow));
        contentFrame.addView(sgadow, MATCH_PARENT, dimen.dpi(3));

        root = new MyContentRoot(this, currentColorSet.statusbar, currentColorSet.darkBackground);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(toolbar, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        root.addView(contentFrame);


        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(root);
    }

    final Handler h = new Handler();

    private void animateTheme() {
//        trace(500);
        if (currentColorSet == ColorSet.DAY) {
            currentColorSet = ColorSet.NIGHT;
        } else {
            currentColorSet = ColorSet.DAY;
        }
        final boolean animateChart = true;
        final boolean animateUI = true;
        final boolean animateDayNight = Build.VERSION.SDK_INT >= 21;
        final int colorAnimationDuration;
        if (animateDayNight) {
            colorAnimationDuration = MyAnimation.ANIM_DRATION;
        } else {
            colorAnimationDuration = 0;
        }
        if (animateUI) {

            root.animateColors(currentColorSet.statusbar, currentColorSet.darkBackground, colorAnimationDuration);
            bgToolbar.animate(currentColorSet.toolbar, colorAnimationDuration);
        }
        if (animateChart) {

            for (int i = 0, chartsSize = charts.size(); i < chartsSize; i++) {
                ChartViewGL chart = charts.get(i);
                View root = chartsRoots.get(i);
                int top = root.getTop();
                int bottom = root.getBottom();
//                if (LOGGING) Log.d("Chart", "top" + top);
                //todo do not animate if
                ScrollView parent = (ScrollView) root.getParent().getParent();
                int height = parent.getHeight();
                int y = parent.getScrollY();
                top -= y;
                bottom -= y;
                if (bottom < 0 || top > height) {
//                    Log.d(TAG, "anim no op");
                    chart.animateToColors(currentColorSet, 0);
                } else {
//                    Log.d(TAG, "anim ");
                    chart.animateToColors(currentColorSet, colorAnimationDuration);
                }
//                boolean offscreen = top > parent.getHeight();
//
//                if (animateDayNight && !offscreen) {
//
//                } else {
//
//                }
            }
        }
        if (animateUI) {

            h.removeCallbacksAndMessages(null);
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    imageButton.setBackgroundDrawable(createButtonBackground(currentColorSet.pressedButton, true));
                    if (Build.VERSION.SDK_INT >= 21) {
                        ActivityManager.TaskDescription d = new ActivityManager.TaskDescription("Statistics", null, currentColorSet.toolbar);
                        setTaskDescription(d);
                    }
                }
            }, 300);
        }

        if (animateUI) {
            textColorAnim = new MyAnimation.Color(colorAnimationDuration, textColor, currentColorSet.textColor);
            dividerAnim = new MyAnimation.Color(colorAnimationDuration, dividerColor, currentColorSet.ruler);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    long t = SystemClock.uptimeMillis();
                    boolean post = false;
                    if (textColorAnim != null) {
                        textColor = textColorAnim.tick(t);
                        if (textColorAnim.ended) {
                            textColorAnim = null;
                        } else {
                            post = true;
                        }
                    }
                    if (dividerAnim != null) {
                        dividerColor = dividerAnim.tick(t);
                        if (dividerAnim.ended) {
                            dividerAnim = null;
                        } else {
                            post = true;
                        }
                    }
                    if (post) {
                        root.postOnAnimation(this);
                    }
                    for (int i = 0, dividersSize = dividers.size(); i < dividersSize; i++) {
                        View divider = dividers.get(i);
                        divider.setBackgroundColor(dividerColor);
                    }
//                for (int i = 0, buttonsSize = buttons.size(); i < buttonsSize; i++) {
//                    TextView button = buttons.get(i);
//                    button.setTextColor(textColor);
//                }
                    for (int i = 0, checkboxesSize = checkboxes.size(); i < checkboxesSize; i++) {
                        TextView button = checkboxes.get(i);
                        button.setTextColor(textColor);
                    }
                }
            };
            root.postOnAnimation(r);

        }

    }


    public List<ChartData> readData() {
        InputStream inputStream = getResources().openRawResource(R.raw.data);
        try {
            byte[] bytes = readAll(inputStream);
            String s = new String(bytes, "UTF-8");
            JSONArray o = new JSONArray(s);
            return ChartData.parseMany(o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
    }

    public static byte[] readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8 * 192];
        while (true) {
            int r = stream.read(buf);
            if (r == -1) {
                break;
            } else {
                baos.write(buf, 0, r);
            }
        }
        return baos.toByteArray();
    }


    public final Drawable createButtonBackground(int pressedColor, boolean borderless) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !USE_RIPPLE) {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
            return stateListDrawable;
        } else {
            ColorDrawable maskDrawable = borderless ? null : new ColorDrawable(Color.RED);
            return new RippleDrawable(ColorStateList.valueOf(pressedColor), null, maskDrawable);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        for (ChartViewGL chart : charts) {
            chart.invalidateRender();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ChartViewGL c : charts) {
            c.release();
        }
    }

    public static class MyContentRoot extends LinearLayout {
        public static final boolean USE_INSETS = Build.VERSION.SDK_INT >= 21;
        private final Paint paintStatus;
        private final Paint paintBg;
        private MyAnimation.Color animStatus;
        private MyAnimation.Color animBg;
        int colorStatusbar;
        int colorBackground;

        public MyContentRoot(Context context, int colorStatusbar, int colorBackground) {
            super(context);
//            setFitsSystemWindows(true);
            setWillNotDraw(false);
            this.colorBackground = colorBackground;
            this.colorStatusbar = colorStatusbar;
            paintStatus = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintStatus.setColor(colorStatusbar);

            paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintBg.setColor(colorBackground);
        }

        @Override
        public boolean isOpaque() {
            return true;
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            if (USE_INSETS) {
                int systemWindowInsetTop = insets.getSystemWindowInsetTop();
                this.setPadding(insets.getSystemWindowInsetLeft(),
                        systemWindowInsetTop,
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());

                return insets.consumeSystemWindowInsets();
            } else {
                return insets;
            }
        }

        public void animateColors(int status, int bg, int colorAnimationDuration) {
            animStatus = new MyAnimation.Color(colorAnimationDuration, this.colorStatusbar, status);
            animBg = new MyAnimation.Color(colorAnimationDuration, this.colorBackground, bg);
            invalidate();
        }


        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            long time = -1;
            boolean invalidated = false;
            if (animBg != null) {
                time = SystemClock.uptimeMillis();
                colorBackground = animBg.tick(time);
                paintBg.setColor(colorBackground);
                if (animBg.ended) {
                    animBg = null;
                }
                invalidated = true;
                invalidate();
            }
            if (animStatus != null) {
                if (time == -1) {
                    time = SystemClock.uptimeMillis();
                }
                colorStatusbar = animStatus.tick(time);
                paintStatus.setColor(colorStatusbar);
                if (animStatus.ended) {
                    animStatus = null;
                }
                if (!invalidated) {
                    invalidate();
                }
            }
            if (USE_INSETS) {
                int top = getPaddingTop();
                canvas.drawRect(0, 0, getWidth(), top, paintStatus);
                canvas.drawRect(0, top, getWidth(), getHeight(), paintBg);
            } else {
                canvas.drawRect(0, 0, getWidth(), getHeight(), paintBg);
            }
        }
    }

    private static class MyCheckBox extends TextView {
        private  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Dimen dimen;
        private final float l;
        private final float t;
        private final float b;
        private final float r;
//        private final ImageView img;
//        private final TextView text;
        private final Drawable icchecked;
        private final Drawable icNonChecked;
        private Drawable ic;
        private float myalpha;
        private MyAnimation.Float alphaanim;
        private final Handler h = new Handler(Looper.getMainLooper());
        private MyAnimationTick action;
        private CompoundButton.OnCheckedChangeListener listener;

        public MyCheckBox(Context c, Dimen dimen, String str, int tint) {
            super(c);
            this.dimen = dimen;
            p.setColor(Color.WHITE);

            myalpha = 1f;
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setChecked(!checked);
                }
            });
//            int icl = dimen.dpi()
            icchecked = getResources().getDrawable(R.drawable.abc_btn_check_to_on_mtrl_015).mutate();
            icNonChecked = getResources().getDrawable(R.drawable.abc_btn_check_to_on_mtrl_000).mutate();
            int icw = icchecked.getIntrinsicWidth();
            int ich = icchecked.getIntrinsicHeight();
            int icl = dimen.dpi(28) - icw / 2;
            int ict = dimen.dpi(25) - ich / 2;
            icchecked.setBounds(icl, ict, icl + icw, ict + ich);
            icNonChecked.setBounds(icl, ict, icl + icw, ict + ich);
            int pad = dimen.dpi(8);
            l = icchecked.getBounds().left + pad;
            t = icchecked.getBounds().top + pad;
            b = icchecked.getBounds().bottom - pad;
            r = icchecked.getBounds().right- pad;


            icchecked.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
            icNonChecked.setColorFilter(tint, PorterDuff.Mode.SRC_IN);

            ic = icchecked;
            setText(str);
            setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
//            setBackgroundColor(Color.RED);
        }
        boolean checked = true;

        //        @Override
        public void setChecked(boolean checked) {
            this.checked = checked;
            ic = checked ? icchecked : icNonChecked;
            if (action != null) {
                action.canceled = true;
            }
//            int d = MyAnimation.ANIM_DRATION;
            int d = 0;
            alphaanim = new MyAnimation.Float(d, myalpha, checked ? 1f : 0f);
            action = new MyAnimationTick();
            postOnAnimation(action);
            listener.onCheckedChanged(null, checked);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (myalpha != 0f) {
                canvas.drawRect(l, t, r, b, p);
            }
            ic.draw(canvas);
            super.onDraw(canvas);
        }

        public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
            this.listener = onCheckedChangeListener;
        }

        private class MyAnimationTick implements Runnable {
            boolean canceled = false;
            @Override
            public void run() {
                if (canceled) {
                    return;
                }
                if (alphaanim != null) {
                    myalpha = alphaanim.tick(SystemClock.uptimeMillis());
                    p.setAlpha((int) (myalpha * 255));
                    invalidate();
                    if (alphaanim.ended) {
                        alphaanim = null;
                    } else {

                        postOnAnimation(this);
                    }
                }
            }
        }
    }



}
