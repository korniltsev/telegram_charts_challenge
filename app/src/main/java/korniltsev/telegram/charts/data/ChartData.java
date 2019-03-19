package korniltsev.telegram.charts.data;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import korniltsev.telegram.charts.MainActivity;

public class ChartData {

    public static final String COLUMN_ID_X = "x";
    public final ColumnData[] data;

    public ChartData(ColumnData[] data) {
        this.data = data;
    }

    public static ChartData[] parse(JSONArray charts) throws JSONException {
        //todo stream parser
        ChartData[] ret = new ChartData[charts.length()];
        for (int c = 0; c < charts.length(); c++) {
            JSONObject o = charts.getJSONObject(c);
            JSONArray columns = o.getJSONArray("columns");
            ColumnData[] jcolumn = new ColumnData[columns.length()];
            for (int i = 0; i < columns.length(); i++) {
                JSONArray column = columns.getJSONArray(i);
                String id = column.getString(0);
                int jl = column.length();
//                int jl = 6;
                long[] vs = new long[jl - 1];
                long maxValue = Long.MIN_VALUE;
                long minValue = Long.MAX_VALUE;
                for (int j = 1; j < jl; ++j) {
                    long v = column.getLong(j);
                    if (MainActivity.DEBUG && v < 0) {
                        throw new AssertionError("v < 0");
                    }
                    minValue = Math.min(minValue, v);
                    maxValue = Math.max(maxValue, v);
                    vs[j - 1] = v;
                }

                if (MainActivity.DEBUG && minValue > maxValue) {
                    throw new AssertionError("minValue > maxValue");
                }
                String type = o.getJSONObject("types").getString(id);
                String name = o.getJSONObject("names").optString(id, id);
                String strcolor = o.getJSONObject("colors").optString(id, null);
                int color;
                if (strcolor == null) {
                    color = 0;
                } else {
                    color = Color.parseColor(strcolor);
                }

                jcolumn[i] = new ColumnData(id, name, vs, maxValue, minValue, type, color);
            }
            ret[c] = new ChartData(jcolumn);
        }
        return ret;
    }
}