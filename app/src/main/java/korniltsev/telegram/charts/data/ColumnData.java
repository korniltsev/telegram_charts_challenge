package korniltsev.telegram.charts.data;

public class ColumnData {
    public final String id;
    public final String name;
    public final long[] values;
    public final long max;
    public final long min;
    public final String type;
    public final int color;


    public ColumnData(String id, String name, long[] values, String type, int color) {
        this.id = id;
        this.name = name;
        this.values = values;
        long min = values[0];
        long max = values[0];
        for (long value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        this.max = max;
        this.min = min;
        this.type = type;
        this.color = color;
    }


}
