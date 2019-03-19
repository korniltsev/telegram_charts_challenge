package korniltsev.telegram.charts;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
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

import korniltsev.telegram.charts.data.ChartData;
import korniltsev.telegram.charts.data.ColumnData;
import korniltsev.telegram.charts.gl.ChartViewGL;

import static android.graphics.PixelFormat.OPAQUE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


public class MainActivity extends Activity {


    public static final String TAG = "tg.ch";
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean LOGGING = DEBUG;

    public static final int COLOR_ACTION_BAR_LIGHT = 0xff517DA2;//todo make a theme object


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


        LinearLayout.LayoutParams toolbarLP = new LinearLayout.LayoutParams(MATCH_PARENT, toolbar_size);
        bgToolbar = new MyColorDrawable(COLOR_ACTION_BAR_LIGHT);
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
        frame.setBackgroundDrawable(new MyColorDrawable(Color.WHITE));//todo set in theme
        frame.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        chart.setLayoutParams(lp);


        frame.addView(toolbar, lp);
        frame.addView(chart);
        for (final ColumnData c : datum.data) {
            if (c.id.equals(ChartData.COLUMN_ID_X)) {
                continue;
            }
            CheckBox cb = new CheckBox(this);
            cb.setText(c.name);
            cb.setChecked(true);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    chart.setChecked(c.id, isChecked);
                }
            });
            frame.addView(cb);

        }
        setContentView(frame);

        ColorDrawable d;
//        d.setColor();

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

    public class MyColorDrawable extends Drawable {
        int color;

        public MyColorDrawable(int color) {
            this.color = color;
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


}
