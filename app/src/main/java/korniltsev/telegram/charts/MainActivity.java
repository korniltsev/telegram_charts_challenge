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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
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
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean TRACE = BuildConfig.DEBUG && false;
    public static final boolean USE_RIPPLE = true;
    public static final boolean LOGGING = DEBUG;
    public static final int DATASET = 4;
    public static final boolean DIRTY_CHECK = true;
    public static final boolean LOG_FPS = true;

    //    private MyColorDrawable bgRoot;
    private ArrayList<MyColorDrawable> ds = new ArrayList<>();

    ColorSet currentColorSet;


    private Dimen dimen;

    private LinearLayout toolbar;
    private MyColorDrawable bgToolbar;
    private TextView title;
    private ImageView imageButton;
    private ChartViewGL chart_;
    private MyContentRoot root;
    private FrameLayout contentFrame;
    private View sgadow;
    private View currentView;
    private ChartData[] data;

    private View chartList;
    private ArrayList<TextView> buttons;
    private boolean chartVisible;
    private int textColor;
    private MyAnimation.Color textColorAnim;
    private List<CheckBox> checkboxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentColorSet = ColorSet.DAY;
        textColor = currentColorSet.textColor;
        dimen = new Dimen(this);

//        long t1 = SystemClock.elapsedRealtimeNanos();
        data = readData();//todo make async
//        long t2 = SystemClock.elapsedRealtimeNanos();
//        Log.d(TAG, "reading " + (t2 - t1)/1000000f);

        ChartData datum = data[DATASET];
//        ChartData datum = new ChartData(new ColumnData[]{
//                new ColumnData("x", "x", new long[]{1,2,3}, "line",  MyColor.red),
//                new ColumnData("Y1", "Y1", new long[]{5, 10, 5}, "line",  MyColor.red),
//                new ColumnData("Y2", "Y2", new long[]{2, 20, 2}, "line",  MyColor.green),
//        });


        prepareRootView();

        createChartList();
//        createChart(datum);


    }

    private void createChartList() {
        if (chartList == null) {

            ScrollView.LayoutParams listLP = new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setLayoutParams(listLP);
            list.setPadding(0, dimen.dpi(8), 0, dimen.dpi(8));
            MyColorDrawable background = new MyColorDrawable(currentColorSet.lightBackground);
            ds.add(background);
            list.setBackgroundDrawable(background);
            TypedValue outValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

            buttons = new ArrayList<>();
            for (int i = 0; i < 5; i++) {

                TextView v = new TextView(this);
                v.setTextColor(textColor);
                v.setText("Chart " + (i + 1));
                v.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                v.setPadding(dimen.dpi(16), 0, 0, 0);
                v.setBackgroundDrawable(createButtonBackground(currentColorSet.listButtonPressedColor, false));
                v.setClickable(true);
//                v.setClip
//                v.set

//                v.setBackgroundResource(outValue.resourceId);
                final int finalI = i;
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showChart(finalI);

                    }
                });
                list.addView(v, MATCH_PARENT, dimen.dpi(50));
                buttons.add(v);
            }

            View shadow = new View(this);
            shadow.setBackgroundResource(R.drawable.header_shadow);
//            list.addView(shadow, MATCH_PARENT, WRAP_CONTENT);
//        list.addView(legend);
//        list.addView(chart_);

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(list, MATCH_PARENT, WRAP_CONTENT);
            container.addView(shadow, MATCH_PARENT, WRAP_CONTENT);


            ScrollView scrollView = new ScrollView(this);
            scrollView.addView(container);
            scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            chartList = scrollView;
        }
        mySetContentVie(chartList);

    }

    private void showChart(int finalI) {
        chartVisible = true;
        createChart(data[finalI]);
    }

    private void createChart(ChartData datum) {
        LinearLayout.LayoutParams legendLP = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        TextView legend = new TextView(this);
        legend.setPadding(dimen.dpi(16), dimen.dpi(8), 0, dimen.dpi(8));
        legend.setTextSize(16f);
        legend.setTextColor(currentColorSet.legendTitle);
        legend.setText("Followers");
        legend.setLayoutParams(legendLP);
        MyColorDrawable d1 = new MyColorDrawable(currentColorSet.lightBackground);
        ds.add(d1);
        legend.setBackgroundDrawable(d1);


        chart_ = new ChartViewGL(this, datum, dimen, currentColorSet);
        chart_.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));


        ScrollView.LayoutParams listLP = new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(listLP);
        list.addView(legend);
        list.addView(chart_);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(list);

        ColumnData[] data1 = datum.data;
        LinearLayout checkboxlist = new LinearLayout(this);
        checkboxlist.setOrientation(LinearLayout.VERTICAL);
        MyColorDrawable d = new MyColorDrawable(currentColorSet.lightBackground);
        ds.add(d);
        checkboxlist.setBackgroundDrawable(d);
        for (int i = 0, data1Length = data1.length; i < data1Length; i++) {
            final ColumnData c = data1[i];
            if (c.id.equals(ChartData.COLUMN_ID_X)) {
                continue;
            }
            CheckBox cb = new MyCheckBox(this, dimen);
//            cb.setBackgroundDrawable(createButtonBackground(currentColorSet.listButtonPressedColor, true));
            if (Build.VERSION.SDK_INT >= 21) {
                cb.setButtonTintList(new ColorStateList(new int[][]{
                                new int[]{android.R.attr.state_checked},
                                new int[]{},
                        }, new int[]{
                                c.color,
                                c.color,
                        })
                );
            } else {
                Drawable iccb = getResources().getDrawable(R.drawable.ic_checkbox).mutate();
                iccb.setColorFilter(c.color, PorterDuff.Mode.SRC_IN);
                cb.setButtonDrawable(iccb);
//                cb.setCompoundDrawablesWithIntrinsicBounds(iccb, null, null, null);
            }
            int dip18 = dimen.dpi(18);
            cb.setPadding(dip18, 0, dip18, 0);
//            cb.setComp
            cb.setTextColor(textColor);
//            MyColorDrawable d = ;
//            ds.add(d);
//            cb.setBackgroundDrawable(d);
            cb.setText(c.name);
            cb.setChecked(true);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    chart_.setChecked(c.id, isChecked);
                }
            });
            LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(MATCH_PARENT, dimen.dpi(50));
            cblp.leftMargin = dip18;
            cb.setLayoutParams(cblp);

            checkboxlist.addView(cb);

            checkboxes.add(cb);

        }

        View shadow = new View(this);
        shadow.setBackgroundResource(R.drawable.header_shadow);
        list.addView(checkboxlist, MATCH_PARENT, WRAP_CONTENT);
        list.addView(shadow, MATCH_PARENT, WRAP_CONTENT);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mySetContentVie(scrollView);
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
        toolbar = new LinearLayout(this);
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
        if (Build.VERSION.SDK_INT >= 21) {
            root.animateColors(currentColorSet.statusbar, currentColorSet.darkBackground);
            //todo replace with insets and color animatione
//            getWindow().setStatusBarColor(currentColorSet.statusbar);
        }
        bgToolbar.animate(currentColorSet.toolbar);
        for (MyColorDrawable d : ds) {
            d.animate(currentColorSet.lightBackground);
        }
        if (chart_ != null) {
            chart_.animateToColors(currentColorSet);
        }
        h.removeCallbacksAndMessages(null);
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                imageButton.setBackgroundDrawable(createButtonBackground(currentColorSet.pressedButton, true));
                for (TextView button : buttons) {
                    button.setBackgroundDrawable(createButtonBackground(currentColorSet.listButtonPressedColor, false));
                }
                if (Build.VERSION.SDK_INT >= 21) {
                    for (CheckBox cb : checkboxes) {
                        cb.setBackgroundDrawable(createButtonBackground(currentColorSet.listButtonPressedColor, true));
                    }
                    ActivityManager.TaskDescription d = new ActivityManager.TaskDescription("Statistics", null, currentColorSet.toolbar);
                    setTaskDescription(d);
                }
            }
        }, 300);


        textColorAnim = new MyAnimation.Color(textColor, currentColorSet.textColor);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                long t = SystemClock.uptimeMillis();
                if (textColorAnim != null) {
                    textColor = textColorAnim.tick(t);
                    if (textColorAnim.ended) {
                        textColorAnim = null;
                    } else {
                        root.postOnAnimation(this);
                    }
                }
                for (TextView button : buttons) {
                    button.setTextColor(textColor);
                }
                for (TextView button : checkboxes) {
                    button.setTextColor(textColor);
                }
            }
        };
        root.postOnAnimation(r);
    }

    private void trace(int delayMillis) {
        if (TRACE) {
            File filesDir = getFilesDir();
            File trace = new File(filesDir, "trace");
            Debug.startMethodTracing(trace.getAbsolutePath(), 1024 * 1024 * 10);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Debug.stopMethodTracing();
                }
            }, delayMillis);
        }
    }

    public ChartData[] readData() {
        InputStream inputStream = getResources().openRawResource(R.raw.data);
        try {
            byte[] bytes = readAll(inputStream);
            String s = new String(bytes, "UTF-8");
            JSONArray o = new JSONArray(s);
            return ChartData.parse(o);
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
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (chart_ != null && chartVisible) {
            chartVisible = false;
            mySetContentVie(chartList);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //todo stop & destroy chart
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

        public void animateColors(int status, int bg) {
            animStatus = new MyAnimation.Color(MyAnimation.ANIM_DRATION, this.colorStatusbar, status);
            animBg = new MyAnimation.Color(MyAnimation.ANIM_DRATION, this.colorBackground, bg);
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

    private static class MyCheckBox extends CheckBox {
        private  final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Dimen dimen;
        private final float l;
        private final float t;
        private final float b;
        private final float r;
        private float myalpha;
        private MyAnimation.Float alphaanim;
        private final Handler h = new Handler(Looper.getMainLooper());
        private MyAnimationTick action;

        public MyCheckBox(Context c, Dimen dimen) {
            super(c);
            this.dimen = dimen;
            p.setColor(Color.WHITE);
            l = dimen.dpf(8);
            t = dimen.dpf(18);
            b = t + dimen.dpf(14);
            r = l + dimen.dpf(16);
            myalpha = 1f;
        }

        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            if (action != null) {
                action.canceled = true;
            }
            alphaanim = new MyAnimation.Float(MyAnimation.ANIM_DRATION, myalpha, checked ? 1f : 0f);
            action = new MyAnimationTick();
            postOnAnimation(action);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (myalpha != 0f) {
                canvas.drawRect(l, t, r, b, p);
            }
            super.onDraw(canvas);
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
//
//    private static class MyTextView extends TextView {
//        private int color;
//        private MyAnimation.Color colorAnim;
//
//        public MyTextView(Context ctx, ColorSet currentColorSet) {
//            super(ctx);
//            setTextColor(currentColorSet.textColor);
//            color = currentColorSet.textColor;
//        }
//
//        public void animate(ColorSet cs) {
//            colorAnim = new MyAnimation.Color(color, cs.textColor);
//            invalidate();
//        }
//
//        @Override
//        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
//            if (colorAnim != null) {
//                color = colorAnim.tick(SystemClock.uptimeMillis());
//                setTextColor(color);
//                if (colorAnim.ended) {
//                    colorAnim = null;
//                }
//            }
//        }
//    }

}
