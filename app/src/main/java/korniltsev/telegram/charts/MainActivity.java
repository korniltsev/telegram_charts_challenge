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
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean TRACE = BuildConfig.DEBUG && false;
    public static final boolean USE_RIPPLE = true;
    public static final boolean LOGGING = DEBUG;
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
    private List<ChartViewGL> charts = new ArrayList<>();
    private List<View> chartsRoots = new ArrayList<>();
    private MyContentRoot root;
    private FrameLayout contentFrame;
    private View sgadow;
    private View currentView;
    private ChartData[] data;

//    private View chartList;
//    private ArrayList<TextView> buttons;
//    private boolean chartVisible;
    private int textColor;
    private MyAnimation.Color textColorAnim;
    private List<CheckBox> checkboxes = new ArrayList<>();
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

        for (int i = 0, dataLength = data.length; i < dataLength; i++) {
            ChartData dataset = data[i];
            ChartData dataset1 = dataset;
            View chart = createChart(dataset1);
            list.addView(chart, MATCH_PARENT, WRAP_CONTENT);

        }
        scrollView.addView(list);
        mySetContentVie(scrollView);
        getWindow().getDecorView().setBackgroundDrawable(null);
    }


    private View createChart(ChartData datum) {
        FrameLayout.LayoutParams legendLP = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        TextView legend = new TextView(this);
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
//        LinearLayout checkboxlist = new LinearLayout(this);
//        checkboxlist.setOrientation(LinearLayout.VERTICAL);
        MyColorDrawable d = new MyColorDrawable(currentColorSet.lightBackground);
        ds.add(d);
//        checkboxlist.setBackgroundDrawable(d);
        for (int i = 0, data1Length = data1.length; i < data1Length; i++) {
            final ColumnData c = data1[i];
            if (c.id.equals(ChartData.COLUMN_ID_X)) {
                continue;
            }
            if (i != 1) {
                View divider = new View(this);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH_PARENT, dimen.dpi(1));
                lp.leftMargin = dimen.dpi(59);
                lp.gravity = Gravity.BOTTOM;
                divider.setBackgroundColor(currentColorSet.ruler);
                list.addView(divider, lp);
                dividers.add(divider);
            }
            CheckBox cb = new MyCheckBox(this, dimen);
            cb.setBackgroundDrawable(null);// remove ripples

                Drawable iccb = getResources().getDrawable(R.drawable.ic_checkbox).mutate();
                iccb.setColorFilter(c.color, PorterDuff.Mode.SRC_IN);
                cb.setButtonDrawable(iccb);
            int dip14 = dimen.dpi(14);
            cb.setPadding(dip14, 0, dip14, 0);
            cb.setTextColor(textColor);
            cb.setText(c.name);
            cb.setChecked(true);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    newChart.setChecked(c.id, isChecked);
                }
            });
            FrameLayout.LayoutParams cblp = new FrameLayout.LayoutParams(MATCH_PARENT, dimen.dpi(50));
            cblp.gravity = Gravity.BOTTOM;
//            cblp.leftMargin = dip14;
            cb.setLayoutParams(cblp);

            list.addView(cb);

            checkboxes.add(cb);

        }

//        list.addView(checkboxlist, MATCH_PARENT, WRAP_CONTENT);
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
                return super.isOpaque();
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
        if (animateUI) {

            root.animateColors(currentColorSet.statusbar, currentColorSet.darkBackground);
            bgToolbar.animate(currentColorSet.toolbar);
            for (MyColorDrawable d : ds) {
                d.animate(currentColorSet.lightBackground);
            }
        }
        if (animateChart) {

            for (int i = 0, chartsSize = charts.size(); i < chartsSize; i++) {
                ChartViewGL chart = charts.get(i);
                View root = chartsRoots.get(i);
                int top = root.getTop();
                if (LOGGING) Log.d("Chart", "top" + top);
                //todo do not animate if
                View parent = (View) root.getParent().getParent();
                if (top > parent.getHeight()) {
                    chart.animateToColors(currentColorSet, 0);
                } else {
                    chart.animateToColors(currentColorSet, MyAnimation.ANIM_DRATION);

                }
            }
        }
        if (animateUI) {

            h.removeCallbacksAndMessages(null);
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    imageButton.setBackgroundDrawable(createButtonBackground(currentColorSet.pressedButton, true));
                    if (Build.VERSION.SDK_INT >= 21) {
                        for (CheckBox cb : checkboxes) {
//                        cb.setBackgroundDrawable(createButtonBackground(currentColorSet.listButtonPressedColor, true));
                        }
                        ActivityManager.TaskDescription d = new ActivityManager.TaskDescription("Statistics", null, currentColorSet.toolbar);
                        setTaskDescription(d);
                    }
                }
            }, 300);
        }

        if (animateUI) {
            textColorAnim = new MyAnimation.Color(MyAnimation.ANIM_DRATION, textColor, currentColorSet.textColor);
            dividerAnim = new MyAnimation.Color(MyAnimation.ANIM_DRATION, dividerColor, currentColorSet.ruler);
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
        return null;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !USE_RIPPLE) {
//            StateListDrawable stateListDrawable = new StateListDrawable();
//            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
//            return stateListDrawable;
//        } else {
//            ColorDrawable maskDrawable = borderless ? null : new ColorDrawable(Color.RED);
//            return new RippleDrawable(ColorStateList.valueOf(pressedColor), null, maskDrawable);
//        }
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
//            int d = MyAnimation.ANIM_DRATION;
            int d = 0;
            alphaanim = new MyAnimation.Float(d, myalpha, checked ? 1f : 0f);
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



}
