package korniltsev.telegram.charts;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.gl.ChartViewGL;
import korniltsev.telegram.charts.ui.Dimen;

import static android.graphics.PixelFormat.OPAQUE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class MainActivity extends Activity {


    public static final String TAG = "tg.ch";
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean LOGGING = DEBUG;

//    public static final int COLOR_ACTION_BAR_LIGHT = 0xff517DA2;//todo make a theme object
//    public static final int COLOR_ACTION_BAR_DARK = 0xff212D3B;//todo make a theme object
    public static final ColorSet DAY = new ColorSet(0xff517DA2, 0xffF0F0F0,0xffffffff);
    public static final ColorSet NIGHT = new ColorSet(0xff212D3B, 0xff161E27,0xff1D2733);
    private MyColorDrawable bgRoot;
    private ArrayList<MyColorDrawable> ds = new ArrayList<>();

    //todo first animation is SLOW!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    static class ColorSet {
        public final int toolbar;
        public final int darkBackground;
        public final int lightBackground;

        public ColorSet(int toolbar, int darkBackground, int lightBackground) {
            this.toolbar = toolbar;
            this.darkBackground = darkBackground;
            this.lightBackground = lightBackground;
        }
    }

    ColorSet currentColorSet = DAY;


    private Dimen dimen;
    private LinearLayout toolbar;
    private MyColorDrawable bgToolbar;
    private TextView title;
    private ImageView imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dimen = new Dimen(this);
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
        imageButton.setBackgroundDrawable(createButtonBackground(0xff446D91));
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


        ChartData[] data = readData();
        Log.d(TAG, "data len " + data.length);

        ChartData datum = data[4];

        final ChartViewGL chart = new ChartViewGL(this, datum.data, dimen);

        LinearLayout frame = new LinearLayout(this);
        bgRoot = new MyColorDrawable(currentColorSet.darkBackground);
        frame.setBackgroundDrawable(bgRoot);//todo set in theme
        frame.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        chart.setLayoutParams(lp);


        frame.addView(toolbar, lp);
        frame.addView(chart);
        ColumnData[] data1 = datum.data;
        for (int i = 0, data1Length = data1.length; i < data1Length; i++) {
            final ColumnData c = data1[i];
            if (c.id.equals(ChartData.COLUMN_ID_X)) {
                continue;
            }
            CheckBox cb = new CheckBox(this);
            MyColorDrawable d = new MyColorDrawable(Color.WHITE);
            ds.add(d);
            cb.setBackgroundDrawable(d);
            cb.setText(c.name);
            cb.setChecked(true);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    chart.setChecked(c.id, isChecked);
                }
            });
            LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            cblp.topMargin = 1;//todo this is wrong, it is not full width
            cb.setLayoutParams(cblp);
            frame.addView(cb);

        }
        setContentView(frame);

        ColorDrawable d;
//        d.setColor();

    }


    private void animateTheme() {
        if (currentColorSet == DAY) {
            currentColorSet = NIGHT;
        } else {
            currentColorSet = DAY;
        }
        bgToolbar.animate(currentColorSet.toolbar);
        bgRoot.animate(currentColorSet.darkBackground);
        for (MyColorDrawable d : ds) {
            d.animate(currentColorSet.lightBackground);
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

    public static class MyColorDrawable extends Drawable {
        public int color;
        private ValueAnimator anim;

        public MyColorDrawable(int color) {
            this.color = color;
        }

        public void animate(int to) {
            if (anim != null) {
                anim.cancel();
            }
            anim = ValueAnimator.ofInt(color, to);
            anim.setEvaluator(new ArgbEvaluator());
            anim.setDuration(160);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int v = (int) animation.getAnimatedValue();//todo unneded allocations
                    color = v;
                    invalidateSelf();
                }
            });
            anim.start();
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawColor(color);
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return OPAQUE;
        }
    }

    public final Drawable createButtonBackground(int pressedColor) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
            return stateListDrawable;
        } else {
            ColorDrawable maskDrawable = null;
            return new RippleDrawable(ColorStateList.valueOf(pressedColor), null, maskDrawable);
        }
    }


}
