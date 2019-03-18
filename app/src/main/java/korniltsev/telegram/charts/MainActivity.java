package korniltsev.telegram.charts;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import korniltsev.telegram.charts.gl.ChartViewGL;


public class MainActivity extends Activity {


    private Dimen dimen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dimen = new Dimen(this);
        ChartData[] data = readData();
        Log.d(ChartView.TAG, "data len " + data.length);

        long[] vs1 = {50, 100, 50};
        long[] vs2 = {0, 50, 0};
        ChartData tesdata = new ChartData(new ColumnData[]{
                new ColumnData("x", "x", vs1, 100, 50, "line", Color.RED),
                new ColumnData("#y1", "#y1", vs1, 100, 50, "line", Color.RED),
                new ColumnData("#y2", "#y2", vs2, 50, 0, "line", Color.RED),
        });
//        ChartData datum = data[4];
        ChartData datum = tesdata;


        final ChartViewGL chart = new ChartViewGL(this, datum.data, dimen);

        LinearLayout frame = new LinearLayout(this);
        frame.setBackgroundColor(Color.WHITE);//todo set in theme
        frame.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chart.setLayoutParams(lp);
        frame.addView(chart);
        for (final ColumnData c : datum.data) {
            if (c.id.equals(ChartView.COLUMN_ID_X)) {
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


}
