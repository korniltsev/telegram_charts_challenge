package korniltsev.telegram.charts;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
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
    public static final boolean LOGGING = DEBUG;
    public static final int DATASET = 0;
    public static final boolean DIRTY_CHECK = false;
    public static final boolean LOG_FPS = true;

    //    private MyColorDrawable bgRoot;
    private ArrayList<MyColorDrawable> ds = new ArrayList<>();

    ColorSet currentColorSet = ColorSet.DAY;

//todo first animation is SLOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private Dimen dimen;
    private LinearLayout toolbar;
    private MyColorDrawable bgToolbar;
    private TextView title;
    private ImageView imageButton;
    private ChartViewGL chart_;
    private MyContentRoot root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ChartData[] data = readData();
//        if (LOGGING) Log.d(TAG, "data len " + data.length);

        ChartData datum = data[DATASET];
        if (DEBUG) {
//            ColumnData vs = datum.data[datum.data.length - 1];
//            vs.minValue = 0;
//            vs.values[vs.values.length - 1] = 0;
        }

        dimen = new Dimen(this);

//        bgRoot = new MyColorDrawable(currentColorSet.darkBackground, false);
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
        imageButton.setBackgroundDrawable(createButtonBackground(currentColorSet.pressedButton));
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateTheme();
            }
        });


        LinearLayout.LayoutParams toolbarLP = new LinearLayout.LayoutParams(MATCH_PARENT, toolbar_size);
        bgToolbar = new MyColorDrawable(currentColorSet.toolbar, false);
        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundDrawable(bgToolbar);
        toolbar.setLayoutParams(toolbarLP);
        toolbar.addView(title);
        toolbar.addView(imageButton);


        LinearLayout.LayoutParams legendLP = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        TextView legend = new TextView(this);
        legend.setPadding(dimen.dpi(16), dimen.dpi(8), 0, dimen.dpi(8));
        legend.setTextSize(16f);
        legend.setTextColor(currentColorSet.legendTitle);
        legend.setText("Followers");
        legend.setLayoutParams(legendLP);
        MyColorDrawable d1 = new MyColorDrawable(currentColorSet.lightBackground, false);
        ds.add(d1);
        legend.setBackgroundDrawable(d1);


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        chart_ = new ChartViewGL(this, datum.data, dimen, currentColorSet);
        chart_.setLayoutParams(lp);


        ScrollView.LayoutParams listLP = new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(listLP);
        list.addView(legend);
        list.addView(chart_);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(list);

        LinearLayout.LayoutParams frameLP = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1f);
        FrameLayout contentFrame = new FrameLayout(this);
        contentFrame.setLayoutParams(frameLP);
//        contentFrame.addView(scrollView, MATCH_PARENT, dimen.dpi(400));
        contentFrame.addView(scrollView, MATCH_PARENT, MATCH_PARENT);
        View sgadow = new View(this);
        sgadow.setBackgroundDrawable(getResources().getDrawable(R.drawable.header_shadow));
        contentFrame.addView(sgadow, MATCH_PARENT, dimen.dpi(3));

        root = new MyContentRoot(this, currentColorSet.statusbar, currentColorSet.darkBackground);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(toolbar, lp);
        root.addView(contentFrame);

        ColumnData[] data1 = datum.data;
        for (int i = 0, data1Length = data1.length; i < data1Length; i++) {
            final ColumnData c = data1[i];
            if (c.id.equals(ChartData.COLUMN_ID_X)) {
                continue;
            }
            CheckBox cb = new CheckBox(this);
            MyColorDrawable d = new MyColorDrawable(currentColorSet.lightBackground, i == 1);
            ds.add(d);
            cb.setBackgroundDrawable(d);
            cb.setText(c.name);
            cb.setChecked(true);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    chart_.setChecked(c.id, isChecked);
                }
            });
            LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            cblp.topMargin = 1;//todo this is wrong, it is not full width
            cb.setLayoutParams(cblp);
            list.addView(cb);

        }

        if (Build.VERSION.SDK_INT >= 21) {
//            root.isScrollContainer()
//            root.requestApplyInsets();
//            //todo
//            root.setFitsSystemWindows(false);


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
        chart_.animateToColors(currentColorSet);
        h.removeCallbacksAndMessages(null);
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                imageButton.setBackgroundDrawable(createButtonBackground(currentColorSet.pressedButton));
            }
        }, 300);

        if (Build.VERSION.SDK_INT >= 21) {
            title.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActivityManager.TaskDescription d = new ActivityManager.TaskDescription("Statistics", null, currentColorSet.toolbar);
                    setTaskDescription(d);
                }
            }, 300);
        }
        //todo animate titlebar
    }

    private void trace(int delayMillis) {
        if (TRACE) {
            File filesDir = getFilesDir();
            File trace = new File(filesDir, "trace");
            Debug.startMethodTracing(trace.getAbsolutePath(), 1024  * 1024 * 10);
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

    public static final boolean USE_RIPPLE = true;
    public final Drawable createButtonBackground(int pressedColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !USE_RIPPLE) {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
            return stateListDrawable;
        } else {
            ColorDrawable maskDrawable = null;
            return new RippleDrawable(ColorStateList.valueOf(pressedColor), null, maskDrawable);
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

        public void animateColors(int status, int bg){
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
}
