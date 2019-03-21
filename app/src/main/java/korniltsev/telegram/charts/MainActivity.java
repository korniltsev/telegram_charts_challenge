package korniltsev.telegram.charts;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import korniltsev.telegram.charts.ui.MyColorDrawable;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class MainActivity extends Activity {


    public static final String TAG = "tg.ch";
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean TRACE = BuildConfig.DEBUG && false;
    public static final boolean LOGGING = DEBUG;
    public static final int DATASET = 4;

    private MyColorDrawable bgRoot;
    private ArrayList<MyColorDrawable> ds = new ArrayList<>();

    ColorSet currentColorSet = ColorSet.DAY;

//todo first animation is SLOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    private Dimen dimen;
    private LinearLayout toolbar;
    private MyColorDrawable bgToolbar;
    private TextView title;
    private ImageView imageButton;
    private ChartViewGL chart_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ChartData[] data = readData();
        Log.d(TAG, "data len " + data.length);

        ChartData datum = data[DATASET];
        if (DEBUG) {
//            ColumnData vs = datum.data[datum.data.length - 1];
//            vs.minValue = 0;
//            vs.values[vs.values.length - 1] = 0;
        }

        dimen = new Dimen(this);

        bgRoot = new MyColorDrawable(currentColorSet.darkBackground, false);
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


        FrameLayout.LayoutParams listLP = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutParams(listLP);
        list.addView(legend);
        list.addView(chart_);

        LinearLayout.LayoutParams frameLP = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1f);
        FrameLayout contentFrame = new FrameLayout(this);
        contentFrame.setLayoutParams(frameLP);
        contentFrame.addView(list);
        View sgadow = new View(this);
        sgadow.setBackgroundDrawable(getResources().getDrawable(R.drawable.header_shadow));
        contentFrame.addView(sgadow, MATCH_PARENT, dimen.dpi(3));

        LinearLayout root = new LinearLayout(this){
            @Override
            public WindowInsets onApplyWindowInsets(WindowInsets insets) {
                return super.onApplyWindowInsets(insets);
            }
        };
        root.setBackgroundDrawable(bgRoot);
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
//            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
//                @Override
//                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
//                    return insets.consumeSystemWindowInsets();
//                }
//            });

        }
        setContentView(root);

    }


    private void animateTheme() {
//        trace(500);
        if (currentColorSet == ColorSet.DAY) {
            currentColorSet = ColorSet.NIGHT;
        } else {
            currentColorSet = ColorSet.DAY;
        }
        if (Build.VERSION.SDK_INT >= 21) {
            //todo replace with insets and color animatione
            getWindow().setStatusBarColor(currentColorSet.statusbar);
        }
        bgToolbar.animate(currentColorSet.toolbar);
        bgRoot.animate(currentColorSet.darkBackground);
        for (MyColorDrawable d : ds) {
            d.animate(currentColorSet.lightBackground);
        }
        chart_.animateToColors(currentColorSet);
        imageButton.setBackgroundDrawable(createButtonBackground(currentColorSet.pressedButton));
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

    public final Drawable createButtonBackground(int pressedColor) {
        boolean useRipple = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !useRipple) {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
            return stateListDrawable;
        } else {
            ColorDrawable maskDrawable = null;
            return new RippleDrawable(ColorStateList.valueOf(pressedColor), null, maskDrawable);
//            return null;
        }
    }


}
