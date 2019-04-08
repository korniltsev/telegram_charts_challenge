package korniltsev.telegram.charts.data;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import korniltsev.telegram.charts.MainActivity;

public class ChartData {

    public static final String COLUMN_ID_X = "x";
    public final ColumnData[] data;
    public final boolean percentage;
    public final boolean stacked;
    public final boolean y_scaled;
    public final ColumnData.Type type;

    public ChartData(ColumnData[] data, boolean percentage, boolean stacked, boolean y_scaled, ColumnData.Type type) {
        this.data = data;
        this.percentage = percentage;
        this.stacked = stacked;
        this.y_scaled = y_scaled;
        this.type = type;
    }

    public static ArrayList<ChartData> parseMany(JSONArray charts) throws JSONException {
        //todo stream parser
        ArrayList<ChartData> ret = new ArrayList<>(charts.length());
        for (int c = 0; c < charts.length(); c++) {
            JSONObject o = charts.getJSONObject(c);
            ChartData e = pareOne(o);
            ret.add(e);
        }
        return ret;
    }

    public static ChartData pareOne(JSONObject o) throws JSONException {
//        chart.names – Name for each variable.
//        chart.percentage – true for percentage based values.
//        chart.stacked – true for values stacking on top of each other.
//        chart.y_scaled – true for charts with 2 Y axes.
        JSONArray columns = o.getJSONArray("columns");
        ColumnData[] jcolumn = new ColumnData[columns.length()];
        ColumnData.Type lastT = null;
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
            String stype = o.getJSONObject("types").getString(id);
            String name = o.getJSONObject("names").optString(id, id);
            String strcolor = o.getJSONObject("colors").optString(id, null);
            int color;
            if (strcolor == null) {
                color = 0;
            } else {
                color = Color.parseColor(strcolor);
            }
            ColumnData.Type t= ColumnData.Type.line;
            for (ColumnData.Type v : ColumnData.Type.values()) {
                if (v.name.equals(stype)) {
                    t = v;
                    break;
                }

            }

            jcolumn[i] = new ColumnData(id, name, vs, t, color);
            lastT = t;
        }
        boolean percentage = o.optBoolean("percentage", false);
        boolean stacked = o.optBoolean("stacked", false);
        boolean y_scaled = o.optBoolean("y_scaled", false);
        return new ChartData(jcolumn, percentage, stacked, y_scaled, lastT );
    }
}
