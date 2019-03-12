package korniltsev.telegram.charts;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ColumnData {
    public final String id;
    public final String name;
    public final long[] values;
    public final String type;
    public final int color;


    public ColumnData(String id, String name, long[] values, String type, int color) {
        this.id = id;
        this.name = name;
        this.values = values;
        this.type = type;
        this.color = color;
    }


}
